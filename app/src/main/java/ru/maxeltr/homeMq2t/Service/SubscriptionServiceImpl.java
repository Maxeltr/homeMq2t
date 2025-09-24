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
import java.util.Objects;
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

    private final ConcurrentMap<String, Subscription> subscriptions = new ConcurrentHashMap<>();

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private CardPropertiesProvider cardPropertiesProvider;

    @Autowired
    private CommandPropertiesProvider commandPropertiesProvider;

    @Autowired
    private ComponentPropertiesProvider componentPropertiesProvider;

    @Override
    public void subscribeFromConfig() {
        subscribe(Stream.of(
                cardPropertiesProvider.getAllSubscriptions(),
                commandPropertiesProvider.getAllSubscriptions(),
                componentPropertiesProvider.getAllSubscriptions())
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(ArrayList::new))
        );
    }

    @Override
    public void subscribe(List<HasSubscription> entities) {
        if (entities == null || entities.isEmpty()) {
            logger.debug("Empty entity list for subscription was given.");
            return;
        }

        //Use ArrayList to preserve insertion order
        List<String> toSubscribe = new ArrayList<>();
        List<String> toResubscribe = new ArrayList<>();

        for (var entity : entities) {
            if (entity == null) {
                logger.debug("Entity is null.");
                continue;
            }

            String topic = entity.getSubscriptionTopic();
            if (StringUtils.isBlank(topic)) {
                logger.debug("Subscription topic of entity is empty. {}", entity);
                continue;
            }

            subscriptions.compute(topic, (k, v) -> {
                Subscription sub = v;
                if (sub == null) {
                    toSubscribe.add(topic);
                    sub = new Subscription(topic);
                    sub.addSubscriberAndUpdateQos(entity);
                } else {
                    boolean qosChanged = sub.addSubscriberAndUpdateQos(entity);
                    if (qosChanged) {
                        toResubscribe.add(topic);
                    }
                }
                return sub;
            });
        }

        if (toSubscribe.isEmpty()) {
            logger.debug("List to subscribe is empty.");
        } else {
            subscribeAndUpdateStatusOfSubscriptions(toSubscribe);
        }

        if (toResubscribe.isEmpty()) {
            logger.debug("List to resubscribe is empty.");
        } else {
            unsubscribeAndRemoveSubscriptions(toResubscribe);
            subscribeAndUpdateStatusOfSubscriptions(toResubscribe);
        }

//        if (toSubscribe.isEmpty()) {
//            logger.info("List to subscribe is empty.");
//        } else {
//            List<MqttTopicSubscription> prepared = toSubscribe.stream().map(t -> new MqttTopicSubscription(t, MqttQoS.valueOf(subscriptions.get(t).getMaxQos()))).collect(Collectors.toList());
//            logger.debug("Prepared list of subscriptions {}", prepared);
//            Promise<MqttSubAckMessage> promise = mediator.subscribe(prepared);
//            promise.awaitUninterruptibly(this.connectTimeout);
//            if (promise.isSuccess()) {
//                MqttSubAckMessage ack = promise.getNow();
//                logger.info("SUBACK received id={}.", ack.variableHeader().messageId());
//                List<Integer> granted = ack.payload().grantedQoSLevels();
//                for (int i = 0; i < granted.size(); i++) {
//                    int grantedQos = granted.get(i);
//                    String grantedTopic = prepared.get(i).topicName();
//                    if (grantedQos == MqttUtils.MQTT_SUBACK_FAILURE) {
//                        logger.warn("SUBACK rejected. Topic={}.", grantedTopic);
//                        subscriptions.get(grantedTopic).setStatus(Status.FAIL);
//                    } else {
//                        logger.info("SUBACK accepted. Topic={}. QoS={}", grantedTopic, grantedQos);
//                        subscriptions.get(grantedTopic).setStatus(Status.OK);
//                    }
//                }
//            } else {
//                logger.warn("SUBSCRIBE failed. {}", promise.cause());
//                subscriptions.values().stream().forEach(s -> s.setStatus(Status.FAIL));
//            }
//        }
    }

    private void subscribeAndUpdateStatusOfSubscriptions(List<String> toSubscribe) {
        List<MqttTopicSubscription> prepared = toSubscribe.stream()
                .map(t -> new MqttTopicSubscription(t, MqttQoS.valueOf(subscriptions.get(t).getMaxQos())))
                .collect(Collectors.toCollection(ArrayList::new));
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
                Subscription sub = subscriptions.get(grantedTopic);
                if (sub == null) {
                    logger.warn("Received SUBACK for topic {} which is not present in subscriptions. Message id={}", grantedTopic, ack.variableHeader().messageId());
                    continue;
                }
                if (grantedQos == MqttUtils.MQTT_SUBACK_FAILURE) {
                    logger.warn("SUBACK rejected. Topic={}.", grantedTopic);
                    sub.setStatus(Status.FAIL);
                } else {
                    logger.info("SUBACK accepted. Topic={}. QoS={}", grantedTopic, grantedQos);
                    sub.setStatus(Status.OK);
                }
            }
        } else {
            logger.warn("SUBSCRIBE failed. {}", promise.cause());
            toSubscribe.forEach(t -> {
                Subscription s = subscriptions.get(t);
                if (s != null) {
                    s.setStatus(Status.FAIL);
                }
            });
        }
    }

    @Override
    public void unsubscribe(List<HasSubscription> entities) {
        if (entities == null || entities.isEmpty()) {
            logger.debug("Empty entity list for unsubscribe was given.");
            return;
        }

        List<String> toUnsubscribe = new ArrayList<>();
        List<String> toResubscribe = new ArrayList<>();

        for (var entity : entities) {
            if (entity == null) {
                logger.debug("Entity is null.");
                continue;
            }

            String topic = entity.getSubscriptionTopic();
            if (StringUtils.isBlank(topic)) {
                logger.debug("Subscription topic of entity is empty. Skipping. {}", entity);
                continue;
            }

            subscriptions.computeIfPresent(topic, (k, v) -> {
                Subscription sub = v;
                boolean qosChanged = sub.removeSubscriberAndUpdateQos(entity);

                if (!sub.hasSubscribers()) {
                    toUnsubscribe.add(topic);
                    return null;
                } else {
                    if (qosChanged) {
                        toResubscribe.add(topic);
                    }
                    return sub;
                }
            });
        }

        if (toUnsubscribe.isEmpty()) {
            logger.debug("List to unsubscribe is empty.");
        } else {
            unsubscribeAndRemoveSubscriptions(toUnsubscribe);
        }

        if (toResubscribe.isEmpty()) {
            logger.debug("List to resubscribe is empty.");
        } else {
            unsubscribeAndRemoveSubscriptions(toResubscribe);
            subscribeAndUpdateStatusOfSubscriptions(toResubscribe);
        }
    }

    private void unsubscribeAndRemoveSubscriptions(List<String> prepared) {
        logger.debug("Prepared list of unsubscriptions {}", prepared);
        prepared.stream().forEach(t -> {
            Subscription s = subscriptions.get(t);
            if (s != null) {
                s.setStatus(Status.UNKNOWN);
            }
        });
        Promise<MqttUnsubAckMessage> promise = mediator.unsubscribe(prepared);
        promise.awaitUninterruptibly(this.connectTimeout);
        if (promise.isSuccess()) {
            MqttUnsubAckMessage ack = promise.getNow();
            logger.info("UNSUBACK id={} received for {} topics.", ack.variableHeader().messageId(), prepared.size());
            prepared.forEach(subscriptions::remove);
        } else {
            logger.warn("UNSUBSCRIBE failed. {}", promise.cause());
        }
    }

    private static class Subscription {

        private final String topic;
        private final List<HasSubscription> subscribers = new ArrayList<>();
        private final AtomicInteger maxQos = new AtomicInteger(0);
        private Status status = Status.UNKNOWN;

        Subscription(String topic) {
            this.topic = Objects.requireNonNull(topic);
        }

        String getTopic() {
            return topic;
        }

        List<HasSubscription> getSubscribers() {
            return new ArrayList<>(subscribers);
        }

        boolean hasSubscribers() {
            return !subscribers.isEmpty();
        }

        boolean addSubscriberAndUpdateQos(HasSubscription entity) {
            boolean qosChanged = false;
            if (entity == null) {
                logger.debug("Entity is null.");
                return qosChanged;
            }

            if (!this.topic.equals(entity.getSubscriptionTopic())) {
                throw new IllegalArgumentException("Subscription topic is not equal entity topic");
            }

            int prevQos = getMaxQos();
            int newQos = MqttUtils.convertToMqttQos(entity.getSubscriptionQos()).value();

            if (prevQos < newQos) {
                maxQos.set(newQos);
                logger.debug("Topic {}. Qoschanged from {} to {}", topic, prevQos, newQos);
                qosChanged = true;
            }

            if (!hasSubscriber(entity)) {
                subscribers.add(entity);
            }

            logger.debug("Add subscriber {} to topic {}. Total susbcribers={}", entity, topic, subscribers.size());

            return qosChanged;
        }

        boolean removeSubscriberAndUpdateQos(HasSubscription entity) {
            var removed = subscribers.remove(entity);
            if (!removed) {
                return false;
            }
            logger.debug("Remove subscriber {} from topic {}. Total susbcribers={}", entity, topic, subscribers.size());

            var prevQos = getMaxQos();
            int newQos = subscribers
                    .stream()
                    .mapToInt(e -> MqttUtils.convertToMqttQos(e.getSubscriptionQos()).value())
                    .max()
                    .orElse(0);

            if (prevQos != newQos) {
                maxQos.set(newQos);
                logger.debug("Topic {}. Qoschanged from {} to {}", topic, prevQos, newQos);
                return true;
            }
            return false;
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
