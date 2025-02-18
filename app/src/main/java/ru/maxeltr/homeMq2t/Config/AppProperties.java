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
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;

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

    public List<String> getCommandNumbersByTopic(String topic) {
        return topicsAndCommands.get(topic);
    }

    public String getCommandPubTopic(String command) {
        return env.getProperty("command[" + commandsAndNumbers.get(command) + "]." + "publication.topic", ERROR_TOPIC);
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

    public List<String> getCardNumbersByTopic(String topic) {
        return topicsAndCards.get(topic);
    }

    public String getCardName(String id) {
        return env.getProperty("card[" + id + "].name", "");
    }

    public String getCardSubDataName(String id) {
        return env.getProperty("card[" + id + "].subscription.data.name", "");
    }

    public String getCardSubDataType(String id) {
        return env.getProperty("card[" + id + "].subscription.data.type", MediaType.APPLICATION_JSON_VALUE);
    }

    public String getCardSubJsonPathExpression(String id) {
        return env.getProperty("card[" + id + "].subscription.data.jsonpath", "");
    }

    public String getCardPubTopic(String id) {
        return env.getProperty("card[" + id + "].publication.topic", ERROR_TOPIC);
    }

    public String getCardPubQos(String id) {
        return env.getProperty("card[" + id + "].publication.qos", "AT_MOST_ONCE");
    }

    public String getCardPubRetain(String id) {
        return env.getProperty("card[" + id + "].publication.retain", "false");
    }

    public String getCardPubData(String id) {
        return env.getProperty("card[" + id + "].publication.data", "");
    }

    public String getCardPubDataType(String id) {
        return env.getProperty("card[" + id + "].publication.data.type", "");
    }

    public String getComponentName(String id) {
        return env.getProperty("component[" + id + "].name", "");
    }

    public List<String> getComponentNumbersByTopic(String topic) {
        return topicsAndComponents.get(topic);
    }

    public String getComponentPubTopic(String component) {
        return env.getProperty("component[" + componentsAndNumbers.get(component) + "].publication.topic", ERROR_TOPIC);
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

}
