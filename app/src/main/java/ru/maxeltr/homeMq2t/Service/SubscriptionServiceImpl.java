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
import java.util.concurrent.atomic.AtomicInteger;
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

    private final ConcurrentMap<String, AtomicInteger> counts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Integer> maxQos = new ConcurrentHashMap<>();

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

        String topic = subscription.topicName();

        AtomicInteger count = counts.computeIfAbsent(topic, k -> new AtomicInteger(0));

        int v = count.incrementAndGet();
        logger.debug("Incremented refcount for {} -> {}", topic, v);
        if (v == 1) {
            logger.debug("Refcount={}. Subscribing to {}", v, topic);
            mediator.subscribe(List.of(subscription));
            return true;
        }
        return false;
    }

    @Override
    public void subscribe(List<MqttTopicSubscription> subscriptions) {
        if (subscriptions == null || subscriptions.isEmpty()) {
            logger.debug("Empty subscription list given");
            return;
        }
        List<String> tempSubList = new ArrayList<>();
        for (MqttTopicSubscription subscription : subscriptions) {
            String topic = subscription.topicName();
            Integer newQos = subscription.qualityOfService().value();

            Integer prevQos = maxQos.computeIfAbsent(topic, k -> newQos);
            if (newQos > prevQos) {
                maxQos.put(topic, newQos);
                logger.debug("Incremented QoS for {}. {} -> {}", topic, prevQos, newQos);
            }

            AtomicInteger count = counts.computeIfAbsent(topic, k -> new AtomicInteger(0));
            int v = count.incrementAndGet();
            logger.debug("Incremented refcount for {} -> {}", topic, v);
            if (v == 1) {
                logger.debug("Refcount={}. Add to list for subscribing to {}", v, topic);
                tempSubList.add(topic);
            }
        }

        List<MqttTopicSubscription> prepearedSubList = new ArrayList<>();
        for (String subscription : tempSubList) {
            prepearedSubList.add(new MqttTopicSubscription(subscription, MqttQoS.valueOf(maxQos.get(subscription))));
        }

        logger.debug("Prepeared list of subscriptions {}", prepearedSubList);
        mediator.subscribe(prepearedSubList);
    }

    @Override
    public Promise<MqttUnsubAckMessage> unsubscribe(String topic) {
        if (StringUtils.isBlank(topic)) {
            return null;
        }

        AtomicInteger count = counts.get(topic);
        if (count == null) {
            logger.debug("Unsubscribe called for absent key {}", topic);
            return null;
        }
        int v = count.decrementAndGet();
        logger.debug("Decremented refcount for {} -> {}", topic, v);
        if (v <= 0) {
            counts.remove(topic, count);
            logger.debug("Refcount for {} dropped to {}, remove entry", topic, v);
            return mediator.unsubscribe(List.of(topic));
        }
        return null;
    }

}
