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
package ru.maxeltr.homeMq2t.Service;

import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.util.concurrent.Promise;
import jakarta.annotation.PostConstruct;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import ru.maxeltr.homeMq2t.Model.Msg;
import ru.maxeltr.homeMq2t.Mqtt.HmMq2t;
import ru.maxeltr.homeMq2t.Mqtt.MqttChannelInitializer;
import ru.maxeltr.homeMq2t.Mqtt.MqttPublishHandler;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class ServiceMediatorImpl implements ServiceMediator {

    @Autowired
    private CommandService commandService;

    @Autowired
    private UIService uiService;

    @Autowired
    private HmMq2t hmMq2t;

    @Autowired
    private MqttChannelInitializer mqttChannelInitializer;

    private final Map<String, List<String>> topicMatcher = new HashMap();

    public ServiceMediatorImpl() {

//        int i = 0;
//        String topic;
//        Map<String, List<Consumer<Msg.Builder>>> topicMatcher = new HashMap();
//        List<Consumer<Msg.Builder>> methods = new ArrayList<>();
//        while (!env.getProperty("card[" + i + "].name", "").isEmpty()) {
//            topic = env.getProperty("card[" + i + "].subscription.topic", "");
//            methods.add(this.uiService::display);
//            topicMatcher.put(topic, methods);
//            ++i;
//        }


    }

    @PostConstruct
    public void setMediator() {
        commandService.setMediator(this);
        uiService.setMediator(this);
        hmMq2t.setMediator(this);
        mqttChannelInitializer.setMediator(this);
    }

    @Override
    public void publish(Msg data) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void execute(Msg command) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void display(Msg.Builder data) {
        uiService.display(data);
    }

    @Override
    public void update() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void update(Component component) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void handleMessage(MqttPublishMessage message) {
        Msg.Builder msg = new Msg.Builder("receiveMessage").type("application/json");
        msg.timestamp(String.valueOf(Instant.now().toEpochMilli()));
        msg.topic(message.variableHeader().topicName());
        msg.payload(message.payload().toString(Charset.forName("UTF-8")));
        this.display(msg);
    }

    @Override
    public Promise<MqttConnAckMessage> connect() {
        return hmMq2t.connect();
    }

    @Override
    public void disconnect(byte reasonCode) {
        hmMq2t.disconnect(reasonCode);
    }
}
