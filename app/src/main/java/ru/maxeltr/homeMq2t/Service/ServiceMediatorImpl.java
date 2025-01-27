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
import io.netty.handler.codec.mqtt.MqttReasonCodeAndPropertiesVariableHeader;
import io.netty.util.concurrent.Promise;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ru.maxeltr.homeMq2t.AppShutdownManager;
import ru.maxeltr.homeMq2t.Config.AppProperties;
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
    private ComponentService componentService;

    @Autowired
    private CommandService commandService;

    @Autowired
    private UIService uiService;

    @Autowired
    private HmMq2t hmMq2t;

    @Autowired
    private MqttChannelInitializer mqttChannelInitializer;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    AppShutdownManager appShutdownManager;

    @Autowired
    private AppProperties appProperties;

    @Value("${wait-disconnect-while-shutdown:1000}")
    private Integer waitDisconnect;

    @PostConstruct
    public void postConstruct() {
        this.setMediator();
    }

    public void setMediator() {
        componentService.setMediator(this);
        commandService.setMediator(this);
        uiService.setMediator(this);
        hmMq2t.setMediator(this);
        mqttChannelInitializer.setMediator(this);
    }

    @Override
    public void publish(Msg msg, String topic, MqttQoS qos, boolean retain) {
        logger.info("Publish message has been passed to mqtt client. topic={}, qos={}, retain={}. {}", topic, qos, retain, msg);
        try {
            this.hmMq2t.publish(topic, Unpooled.wrappedBuffer(this.mapper.writeValueAsBytes(msg)), qos, retain);
        } catch (JsonProcessingException ex) {
            logger.warn("Cannot convert msg to json {}", msg, ex.getMessage());
        }
    }

    @Override
    public void execute(Msg.Builder command, String commandNumber) {
        this.commandService.execute(command, commandNumber);
        logger.info("Data has been passed to the command service. Data={}.", command);
    }

    @Override
    public void display(Msg.Builder data, String cardNumber) {
        this.uiService.display(data, cardNumber);
        logger.info("Data has been passed to the ui service. Card number={}, data={}.", cardNumber, data);
    }

    @Override
    public void process(Msg.Builder data, String componentNumber) {
        this.componentService.process(data, componentNumber);
        logger.info("Data has been passed to the component service. Data={}.", data);
    }

    @Override
    public void handleMessage(MqttPublishMessage mqttMessage) {
        int id = mqttMessage.variableHeader().packetId();
        logger.debug("Start handle message id={}. mqttMessage={}.", id, mqttMessage);

        Msg.Builder builder;
        try {
            builder = this.mapper.readValue(mqttMessage.payload().toString(Charset.forName("UTF-8")), Msg.Builder.class);
        } catch (JsonProcessingException ex) {
            logger.warn("Cannot convert json to Msg. {} id={}. MqttMessage={}", ex.getMessage(), id, mqttMessage.payload().toString(Charset.forName("UTF-8")));
            builder = new Msg.Builder();
            builder.data(mqttMessage.payload().toString(Charset.forName("UTF-8")));
            builder.timestamp(String.valueOf(Instant.now().toEpochMilli()));
        }

        String topicName = (mqttMessage.variableHeader().topicName());
        String cardNumber = this.appProperties.getCardNumberByTopic(topicName);
        if (!cardNumber.isEmpty()) {
            this.display(builder, cardNumber);
            logger.debug("Message id={} has been passed to ui service. mqttMessage={}.", id, mqttMessage);

        }

        String commandNumber = this.appProperties.getCommandNumberByTopic(topicName);
        if (!commandNumber.isEmpty()) {
            this.execute(builder, commandNumber);
            logger.debug("Message id={} has been passed to command service. mqttMessage={}.", id, mqttMessage);

        }

        String componentNumber = this.appProperties.getComponentNumberByTopic(topicName);
        if (!componentNumber.isEmpty()) {
            this.process(builder, componentNumber);
            logger.debug("Message id={} has been passed to component service. mqttMessage={}.", id, mqttMessage);

        }

        logger.debug("End handle message id={}. mqttMessage={}.", id, mqttMessage);
    }

    @Override
    public Promise<MqttConnAckMessage> connect() {
        return this.hmMq2t.connect();
    }

    @Override
    public void reconnect() {
        this.hmMq2t.reconnect();
    }

    @Override
    public void disconnect(byte reasonCode) {
        this.hmMq2t.disconnect(reasonCode);
    }

    @Override
    public void shutdown() {
        this.disconnect(MqttReasonCodeAndPropertiesVariableHeader.REASON_CODE_OK);
        this.componentService.stopPolling();
        try {
            TimeUnit.MILLISECONDS.sleep(waitDisconnect);
        } catch (InterruptedException ex) {
            logger.info("Shutdown. InterruptedException while disconnect timeout.", ex);
        }

        this.appShutdownManager.shutdownApp(0);
    }

    @Override
    public boolean isConnected() {
        return this.hmMq2t.isConnected();
    }
}
