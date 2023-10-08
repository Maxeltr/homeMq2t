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
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class MqttAckMediatorImpl implements MqttAckMediator {

    private Promise<MqttConnAckMessage> connectFuture;

    @Autowired
    private HmMq2t hmMq2t;

    //@Autowired
    //private MqttConnectHandler mqttConnectHandler;

    private final ConcurrentHashMap<String, Promise<MqttMessage>> futures = new ConcurrentHashMap<>();

    @PostConstruct
    public void setMediator() {
        hmMq2t.setMediator(this);
        //mqttConnectHandler.setMediator(this);
        //mqttPublishHandler.setMediator(this);
    }

    @Override
    public Promise<MqttMessage> get(String key) {
        return this.futures.get(key);
    }

    @Override
    public void add(String key, Promise<MqttMessage> value) {
        this.futures.put(key, value);
    }

    @Override
    public void remove(String key) {
        this.futures.remove(key);
    }

    @Override
    public void setConnectFuture(Promise<MqttConnAckMessage> future) {
        this.connectFuture = future;
    }

    public Promise<MqttConnAckMessage> getConnectFuture() {
        return this.connectFuture;
    }
}
