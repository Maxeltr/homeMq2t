package ru.maxeltr.homeMq2t.Mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.util.concurrent.Promise;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import java.util.List;
import ru.maxeltr.homeMq2t.Service.ServiceMediator;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public interface HmMq2t {

    Promise<MqttConnAckMessage> connect();

    void reconnect();

    void disconnect(byte reasonCode);

    Promise<MqttSubAckMessage> subscribe(List<MqttTopicSubscription> subscriptions);

    Promise<MqttUnsubAckMessage> unsubscribe(List<String> topics);

    void publish(String topic, ByteBuf payload, MqttQoS qos, boolean retain);

    /**
     * Ретранслирует сохранённое pending-сообщение PUBLISH с уже выделенным
     * packetId (после восстановления соединения). Отправляется с dup=true,
     * БД не изменяется — подтверждения обрабатываются как при обычной отправке.
     *
     * @param topic тема сообщения
     * @param payload полезная нагрузка сообщения
     * @param qos уровень QoS (AT_LEAST_ONCE или EXACTLY_ONCE)
     * @param retain retain-флаг сообщения
     * @param packetId сохранённый идентификатор пакета
     */
    void retransmitPublish(String topic, byte[] payload, MqttQoS qos, boolean retain, int packetId);

    /**
     * Ретранслирует PUBREL для сообщения с QoS=2, находящегося в статусе
     * SEND_PUBREL (после восстановления соединения), с сохранённым packetId.
     *
     * @param packetId сохранённый идентификатор пакета
     */
    void retransmitPubRel(int packetId);

    void setMediator(ServiceMediator serviceMediator);

    boolean isConnected();
}
