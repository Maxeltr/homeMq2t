/*
 * The MIT License
 *
 * Copyright 2023 Maxim Eltratov <<Maxim.Eltratov@ya.ru>>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ru.maxeltr.homeMq2t.Mqtt;

import io.netty.handler.codec.mqtt.MqttQoS;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import ru.maxeltr.homeMq2t.Config.AppProperties;
import ru.maxeltr.homeMq2t.Entity.MqttMessageEntity;
import ru.maxeltr.homeMq2t.Repository.MqttMessageRepository;

/**
 * Реализация ретрансляции pending-сообщений из БД после восстановления
 * подключения к брокеру.
 *
 * Потокобезопасность: публичный метод выполняет всю работу асинхронно
 * в processExecutor, поэтому может безопасно вызываться из event loop Netty
 * (например, из {@link MqttConnectHandler#handleConnackMessage}).
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class MqttRetransmitServiceImpl implements MqttRetransmitService {

    private static final Logger logger = LoggerFactory.getLogger(MqttRetransmitServiceImpl.class);

    @Autowired
    private MqttMessageRepository messageRepository;

    @Autowired
    @Qualifier("processExecutor")
    private TaskExecutor processExecutor;

    @Autowired
    private HmMq2t hmMq2t;

    @Autowired
    private AppProperties appProperties;

    /**
     * Защита от параллельных прогонов: периодический таймер и триггер
     * на успешный CONNACK могут совпасть. Пока идёт ретрансляция,
     * новые запуски пропускаются.
     */
    private final AtomicBoolean retransmitting = new AtomicBoolean(false);

    /**
     * Периодическая ретрансляция pending-сообщений из БД. Интервал задаётся
     * свойством retransmit-delay (миллисекунды, по умолчанию 60000).
     * Работает и во время активной сессии: если у сообщения истекли таймауты
     * ожидания подтверждения (PUBACK/PUBREC/PUBCOMP) и promise завершился
     * с ошибкой, строка остаётся в БД и будет отправлена повторно здесь.
     */
    @Scheduled(fixedDelayString = "${retransmit-delay:60000}", initialDelayString = "${retransmit-delay:60000}")
    public void retransmitPendingMessagesPeriodically() {
        this.retransmitPendingMessages();
    }

    @Override
    public void retransmitPendingMessages() {
        if (!this.retransmitting.compareAndSet(false, true)) {
            logger.debug("Retransmission is already in progress. Skipping this run.");
            return;
        }

        try {
            CompletableFuture.runAsync(this::doRetransmit, this.processExecutor)
                    .whenComplete((v, ex) -> {
                        this.retransmitting.set(false);
                        if (ex != null) {
                            logger.error("Failed to retransmit pending messages from DB.", ex);
                        }
                    });
        } catch (Exception ex) {
            this.retransmitting.set(false);
            throw ex;
        }
    }

    private void doRetransmit() {
        if (!this.hmMq2t.isConnected()) {
            logger.warn("Retransmission skipped: client is not connected.");
            return;
        }

        if (this.appProperties.getCleanSession()) {
            // При cleanSession=true брокер сбрасывает состояние сессии при обрыве,
            // поэтому ретранслировать нечего. Зависшие строки должны быть удалены
            // при disconnect(); на случай гонки (быстрый реконнект до завершения
            // асинхронного удаления) очищаем их здесь повторно.
            logger.info("CleanSession=true. Deleting stale pending messages instead of retransmission.");
            this.messageRepository.deleteAll();
            return;
        }

        int publishCount = this.retransmitPendingPublishes();
        int pubRelCount = this.retransmitPendingPubRels();

        logger.info("Retransmission finished. PUBLISH retransmitted={}, PUBREL retransmitted={}.",
                publishCount, pubRelCount);
    }

    /**
     * Ретранслирует сообщения со статусом SEND_PUBLISH: повторная отправка
     * PUBLISH (dup=true) с сохранённым packetId, topic, qos и retain.
     * Строка в БД остаётся до получения подтверждения (PUBACK/PUBREC/PUBCOMP),
     * где она удаляется или обновляется существующей логикой HmMq2tImpl.
     *
     * @return число ретранслированных сообщений
     */
    private int retransmitPendingPublishes() {
        List<MqttMessageEntity> pending;
        try {
            pending = this.messageRepository.findByStatus(MqttMessageEntity.STATUS_SEND_PUBLISH);
        } catch (Exception ex) {
            logger.error("Failed to read pending PUBLISH messages (status={}) from DB.",
                    MqttMessageEntity.STATUS_SEND_PUBLISH, ex);
            return 0;
        }

        int count = 0;
        for (MqttMessageEntity entity : pending) {
            try {
                logger.info("Retransmitting PUBLISH message id={}, t={}, q={}, r={}, dup=true.",
                        entity.getPacketId(), entity.getTopic(), entity.getQos(), entity.isRetain());
                this.hmMq2t.retransmitPublish(entity.getTopic(), entity.getPayload(),
                        MqttQoS.valueOf(entity.getQos()), entity.isRetain(), entity.getPacketId());
                count++;
            } catch (Exception ex) {
                logger.error("Failed to retransmit PUBLISH message id={} from DB.",
                        entity.getPacketId(), ex);
            }
        }
        return count;
    }

    /**
     * Ретранслирует сообщения со статусом SEND_PUBREL: повторная отправка PUBREL
     * с сохранённым packetId (payload для PUBREL не требуется).
     *
     * @return число ретранслированных сообщений
     */
    private int retransmitPendingPubRels() {
        List<MqttMessageEntity> pending;
        try {
            pending = this.messageRepository.findByStatus(MqttMessageEntity.STATUS_SEND_PUBREL);
        } catch (Exception ex) {
            logger.error("Failed to read pending PUBREL messages (status={}) from DB.",
                    MqttMessageEntity.STATUS_SEND_PUBREL, ex);
            return 0;
        }

        int count = 0;
        for (MqttMessageEntity entity : pending) {
            try {
                logger.info("Retransmitting PUBREL message id={}.", entity.getPacketId());
                this.hmMq2t.retransmitPubRel(entity.getPacketId());
                count++;
            } catch (Exception ex) {
                logger.error("Failed to retransmit PUBREL message id={} from DB.",
                        entity.getPacketId(), ex);
            }
        }
        return count;
    }
}
