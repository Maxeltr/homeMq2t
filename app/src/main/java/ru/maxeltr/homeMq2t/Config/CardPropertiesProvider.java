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

import java.util.List;
import java.util.Optional;
import ru.maxeltr.homeMq2t.Entity.CardEntity;
import ru.maxeltr.homeMq2t.Model.CardModel;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public interface CardPropertiesProvider {

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
    public List<String> getCardNumbersByTopic(String topic);

    /**
     * Retrieves the name of a card based on its number.
     *
     * @param number the number of the card whose name is to be retrieved.
     * @return the name of the card associated with the specified number,
     * returns an empty string if card name is not found.
     */
    public String getCardName(String number);

    public Optional<CardEntity> getCardEntity(String number);

    public Optional<CardModel> getCardSettings(String number);		//TODO rename to getCardSettingsModel or getCardSettingsForm


    /**
     * Retrieves the card number associated with the specified name.
     *
     * @param name the name associated with the card number
     * @return the card number if found, or an empty string.
     */
    public String getCardNumber(String name);

    /**
     * Retrieves the subscription topic associated with the specified card
     * number.
     *
     * @param number the number of the card for which to retrieve the
     * subscription topic
     * @return the subscription topic if found, or an empty string.
     */
    public String getCardSubTopic(String number);

    /**
     * Retrieves the Quality of Service (QoS) level associated with the
     * specified card number for subscription.
     *
     * @param number the number of the card for which to retrieve the
     * subscription QoS level
     * @return the subscription QoS level if found, or "AT_MOST_ONCE".
     */
    public String getCardSubQos(String number);

    /**
     * Retrieves the subscription data name associated with the specified card
     * number.
     *
     * @param number the number of the card for which to retrieve the
     * subscription data name
     * @return the subscription data name if found, or an empty string.
     */
    public String getCardSubDataName(String number);

    /**
     * Retrieves the subscription data type associated with the specified card
     * number.
     *
     * @param number the number of the card for which to retrieve the
     * subscription data type
     * @return the subscription data type if found, or an empty string.
     */
    public String getCardSubDataType(String number);

    /**
     * Retrieves the jsonpath expression associated with the specified card
     * number.
     *
     * @param number the number of the card for which to retrieve the jsonpath
     * expression
     * @return the jsonpath expression if found, or an empty string.
     */
    public String getCardJsonPathExpression(String number);

    /**
     * Retrieves the publication topic associated with the specified card
     * number.
     *
     * @param number the number of the card for which to retrieve the
     * publication topic
     * @return the publication topic if found, or an empty string.
     */
    public String getCardPubTopic(String number);

    /**
     * Retrieves the Quality of Service (QoS) level associated with the
     * specified card number for publication.
     *
     * @param number the number of the card for which to retrieve the
     * publication QoS level
     * @return the publication QoS level if found, or "AT_MOST_ONCE".
     */
    public String getCardPubQos(String number);

    /**
     * Retrieves the retain flag associated with the specified card number for
     * publication.
     *
     * @param number the number of the card for which to retrieve the
     * publication retain flag
     * @return the publication retain flag if found, or "false".
     */
    public String getCardPubRetain(String number);

    /**
     * Retrieves the publication data associated with the specified card number.
     *
     * @param number the number of the card for which to retrieve the
     * publication data
     * @return the publication data if found, or an empty string.
     */
    public String getCardPubData(String number);

    /**
     * Retrieves the publication data type associated with the specified card
     * number.
     *
     * @param number the number of the card for which to retrieve the
     * publication data type
     * @return the publication data type if found, or an empty string.
     */
    public String getCardPubDataType(String number);

    /**
     * Retrieves the local task path associated with the specified card number.
     *
     * @param number the number of the card for which to retrieve the local task
     * path
     * @return the local task path if found, or an empty string.
     */
    public String getCardLocalTaskPath(String number);

    /**
     * Retrieves the local task arguments associated with the specified card
     * number.
     *
     * @param number the number of the card for which to retrieve the local task
     * arguments
     * @return the local task arguments if found, or an empty string.
     */
    public String getCardLocalTaskArguments(String number);

    /**
     * Retrieves the local task data type associated with the specified card
     * number.
     *
     * @param number the number of the card for which to retrieve the local task
     * data type
     * @return the local task data type if found, or an empty string.
     */
    public String getCardLocalTaskDataType(String number);

}
