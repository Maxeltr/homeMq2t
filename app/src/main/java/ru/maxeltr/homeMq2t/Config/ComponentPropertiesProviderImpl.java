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
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import static ru.maxeltr.homeMq2t.Config.AppProperties.MEDIA_TYPES;
import ru.maxeltr.homeMq2t.Entity.ComponentEntity;
import ru.maxeltr.homeMq2t.Model.ComponentSettingsImpl;
import ru.maxeltr.homeMq2t.Model.ViewModel;
import ru.maxeltr.homeMq2t.Mqtt.MqttUtils;
import ru.maxeltr.homeMq2t.Repository.ComponentRepository;
import ru.maxeltr.homeMq2t.Utils.AppUtils;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class ComponentPropertiesProviderImpl implements ComponentPropertiesProvider {

    private static final Logger logger = LoggerFactory.getLogger(ComponentPropertiesProviderImpl.class);

    @Autowired
    private Environment env;

    @Autowired
    private ComponentRepository componentRepository;

    @Override
    public ComponentEntity saveComponentEntity(ComponentEntity entity) {
        return this.componentRepository.save(entity);
    }

    @Override
    public void deleteComponent(String id) {
        this.componentRepository.deleteById(Long.valueOf(id));
    }

    @Override
    public Optional<ViewModel> getComponentSettings(String number) {
        String commandSettingsPathname = env.getProperty(ComponentPropertiesProvider.COMPONENT_SETTINGS_TEMPLATE_PATH, "");
        if (StringUtils.isEmpty(commandSettingsPathname)) {
            logger.info("No value defined for component settings template pathname.");
            return Optional.empty();
        }

        Optional<ComponentEntity> componentEntity = AppUtils.safeParseInt(number).flatMap(componentRepository::findByNumber);
        if (componentEntity.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new ComponentSettingsImpl(componentEntity.get(), commandSettingsPathname, MEDIA_TYPES));
    }

    @Override
    public Optional<ViewModel> getEmptyComponentSettings() {
        String componentSettingsPathname = env.getProperty(ComponentPropertiesProvider.COMPONENT_SETTINGS_TEMPLATE_PATH, "");
        if (StringUtils.isEmpty(componentSettingsPathname)) {
            logger.error("No value defined for component settings template pathname.");
            return Optional.empty();
        }

        ComponentEntity componentEntity = new ComponentEntity();
        componentEntity.setName("default");
        componentEntity.setSubscriptionQos("AT_MOST_ONCE");	//TODO use MqttQoS
        componentEntity.setPublicationQos("AT_MOST_ONCE");
        componentEntity.setPublicationRetain(false);
        componentEntity.setPublicationDataType(MediaType.TEXT_PLAIN_VALUE);

        return Optional.of(new ComponentSettingsImpl(componentEntity, componentSettingsPathname, MEDIA_TYPES));
    }

    /**
     * Retrieves the name of a component based on its number.
     *
     * @param number the number of the card whose name is to be retrieved.
     * @return the name of the component associated with the specified number,
     * returns an empty string if component name is not found.
     */
    @Override
    public String getComponentName(String number) {
        return AppUtils.safeParseInt(number).flatMap(componentRepository::findByNumber).map(ComponentEntity::getName).orElse("");
    }

    /**
     * Retrieves the list of component numbers associated with a specific
     * subscription topic.
     *
     * The metod searches for component numbers that are subscribed to the given
     * topic.
     *
     * @param topic the subscription topic for which to retrieve component
     * numbers.
     * @return a list of component numbers subscribed to the specified topic,
     * returns empty list if no components are found for the topic.
     */
    @Override
    public List<String> getComponentNumbersByTopic(String topic) {
        List<String> componentNumbers = new ArrayList<>();
        List<ComponentEntity> components = componentRepository.findBySubscriptionTopic(topic);
        components.forEach(component -> {
            componentNumbers.add(String.valueOf(component.getNumber()));
        });

        return componentNumbers;
    }

    /**
     * Retrieves the publication topic associated with the specified component
     * name.
     *
     * @param name the name of the component for which to retrieve the
     * publication topic
     * @return the publication topic if found, or an empty string.
     */
    public String getComponentPubTopic(String name) {
        return componentRepository.findByName(name).map(ComponentEntity::getPublicationTopic).orElse("");
    }

    /**
     * Retrieves the Quality of Service (QoS) level associated with the
     * specified component for publication.
     *
     * @param name the name of the component for which to retrieve the
     * publication QoS level
     * @return the publication QoS level if found, or "AT_MOST_ONCE".
     */
    @Override
    public String getComponentPubQos(String name) {
        return componentRepository.findByName(name).map(ComponentEntity::getPublicationQos).orElse("AT_MOST_ONCE");
    }

    /**
     * Retrieves the retain flag associated with the specified component for
     * publication.
     *
     * @param name the name of the component for which to retrieve the
     * publication retain flag
     * @return the publication retain flag if found, or "false".
     */
    @Override
    public String getComponentPubRetain(String name) {
        return componentRepository.findByName(name).map(ComponentEntity::getPublicationRetain).map(String::valueOf).orElse("false");
    }

    /**
     * Retrieves the publication data type associated with the specified
     * component.
     *
     * @param name the name of the component for which to retrieve the
     * publication data type
     * @return the publication data type if found, or an empty string.
     */
    @Override
    public String getComponentPubDataType(String name) {
        return componentRepository.findByName(name).map(ComponentEntity::getPublicationDataType).orElse(MediaType.TEXT_PLAIN_VALUE);
    }

    /**
     * Retrieves the card of the local dashboard associated with the specified
     * component.
     *
     * @param name the name of the component for which to retrieve the local
     * card path
     * @return the local card number if found, or an empty string.
     */
    @Override
    public String getComponentPubLocalCard(String name) {
        return componentRepository.findByName(name).map(ComponentEntity::getPublicationLocalCardId).orElse("");
    }

    /**
     * Retrieves the provider for a specified component.
     *
     * @param name the name of the component for which to retrieve the provider
     * path
     * @return the provider name if found, or an empty string.
     */
    @Override
    public String getComponentProvider(String name) {
        return componentRepository.findByName(name).map(ComponentEntity::getProvider).orElse("");
    }

    /**
     * Retrieves the provider arguments associated with the specified component.
     *
     * @param name the name of the component for which to retrieve the provider
     * arguments
     * @return the provider arguments if found, or an empty string.
     */
    @Override
    public String getComponentProviderArgs(String name) {
        return componentRepository.findByName(name).map(ComponentEntity::getProviderArgs).orElse("");
    }

    /**
     * Creates a list of Mqtt topic subscriptions.
     *
     * @return a list of Mqtt topic subscriptions.
     */
    public List<MqttTopicSubscription> getAllSubscriptions() {
        List<MqttTopicSubscription> subscriptions = new ArrayList<>();

        List<ComponentEntity> componentEntities = componentRepository.findAll();
        componentEntities.forEach(componentEntity -> {
            if (StringUtils.isNotBlank(componentEntity.getSubscriptionTopic())) {
                subscriptions.add(new MqttTopicSubscription(componentEntity.getSubscriptionTopic(), MqttUtils.convertToMqttQos(componentEntity.getSubscriptionQos())));
                logger.info("Add subscription={} with qos={} to subscription list.", componentEntity.getSubscriptionTopic(), componentEntity.getSubscriptionQos());
            }
        });

        return subscriptions;
    }

}
