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
package ru.maxeltr.homeMq2t.Service;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.util.concurrent.Promise;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import ru.maxeltr.homeMq2t.Config.AppProperties;
import ru.maxeltr.homeMq2t.Config.CardPropertiesProvider;
import ru.maxeltr.homeMq2t.Config.CommandPropertiesProvider;
import ru.maxeltr.homeMq2t.Config.ComponentPropertiesProvider;
import ru.maxeltr.homeMq2t.Entity.CardEntity;
import ru.maxeltr.homeMq2t.Entity.HasSubscription;
import ru.maxeltr.homeMq2t.Model.Status;
import ru.maxeltr.homeMq2t.Mqtt.MqttUtils;

public class SubscriptionServiceImpl implements SubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionServiceImpl.class);

    @Value("${connect-timeout:5000}")
    private Integer connectTimeout;

    @Autowired
    @Lazy               //TODO
    private ServiceMediator mediator;

    private final ConcurrentMap<String, Subscription> subscribers = new ConcurrentHashMap<>();

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private CardPropertiesProvider cardPropertiesProvider;

    @Autowired
    private CommandPropertiesProvider commandPropertiesProvider;

    @Autowired
    private ComponentPropertiesProvider componentPropertiesProvider;

    @Override
    public void clearSubscriptionsAndSubscribeFromConfig() {
        subscribe(Stream.of(
                cardPropertiesProvider.getAllSubscriptions(),
                commandPropertiesProvider.getAllSubscriptions(),
                componentPropertiesProvider.getAllSubscriptions())
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(ArrayList::new))
        );
    }

    @Override
    public void subscribe(List<HasSubscription> subscriptions) {
        synchronized (this) {
            if (subscriptions == null || subscriptions.isEmpty()) {
                logger.debug("Empty subscription list given");
                return;
            }
            logger.debug("Prepare list to subscribe to topics {}", subscriptions);

            //Use ArrayList to preserve insertion order
            List<String> toSubscribe = new ArrayList<>();

            for (var entity : subscriptions) {
                String topic = entity.getSubscriptionTopic();
                subscribers.compute(topic, (k, v) -> {
                    var sub = v;
                    if (sub == null) {
                        toSubscribe.add(topic);
                        sub = new Subscription();
                        sub.setTopic(topic);
                        sub.addSubscriberAndUpdateQos(entity);
                    } else {
                        if (!sub.hasSubscriber(entity)) {
                            sub.addSubscriberAndUpdateQos(entity);
                        }
                    }
                    return sub;
                });
            }

            if (toSubscribe.isEmpty()) {
                logger.info("List to subscribe is empty.");
            } else {
                List<MqttTopicSubscription> prepared = toSubscribe.stream().map(t -> new MqttTopicSubscription(t, MqttQoS.valueOf(subscribers.get(t).getMaxQos()))).collect(Collectors.toList());
                logger.debug("Prepared list of subscriptions {}", prepared);
                Promise<MqttSubAckMessage> promise = mediator.subscribe(prepared);
                promise.awaitUninterruptibly(this.connectTimeout);
                if (promise.isSuccess()) {
                    MqttSubAckMessage ack = promise.getNow();
                    logger.info("SUBACK received id={}.", ack.variableHeader().messageId());
                    List<Integer> granted = ack.payload().grantedQoSLevels();
                    for (int i = 0; i < granted.size(); i++) {
                        int grantedQos = granted.get(i);
                        String grantedTopic = prepared.get(i).topicName();
                        if (grantedQos == MqttUtils.MQTT_SUBACK_FAILURE) {
                            logger.warn("SUBACK rejected. Topic={}.", grantedTopic);
                            subscribers.get(grantedTopic).setStatus(Status.FAIL);
                        } else {
                            logger.info("SUBACK accepted. Topic={}. QoS={}", grantedTopic, grantedQos);
                            subscribers.get(grantedTopic).setStatus(Status.OK);
                        }
                    }
                } else {
                    logger.warn("SUBSCRIBE failed. {}", promise.cause());
                    subscribers.values().stream().forEach(s -> s.setStatus(Status.FAIL));
                }
            }
        }
    }

    @Override
    public Promise<MqttUnsubAckMessage> unsubscribe(List<String> topics) {
        throw new UnsupportedOperationException("Not supported yet.");
    }



    private static class Subscription {

        private String topic = "";
        private final List<HasSubscription> subscribers = new ArrayList<>();
        private final AtomicInteger maxQos = new AtomicInteger(0);
        private Status status = Status.UNKNOWN;

        String getTopic() {
            return topic;
        }

        void setTopic(String topic) {
            this.topic = topic;
        }

        List<HasSubscription> getSubscribers() {
            return subscribers;
        }

        Integer addSubscriberAndUpdateQos(HasSubscription entity) {
            subscribers.add(entity);
            return maxQos.updateAndGet(prev -> Math.max(prev, MqttUtils.convertToMqttQos(entity.getSubscriptionQos()).value()));
        }

        Integer removeSubscriberAndUpdateQos(HasSubscription entity) {
            subscribers.remove(entity);
            Integer newQos = subscribers
                    .stream()
                    .max(Comparator.comparing(e -> MqttUtils.convertToMqttQos(e.getSubscriptionQos()).value()))
                    .map(e -> MqttUtils.convertToMqttQos(e.getSubscriptionQos()).value())
                    .orElse(0);
            maxQos.set(newQos);

            return newQos;
        }

        boolean hasSubscriber(HasSubscription entity) {
            return subscribers.contains(entity);
        }

        int getMaxQos() {
            return maxQos.get();
        }

        Status getStatus() {
            return status;
        }

        void setStatus(Status status) {
            this.status = status;
        }
    }

}
