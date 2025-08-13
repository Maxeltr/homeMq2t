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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import ru.maxeltr.homeMq2t.Config.AppProperties;
import ru.maxeltr.homeMq2t.Controller.OutputUIController;
import ru.maxeltr.homeMq2t.Model.Msg;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import ru.maxeltr.homeMq2t.Config.UIPropertiesProvider;
import ru.maxeltr.homeMq2t.Service.ServiceMediator;

public class UIServiceImpl implements UIService {

    private static final Logger logger = LoggerFactory.getLogger(UIServiceImpl.class);

    private ServiceMediator mediator;

    @Autowired
    @Qualifier("getUIPropertiesProvider")
    private UIPropertiesProvider appProperties;

    @Autowired
    private OutputUIController uiController;

    @Autowired
    private ConnectManager connectManager;

    @Autowired
    private DashboardItemManager dashboardItemManager;

    @Autowired
    private PublishManager publishManager;

    @Autowired
    private LocalTaskManager localTaskManager;

    @Autowired
    private HtmlSanitizer htmlSanitizer;

    @Autowired
    private UIJsonFormatter jsonFormatter;

    @Override
    public void setMediator(ServiceMediator mediator) {
        this.mediator = mediator;
    }

    /**
     * Attempts to establish a connection to a remote server and waits for the
     * connection attempt to comlete. This method returns html string for
     * display on the screen indicating success or failure.
     */
    @Override
    public void connect() {
        logger.info("Do connect.");
        this.display(this.connectManager.connect(), "");
    }

    @Override
    public void displayCardSettings(Msg.Builder msg) {
        logger.info("Do edit settings {}.", msg);
        this.display(this.dashboardItemManager.getCardSettings(msg), "");
    }

    @Override
    public void saveCardSettings(Msg.Builder msg) {
        logger.info("Do save settings {}.", msg.getData());
        this.dashboardItemManager.saveCardSettings(msg);

    }

    @Override
    public void deleteCard(Msg.Builder msg) {
        logger.info("Do delete card {}.", msg.getData());
        this.dashboardItemManager.deleteCard(msg);

    }

    @Override
    public void disconnect(byte reasonCode) {
        logger.info("Do disconnect with reason code {}.", reasonCode);
        this.connectManager.disconnect(reasonCode);
    }

    @Override
    public void shutdownApp() {
        logger.info("Do shutdown application.");
        this.connectManager.shutdownApp();   //disconnect(MqttReasonCodeAndPropertiesVariableHeader.REASON_CODE_OK);
    }

    @Override
    public void publish(Msg.Builder msg) {
        logger.info("Do publish message from card {}.", msg.getId());
        this.publishManager.publish(msg);
    }

    @Override
    public void launch(Msg.Builder msg) {
        logger.info("Do run local task from card {}.", msg.getId());
        this.display(this.localTaskManager.run(msg), msg.getId());
    }

    @Async("processExecutor")
    @Override
    public void display(Msg.Builder builder, String cardNumber) {
        String type = this.appProperties.getCardSubDataType(cardNumber);
        if (StringUtils.isNotEmpty(type)) {
            builder.type(type);
            logger.info("Changed type to defined in properties for card={}. Set type={}.", cardNumber, type);
        } else {
            if (StringUtils.isEmpty(builder.getType())) {
                builder.type(MediaType.APPLICATION_JSON_VALUE);
                logger.info("Type is undefined for card={}. Set {}.", cardNumber, builder.getType());
            }
        }

        if (builder.getType().equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE)) {
            String jsonPathExpression = this.appProperties.getCardJsonPathExpression(cardNumber);
            if (StringUtils.isNotEmpty(jsonPathExpression)) {
                builder.data(this.jsonFormatter.createJson(builder.getData(), jsonPathExpression));
            } else {
                logger.debug("JsonPath expression is empty for card={}.", cardNumber);
            }
        }

        builder.data(this.htmlSanitizer.sanitize(builder.getData()));

        logger.debug("Display data={}. Card={}", builder, cardNumber);
        this.uiController.display(builder.build(), cardNumber);
    }

}
