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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.Promise;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import ru.maxeltr.homeMq2t.Model.Msg;
import ru.maxeltr.homeMq2t.Mqtt.HmMq2t;
import ru.maxeltr.homeMq2t.Mqtt.MqttChannelInitializer;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class ServiceMediatorImpl implements ServiceMediator {

    private static final Logger logger = LoggerFactory.getLogger(ServiceMediatorImpl.class);

    @Autowired
    private Environment env;

    @Autowired
    private CommandService commandService;

    @Autowired
    private UIService uiService;

    @Autowired
    private HmMq2t hmMq2t;

    @Autowired
    private MqttChannelInitializer mqttChannelInitializer;

    private final Map<String, String> topicsAndCardNumbersForDisplay = new HashMap();

    @Autowired
    private ObjectMapper mapper;

//    public ServiceMediatorImpl() {
//        int i = 0;
//        while (!env.getProperty("card[" + i + "].name", "").isEmpty()) {
//            topicsAndCards.put(
//                    env.getProperty("card[" + i + "].subscription.topic", ""),
//                    env.getProperty("card[" + i + "].name", "")
//            );
//            ++i;
//        }
//    }

    @PostConstruct
    public void postConstruct() {
        this.setMediator();
        
        int i = 0;
        while (!env.getProperty("card[" + i + "].name", "").isEmpty()) {
            topicsAndCardNumbersForDisplay.put(
                    env.getProperty("card[" + i + "].subscription.topic", ""),
                    String.valueOf(i)
            );
            ++i;
        }
    }
    
    public void setMediator() {
        commandService.setMediator(this);
        uiService.setMediator(this);
        hmMq2t.setMediator(this);
        mqttChannelInitializer.setMediator(this);
    }

    @Override
    public void publish(Msg msg, String topic, MqttQoS qos, boolean retain) {
        logger.info("publish message {}. topic={}, qos={}, retain={}", msg, topic, qos, retain);
        try {
            this.hmMq2t.publish(topic, Unpooled.wrappedBuffer(mapper.writeValueAsBytes(msg)), qos, retain);
        } catch (JsonProcessingException ex) {
            logger.warn("can not convert msg to json {}", msg, ex);
        }
    }

    @Override
    public void execute(Msg command) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void display(Msg.Builder data, String cardNumber) {
        uiService.display(data, cardNumber);
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
    public void handleMessage(MqttPublishMessage mqttMessage) {
        Msg.Builder payload;
        try {
            payload = mapper.readValue(mqttMessage.payload().toString(Charset.forName("UTF-8")), Msg.Builder.class);
        } catch (JsonProcessingException ex) {
            logger.warn("can not convert json to Msg. {}", mqttMessage.payload().toString(Charset.forName("UTF-8")), ex);
            return; //TODO
        }
        
        if (this.topicsAndCardNumbersForDisplay.containsKey(mqttMessage.variableHeader().topicName())) {
            this.display(payload, this.topicsAndCardNumbersForDisplay.get(mqttMessage.variableHeader().topicName()));
        } else {
            logger.warn("can not handle message. There are no actions for {}", mqttMessage);
        }

    }

    @Override
    public Promise<MqttConnAckMessage> connect() {
        return hmMq2t.connect();
    }

    @Override
    public void disconnect(byte reasonCode) {
        hmMq2t.disconnect(reasonCode);
    }

    private boolean isJsonValid(String jsonInString) {
        try {

            mapper.readTree(jsonInString);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
