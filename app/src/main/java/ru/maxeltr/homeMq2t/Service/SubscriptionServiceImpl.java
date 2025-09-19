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
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.util.concurrent.Promise;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import ru.maxeltr.homeMq2t.Config.AppProperties;
import ru.maxeltr.homeMq2t.Config.CardPropertiesProvider;
import ru.maxeltr.homeMq2t.Config.CommandPropertiesProvider;
import ru.maxeltr.homeMq2t.Config.ComponentPropertiesProvider;
import ru.maxeltr.homeMq2t.Entity.CardEntity;
import ru.maxeltr.homeMq2t.Entity.HasSubscription;

public class SubscriptionServiceImpl implements SubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionServiceImpl.class);

    @Autowired
    @Lazy               //TODO
    private ServiceMediator mediator;

//    private final ConcurrentMap<String, AtomicInteger> counts = new ConcurrentHashMap<>();
//    private final ConcurrentMap<String, Integer> maxQos = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, SubscriptionState> states = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, List<HasSubscription>> subscribers = new ConcurrentHashMap<>();

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
        List<? extends HasSubscription> subs = cardPropertiesProvider.getAllSubscriptions();
        logger.debug("List of subscriptions {}", subs);
        //var clearSusbs = MqttUtils.removeCopiesAndSelectMaxQos(subs);
        //logger.debug("List of subscription after removing copies {}", clearSusbs);
        states.clear();
        subscribe(subs);
    }

    @Override
    public void subscribe(List<? extends HasSubscription> subscriptions) {
        if (subscriptions == null || subscriptions.isEmpty()) {
            logger.debug("Empty subscription list given");
            return;
        }
        logger.debug("Prepare list to subscribe to topics {}", subscriptions);

        List<String> toSubscribe = new ArrayList<>();

        for (var entity : subscriptions) {
            String topic = entity.getSubscriptionTopic();
            String qos = entity.getSubscriptionQos();

            subscribers.compute(topic, (k, v) -> {
                var subs = v;
                if (subs == null) {
                    subs = new ArrayList<>();
                    subs.add(entity);
                } else {
                    if (!subs.contains(entity)) {
                        subs.add(entity);
                    }
                }
                return subs;
            });
        }

    }

//    @Override
//    public void subscribe(List<MqttTopicSubscription> subscriptions) {
//        if (subscriptions == null || subscriptions.isEmpty()) {
//            logger.debug("Empty subscription list given");
//            return;
//        }
//
//        logger.debug("Prepare list to subscribe to topics {}", subscriptions);
//
//        List<String> toSubscribe = new ArrayList<>();
//
//        for (var sub : subscriptions) {
//            String topic = sub.topicName();
//            int qos = sub.qualityOfService().value();
//            boolean shouldSubscribe = states.compute(topic, (k, s) -> {
//                SubscriptionState oldState = s;
//                if (oldState == null) {
//                    oldState = new SubscriptionState();
//                }
//                oldState.updateMaxQos(qos);
//                oldState.increment();
//                return oldState;
//            }).getRefCount() == 1;
//
//            if (shouldSubscribe) {
//                toSubscribe.add(topic);
//            }
//        }
//
//        if (!toSubscribe.isEmpty()) {
//            List<MqttTopicSubscription> prepared = toSubscribe.stream().map(t -> new MqttTopicSubscription(t, MqttQoS.valueOf(states.get(t).getMaxQos()))).collect(Collectors.toList());
//            logger.debug("Prepared list of subscriptions {}", prepared);
//            mediator.subscribe(prepared);
//        } else {
//            logger.info("List to subscribe is empty.");
//        }
//    }

    @Override
    public Promise<MqttUnsubAckMessage> unsubscribe(List<String> topics) {
        if (topics == null || topics.isEmpty()) {
            logger.debug("No topics to unsubscribe");
            return null;
        }

        List<String> toUnsubscribe = new ArrayList<>(topics.size());

        for (String topic : topics) {
            if (StringUtils.isBlank(topic)) {
                continue;
            }

            AtomicBoolean removed = new AtomicBoolean(false);
            states.computeIfPresent(topic, (k, state) -> {
                int count = state.decrement();
                logger.debug("Decremented refcount for {} -> {}", topic, count);
                if (count <= 0) {
                    removed.set(true);
                    return null;
                }
                return state;
            });

            if (removed.get()) {
                toUnsubscribe.add(topic);
            }
        }

        if (toUnsubscribe.isEmpty()) {
            logger.debug("No subscriptions reached zero refcount; nothing to insubscribe");
            return null;
        }

        logger.debug("Unsubscribing from topics: {}", toUnsubscribe);
        return mediator.unsubscribe(toUnsubscribe);
    }

    private static class SubscriptionState {

        final AtomicInteger refCount = new AtomicInteger(0);
        final AtomicInteger maxQos = new AtomicInteger(0);

        int increment() {
            return refCount.incrementAndGet();
        }

        int decrement() {
            return refCount.decrementAndGet();
        }

        int getRefCount() {
            return refCount.get();
        }

        void updateMaxQos(int qos) {
            maxQos.updateAndGet(prev -> Math.max(prev, qos));
        }

        int getMaxQos() {
            return maxQos.get();
        }
    }
}
