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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import static ru.maxeltr.homeMq2t.Config.AppProperties.MEDIA_TYPES;
import ru.maxeltr.homeMq2t.Entity.CardEntity;
import ru.maxeltr.homeMq2t.Entity.DashboardEntity;
import ru.maxeltr.homeMq2t.Model.CardSettingsImpl;
import ru.maxeltr.homeMq2t.Model.Dashboard;
import ru.maxeltr.homeMq2t.Model.ViewModel;
import ru.maxeltr.homeMq2t.Mqtt.MqttUtils;
import ru.maxeltr.homeMq2t.Repository.CardRepository;
import ru.maxeltr.homeMq2t.Repository.DashboardRepository;
import ru.maxeltr.homeMq2t.Utils.AppUtils;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class CardPropertiesProviderImpl implements CardPropertiesProvider {

    private static final Logger logger = LoggerFactory.getLogger(CardPropertiesProviderImpl.class);

    @Autowired
    private Environment env;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    @Qualifier("getDashboardPropertiesProvider")
    private DashboardPropertiesProvider dashboardPropertiesProvider;

    /**
     * Retrieves the CardEntity for the specified card number.
     *
     * @param number The card number as a String
     * @return an Optional containing the CardEntity if found, otherwise an
     * empty Optional
     */
    @Override
    public Optional<CardEntity> getCardEntity(String number) {
        return AppUtils.safeParseInt(number).flatMap(cardRepository::findByNumber);
    }

    /**
     * Persist the given CardEntity
     *
     * @param cardEntity The CardEntity to save
     * @return the saved CardEntity (may include updated fields such as
     * generated id)
     */
    @Override
    public CardEntity saveCardEntity(CardEntity cardEntity) {
        return this.cardRepository.save(cardEntity);
    }

    /**
     * Deletes a card by its id
     *
     * @param id The id of the card to delete
     */
    @Override
    public void deleteCard(String id) {
        this.cardRepository.deleteById(Long.valueOf(id));
    }

    /**
     * Build view model for card settings for a card identified by number.
     *
     * The method reads the configured card settings template pathname from the
     * environment.
     *
     * @param number The card number as a String
     * @return an Optional containing the ViewModel for the card settings. If
     * not configured or the card cannot be found, an empty Optional is
     * returned.
     */
    @Override
    public Optional<ViewModel> getCardSettings(String number) {
        String cardSettingsPathname = env.getProperty(CardPropertiesProvider.CARD_SETTINGS_TEMPLATE_PATH, "");
        if (StringUtils.isEmpty(cardSettingsPathname)) {
            logger.info("No value defined for card settings template pathname.");
            return Optional.empty();
        }

        Optional<CardEntity> cardEntity = AppUtils.safeParseInt(number).flatMap(cardRepository::findByNumber);
        if (cardEntity.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new CardSettingsImpl(cardEntity.get(), cardSettingsPathname, dashboardPropertiesProvider.getAllDashboards(), MEDIA_TYPES));
    }

    /**
     * Creates an empty/default card settings ViewModel based on the configured
     * template.
     *
     * @return an Optional containing the VeiwModel for the empty/default card
     * settings-template-path
     */
    @Override
    public Optional<ViewModel> getEmptyCardSettings() {
        String cardSettingsPathname = env.getProperty(CardPropertiesProvider.CARD_SETTINGS_TEMPLATE_PATH, "");
        if (StringUtils.isEmpty(cardSettingsPathname)) {
            logger.error("No value defined for card settings template pathname.");
            return Optional.empty();
        }

        Optional<ViewModel<DashboardEntity>> startDashboardOpt = dashboardPropertiesProvider.getStartDashboard();
        if (startDashboardOpt == null) {
            logger.error("No start dashboards found.");
            return Optional.empty();
        }

        Optional<DashboardEntity> startDashboardEntityOpt = AppUtils.safeParseInt(startDashboardOpt.get().getNumber()).flatMap(dashboardRepository::findByNumber);
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

        return Optional.of(new CardSettingsImpl(cardEntity, cardSettingsPathname, dashboardPropertiesProvider.getAllDashboards(), MEDIA_TYPES));
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
    @Override
    public List<String> getCardNumbersByTopic(String topic) {
        List<String> cardNumbers = new ArrayList<>();
        List<CardEntity> cards = cardRepository.findBySubscriptionTopic(topic);
        cards.forEach(card -> {
            cardNumbers.add(String.valueOf(card.getNumber()));
        });

        return cardNumbers;
    }

    /**
     * Retrieves the name of a card based on its number.
     *
     * @param number the number of the card whose name is to be retrieved.
     * @return the name of the card associated with the specified number,
     * returns an empty string if card name is not found.
     */
    @Override
    public String getCardName(String number) {
        return AppUtils.safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getName).orElse("");
    }

    /**
     * Retrieves the card number associated with the specified name.
     *
     * @param name the name associated with the card number
     * @return the card number if found, or an empty string.
     */
    @Override
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
    @Override
    public String getCardSubTopic(String number) {
        return AppUtils.safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getSubscriptionTopic).orElse("");
    }

    /**
     * Retrieves the Quality of Service (QoS) level associated with the
     * specified card number for subscription.
     *
     * @param number the number of the card for which to retrieve the
     * subscription QoS level
     * @return the subscription QoS level if found, or "AT_MOST_ONCE".
     */
    @Override
    public String getCardSubQos(String number) {
        return AppUtils.safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getSubscriptionQos).orElse("AT_MOST_ONCE");
    }

    /**
     * Retrieves the subscription data name associated with the specified card
     * number.
     *
     * @param number the number of the card for which to retrieve the
     * subscription data name
     * @return the subscription data name if found, or an empty string.
     */
    @Override
    public String getCardSubDataName(String number) {
        return AppUtils.safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getSubscriptionDataName).orElse("");
    }

    /**
     * Retrieves the subscription data type associated with the specified card
     * number.
     *
     * @param number the number of the card for which to retrieve the
     * subscription data type
     * @return the subscription data type if found, or an empty string.
     */
    @Override
    public String getCardSubDataType(String number) {
        return AppUtils.safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getSubscriptionDataType).orElse("");
    }

    /**
     * Retrieves the jsonpath expression associated with the specified card
     * number.
     *
     * @param number the number of the card for which to retrieve the jsonpath
     * expression
     * @return the jsonpath expression if found, or an empty string.
     */
    @Override
    public String getCardJsonPathExpression(String number) {
        return AppUtils.safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getDisplayDataJsonpath).orElse("");
    }

    /**
     * Retrieves the publication topic associated with the specified card
     * number.
     *
     * @param number the number of the card for which to retrieve the
     * publication topic
     * @return the publication topic if found, or an empty string.
     */
    @Override
    public String getCardPubTopic(String number) {
        return AppUtils.safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getPublicationTopic).orElse("");
    }

    /**
     * Retrieves the Quality of Service (QoS) level associated with the
     * specified card number for publication.
     *
     * @param number the number of the card for which to retrieve the
     * publication QoS level
     * @return the publication QoS level if found, or "AT_MOST_ONCE".
     */
    @Override
    public String getCardPubQos(String number) {
        return AppUtils.safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getPublicationQos).orElse("AT_MOST_ONCE");
    }

    /**
     * Retrieves the retain flag associated with the specified card number for
     * publication.
     *
     * @param number the number of the card for which to retrieve the
     * publication retain flag
     * @return the publication retain flag if found, or "false".
     */
    @Override
    public String getCardPubRetain(String number) {
        return AppUtils.safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getPublicationRetain).map(String::valueOf).orElse("false");
    }

    /**
     * Retrieves the publication data associated with the specified card number.
     *
     * @param number the number of the card for which to retrieve the
     * publication data
     * @return the publication data if found, or an empty string.
     */
    @Override
    public String getCardPubData(String number) {
        return AppUtils.safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getPublicationData).orElse("");
    }

    /**
     * Retrieves the publication data type associated with the specified card
     * number.
     *
     * @param number the number of the card for which to retrieve the
     * publication data type
     * @return the publication data type if found, or an empty string.
     */
    @Override
    public String getCardPubDataType(String number) {
        return AppUtils.safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getPublicationDataType).orElse("");
    }

    /**
     * Retrieves the local task path associated with the specified card number.
     *
     * @param number the number of the card for which to retrieve the local task
     * path
     * @return the local task path if found, or an empty string.
     */
    @Override
    public String getCardLocalTaskPath(String number) {
        return AppUtils.safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getLocalTaskPath).orElse("");
    }

    /**
     * Retrieves the local task arguments associated with the specified card
     * number.
     *
     * @param number the number of the card for which to retrieve the local task
     * arguments
     * @return the local task arguments if found, or an empty string.
     */
    @Override
    public String getCardLocalTaskArguments(String number) {
        return AppUtils.safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getLocalTaskArguments).orElse("");
    }

    /**
     * Retrieves the local task data type associated with the specified card
     * number.
     *
     * @param number the number of the card for which to retrieve the local task
     * data type
     * @return the local task data type if found, or an empty string.
     */
    @Override
    public String getCardLocalTaskDataType(String number) {
        return AppUtils.safeParseInt(number).flatMap(cardRepository::findByNumber).map(CardEntity::getLocalTaskDataType).orElse("");
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

        return subscriptions;
    }

}
