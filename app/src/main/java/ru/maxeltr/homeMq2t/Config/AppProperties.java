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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import ru.maxeltr.homeMq2t.Entity.CardEntity;
import ru.maxeltr.homeMq2t.Repository.CardRepository;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class AppProperties {

    @Autowired
    private Environment env;

    private final static String ERROR_TOPIC = "mq2t/error";

    @Autowired
    private Map<String, List<String>> topicsAndCards;

    @Autowired
    private Map<String, List<String>> topicsAndCommands;

    @Autowired
    private Map<String, List<String>> topicsAndComponents;

    @Autowired
    private Map<String, String> commandsAndNumbers;

    @Autowired
    private Map<String, String> componentsAndNumbers;

    @Autowired
    private Map<String, String> cardsAndNumbers;

    @Autowired
    @Qualifier("startupTasks")
    private Map<String, String> startupTasksAndNumbers;

    @Autowired
    private CardRepository cardRepository;

    private final List<String> emptyArray = List.of();

    public String getStartupTaskNumber(String taskName) {
        return startupTasksAndNumbers.getOrDefault(taskName, "");
    }

    public String getStartupTaskArguments(String taskName) {
        return env.getProperty("startup.task[" + startupTasksAndNumbers.get(taskName) + "]." + "arguments", "");
    }

    public String getStartupTaskPath(String taskName) {
        return env.getProperty("startup.task[" + startupTasksAndNumbers.get(taskName) + "]." + "path", "");
    }

    public List<String> getCommandNumbersByTopic(String topic) {
        return topicsAndCommands.getOrDefault(topic, emptyArray);
    }

    public String getCommandPubTopic(String command) {
        return env.getProperty("command[" + commandsAndNumbers.get(command) + "]." + "publication.topic", "");
    }

    public String getCommandPubQos(String command) {
        return env.getProperty("command[" + commandsAndNumbers.get(command) + "]." + "publication.qos", "EXACTLY_ONCE");
    }

    public String getCommandPubRetain(String command) {
        return env.getProperty("command[" + commandsAndNumbers.get(command) + "]." + "publication.retain", "false");
    }

    public String getCommandPubDataType(String command) {
        return env.getProperty("command[" + commandsAndNumbers.get(command) + "]." + "publication.data.type", MediaType.TEXT_PLAIN_VALUE);
    }

    public String getCommandPath(String command) {
        return env.getProperty("command[" + commandsAndNumbers.get(command) + "]." + "path", "");
    }

    public String getCommandArguments(String command) {
        return env.getProperty("command[" + commandsAndNumbers.get(command) + "]." + "arguments", "");
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
        //return topicsAndCards.getOrDefault(topic, emptyArray);

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
    public String getCardName(String number) {
        //return env.getProperty("card[" + id + "].name", "");

        return cardRepository.findByNumber(Integer.valueOf(number)).map(CardEntity::getName).orElse("");
    }

    /**
     * Retrieves the card number associated with the specified name.
     *
     * @param name the name associated with the card number
     * @return the card number if found, or an empty string.
     */
    public String getCardNumber(String name) {
        //return cardsAndNumbers.getOrDefault(name, "");

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
        return cardRepository.findByNumber(Integer.valueOf(number)).map(CardEntity::getSubscriptionTopic).orElse("");
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
        return cardRepository.findByNumber(Integer.valueOf(number)).map(CardEntity::getSubscriptionQos).orElse("AT_MOST_ONCE");
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
        //return env.getProperty("card[" + id + "].subscription.data.name", "");

        //return cardRepository.findByNumber(Integer.valueOf(number)).map(CardEntity::getSubscriptionDataName).orElse("");
        return Optional.ofNullable(number)
                .filter(StringUtils::isNotBlank)
                .map(Integer::valueOf)
                .flatMap(cardRepository::findByNumber)
                .map(CardEntity::getSubscriptionDataName)
                .orElse("");
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
        //return env.getProperty("card[" + id + "].subscription.data.type", "");

        return cardRepository.findByNumber(Integer.valueOf(number)).map(CardEntity::getSubscriptionDataType).orElse("");
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
        //return env.getProperty("card[" + id + "].display.data.jsonpath", "");

        return cardRepository.findByNumber(Integer.valueOf(number)).map(CardEntity::getDisplayDataJsonpath).orElse("");
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
        //return env.getProperty("card[" + id + "].publication.topic", "");

        return cardRepository.findByNumber(Integer.valueOf(number)).map(CardEntity::getPublicationTopic).orElse("");
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
        //return env.getProperty("card[" + id + "].publication.qos", "AT_MOST_ONCE");

        return cardRepository.findByNumber(Integer.valueOf(number)).map(CardEntity::getPublicationQos).orElse("AT_MOST_ONCE");
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
        return cardRepository.findByNumber(Integer.valueOf(number)).map(CardEntity::getPublicationRetain).map(String::valueOf).orElse("false");
    }

    /**
     * Retrieves the publication data associated with the specified card number.
     *
     * @param number the number of the card for which to retrieve the
     * publication data
     * @return the publication data if found, or an empty string.
     */
    public String getCardPubData(String number) {
        //return env.getProperty("card[" + id + "].publication.data", "");

        return cardRepository.findByNumber(Integer.valueOf(number)).map(CardEntity::getPublicationData).orElse("");
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
        //return env.getProperty("card[" + id + "].publication.data.type", "");

        return cardRepository.findByNumber(Integer.valueOf(number)).map(CardEntity::getPublicationDataType).orElse("");
    }

    /**
     * Retrieves the local task path associated with the specified card number.
     *
     * @param number the number of the card for which to retrieve the local task
     * path
     * @return the local task path if found, or an empty string.
     */
    public String getCardLocalTaskPath(String number) {
        return cardRepository.findByNumber(Integer.valueOf(number)).map(CardEntity::getLocalTaskPath).orElse("");
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
        //return env.getProperty("card[" + id + "].local.task.arguments", "");

        return cardRepository.findByNumber(Integer.valueOf(number)).map(CardEntity::getLocalTaskArguments).orElse("");
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
        //return env.getProperty("card[" + id + "].local.task.data.type", "");

        return cardRepository.findByNumber(Integer.valueOf(number)).map(CardEntity::getLocalTaskDataType).orElse("");
    }

    public String getComponentName(String id) {
        return env.getProperty("component[" + id + "].name", "");
    }

    public List<String> getComponentNumbersByTopic(String topic) {
        return topicsAndComponents.getOrDefault(topic, emptyArray);
    }

    public String getComponentPubTopic(String component) {
        return env.getProperty("component[" + componentsAndNumbers.get(component) + "].publication.topic", "");
    }

    public String getComponentPubQos(String component) {
        return env.getProperty("component[" + componentsAndNumbers.get(component) + "].publication.qos", "AT_MOST_ONCE");
    }

    public String getComponentPubRetain(String component) {
        return env.getProperty("component[" + componentsAndNumbers.get(component) + "].publication.retain", "false");
    }

    public String getComponentPubDataType(String component) {
        return env.getProperty("component[" + componentsAndNumbers.get(component) + "].publication.data.type", MediaType.TEXT_PLAIN_VALUE);
    }

    public String getComponentPubLocalCard(String component) {
        return env.getProperty("component[" + componentsAndNumbers.get(component) + "].publication.local.card", "");
    }

    public String getComponentProvider(String component) {
        return env.getProperty("component[" + componentsAndNumbers.get(component) + "].provider", "");
    }

    public String getComponentProviderArgs(String component) {
        return env.getProperty("component[" + componentsAndNumbers.get(component) + "].provider.args", "");
    }
}
