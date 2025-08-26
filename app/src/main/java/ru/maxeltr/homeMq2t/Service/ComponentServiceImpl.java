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
import ru.maxeltr.homeMq2t.Config.CardPropertiesProvider;
import ru.maxeltr.homeMq2t.Config.CommandPropertiesProvider;
import ru.maxeltr.homeMq2t.Config.ComponentPropertiesProvider;
import ru.maxeltr.homeMq2t.Model.Msg;
import ru.maxeltr.homeMq2t.Model.MsgImpl;
import ru.maxeltr.homeMq2t.Mqtt.MqttUtils;
import ru.maxeltr.mq2tLib.Mq2tComponent;
import ru.maxeltr.mq2tLib.Mq2tCallbackComponent;
import ru.maxeltr.mq2tLib.Mq2tPollableComponent;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class ComponentServiceImpl implements ComponentService {

    private static final Logger logger = LoggerFactory.getLogger(ComponentServiceImpl.class);

    /**
     * The service mediator used for communication between components and
     * external systems. This property holds an instance of ServiceMediator,
     * which facilitates the interaction between the various components managed
     * by this service and other parts of the appliction or external services.
     * The mediator is responsible for handling message passing, data publishing
     * and other communication tasks. The mediator is set using the setMediator
     * method and it expected to be non-null. It allows components to publish
     * messages and receive data in a decoupled manner, promoting a modular
     * architecture.
     */
    private ServiceMediator mediator;

    /**
     * This list contains instances of Mq2tComponent that represetn various
     * components which can be utilized by the service. These components are
     * loaded using the ServiceLoder mechanism, allowing for dynamic discovery
     * and integration of new components. The components are injected through
     * thr constructor of the servcie
     */
    private final List<Mq2tComponent> pluginComponents;

    /**
     * A list of initialized callback components.
     *
     * This list holds instances of Mq2tCallbackComponent that have been
     * initialized during the postConstruct method. Each component is configured
     * with a callback function that processes incoming data.
     */
    private final List<Mq2tCallbackComponent> initializedCallbackComponents = new ArrayList<>();

    /**
     * This property holds an instance of ObjectMapper from the Jackson library,
     * which is utilized for converting Java objects to Json and vice versa.
     */
    @Autowired
    private ObjectMapper mapper;

    /**
     * The application properties used fpr configuration.
     */
    @Autowired
    private ComponentPropertiesProvider componentPropertiesProvider;

    @Autowired
    CardPropertiesProvider cardPropertiesProvider;

    /**
     * This property holds an instance of ThreadPoolTaskScheduler used to
     * schedule and execute tasks asynchronously, such as polling components at
     * regular intervals.
     */
    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    /**
     * This property holds an instance of PeriosicTrigger that defines the
     * interval and configuration for executing polling tasks. It is used to
     * determine how frequently the service should poll components for data.
     */
    @Autowired
    private PeriodicTrigger pollingPeriodicTrigger;

    /**
     * This property holds a ScheduledFuture that represents the ongoing polling
     * task. It is used to mange the lifecycle of the polling task.
     */
    private ScheduledFuture<?> scheduledPollingTask;

    public ComponentServiceImpl(List<Mq2tComponent> pluginComponents) {
        this.pluginComponents = pluginComponents;
    }

    /**
     * Sets mediator for this component service.
     *
     * @param mediator the ServiceMediator instance to be set
     */
    @Override
    public void setMediator(ServiceMediator mediator) {
        this.mediator = mediator;
    }

    /**
     * Performs initialization tasks after the component service has been
     * constructed. It logs the initialization process, iterates through the
     * plugin components.
     */
    @PostConstruct
    public void postConstruct() {
        logger.debug("Starting postConstruct initialization for {}.", ComponentServiceImpl.class);

        for (Mq2tComponent component : this.pluginComponents) {
            logger.info("There is component={} as provider.", component);
        }

        this.initializeCallbackComponents();

        this.startSensorStreaming();

    }

    /**
     * Initialize the callback components based on the configuration provided in
     * the application properties.
     *
     * This method iterates through the components names defined in the
     * application properties and attempts to retrieve the corresponding
     * component providers. If a provider is found and it is an instance of
     * Mq2tCallbackComponent, a callback is set for that component. The
     * initialized callback components are stored in the list for further
     * proccessing.
     */
    private void initializeCallbackComponents() {
        int i = 0;
        String componentName;
        while (StringUtils.isNotEmpty(componentName = componentPropertiesProvider.getComponentName(String.valueOf(i)))) {
            this.initializeCallbackComponent(componentName);
            i++;
        }
    }

    /**
     * Initializes a callback component based on the provided component name.
     *
     * This method attempts to retrieve the component provider associated with
     * the specified component name from the application properties. If a valid
     * provider is found and it is an instance of Mq2tCallbackComponent, a
     * callback is set for that component toEpochMilli handle incoming data. The
     * initialized callback component is then added to the list. If the
     * component is not of type Mq2tCallbackComponent, no action is taken, and
     * the method exits without modifying the state.
     *
     * @param componentName the name of the component to initialize
     */
    private void initializeCallbackComponent(String componentName) {
        Optional<Mq2tComponent> componentOpt = this.lookUpComponentProvider(this.componentPropertiesProvider.getComponentProvider(componentName));
        if (componentOpt.isEmpty()) {
            logger.warn("Could not get provider for component name={}.", componentName);
            return;
        }

        if (componentOpt.get() instanceof Mq2tCallbackComponent mq2tCallbackComponent) {
            this.setCallback(mq2tCallbackComponent);
            this.initializedCallbackComponents.add(mq2tCallbackComponent);
        }
    }

    /**
     * Sets a callback for the specified Mq2tCallbackComponent th handle
     * incoming data.
     *
     * @param mq2tCallbackComponent the callback component for which the
     * callback is to be set.
     */
    private void setCallback(Mq2tCallbackComponent mq2tCallbackComponent) {
        String componentName = mq2tCallbackComponent.getName();
        mq2tCallbackComponent.setCallback(
                data -> {
                    try {
                        if (data instanceof String dataStr) {
                            onDataReceived(dataStr, componentName);
                        } else {
                            logger.warn("Invalid data type received from provider={}.", componentName);
                        }
                    } catch (Exception ex) {
                        logger.error("Error in callback for {}:", componentName, ex);
                    }
                });
    }

    /**
     * Handles the reception of data from callback components.
     *
     * This method is triggered when data is received from a registered callback
     * component. It processes the incoming data and publish the data
     * accordingly.
     *
     * @param data the received data
     * @param componentName
     */
    public void onDataReceived(String data, String componentName) {
        logger.info("Callback method of component {} was triggered. Received data={}", componentName, data);
        publish(createMessage(componentName, data).build(), componentName);
    }

    /**
     * Processes a message for the specified component.
     *
     * The method retrives the component name assosiated with the given
     * component number. It checks the message type and retrieves command
     * accordingly. Then it processes the command contained in the incoming
     * message.
     *
     * @param msg the Msg used to construct the message to be processed
     * @param componentNumber the identifier of the component for which the
     * message is being processed.
     */
    @Async("processExecutor")
    @Override
    public void process(Msg msg, String componentNumber) {
        logger.info("Process message. Component number={}, msg={}", componentNumber, msg);

        String componentName = componentPropertiesProvider.getComponentName(componentNumber);
        if (StringUtils.isEmpty(componentName)) {
            logger.info("Could not process for component={}. Component name is empty.", componentNumber);
            return;
        }
        logger.info("Process message. Component number={}, component name={}", componentNumber, componentName);

        String type = msg.getType();
        if (type.equalsIgnoreCase(MediaType.TEXT_PLAIN_VALUE)) {
            String command = msg.getData();
            if (StringUtils.isEmpty(command)) {
                logger.info("Could not process for component={}, component number={}. Data is empty.", componentName, componentNumber);
                return;
            }

            if (command.equalsIgnoreCase("update")) {
                logger.info("Process command={}.", command);
                this.doUpdate(componentName);
            } else {
                logger.warn("Could not process. Unknown command={}.", command);
            }
        } else {
            logger.warn("Could not process. Unknown type={}.", type);
        }
    }

    /**
     * Perfoms an update operation for the specified component.
     *
     * @param componentName the name pf the component to be updated
     */
    private void doUpdate(String componentName) {
        logger.info("Update readings of component={}.", componentName);
        this.readAndPublish(componentName);
    }

    /**
     * This method starts polling tasks for the cofigured components and begins
     * streaming data from initialized callback components.
     */
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

    /**
     * This method iterates through the list of initialized callback components
     * and invokes their start methods to begin data streaming.
     */
    private void startCallbackComponentStreaming() {
        for (Mq2tCallbackComponent mq2tCallbackComponent : this.initializedCallbackComponents) {
            mq2tCallbackComponent.start();
            logger.debug("Start streaming component={}", mq2tCallbackComponent.getName());
        }
    }

    /**
     * This method cancels any ongoing polling tasks and stops the streaming for
     * all initiliazed callback components.
     */
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

    /**
     * This method iterates through the list of initilialized callback
     * components and invokes their stop methods to halt data streaming.
     */
    private void stopCallbackComponentStreaming() {
        for (Mq2tCallbackComponent mq2tCallbackComponent : this.initializedCallbackComponents) {
            mq2tCallbackComponent.stop();
            logger.debug("Stop streaming component={}", mq2tCallbackComponent.getName());
        }
    }

    /**
     * This method iterates through the list of plugin components and invokes
     * their shutdown methods to release resources and perfom any necessary
     * cleanup.
     */
    @Override
    public void shutdown() {
        for (Mq2tComponent component : this.pluginComponents) {
            component.shutdown();
            logger.debug("Try to shutdown component={}", component.getName());
        }
    }

    /**
     * Looks up a component provider by its name.
     *
     * This method searches through the list of registered plugin components to
     * find a component that matches the specified name.
     *
     * @param name the name of the component provider to look up
     * @return an Optional containing the matching Mq2tComponent if found, or an
     * empty Optional if no match is found.
     */
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

        String providerName = this.componentPropertiesProvider.getComponentProvider(componentName);
        Optional<Mq2tComponent> componentOpt = this.lookUpComponentProvider(providerName);
        if (componentOpt.isEmpty()) {
            logger.warn("Could not get provider for component name={}.", componentName);
            return;
        }

        if (componentOpt.get() instanceof Mq2tPollableComponent mq2tPollableComponent) {
            String[] args = {};
            String argsProperty = this.componentPropertiesProvider.getComponentProviderArgs(componentName);
            if (StringUtils.isNotEmpty(argsProperty)) {
                args = argsProperty.split(",");
            }

            String data;
            try {
                data = mq2tPollableComponent.getData(args);
                logger.info("Get data from component={}. Data={}", componentName, data);
            } catch (Exception ex) {
                logger.warn("Could not get data from component={}.", componentName, ex);
                return;
            }
            Msg msg = this.createMessage(componentName, data).build();
            this.publish(msg, componentName);
            this.publishLocally(msg, componentName);
        }
    }

    private void publishLocally(Msg msg, String componentName) {
        String cardName = this.componentPropertiesProvider.getComponentPubLocalCard(componentName);
        if (StringUtils.isEmpty(cardName)) {
            logger.info("There is no local card for component={} to publish locally.", componentName);
            return;
        }

        String cardId = this.cardPropertiesProvider.getCardNumber(cardName);
        if (StringUtils.isEmpty(cardId)) {
            logger.warn("Could not get card number by card name={}.", cardName);
            return;
        }

        if (this.mediator != null && this.mediator.isConnected()) {
            logger.info("Message passes to dispay locally. Message={}, card id={}", msg, cardId);
            this.mediator.display(msg, cardId);
        } else {
            logger.debug("Message could not passes to dispay locally, , because mediator is null. Message={}, card id={}", msg, cardId);
        }
    }

    private Msg.Builder createMessage(String componentName, String data) {
        String type = componentPropertiesProvider.getComponentPubDataType(componentName);
        if (StringUtils.isEmpty(type)) {
            type = MediaType.TEXT_PLAIN_VALUE;
            logger.info("Type is empty for component={}. Set {}.", componentName, type);
        }

        Msg.Builder builder = MsgImpl.newBuilder()
                .data(data)
                .type(type)
                .timestamp(String.valueOf(Instant.now().toEpochMilli()));
        logger.debug("Create message. Component name={}, Data={}", componentName, builder);

        return builder;
    }

    @Async("processExecutor")
    private void publish(Msg msg, String componentName) {       // TODO several topics?
        String topic = componentPropertiesProvider.getComponentPubTopic(componentName);
        if (StringUtils.isEmpty(topic)) {
            logger.info("Could not publish. There is no topic for component={}", componentName);
            return;
        }

        MqttQoS qos = MqttUtils.convertToMqttQos(componentPropertiesProvider.getComponentPubQos(componentName));

        boolean retain = Boolean.parseBoolean(componentPropertiesProvider.getComponentPubRetain(componentName));

        if (this.mediator != null && this.mediator.isConnected()) {
            logger.info("Message passes to publish. Message={}, topic={}, qos={}, retain={}", msg, topic, qos, retain);
            this.mediator.publish(msg, topic, qos, retain);
        } else {
            logger.debug("Message could not pass to publish, because mq2t is disconnected. Message={}, topic={}, qos={}, retain={}", msg, topic, qos, retain);
        }
    }

    class PollingTask implements Runnable {

        @Override
        public void run() {
            logger.debug("Start/resume polling");

            int i = 0;
            String componentName;
            while (StringUtils.isNotEmpty(componentName = componentPropertiesProvider.getComponentName(String.valueOf(i)))) {
                logger.debug("Polling component={}.", componentName);
                readAndPublish(componentName);
                i++;
            }

            logger.debug("Pause polling");
        }
    }
}
