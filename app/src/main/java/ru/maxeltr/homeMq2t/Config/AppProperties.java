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
package ru.maxeltr.homeMq2t.Config;

import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import ru.maxeltr.homeMq2t.Entity.CardEntity;
import ru.maxeltr.homeMq2t.Entity.CommandEntity;
import ru.maxeltr.homeMq2t.Entity.ComponentEntity;
import ru.maxeltr.homeMq2t.Entity.StartupTaskEntity;
import ru.maxeltr.homeMq2t.Repository.CardRepository;
import ru.maxeltr.homeMq2t.Repository.CommandRepository;
import ru.maxeltr.homeMq2t.Repository.ComponentRepository;
import ru.maxeltr.homeMq2t.Repository.StartupTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import ru.maxeltr.homeMq2t.Entity.MqttSettingsEntity;
import ru.maxeltr.homeMq2t.Model.MqttSettingsImpl;
import ru.maxeltr.homeMq2t.Model.ViewModel;
import ru.maxeltr.homeMq2t.Mqtt.MqttUtils;
import ru.maxeltr.homeMq2t.Repository.DashboardRepository;
import ru.maxeltr.homeMq2t.Repository.MqttSettingsRepository;
import ru.maxeltr.homeMq2t.Utils.AppUtils;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class AppProperties implements StartupTaskPropertiesProvider {

    private final static String NAME_OF_MQTT_SETTINGS = "Mqtt Settings";

    private final static String MQTT_SETTINGS_TEMPLATE_PATH = "mqtt-settings-template-path";

    @Autowired
    private Environment env;

    private final static String ERROR_TOPIC = "mq2t/error";

    public final static List<String> MEDIA_TYPES = Arrays.asList(MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE, MediaTypes.IMAGE_JPEG_BASE64.getValue(), MediaTypes.TEXT_HTML_BASE64.getValue());

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private ComponentRepository componentRepository;

    @Autowired
    private CommandRepository commandRepository;

    @Autowired
    private StartupTaskRepository startupTaskRepository;

    @Autowired
    private MqttSettingsRepository mqttSettingsRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    private final List<String> emptyArray = List.of();

    private static final Logger logger = LoggerFactory.getLogger(AppProperties.class);

    public List<StartupTaskEntity> getAllStartupTasks() {
        return startupTaskRepository.findAll();
    }

    /**
     * Retrieves the startup task number associated with the specified name.
     *
     * @param name the name associated with the startup task
     * @return the startup task number if found, or an empty string.
     */
    public String getStartupTaskNumber(String name) {
        return startupTaskRepository.findByName(name).map(StartupTaskEntity::getNumber).map(String::valueOf).orElse("");
    }

    /**
     * Retrieves the startup task arguments associated with the specified
     * startup task.
     *
     * @param name the name associated with the startup task
     * @return the startup task arguments if found, or an empty string.
     */
    public String getStartupTaskArguments(String name) {
        return startupTaskRepository.findByName(name).map(StartupTaskEntity::getArguments).orElse("");
    }

    /**
     * Retrieves the startup task path associated with the specified startup
     * task.
     *
     * @param name the name associated with the startup task
     * @return the startup task path if found, or an empty string.
     */
    public String getStartupTaskPath(String name) {
        return startupTaskRepository.findByName(name).map(StartupTaskEntity::getPath).orElse("");
    }

    /**
     * Creates a list of Mqtt topic subscriptions.
     *
     * @return a list of Mqtt topic subscriptions.
     */
    public List<MqttTopicSubscription> getAllSubscriptions() {
        List<MqttTopicSubscription> subscriptions = new ArrayList<>();

        List<CardEntity> cardEntities = cardRepository.findAll();
        cardEntities.forEach(cardEntity -> {
            if (StringUtils.isNotBlank(cardEntity.getSubscriptionTopic())) {
                subscriptions.add(new MqttTopicSubscription(cardEntity.getSubscriptionTopic(), MqttUtils.convertToMqttQos(cardEntity.getSubscriptionQos())));
                logger.info("Add subscription={} with qos={} to subscription list.", cardEntity.getSubscriptionTopic(), cardEntity.getSubscriptionQos());
            }
        });

        List<CommandEntity> commandEntities = commandRepository.findAll();
        commandEntities.forEach(commandEntity -> {
            if (StringUtils.isNotBlank(commandEntity.getSubscriptionTopic())) {
                subscriptions.add(new MqttTopicSubscription(commandEntity.getSubscriptionTopic(), MqttUtils.convertToMqttQos(commandEntity.getSubscriptionQos())));
                logger.info("Add subscription={} with qos={} to subscription list.", commandEntity.getSubscriptionTopic(), commandEntity.getSubscriptionQos());
            }
        });

        List<ComponentEntity> componentEntities = componentRepository.findAll();
        componentEntities.forEach(componentEntity -> {
            if (StringUtils.isNotBlank(componentEntity.getSubscriptionTopic())) {
                subscriptions.add(new MqttTopicSubscription(componentEntity.getSubscriptionTopic(), MqttUtils.convertToMqttQos(componentEntity.getSubscriptionQos())));
                logger.info("Add subscription={} with qos={} to subscription list.", componentEntity.getSubscriptionTopic(), componentEntity.getSubscriptionQos());
            }
        });

        return subscriptions;
    }

    public Optional<MqttSettingsEntity> getMqttSettingsEntity() {
        return mqttSettingsRepository.findByName(NAME_OF_MQTT_SETTINGS);
    }

    public Optional<ViewModel> getMqttSettings(String name) {
        String templatePathname = env.getProperty(MQTT_SETTINGS_TEMPLATE_PATH, "");
        if (StringUtils.isEmpty(templatePathname)) {
            logger.info("No value defined for mqtt settings template pathname.");
            return Optional.empty();
        }

        Optional<MqttSettingsEntity> mqttEntity = mqttSettingsRepository.findByName(name);
        if (mqttEntity.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new MqttSettingsImpl(mqttEntity.get(), templatePathname));
    }

    public Optional<ViewModel> getEmptyMqttSettings() {
        String templatePathname = env.getProperty(MQTT_SETTINGS_TEMPLATE_PATH, "");
        if (StringUtils.isEmpty(templatePathname)) {
            logger.error("No value defined for mqtt settings template pathname.");
            return Optional.empty();
        }

        MqttSettingsEntity mqttSettingsEntity = new MqttSettingsEntity();
        mqttSettingsEntity.setName("default");
        mqttSettingsEntity.setHost("");
        mqttSettingsEntity.setPort("");
        mqttSettingsEntity.setMq2tPassword("");
        mqttSettingsEntity.setMq2tUsername("");
        mqttSettingsEntity.setClientId("");
        mqttSettingsEntity.setHasUsername(false);
        mqttSettingsEntity.setHasPassword(false);
        mqttSettingsEntity.setWillQos("");
        mqttSettingsEntity.setWillRetain(false);
        mqttSettingsEntity.setWillFlag(false);
        mqttSettingsEntity.setCleanSession(false);
        mqttSettingsEntity.setAutoConnect(false);
        mqttSettingsEntity.setWillTopic("");
        mqttSettingsEntity.setWillMessage("");
        mqttSettingsEntity.setReconnect(false);

        return Optional.of(new MqttSettingsImpl(mqttSettingsEntity, templatePathname));
    }
}
