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

import ru.maxeltr.homeMq2t.Service.Command.CommandService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttReasonCodeAndPropertiesVariableHeader;
import io.netty.util.concurrent.Promise;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ru.maxeltr.homeMq2t.AppShutdownManager;
import ru.maxeltr.homeMq2t.Config.AppProperties;
import ru.maxeltr.homeMq2t.Config.CardPropertiesProvider;
import ru.maxeltr.homeMq2t.Config.CommandPropertiesProvider;
import ru.maxeltr.homeMq2t.Config.ComponentPropertiesProvider;
import ru.maxeltr.homeMq2t.Model.Msg;
import ru.maxeltr.homeMq2t.Model.MsgImpl;
import ru.maxeltr.homeMq2t.Mqtt.HmMq2t;
import ru.maxeltr.homeMq2t.Mqtt.MqttChannelInitializer;
import ru.maxeltr.homeMq2t.Service.UI.UIService;

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

    @Autowired
    private CardPropertiesProvider cardPropertiesProvider;

    @Autowired
    private CommandPropertiesProvider commandPropertiesProvider;

    @Autowired
    private ComponentPropertiesProvider componentPropertiesProvider;

    @Value("${wait-disconnect-while-shutdown:1000}")
    private int waitDisconnect;

    @PostConstruct
    public void postConstruct() {
        this.setMediator();

        appProperties.getAllStartupTasks().forEach(
                startupTask -> {
                    try {
                        this.commandService.execute(startupTask.getPath(), startupTask.getArguments());
                    } catch (Exception ex) {
                        logger.warn("Could not execute start task.", ex);
                    }
                }
        );
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
            byte[] jsonMsg = this.mapper.writeValueAsBytes(msg);
            this.hmMq2t.publish(topic, Unpooled.wrappedBuffer(jsonMsg), qos, retain);
        } catch (JsonProcessingException ex) {
            logger.warn("Cannot convert msg to json {}", msg, ex.getMessage());
        }
    }

    @Override
    public void execute(Msg command, String commandNumber) {
        this.commandService.execute(command, commandNumber);
        logger.info("Data has been passed to the command service. Data={}.", command);
    }

    @Override
    public String execute(String commandPath, String arguments) {
        logger.debug("Pass command to the command service. commandPath={}, arguments={}.", commandPath, arguments);
        return this.commandService.execute(commandPath, arguments);
    }

    @Override
    public void display(Msg data, String cardNumber) {
        this.uiService.display(data, cardNumber);
        logger.info("Data has been passed to the ui service. Card number={}, data={}.", cardNumber, data);
    }

    @Override
    public void process(Msg data, String componentNumber) {
        this.componentService.process(data, componentNumber);
        logger.info("Data has been passed to the component service. Data={}.", data);
    }

    /**
     * Retrive card numbers assosiatied with the given MQTT topic. This method
     * delegates to the cardPRopertiesProvider to resolve which card identifiers
     * (numbers) are bound to the specified topic.
     *
     * @param topic the MQTT topic name
     * @return a list of card numbers associated with the topic
     *
     * @implNote This method is package-private and intended for use by
     * ServiceType numbers-getter method references (e.g.
     * ServiceMediator::getCardNumbersByTopic).
     */
    List<String> getCardNumbersByTopic(String topic) {
        return cardPropertiesProvider.getCardNumbersByTopic(topic);
    }

    /**
     * Retrive command numbers assosiatied with the given MQTT topic. This
     * method delegates to the commandPropertiesProvider to resolve which
     * command identifiers (numbers) are bound to the specified topic.
     *
     * @param topic the MQTT topic name
     * @return a list of command numbers associated with the topic
     *
     * @implNote This method is package-private and intended for use by
     * ServiceType numbers-getter method references (e.g.
     * ServiceMediator::getCommandNumbersByTopic).
     */
    List<String> getCommandNumbersByTopic(String topic) {
        return commandPropertiesProvider.getCommandNumbersByTopic(topic);
    }

    /**
     * Retrive component numbers assosiatied with the given MQTT topic. This
     * method delegates to the componentPropertiesProvider to resolve which
     * component identifiers (numbers) are bound to the specified topic.
     *
     * @param topic the MQTT topic name
     * @return a list of component numbers associated with the topic
     *
     * @implNote This method is package-private and intended for use by
     * ServiceType numbers-getter method references (e.g.
     * ServiceMediator::getComponentNumbersByTopic).
     */
    List<String> getComponentNumbersByTopic(String topic) {
        return componentPropertiesProvider.getComponentNumbersByTopic(topic);
    }

    /**
     * Handles an incoming Mqtt publish message.
     *
     * This method processes the message payload, attempts to convert it into
     * Msg.Builder object, and then dispatch the message to various services.
     * The method retrieves card numbers, command numbers, and component numbers
     * assotiated with the topic and sends the message to the appropriate
     * service for each valid numbers.
     *
     * @param mqttMessage the Mqtt publish message to be handled
     */
    @Override
    public void handleMessage(MqttPublishMessage mqttMessage) {
        int id = mqttMessage.variableHeader().packetId();
        logger.debug("Start handle message id={}.", id);

        Msg.Builder builder;
        try {
            builder = this.mapper.readValue(mqttMessage.payload().toString(StandardCharsets.UTF_8), Msg.Builder.class); //TODO pass only binary data from Msg.data. Create msg locally.
            logger.debug("Convert mqttMessage to Msg. {}", builder);
        } catch (JsonProcessingException ex) {
            logger.warn("Cannot convert json to Msg. Message id={}. Data was added as plain text. {}", id, ex.getMessage());
            builder = MsgImpl.newBuilder()
                    .data(mqttMessage.payload().toString(StandardCharsets.UTF_8))
                    .timestamp("n/a"); //TODO
        }

        for (ServiceType type : ServiceType.values()) {
            try {
                type.dispatch(this, builder.build(), mqttMessage.variableHeader().topicName());
            } catch (Exception ex) {
                logger.warn("Dispatch failed for type={} message id={}: {}", type.getName(), id, ex);
            }
        }

        logger.debug("End handle message id={}.", id);
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
        logger.info("Do shutdown.");
        this.componentService.stopSensorStreaming();
        this.componentService.shutdown();
        this.disconnect(MqttReasonCodeAndPropertiesVariableHeader.REASON_CODE_OK);

        try {
            TimeUnit.MILLISECONDS.sleep(waitDisconnect);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.info("Shutdown. InterruptedException while disconnect timeout.", ex);
        }

        this.appShutdownManager.shutdownApp(0);
    }

    @Override
    public boolean isConnected() {
        return this.hmMq2t.isConnected();
    }
}
