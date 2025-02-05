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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import ru.maxeltr.homeMq2t.Config.AppProperties;
import ru.maxeltr.homeMq2t.Model.Msg;
import ru.maxeltr.homeMq2t.Model.MsgImpl;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class ComponentServiceImpl implements ComponentService {

    private static final Logger logger = LoggerFactory.getLogger(ComponentServiceImpl.class);

    private ServiceMediator mediator;

    private final List<Object> pluginComponents;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @Autowired
    private PeriodicTrigger pollingPeriodicTrigger;

    private ScheduledFuture<?> pollingScheduledFuture;

    public ComponentServiceImpl(List<Object> pluginComponents) {
        this.pluginComponents = pluginComponents;
    }

    @Override
    public void setMediator(ServiceMediator mediator) {
        this.mediator = mediator;
    }

    @PostConstruct
    public void postConstruct() {
        for (Object component : this.pluginComponents) {
            if (this.isImplements(component, Mq2tCallbackComponent.class)) {
                this.invokeMethod(component, "setCallback", (String data) -> callback(data));
            }
            logger.debug("Loaded component={}", this.invokeMethod(component, "getName"));   //TODO how to unload components?
        }

        this.startSensorStreaming();

        //this.future = taskScheduler.schedule(new RunnableTask(), periodicTrigger);
    }

    private boolean isImplements(Object component, Class testedInterface) {
        for (Class componentInterface : component.getClass().getInterfaces()) {
            if (componentInterface.getSimpleName().equals(testedInterface.getSimpleName())) {
                return true;
            }
        }
        return false;
    }

//    private boolean isImplements(Object component, Class testedInterface) { //TODO check in componentloader
//        return testedInterface.isAssignableFrom(component.getClass());
//    }
    private void invokeMethod(Object component, String methodName, Consumer<String> param) {
        try {
            Method method = component.getClass().getMethod(methodName, Consumer.class);
            method.invoke(component, param);
            logger.debug("Method={} has been  invoked in component={}", methodName, component);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException ex) {
            logger.warn("Could not invoke method={} to component={}. {}", methodName, component, ex.getMessage());
        }
    }

    private String invokeMethod(Object component, String methodName) {
        String data = "";
        try {
            Method method = component.getClass().getMethod(methodName);
            data = method.invoke(component).toString();
            logger.debug("Method={} has been  invoked in component={}", methodName, component);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException | NullPointerException ex) {
            logger.warn("Could not invoke method={} in component={}. {}", methodName, component, ex.getMessage());
        }

        return data;
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

    public void callback(String data) {
        logger.debug("Callback has been called. Data={}", data);

        Optional<String> componentNameOpt = this.getComponentNameFromJson(data);
        if (componentNameOpt.isEmpty()) {
            logger.warn("Invalid data was passed to callback. Name of component is absent. Data={}", data);
            return;
        }
        String componentName = componentNameOpt.get().toLowerCase();

        Msg.Builder builder = this.createMessage(componentName, data);

        this.publish(builder, componentName);

    }

    @Async("processExecutor")
    @Override
    public void process(Msg.Builder builder, String componentNumber) {
        Msg msg = builder.build();
        logger.info("Process message. Component number={}, msg={}", componentNumber, msg);

        String componentName = appProperties.getComponentName(componentNumber);
        if (componentName == null || componentName.isEmpty()) {
            logger.info("Could not process for component={}. Component name is empty.", componentNumber);
            return;
        }
        logger.info("Process message. Component number={}, component name={}", componentNumber, componentName);

        String type = msg.getType();
        if (type.equalsIgnoreCase(MediaType.TEXT_PLAIN_VALUE)) {
            String command = msg.getData();
            if (command == null || command.isEmpty()) {
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
            logger.warn("Could not process. Unknown type={}.", type);
        }
    }

    private void doUpdate(String componentName) {
        logger.info("Update readings of component={}.", componentName);
        this.getComponentByName(componentName).ifPresentOrElse(
                component -> this.readAndPublish(component),
                () -> logger.warn("Could not update readings of component={}.", componentName)
        );
    }

    @Override
    public Optional<Object> getComponentByName(String componentName) {
        for (Object component : this.pluginComponents) {
            if (componentName.equalsIgnoreCase(this.invokeMethod(component, "getName"))) {
                return Optional.of(component);
            }
        }
        logger.warn("There is no component for name={}.", componentName);

        return Optional.empty();
    }

    @Override
    public void startSensorStreaming() {
        if (this.pollingScheduledFuture == null) {
            logger.info("Start polling components task.");
            this.pollingScheduledFuture = this.threadPoolTaskScheduler.schedule(new PollingTask(), this.pollingPeriodicTrigger);
        } else {
            logger.warn("Could not start polling components task. Previous polling components task was not stopped.");
        }

        for (Object component : this.pluginComponents) {
            if (this.isImplements(component, Mq2tCallbackComponent.class)) {
                this.invokeMethod(component, "start");
                logger.debug("Start streaming component={}", this.invokeMethod(component, "getName"));
            }
        }
    }

    @Override
    public void stopSensorStreaming() {
        if (this.pollingScheduledFuture != null && !this.pollingScheduledFuture.isDone()) {
            this.pollingScheduledFuture.cancel(false);
            this.pollingScheduledFuture = null;
            logger.info("Polling components task is stopped");
        } else {
            logger.info("Polling components task has been stopped already.");
        }

        for (Object component : this.pluginComponents) {
            if (this.isImplements(component, Mq2tCallbackComponent.class)) {
                this.invokeMethod(component, "stop");
                logger.debug("Stop streaming component={}", this.invokeMethod(component, "getName"));
            }
        }
    }

    private void readAndPublish(Object component) {
        String componentName = invokeMethod(component, "getName").toLowerCase();
        String data = invokeMethod(component, "getData");
        logger.info("Get data from component={} in polling task. Data={}", componentName, data);

        Msg.Builder builder = this.createMessage(componentName, data);

        publish(builder, componentName);
    }

    private Msg.Builder createMessage(String componentName, String data) {
        String type = appProperties.getComponentPubDataType(componentName);
        if (type == null || type.isEmpty()) {
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
    private void publish(Msg.Builder msg, String componentName) {
        String topic = appProperties.getComponentPubTopic(componentName);
        if (topic == null || topic.isEmpty()) {
            logger.info("Could not publish. There is no topic for component={}", componentName);
            return;
        }

        MqttQoS qos;
        try {
            qos = MqttQoS.valueOf(appProperties.getComponentPubQos(componentName));
        } catch (IllegalArgumentException ex) {
            logger.error("Invalid QoS value for component={}: {}. Set QoS=0.", componentName, ex.getMessage());
            qos = MqttQoS.valueOf(0);
        }

        boolean retain = Boolean.parseBoolean(appProperties.getComponentPubRetain(componentName));

        if (this.mediator != null && this.mediator.isConnected()) {
            logger.info("Message passes to publish. Message={}, topic={}, qos={}, retain={}", msg, topic, qos, retain);
            this.mediator.publish(msg.build(), topic, qos, retain);
        } else {
            logger.info("Message could not pass to publish, because mq2t is disconnected. Message has been rejected. Message={}, topic={}, qos={}, retain={}", msg, topic, qos, retain);
        }
    }

    class PollingTask implements Runnable {

        @Override
        public void run() {
            logger.debug("Start/resume polling");

            for (Object component : pluginComponents) {
                String componentName = invokeMethod(component, "getName");
                if (!isImplements(component, Mq2tPollableComponent.class)) {
                    logger.debug("Component={} does not implement Mq2tPollingComponent. Skipped.", componentName);
                    continue;
                }
                readAndPublish(component);
            }
            logger.debug("Pause polling");
        }
    }
}
