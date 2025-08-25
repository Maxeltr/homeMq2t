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
import ru.maxeltr.homeMq2t.Model.ViewModel;

/**
 *
 * @author Dev
 */
public interface CommandPropertiesProvider {

    public final static String COMMAND_TEMPLATE_PATH = "command-template-path";

    public final static String COMMAND_SETTINGS_TEMPLATE_PATH = "command-settings-template-path";

    public Optional<ViewModel> getCommandSettings(String number);

    public Optional<ViewModel> getEmptyCommandSettings();

    public Optional<ViewModel> getAllCommands();

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
    public List<String> getCommandNumbersByTopic(String topic);

    /**
     * Retrieves the publication topic associated with the specified command.
     *
     * @param name the name of the command for which to retrieve the publication
     * topic
     * @return the publication topic if found, or an empty string.
     */
    public String getCommandPubTopic(String name);

    /**
     * Retrieves the Quality of Service (QoS) level associated with the
     * specified command for publication.
     *
     * @param name the name of the command for which to retrieve the publication
     * QoS level
     * @return the publication QoS level if found, or "AT_MOST_ONCE".
     */
    public String getCommandPubQos(String name);

    /**
     * Retrieves the retain flag associated with the specified command for
     * publication.
     *
     * @param name the name of the command for which to retrieve the publication
     * retain flag
     * @return the publication retain flag if found, or "false".
     */
    public String getCommandPubRetain(String name);

    /**
     * Retrieves the publication data type associated with the specified
     * command.
     *
     * @param name the name of the command for which to retrieve the publication
     * data type
     * @return the publication data type if found, or an empty string.
     */
    public String getCommandPubDataType(String name);

    /**
     * Retrieves the command path associated with the specified command.
     *
     * @param name the name of the command for which to retrieve the path path
     * @return the command path if found, or an empty string.
     */
    public String getCommandPath(String name);

    /**
     * Retrieves the arguments associated with the specified command.
     *
     * @param name the name of the command for which to retrieve arguments
     * @return the arguments if found, or an empty string.
     */
    public String getCommandArguments(String name);
}
