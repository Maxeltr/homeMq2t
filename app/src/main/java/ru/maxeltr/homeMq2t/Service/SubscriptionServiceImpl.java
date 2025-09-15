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
import ru.maxeltr.homeMq2t.Mqtt.MqttUtils;

public class SubscriptionServiceImpl implements SubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionServiceImpl.class);

    @Autowired
    @Lazy               //TODO
    private ServiceMediator mediator;

//    private final ConcurrentMap<String, AtomicInteger> counts = new ConcurrentHashMap<>();
//    private final ConcurrentMap<String, Integer> maxQos = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, SubscriptionState> states = new ConcurrentHashMap<>();

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
        List<MqttTopicSubscription> subs = appProperties.getAllSubscriptions();
        logger.debug("List of subscriptions {}", subs);
        //var clearSusbs = MqttUtils.removeCopiesAndSelectMaxQos(subs);
        //logger.debug("List of subscription after removing copies {}", clearSusbs);
        subscribe(subs);
    }

    @Override
    public boolean subscribe(MqttTopicSubscription subscription) {
        if (subscription == null) {
            return false;
        }

        logger.debug("Start to subscribe to topic {}", subscription.topicName());
        String topic = subscription.topicName();

        int qos = subscription.qualityOfService().value();

        final boolean shouldSubscribe = states.compute(topic, (k, s) -> {
            SubscriptionState oldState = s;
            if (oldState == null) {
                oldState = new SubscriptionState();
            }
            oldState.updateMaxQos(qos);
            int refcount = oldState.increment();
            logger.debug("Incremented refcount for {} -> {}", topic, refcount);
            return oldState;
        }).getRefCount() == 1;

        if (shouldSubscribe) {
            int effectiveQos = states.get(topic).getMaxQos();
            logger.debug("Refcount=1. Subscribing to {} with qos {}", topic, effectiveQos);
            mediator.subscribe(List.of(new MqttTopicSubscription(topic, MqttQoS.valueOf(effectiveQos))));
            return true;
        }

        return false;

//        AtomicInteger count = counts.computeIfAbsent(topic, k -> new AtomicInteger(0));
//
//        int v = count.incrementAndGet();
//        logger.debug("Incremented refcount for {} -> {}", topic, v);
//        if (v == 1) {
//            logger.debug("Refcount={}. Subscribing to {}", v, topic);
//            mediator.subscribe(List.of(subscription));
//            return true;
//        }
//        return false;
    }

    @Override
    public void subscribe(List<MqttTopicSubscription> subscriptions) {
        if (subscriptions == null || subscriptions.isEmpty()) {
            logger.debug("Empty subscription list given");
            return;
        }

        logger.debug("Start to subscribe to topics {}", subscriptions);
        ConcurrentMap<String, Integer> groupedMaxQos = subscriptions.stream()
                .collect(Collectors.toConcurrentMap(
                        MqttTopicSubscription::topicName, s -> s.qualityOfService().value(), Integer::max));
        List<String> toSubscribe = new ArrayList<>();

        for (var entry : groupedMaxQos.entrySet()) {
            String topic = entry.getKey();
            int qos = entry.getValue();
            boolean shouldSubscribe = states.compute(topic, (k, s) -> {
                SubscriptionState oldState = s;
                if (oldState == null) {
                    oldState = new SubscriptionState();
                }
                oldState.updateMaxQos(qos);
                oldState.increment();
                return oldState;
            }).getRefCount() == 1;

            if (shouldSubscribe) {
                toSubscribe.add(topic);
            }
        }

        if (!toSubscribe.isEmpty()) {
            List<MqttTopicSubscription> prepared = toSubscribe.stream().map(t -> new MqttTopicSubscription(t, MqttQoS.valueOf(states.get(t).getMaxQos()))).collect(Collectors.toList());
            logger.debug("Prepared list of subscriptions {}", prepared);
            mediator.subscribe(prepared);
        } else {
            logger.debug("List to subscribe is empty.");
        }

//        List<String> tempSubList = new ArrayList<>();
//        for (MqttTopicSubscription subscription : subscriptions) {
//            String topic = subscription.topicName();
//            Integer newQos = subscription.qualityOfService().value();
//
//            Integer prevQos = maxQos.computeIfAbsent(topic, k -> newQos);
//            if (newQos > prevQos) {
//                maxQos.put(topic, newQos);
//                logger.debug("Incremented QoS for {}. {} -> {}", topic, prevQos, newQos);
//            }
//
//            AtomicInteger count = counts.computeIfAbsent(topic, k -> new AtomicInteger(0));
//            int v = count.incrementAndGet();
//            logger.debug("Incremented refcount for {} -> {}", topic, v);
//            if (v == 1) {
//                logger.debug("Refcount={}. Add to list for subscribing to {}", v, topic);
//                tempSubList.add(topic);
//            }
//        }
//
//        List<MqttTopicSubscription> prepearedSubList = new ArrayList<>();
//        for (String subscription : tempSubList) {
//            prepearedSubList.add(new MqttTopicSubscription(subscription, MqttQoS.valueOf(maxQos.get(subscription))));
//        }
//
//        logger.debug("Prepeared list of subscriptions {}", prepearedSubList);
//        mediator.subscribe(prepearedSubList);
    }

    @Override
    public Promise<MqttUnsubAckMessage> unsubscribe(String topic) {
        if (StringUtils.isBlank(topic)) {
            return null;
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
            logger.debug("Refcount for {} droppes to 0, unsubscribing", topic);
            return mediator.unsubscribe(List.of(topic));
        }

        return null;

//        AtomicInteger count = counts.get(topic);
//        if (count == null) {
//            logger.debug("Unsubscribe called for absent key {}", topic);
//            return null;
//        }
//        int v = count.decrementAndGet();
//        logger.debug("Decremented refcount for {} -> {}", topic, v);
//        if (v <= 0) {
//            counts.remove(topic, count);
//            logger.debug("Refcount for {} dropped to {}, remove entry", topic, v);
//            return mediator.unsubscribe(List.of(topic));
//        }
//        return null;
    }

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
