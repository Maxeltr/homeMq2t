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

import io.netty.handler.codec.mqtt.MqttQoS;
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
import ru.maxeltr.homeMq2t.Entity.CommandEntity;
import ru.maxeltr.homeMq2t.Model.CommandSettingsImpl;
import ru.maxeltr.homeMq2t.Model.ViewModel;
import ru.maxeltr.homeMq2t.Mqtt.MqttUtils;
import ru.maxeltr.homeMq2t.Repository.CommandRepository;
import ru.maxeltr.homeMq2t.Service.UI.DashboardItemCardManagerImpl;
import ru.maxeltr.homeMq2t.Utils.AppUtils;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class CommandPropertiesProviderImpl implements CommandPropertiesProvider {

    private static final Logger logger = LoggerFactory.getLogger(CommandPropertiesProviderImpl.class);

    @Autowired
    private Environment env;

    @Autowired
    private CommandRepository commandRepository;

    /**
     * Persist the given CommandEntity
     *
     * @param commandEntity The CommandEntity to save
     * @return the saved CommandEntity (may include updated fields such as
     * generated id)
     */
    @Override
    public CommandEntity saveCommandEntity(CommandEntity commandEntity) {
        return this.commandRepository.save(commandEntity);
    }

    /**
     * Deletes a command by its id
     *
     * @param id The id of the command to delete
     */
    @Override
    public void deleteCommand(String id) {
        this.commandRepository.deleteById(Long.valueOf(id));
    }

    /**
     * Build view model for command settings for a command identified by number.
     *
     * The method reads the configured command settings template pathname from
     * the environment.
     *
     * @param number The command number as a String
     * @return an Optional containing the ViewModel for the command settings. If
     * not configured or the command cannot be found, an empty Optional is
     * returned.
     */
    @Override
    public Optional<ViewModel> getCommandSettings(String number) {
        String commandSettingsPathname = env.getProperty(CommandPropertiesProvider.COMMAND_SETTINGS_TEMPLATE_PATH, "");
        if (StringUtils.isEmpty(commandSettingsPathname)) {
            logger.info("No value defined for command settings template pathname.");
            return Optional.empty();
        }

        Optional<CommandEntity> commandEntity = AppUtils.safeParseInt(number).flatMap(commandRepository::findByNumber);
        if (commandEntity.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new CommandSettingsImpl(commandEntity.get(), commandSettingsPathname, MEDIA_TYPES));
    }

    /**
     * Creates an empty/default command settings ViewModel based on the
     * configured template.
     *
     * @return an Optional containing the VeiwModel for the empty/default
     * command settings-template-path
     */
    @Override
    public Optional<ViewModel> getEmptyCommandSettings() {
        String commandSettingsPathname = env.getProperty(CommandPropertiesProvider.COMMAND_SETTINGS_TEMPLATE_PATH, "");
        if (StringUtils.isEmpty(commandSettingsPathname)) {
            logger.error("No value defined for command settings template pathname.");
            return Optional.empty();
        }

        CommandEntity CommandEntity = new CommandEntity();
        CommandEntity.setName("default");
        CommandEntity.setSubscriptionQos("AT_MOST_ONCE");	//TODO use MqttQoS
        CommandEntity.setPublicationQos("AT_MOST_ONCE");
        CommandEntity.setPublicationRetain(false);
        CommandEntity.setPublicationDataType(MediaType.TEXT_PLAIN_VALUE);

        return Optional.of(new CommandSettingsImpl(CommandEntity, commandSettingsPathname, MEDIA_TYPES));
    }

    /**
     * Retrieves the list of command numbers associated with a specific
     * subscriptoin topic.
     *
     * The metod searches for command numbers that are subscribed to the given
     * topic.
     *
     * @param topic the subscription topic for which to retrieve command
     * numbers.
     * @return a list of command numbers subscribed to the specified topic,
     * returns empty list if no command numbers are found for the topic.
     */
    @Override
    public List<String> getCommandNumbersByTopic(String topic) {
        List<String> commandNumbers = new ArrayList<>();
        List<CommandEntity> commands = commandRepository.findBySubscriptionTopic(topic);
        commands.forEach(command -> {
            commandNumbers.add(String.valueOf(command.getNumber()));
        });

        return commandNumbers;
    }

    /**
     * Retrieves the publication topic associated with the specified command.
     *
     * @param name the name of the command for which to retrieve the publication
     * topic
     * @return the publication topic if found, or an empty string.
     */
    @Override
    public String getCommandPubTopic(String name) {
        return commandRepository.findByName(name).map(CommandEntity::getPublicationTopic).orElse("");
    }

    /**
     * Retrieves the Quality of Service (QoS) level associated with the
     * specified command for publication.
     *
     * @param name the name of the command for which to retrieve the publication
     * QoS level
     * @return the publication QoS level if found, or "AT_MOST_ONCE".
     */
    @Override
    public String getCommandPubQos(String name) {
        return commandRepository.findByName(name).map(CommandEntity::getPublicationQos).orElse("AT_MOST_ONCE");
    }

    /**
     * Retrieves the retain flag associated with the specified command for
     * publication.
     *
     * @param name the name of the command for which to retrieve the publication
     * retain flag
     * @return the publication retain flag if found, or "false".
     */
    @Override
    public String getCommandPubRetain(String name) {
        return commandRepository.findByName(name).map(CommandEntity::getPublicationRetain).map(String::valueOf).orElse("false");
    }

    /**
     * Retrieves the publication data type associated with the specified
     * command.
     *
     * @param name the name of the command for which to retrieve the publication
     * data type
     * @return the publication data type if found, or an empty string.
     */
    @Override
    public String getCommandPubDataType(String name) {
        return commandRepository.findByName(name).map(CommandEntity::getPublicationDataType).orElse(MediaType.TEXT_PLAIN_VALUE);
    }

    /**
     * Retrieves the command path associated with the specified command.
     *
     * @param name the name of the command for which to retrieve the path path
     * @return the command path if found, or an empty string.
     */
    @Override
    public String getCommandPath(String name) {
        return commandRepository.findByName(name).map(CommandEntity::getPath).orElse("");
    }

    /**
     * Retrieves the arguments associated with the specified command.
     *
     * @param name the name of the command for which to retrieve arguments
     * @return the arguments if found, or an empty string.
     */
    @Override
    public String getCommandArguments(String name) {
        return commandRepository.findByName(name).map(CommandEntity::getArguments).orElse("");
    }

    /**
     * Creates a list of Mqtt topic subscriptions.
     *
     * @return a list of Mqtt topic subscriptions.
     */
    public List<MqttTopicSubscription> getAllSubscriptions() {
        List<MqttTopicSubscription> subscriptions = new ArrayList<>();

        List<CommandEntity> commandEntities = commandRepository.findAll();
        commandEntities.forEach(commandEntity -> {
            if (StringUtils.isNotBlank(commandEntity.getSubscriptionTopic())) {
                subscriptions.add(new MqttTopicSubscription(commandEntity.getSubscriptionTopic(), MqttUtils.convertToMqttQos(commandEntity.getSubscriptionQos())));
                logger.info("Add subscription={} with qos={} to subscription list.", commandEntity.getSubscriptionTopic(), commandEntity.getSubscriptionQos());
            }
        });

        return subscriptions;
    }

}
