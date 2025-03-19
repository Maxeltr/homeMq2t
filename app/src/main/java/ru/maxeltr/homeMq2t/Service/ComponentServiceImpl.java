/*
 * The MIT License
 *
 * Copyright 2025 Maxim Eltratov <<Maxim.Eltratov@ya.ru>>..
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.mqtt.MqttQoS;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import ru.maxeltr.homeMq2t.Config.AppProperties;
import ru.maxeltr.homeMq2t.Model.Msg;
import ru.maxeltr.homeMq2t.Model.MsgImpl;
import ru.maxeltr.mq2tLib.Mq2tComponent;
import ru.maxeltr.mq2tLib.Mq2tCallbackComponent;
import ru.maxeltr.mq2tLib.Mq2tPollableComponent;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class ComponentServiceImpl implements ComponentService {

    private static final Logger logger = LoggerFactory.getLogger(ComponentServiceImpl.class);

    private ServiceMediator mediator;

    private final List<Mq2tComponent> pluginComponents;

    private final List<Mq2tCallbackComponent> initializedCallbackComponents = new ArrayList<>();

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @Autowired
    private PeriodicTrigger pollingPeriodicTrigger;

    private ScheduledFuture<?> scheduledPollingTask;

    public ComponentServiceImpl(List<Mq2tComponent> pluginComponents) {
        this.pluginComponents = pluginComponents;
    }

    @Override
    public void setMediator(ServiceMediator mediator) {
        this.mediator = mediator;
    }

    @PostConstruct
    public void postConstruct() {
        logger.debug("Starting postConstruct initialization for {}.", ComponentServiceImpl.class);

        for (Mq2tComponent component : this.pluginComponents) {
            logger.info("There is component={} as provider.", component);
        }

        this.initializeCallbackComponents();

        this.startSensorStreaming();

    }

    private void initializeCallbackComponents() {
        int i = 0;
        String componentName;
        while (StringUtils.isNotEmpty(componentName = appProperties.getComponentName(String.valueOf(i)))) {
            Optional<Mq2tComponent> componentOpt = this.lookUpComponentProvider(this.appProperties.getComponentProvider(componentName));
            if (componentOpt.isEmpty()) {
                logger.warn("Could not get provider for component name={}.", componentName);
                i++;
                continue;
            }

            if (componentOpt.get() instanceof Mq2tCallbackComponent mq2tCallbackComponent) {
                mq2tCallbackComponent.setCallback(
                        data -> {
                            if (data instanceof String dataStr) {
                                onDataReceived(dataStr);
                            } else {
                                logger.warn("Invalid data type received from provider={}.", mq2tCallbackComponent.getName());
                            }
                        }
                );
                logger.debug("Set callback to provider={}", mq2tCallbackComponent.getName());   //TODO how to unload components?
                this.initializedCallbackComponents.add(mq2tCallbackComponent);
            }
            i++;
        }
    }

    private Optional<String> getComponentNameFromJson(String data) {
        HashMap<String, String> dataMap;
        try {
            dataMap = mapper.readValue(data, new TypeReference<HashMap<String, String>>() {
            });
        } catch (JsonProcessingException ex) {
            logger.warn("Could not convert json data={} to map. {}", data, ex.getMessage());
            return Optional.empty();
        }

        return Optional.ofNullable(dataMap.get("name"));

    }

    public void onDataReceived(String data) {
        logger.info("Callback method triggered. Received data={}", data);
        this.getComponentNameFromJson(data).ifPresentOrElse(
                componentName -> publish(createMessage(componentName, data), componentName),
                () -> logger.warn("Invalid data was passed to callback. Name of component is absent. Data={}", data)
        );
    }

    @Async("processExecutor")
    @Override
    public void process(Msg.Builder builder, String componentNumber) {
        Msg msg = builder.build();
        logger.info("Process message. Component number={}, msg={}", componentNumber, msg);

        String componentName = appProperties.getComponentName(componentNumber);
        if (StringUtils.isEmpty(componentName)) {
            logger.info("Could not process for component={}. Component name is empty.", componentNumber);
            return;
        }
        logger.info("Process message. Component number={}, component name={}", componentNumber, componentName);

        String type = msg.getType();
        if (type.equalsIgnoreCase(MediaType.TEXT_PLAIN_VALUE)) {
            String command = msg.getData();
            if (StringUtils.isEmpty(command)) {
                logger.info("Could not process for component={}, component number={}. Component data is empty.", componentName, componentNumber);
                return;
            }

            if (command.equalsIgnoreCase("update")) {
                logger.info("Process command={}.", command);
                this.doUpdate(componentName);
            } else {
                logger.warn("Could not process. Unknown command={}.", command);
            }
        } else {
            logger.warn("Could not process. Unknown command type={}.", type);
        }
    }

    private void doUpdate(String componentName) {
        logger.info("Update readings of component={}.", componentName);
        this.readAndPublish(componentName);
    }

    @Override
    public void startSensorStreaming() {
        if (this.scheduledPollingTask == null || this.scheduledPollingTask.isDone()) {
            logger.info("Start polling components task.");
            this.scheduledPollingTask = this.threadPoolTaskScheduler.schedule(new PollingTask(), this.pollingPeriodicTrigger);
        } else {
            logger.warn("Could not start polling components task. Previous polling components task was not stopped.");
        }

        this.startCallbackComponentStreaming();
    }

    private void startCallbackComponentStreaming() {
        for (Mq2tCallbackComponent mq2tCallbackComponent : this.initializedCallbackComponents) {
            mq2tCallbackComponent.start();
            logger.debug("Start streaming component={}", mq2tCallbackComponent.getName());
        }
    }

    @Override
    public void stopSensorStreaming() {
        if (this.scheduledPollingTask != null && !this.scheduledPollingTask.isDone()) {
            this.scheduledPollingTask.cancel(false);
            this.scheduledPollingTask = null;
            logger.info("Polling components task is stopped");
        } else {
            logger.info("Polling components task has been stopped already.");
        }

        this.stopCallbackComponentStreaming();
    }

    private void stopCallbackComponentStreaming() {
        for (Mq2tCallbackComponent mq2tCallbackComponent : this.initializedCallbackComponents) {
            mq2tCallbackComponent.stop();
            logger.debug("Stop streaming component={}", mq2tCallbackComponent.getName());
        }
    }

    @Override
    public void shutdown() {
        for (Mq2tComponent component : this.pluginComponents) {
            component.shutdown();
            logger.debug("Try to shutdown component={}", component.getName());
        }
    }

    private Optional<Mq2tComponent> lookUpComponentProvider(String name) {
        for (Mq2tComponent component : pluginComponents) {
            if (component.getName().equalsIgnoreCase(name)) {
                logger.debug("Found component={} as provider.", component.getName());
                return Optional.of(component);
            }
        }

        logger.debug("No provider was found for component name={}.", name);
        return Optional.empty();
    }

    private void readAndPublish(String componentName) {

        String providerName = this.appProperties.getComponentProvider(componentName);
        Optional<Mq2tComponent> componentOpt = this.lookUpComponentProvider(providerName);
        if (componentOpt.isEmpty()) {
            logger.warn("Could not get provider for component name={}.", componentName);
            return;
        }

        if (componentOpt.get() instanceof Mq2tPollableComponent mq2tPollableComponent) {
            String[] args = {};
            String argsProperty = this.appProperties.getComponentProviderArgs(componentName);
            if (StringUtils.isNotEmpty(argsProperty)) {
                args = argsProperty.split(",");
            }

            String data;
            try {
                data = mq2tPollableComponent.getData(args);
                logger.info("Get data from component={}. Data={}", componentName, data);
            } catch (Exception ex) {
                logger.info("Could not get data from component={}.", componentName, ex);
                data = "Error get data. " + ex.getMessage();
            }
            Msg.Builder builder = this.createMessage(componentName, data);
            this.publish(builder, componentName);
            this.publishLocally(builder, componentName);
        }
    }

    private void publishLocally(Msg.Builder builder, String componentName) {
        String cardName = this.appProperties.getComponentPubLocalCard(componentName);
        if (StringUtils.isEmpty(cardName)) {
            logger.info("There is no local card for component={} to publish locally.", componentName);
            return;
        }

        String cardId = this.appProperties.getCardNumber(cardName);
        if (StringUtils.isEmpty(cardId)) {
            logger.warn("Could not get card number by card name={}.", cardName);
            return;
        }

        if (this.mediator != null && this.mediator.isConnected()) {
            logger.info("Message passes to dispay locally. Message={}, card id={}", builder, cardId);
            this.mediator.display(builder, cardId);
        } else {
            logger.debug("Message could not passes to dispay locally, , because mediator is null. Message={}, card id={}", builder, cardId);
        }
    }

    private Msg.Builder createMessage(String componentName, String data) {
        String type = appProperties.getComponentPubDataType(componentName);
        if (StringUtils.isEmpty(type)) {
            logger.info("Type is empty for component={}. Set text/plain. }.", componentName);
            type = MediaType.TEXT_PLAIN_VALUE;
        }

        Msg.Builder builder = new MsgImpl.MsgBuilder()
                .data(data)
                .type(type)
                .timestamp(String.valueOf(Instant.now().toEpochMilli()));
        logger.debug("Create message. Component name={}, Data={}", componentName, builder);

        return builder;
    }

    @Async("processExecutor")
    private void publish(Msg.Builder msg, String componentName) {       // TODO several topics?
        String topic = appProperties.getComponentPubTopic(componentName);
        if (StringUtils.isEmpty(topic)) {
            logger.info("Could not publish. There is no topic for component={}", componentName);
            return;
        }

        MqttQoS qos = this.convertToMqttQos(appProperties.getComponentPubQos(componentName));

        boolean retain = Boolean.parseBoolean(appProperties.getComponentPubRetain(componentName));

        if (this.mediator != null && this.mediator.isConnected()) {
            logger.info("Message passes to publish. Message={}, topic={}, qos={}, retain={}", msg, topic, qos, retain);
            this.mediator.publish(msg.build(), topic, qos, retain);
        } else {
            logger.debug("Message could not pass to publish, because mq2t is disconnected. Message={}, topic={}, qos={}, retain={}", msg, topic, qos, retain);
        }
    }

    /**
     * Convert the given qos value from string to MqttQos enum instance. If the
     * qos value is invalid, it defaults to qos level 0.
     *
     * @param qosString The qos value as a string. Must not be null.
     * @return The qos level as a MqttQos enum value.
     */
    private MqttQoS convertToMqttQos(String qosString) {
        MqttQoS qos;
        try {
            qos = MqttQoS.valueOf(qosString);
        } catch (IllegalArgumentException ex) {
            logger.warn("Invalid QoS value for the given qos string={}: {}. Set QoS=0.", qosString, ex.getMessage());
            qos = MqttQoS.AT_MOST_ONCE;
        }

        return qos;
    }

    class PollingTask implements Runnable {

        @Override
        public void run() {
            logger.debug("Start/resume polling");

            int i = 0;
            String componentName;
            while (StringUtils.isNotEmpty(componentName = appProperties.getComponentName(String.valueOf(i)))) {
                logger.debug("Polling component={}.", componentName);
                readAndPublish(componentName);
                i++;
            }

            logger.debug("Pause polling");
        }
    }
}
