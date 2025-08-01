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
import ru.maxeltr.homeMq2t.Model.Dashboard;
import ru.maxeltr.homeMq2t.Repository.CardRepository;
import ru.maxeltr.homeMq2t.Repository.CommandRepository;
import ru.maxeltr.homeMq2t.Repository.ComponentRepository;
import ru.maxeltr.homeMq2t.Repository.StartupTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import ru.maxeltr.homeMq2t.Entity.DashboardEntity;
import ru.maxeltr.homeMq2t.Model.CardImpl;
import ru.maxeltr.homeMq2t.Model.CardModel;
import ru.maxeltr.homeMq2t.Model.CardSettingsImpl;
import ru.maxeltr.homeMq2t.Model.DashboardImpl;
import ru.maxeltr.homeMq2t.Repository.DashboardRepository;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class AppProperties {

    @Autowired
    private Environment env;

    private final static String ERROR_TOPIC = "mq2t/error";

    public final static List<String> MEDIA_TYPES = Arrays.asList(MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE, "image/jpeg;base64");

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private ComponentRepository componentRepository;

    @Autowired
    private CommandRepository commandRepository;

    @Autowired
    private StartupTaskRepository startupTaskRepository;

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
    public String getCommandPubDataType(String name) {
        return commandRepository.findByName(name).map(CommandEntity::getPublicationDataType).orElse(MediaType.TEXT_PLAIN_VALUE);
    }

    /**
     * Retrieves the command path associated with the specified command.
     *
     * @param name the name of the command for which to retrieve the path path
     * @return the command path if found, or an empty string.
     */
    public String getCommandPath(String name) {
        return commandRepository.findByName(name).map(CommandEntity::getPath).orElse("");
    }

    /**
     * Retrieves the arguments associated with the specified command.
     *
     * @param name the name of the command for which to retrieve arguments
     * @return the arguments if found, or an empty string.
     */
    public String getCommandArguments(String name) {
        return commandRepository.findByName(name).map(CommandEntity::getArguments).orElse("");
    }

    /**
     * Retrieves the list of card numbers associated with a specific
     * subscriptoin topic.
     *
     * The metod searches for card numbers that are subscribed to the given
     * topic within the dashboard.
     *
     * @param topic the subscription topic for which to retrieve card numbers.
     * @return a list of card numbers subscribed to the specified topic, returns
     * empty list if no cards are found for the topic.
     */
    public List<String> getCardNumbersByTopic(String topic) {
        List<String> cardNumbers = new ArrayList<>();
        List<CardEntity> cards = cardRepository.findBySubscriptionTopic(topic);
        cards.forEach(card -> {
            cardNumbers.add(String.valueOf(card.getNumber()));
        });

        return cardNumbers;
    }

    private Optional<Integer> safeParseInt(String number) {
        try {
            return Optional.ofNullable(number).filter(StringUtils::isNotBlank).map(Integer::valueOf);

        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Retrieves the name of a card based on its number.
     *
     * @param number the number of the card whose name is to be retrieved.
     * @return the name of the card associated with the specified number,
     * returns an empty string if card name is not found.
     */
    public String getCardName(String number) {
        return safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getName).orElse("");
    }

    public Optional<CardEntity> getCardEntity(String number) {
        return safeParseInt(number).flatMap(cardRepository::findByNumber);
    }

    public Optional<DashboardEntity> getDashboardEntity(String number) {
        return safeParseInt(number).flatMap(dashboardRepository::findByNumber);
    }

    public CardEntity saveCardEntity(CardEntity cardEntity) {
        return this.cardRepository.save(cardEntity);
    }

    public Optional<CardModel> getCardSettings(String number) {
        String cardSettingsPathname = env.getProperty("card-settings-template-path", "");
        if (StringUtils.isEmpty(cardSettingsPathname)) {
            logger.info("No value defined for card settings template pathname.");
            return Optional.empty();
        }

        Optional<CardEntity> cardEntity = safeParseInt(number).flatMap(cardRepository::findByNumber);
        if (cardEntity.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new CardSettingsImpl(cardEntity.get(), cardSettingsPathname, this.getDashboards(), MEDIA_TYPES));
    }

    public Optional<Dashboard> getStartDashboard() {
        return this.getDashboards().stream().findFirst();
    }

    public Optional<CardModel> getEmptyCardSettings() {
        String cardSettingsPathname = env.getProperty("card-settings-template-path", "");
        if (StringUtils.isEmpty(cardSettingsPathname)) {
            logger.error("No value defined for card settings template pathname.");
            return Optional.empty();
        }

        Optional<Dashboard> startDashboardOpt = this.getStartDashboard();
        if (startDashboardOpt == null) {
            logger.error("No start dashboards found.");
            return Optional.empty();
        }

        Optional<DashboardEntity> startDashboardEntityOpt = safeParseInt(startDashboardOpt.get().getNumber()).flatMap(dashboardRepository::findByNumber);
        if (startDashboardEntityOpt.isEmpty()) {
            return Optional.empty();
        }

        CardEntity cardEntity = new CardEntity();
        cardEntity.setName("default");
        cardEntity.setSubscriptionQos("AT_MOST_ONCE");	//TODO use MqttQoS
        cardEntity.setSubscriptionDataType(MediaType.TEXT_PLAIN_VALUE);
        cardEntity.setPublicationQos("AT_MOST_ONCE");
        cardEntity.setPublicationRetain(false);
        cardEntity.setPublicationDataType(MediaType.TEXT_PLAIN_VALUE);
        cardEntity.setLocalTaskDataType(MediaType.TEXT_PLAIN_VALUE);
        cardEntity.setDashboard(startDashboardEntityOpt.get());

        return Optional.of(new CardSettingsImpl(cardEntity, cardSettingsPathname, this.getDashboards(), MEDIA_TYPES));
    }

    /**
     * Retrieves the card number associated with the specified name.
     *
     * @param name the name associated with the card number
     * @return the card number if found, or an empty string.
     */
    public String getCardNumber(String name) {
        return cardRepository.findByName(name).map(CardEntity::getNumber).map(String::valueOf).orElse("");
    }

    /**
     * Retrieves the subscription topic associated with the specified card
     * number.
     *
     * @param number the number of the card for which to retrieve the
     * subscription topic
     * @return the subscription topic if found, or an empty string.
     */
    public String getCardSubTopic(String number) {
        return safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getSubscriptionTopic).orElse("");
    }

    /**
     * Retrieves the Quality of Service (QoS) level associated with the
     * specified card number for subscription.
     *
     * @param number the number of the card for which to retrieve the
     * subscription QoS level
     * @return the subscription QoS level if found, or "AT_MOST_ONCE".
     */
    public String getCardSubQos(String number) {
        return safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getSubscriptionQos).orElse("AT_MOST_ONCE");
    }

    /**
     * Retrieves the subscription data name associated with the specified card
     * number.
     *
     * @param number the number of the card for which to retrieve the
     * subscription data name
     * @return the subscription data name if found, or an empty string.
     */
    public String getCardSubDataName(String number) {
        return safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getSubscriptionDataName).orElse("");
    }

    /**
     * Retrieves the subscription data type associated with the specified card
     * number.
     *
     * @param number the number of the card for which to retrieve the
     * subscription data type
     * @return the subscription data type if found, or an empty string.
     */
    public String getCardSubDataType(String number) {
        return safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getSubscriptionDataType).orElse("");
    }

    /**
     * Retrieves the jsonpath expression associated with the specified card
     * number.
     *
     * @param number the number of the card for which to retrieve the jsonpath
     * expression
     * @return the jsonpath expression if found, or an empty string.
     */
    public String getCardJsonPathExpression(String number) {
        return safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getDisplayDataJsonpath).orElse("");
    }

    /**
     * Retrieves the publication topic associated with the specified card
     * number.
     *
     * @param number the number of the card for which to retrieve the
     * publication topic
     * @return the publication topic if found, or an empty string.
     */
    public String getCardPubTopic(String number) {
        return safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getPublicationTopic).orElse("");
    }

    /**
     * Retrieves the Quality of Service (QoS) level associated with the
     * specified card number for publication.
     *
     * @param number the number of the card for which to retrieve the
     * publication QoS level
     * @return the publication QoS level if found, or "AT_MOST_ONCE".
     */
    public String getCardPubQos(String number) {
        return safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getPublicationQos).orElse("AT_MOST_ONCE");
    }

    /**
     * Retrieves the retain flag associated with the specified card number for
     * publication.
     *
     * @param number the number of the card for which to retrieve the
     * publication retain flag
     * @return the publication retain flag if found, or "false".
     */
    public String getCardPubRetain(String number) {
        return safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getPublicationRetain).map(String::valueOf).orElse("false");
    }

    /**
     * Retrieves the publication data associated with the specified card number.
     *
     * @param number the number of the card for which to retrieve the
     * publication data
     * @return the publication data if found, or an empty string.
     */
    public String getCardPubData(String number) {
        return safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getPublicationData).orElse("");
    }

    /**
     * Retrieves the publication data type associated with the specified card
     * number.
     *
     * @param number the number of the card for which to retrieve the
     * publication data type
     * @return the publication data type if found, or an empty string.
     */
    public String getCardPubDataType(String number) {
        return safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getPublicationDataType).orElse("");
    }

    /**
     * Retrieves the local task path associated with the specified card number.
     *
     * @param number the number of the card for which to retrieve the local task
     * path
     * @return the local task path if found, or an empty string.
     */
    public String getCardLocalTaskPath(String number) {
        return safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getLocalTaskPath).orElse("");
    }

    /**
     * Retrieves the local task arguments associated with the specified card
     * number.
     *
     * @param number the number of the card for which to retrieve the local task
     * arguments
     * @return the local task arguments if found, or an empty string.
     */
    public String getCardLocalTaskArguments(String number) {
        return safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getLocalTaskArguments).orElse("");
    }

    /**
     * Retrieves the local task data type associated with the specified card
     * number.
     *
     * @param number the number of the card for which to retrieve the local task
     * data type
     * @return the local task data type if found, or an empty string.
     */
    public String getCardLocalTaskDataType(String number) {
        return safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getLocalTaskDataType).orElse("");
    }

    /**
     * Retrieves the name of a component based on its number.
     *
     * @param number the number of the card whose name is to be retrieved.
     * @return the name of the component associated with the specified number,
     * returns an empty string if component name is not found.
     */
    public String getComponentName(String number) {
        return safeParseInt(number).flatMap(componentRepository::findByNumber).map(ComponentEntity::getName).orElse("");
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
    public String getComponentProviderArgs(String name) {
        return componentRepository.findByName(name).map(ComponentEntity::getProviderArgs).orElse("");
    }

    /**
     * Creates a list of dashboards based on the configuration properties
     * defined in the database. Each dashboard can contain multiple cards, which
     * are defined in the configuration.
     *
     * @return a list of dashboards created from configuration. If no dashboards
     * are defined, an empty list is returned.
     */
    public List<Dashboard> getDashboards() {
        List<Dashboard> dashboards = new ArrayList<>();

        String dashboardPathname = env.getProperty("dashboard-template-path", "");
        if (StringUtils.isEmpty(dashboardPathname)) {
            logger.info("No value defined for dashboard template pathname.");
            return dashboards;
        }

        String cardPathname = env.getProperty("card-template-path", "");
        if (StringUtils.isEmpty(cardPathname)) {
            logger.info("No value defined for card template pathname.");
            return dashboards;
        }

        List<DashboardEntity> dashboardEntities = dashboardRepository.findAll();
        dashboardEntities.forEach(dashboardEntity -> {
            List<CardModel> cards = new ArrayList<>();
            List<CardEntity> cardEntities = cardRepository.findByDashboardNumber(dashboardEntity.getNumber());
            cardEntities.forEach(cardEntity -> {
                CardModel card = new CardImpl(cardEntity, cardPathname);
                cards.add(card);
                logger.info("Card={} has been created and added to card list. Number={}", card.getName(), card.getCardNumber());
            });
            logger.info("Create card list with size={}.", cards.size());
            Dashboard dashboard = new DashboardImpl(String.valueOf(dashboardEntity.getNumber()), dashboardEntity.getName(), cards, dashboardPathname);
            dashboards.add(dashboard);
            logger.info("Dashboard={} has been created and added to dashboard list.", dashboard.getName());
        });

        logger.info("Create dashbord list with size={}.", dashboards.size());

        return dashboards;
    }

    /**
     * Creates a list of Mqtt topic subscriptions.
     *
     * @return a list of Mqtt topic subscriptions.
     */
    public List<MqttTopicSubscription> getSubscriptions() {
        List<MqttTopicSubscription> subscriptions = new ArrayList<>();

        List<CardEntity> cardEntities = cardRepository.findAll();
        cardEntities.forEach(cardEntity -> {
            if (StringUtils.isNotBlank(cardEntity.getSubscriptionTopic())) {
                subscriptions.add(new MqttTopicSubscription(cardEntity.getSubscriptionTopic(), convertToMqttQos(cardEntity.getSubscriptionQos())));
                logger.info("Add subscription={} with qos={} to subscription list.", cardEntity.getSubscriptionTopic(), cardEntity.getSubscriptionQos());
            }
        });

        List<CommandEntity> commandEntities = commandRepository.findAll();
        commandEntities.forEach(commandEntity -> {
            if (StringUtils.isNotBlank(commandEntity.getSubscriptionTopic())) {
                subscriptions.add(new MqttTopicSubscription(commandEntity.getSubscriptionTopic(), convertToMqttQos(commandEntity.getSubscriptionQos())));
                logger.info("Add subscription={} with qos={} to subscription list.", commandEntity.getSubscriptionTopic(), commandEntity.getSubscriptionQos());
            }
        });

        List<ComponentEntity> componentEntities = componentRepository.findAll();
        componentEntities.forEach(componentEntity -> {
            if (StringUtils.isNotBlank(componentEntity.getSubscriptionTopic())) {
                subscriptions.add(new MqttTopicSubscription(componentEntity.getSubscriptionTopic(), convertToMqttQos(componentEntity.getSubscriptionQos())));
                logger.info("Add subscription={} with qos={} to subscription list.", componentEntity.getSubscriptionTopic(), componentEntity.getSubscriptionQos());
            }
        });

        return subscriptions;
    }

    /**
     * Convert the given qos value from string to MqttQos enum instance. If the
     * qos value is invalid, it defaults to qos level 0.
     *
     * @param qosString The qos value as a string. Must not be null.
     * @return The qos level as a MqttQos enum value.
     */
    private MqttQoS convertToMqttQos(String qosString) {		//TODO move to mqtt package as static
        MqttQoS qos;
        try {
            qos = MqttQoS.valueOf(qosString);
        } catch (IllegalArgumentException ex) {
            logger.error("Invalid QoS value for the given qos string={}: {}. Set QoS=0.", qosString, ex.getMessage());
            qos = MqttQoS.AT_MOST_ONCE;
        }

        return qos;
    }
}
