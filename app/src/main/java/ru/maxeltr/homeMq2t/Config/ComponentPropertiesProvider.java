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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.maxeltr.homeMq2t.Entity.ComponentEntity;
import ru.maxeltr.homeMq2t.Model.ViewModel;
import ru.maxeltr.homeMq2t.Repository.ComponentRepository;


/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public interface ComponentPropertiesProvider {

    public final static String COMPONENT_TEMPLATE_PATH = "component-template-path";

    public final static String COMPONENT_SETTINGS_TEMPLATE_PATH = "component-settings-template-path";

    public final static String COMPONENT_LIST_NAME = "Component List";

    public ComponentEntity saveComponentEntity(ComponentEntity entity);

    public void deleteComponent(String id);

    public Optional<ViewModel> getComponentSettings(String number);

    public Optional<ViewModel> getEmptyComponentSettings();

    /**
     * Retrieves the name of a component based on its number.
     *
     * @param number the number of the card whose name is to be retrieved.
     * @return the name of the component associated with the specified number,
     * returns an empty string if component name is not found.
     */
    public String getComponentName(String number);

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
    public List<String> getComponentNumbersByTopic(String topic);

    /**
     * Retrieves the publication topic associated with the specified component
     * name.
     *
     * @param name the name of the component for which to retrieve the
     * publication topic
     * @return the publication topic if found, or an empty string.
     */
    public String getComponentPubTopic(String name);

    /**
     * Retrieves the Quality of Service (QoS) level associated with the
     * specified component for publication.
     *
     * @param name the name of the component for which to retrieve the
     * publication QoS level
     * @return the publication QoS level if found, or "AT_MOST_ONCE".
     */
    public String getComponentPubQos(String name);

    /**
     * Retrieves the retain flag associated with the specified component for
     * publication.
     *
     * @param name the name of the component for which to retrieve the
     * publication retain flag
     * @return the publication retain flag if found, or "false".
     */
    public String getComponentPubRetain(String name);

    /**
     * Retrieves the publication data type associated with the specified
     * component.
     *
     * @param name the name of the component for which to retrieve the
     * publication data type
     * @return the publication data type if found, or an empty string.
     */
    public String getComponentPubDataType(String name);

    /**
     * Retrieves the card of the local dashboard associated with the specified
     * component.
     *
     * @param name the name of the component for which to retrieve the local
     * card path
     * @return the local card number if found, or an empty string.
     */
    public String getComponentPubLocalCard(String name);

    /**
     * Retrieves the provider for a specified component.
     *
     * @param name the name of the component for which to retrieve the provider
     * path
     * @return the provider name if found, or an empty string.
     */
    public String getComponentProvider(String name);

    /**
     * Retrieves the provider arguments associated with the specified component.
     *
     * @param name the name of the component for which to retrieve the provider
     * arguments
     * @return the provider arguments if found, or an empty string.
     */
    public String getComponentProviderArgs(String name);

}
