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
package ru.maxeltr.homeMq2t.Service.UI;

import io.netty.handler.codec.mqtt.MqttQoS;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import ru.maxeltr.homeMq2t.Config.CardPropertiesProvider;
import ru.maxeltr.homeMq2t.Controller.OutputUIController;
import ru.maxeltr.homeMq2t.Model.Msg;
import ru.maxeltr.homeMq2t.Mqtt.MqttUtils;
import ru.maxeltr.homeMq2t.Service.ServiceMediator;

public class DisplayManagerImpl implements DisplayManager {

    private static final Logger logger = LoggerFactory.getLogger(DisplayManagerImpl.class);

    @Autowired
    @Lazy               //TODO
    private ServiceMediator mediator;

    @Autowired
    private OutputUIController uiController;

    @Autowired
    @Qualifier("getCardPropertiesProvider")
    private CardPropertiesProvider appProperties;

    @Autowired
    private UIJsonFormatter jsonFormatter;

    @Autowired
    private HtmlSanitizer htmlSanitizer;

    @Override
    public void display(Msg msg, String cardNumber) {
        var message = msg.toBuilder();
        String type = this.appProperties.getCardSubDataType(cardNumber);
        if (StringUtils.isNotEmpty(type)) {
            message.type(type);
            logger.info("Changed type to defined in properties for card={}. Set type={}.", cardNumber, type);
        } else {
            if (StringUtils.isEmpty(message.getType())) {
                message.type(MediaType.APPLICATION_JSON_VALUE);
                logger.info("Type is undefined for card={}. Set {}.", cardNumber, message.getType());
            }
        }

        if (message.getType().equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE)) {
            List<String> jsonPathExpressions = Arrays.asList(StringUtils.split(this.appProperties.getCardJsonPathExpression(cardNumber), " "));
            if (!jsonPathExpressions.isEmpty()) {
                if (jsonPathExpressions.size() == 1) {
                    message.data(this.jsonFormatter.parseAndCreateJson(message.getData(), jsonPathExpressions.get(0)));
                } else {
                    message.data(this.jsonFormatter.parseAndCreateJson(message.getData(), jsonPathExpressions));
                }
            }
        }

        message.data(this.htmlSanitizer.sanitize(message.getData()));

        logger.debug("Display data={}. Card={}", message, cardNumber);
        this.uiController.display(message.build(), cardNumber);
    }
}
