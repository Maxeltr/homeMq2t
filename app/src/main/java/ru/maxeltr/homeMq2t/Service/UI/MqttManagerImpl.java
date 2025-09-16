/*
 * The MIT License
 *
 * Copyright 2025 Maxim Eltratov <<Maxim.Eltratov@ya.ru>>.
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
package ru.maxeltr.homeMq2t.Service.UI;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.util.concurrent.Promise;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import ru.maxeltr.homeMq2t.Config.CardPropertiesProvider;
import ru.maxeltr.homeMq2t.Entity.BaseEntity;
import ru.maxeltr.homeMq2t.Model.Msg;
import ru.maxeltr.homeMq2t.Mqtt.MqttUtils;
import ru.maxeltr.homeMq2t.Service.ServiceMediator;
import ru.maxeltr.homeMq2t.Service.SubscriptionService;

public class MqttManagerImpl implements MqttManager {

    private static final Logger logger = LoggerFactory.getLogger(MqttManagerImpl.class);

    private final static long ACK_TIMEOUT_MILLIS = 5000L;

    //@Autowired
    //@Lazy               //TODO
    private ServiceMediator mediator;

    @Autowired
    @Qualifier("getCardPropertiesProvider")
    private CardPropertiesProvider appProperties;

    @Autowired
    private SubscriptionService subscriptionService;

    @Override
    public void setMediator(ServiceMediator mediator) {
        this.mediator = mediator;
    }

    @Override
    public void publish(Msg msg) {
        String topic = this.appProperties.getCardPubTopic(msg.getId());
        if (StringUtils.isEmpty(topic)) {
            logger.info("Could not publish. There is no topic for card={}", msg.getId());
            return;
        }

        MqttQoS qos = MqttUtils.convertToMqttQos(this.appProperties.getCardPubQos(msg.getId()));

        boolean retain = Boolean.parseBoolean(this.appProperties.getCardPubRetain(msg.getId()));

        String type = this.appProperties.getCardPubDataType(msg.getId());
        if (StringUtils.isEmpty(type)) {
            type = MediaType.TEXT_PLAIN_VALUE;
            logger.info("Type is empty for card={}. Set {}.", msg.getId(), type);
        }

        logger.info("Creating publish message for card={}, topic={}, QoS={}, retain={}.", msg.getId(), topic, qos, retain);

        var message = msg.toBuilder()
                .type(type)
                .data(this.appProperties.getCardPubData(msg.getId()))
                .timestamp(String.valueOf(Instant.now().toEpochMilli()));

        this.mediator.publish(message.build(), topic, qos, retain);
    }

    @Override
    public <T extends BaseEntity & HasSubscription> void updateSubscription(T before, T after) {
        String oldTopic;
        oldTopic = before != null ? before.getSubscriptionTopic() : "";
        String newTopic = after != null ? after.getSubscriptionTopic() : "";
        String oldQos = before != null ? before.getSubscriptionQos() : "";
        String newQos = after != null ? after.getSubscriptionQos() : "";

        boolean topicChanged = !equalsNullSafe(oldTopic, newTopic);
        boolean qosChanged = !equalsNullSafe(oldQos, newQos);

        if (!topicChanged && !qosChanged) {
            return;
        }

        if (StringUtils.isNotEmpty(oldTopic) && this.mediator.isConnected() && (topicChanged || qosChanged)) {
            Promise<MqttUnsubAckMessage> promise = this.subscriptionService.unsubscribe(List.of(oldTopic));
            if (promise != null) {
                promise.awaitUninterruptibly(ACK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            }
        }

        if (StringUtils.isNotBlank(newTopic) && mediator.isConnected()) {
            this.subscriptionService.subscribe(List.of(new MqttTopicSubscription(newTopic, MqttUtils.convertToMqttQos(newQos))));
        }
    }

    private boolean equalsNullSafe(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }
}
