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

import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.util.concurrent.Promise;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class MqttAckMediatorImpl implements MqttAckMediator {

    private static final Logger logger = LoggerFactory.getLogger(MqttAckMediatorImpl.class);

    private Promise<MqttConnAckMessage> connectFuture;

    @Autowired
    private HmMq2t hmMq2t;

    @Autowired
    private MqttPublishHandler publishHandler;

    //@Autowired
    //private MqttConnectHandler mqttConnectHandler;
    
    private final Map<Integer, Promise<? extends MqttMessage>> futures = Collections.synchronizedMap(new LinkedHashMap<>());

    private final Map<Integer, MqttMessage> messages  = Collections.synchronizedMap(new LinkedHashMap<>());
    
    @PostConstruct
    public void setMediator() {
        hmMq2t.setMediator(this);
        logger.debug("Set {} to the {}", MqttAckMediatorImpl.class, this.hmMq2t.getClass());
        publishHandler.setMediator(this);
        logger.debug("Set {} to the {}", MqttAckMediatorImpl.class, this.publishHandler.getClass());
        //mqttConnectHandler.setMediator(this);
    }

    @Override
    public Promise<? extends MqttMessage> getFuture(int key) {
        return this.futures.get(key);
    }

    @Override
    public MqttMessage getMessage(int key) {
        return this.messages.get(key);
    }

    @Override
    public boolean isContainId(int key) {
        return this.messages.containsKey(key) || this.futures.containsKey(key);
    }

    @Override
    public void add(int key, Promise<? extends MqttMessage> future, MqttMessage message) {
        this.futures.put(key, future);
        logger.debug("Future was added key: {} future: {}. Amount futures: {}", key, future, futures.size());
        this.messages.put(key, message);
        logger.debug("Message was added key: {} message: {}. Amount messages: {}", key, future, messages.size());
    }

    @Override
    public void remove(int key) {
        this.futures.remove(key);
        logger.debug("Future was removed key: {}. Amount futures: {}", key, futures.size());
        this.messages.remove(key);
        logger.debug("Message was removed key: {}. Amount messages: {}", key, messages.size());
    }

    @Override
    public void setConnectFuture(Promise<MqttConnAckMessage> future) {
        this.connectFuture = future;
        logger.debug("Connect future was set: {}.", future);
    }

    @Override
    public Promise<MqttConnAckMessage> getConnectFuture() {
        return this.connectFuture;
    }
}
