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

import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import ru.maxeltr.homeMq2t.Controller.OutputUIController;
import ru.maxeltr.homeMq2t.Model.Msg;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import ru.maxeltr.homeMq2t.Config.CardPropertiesProvider;
import ru.maxeltr.homeMq2t.Service.ServiceMediator;

public class UIServiceImpl implements UIService {

    private static final Logger logger = LoggerFactory.getLogger(UIServiceImpl.class);

    private ServiceMediator mediator;

    @Autowired
    @Qualifier("getCardPropertiesProvider")
    private CardPropertiesProvider appProperties;

    @Autowired
    private OutputUIController uiController;

    @Autowired
    private ConnectManager connectManager;

    @Autowired
    @Qualifier("getDashboardItemCardManager")
    private DashboardItemManager cardManager;

    @Autowired
    @Qualifier("getDashboardItemCommandManager")
    private DashboardItemManager commandManager;

    @Autowired
    @Qualifier("getDashboardItemComponentManager")
    private DashboardItemManager componentManager;

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
        logger.debug("Do connect.");
        this.display(this.connectManager.connect(), "");
    }

    @Override
    public void displayCardDashboard(Msg msg) {
        logger.debug("Do display dashboard {}.", msg);
        this.display(this.cardManager.getItemsByDashboard(msg), "");
    }

    public void displayStartDashboard(Msg msg) {
        logger.debug("Do display start dashboard {}.", msg);
        this.display(this.cardManager.getItemsByDashboard(msg.toBuilder().id("").build()), "");
    }

    @Override
    public void displayCommandDashboard(Msg msg) {
        logger.debug("Do display command dashboard {}.", msg);
        this.display(this.commandManager.getItemsByDashboard(msg), "");
    }

    @Override
    public void displayCardSettings(Msg msg) {
        logger.debug("Do edit card settings {}.", msg);
        this.display(this.cardManager.getItemSettings(msg), "");
    }

    @Override
    public void displayCommandSettings(Msg msg) {
        logger.debug("Do edit command settings {}.", msg);
        this.display(this.commandManager.getItemSettings(msg), "");
    }

    @Override
    public void displayComponentSettings(Msg msg) {
        logger.debug("Do edit component settings {}.", msg);
        this.display(this.componentManager.getItemSettings(msg), "");
    }

    @Override
    public void displayMqttSettings(Msg msg) {
        logger.debug("Do edit mqtt settings {}.", msg);
        this.display(this.componentManager.getItemSettings(msg), "");
    }

    @Override
    public void saveCardSettings(Msg msg) {
        logger.debug("Do save card settings {}.", msg.getData());
        this.cardManager.saveItemSettings(msg);
    }

    @Override
    public void saveCommandSettings(Msg msg) {
        logger.debug("Do save command settings {}.", msg.getData());
        this.commandManager.saveItemSettings(msg);
    }

    @Override
    public void deleteCard(Msg msg) {
        logger.debug("Do delete card {}.", msg.getData());
        this.cardManager.deleteItem(msg);
    }

    @Override
    public void deleteCommand(Msg msg) {
        logger.debug("Do delete command {}.", msg.getData());
        this.commandManager.deleteItem(msg);
    }

    @Override
    public void disconnect(byte reasonCode) {
        logger.debug("Do disconnect with reason code {}.", reasonCode);
        this.connectManager.disconnect(reasonCode);
    }

    @Override
    public void shutdownApp() {
        logger.debug("Do shutdown application.");
        this.connectManager.shutdownApp();   //disconnect(MqttReasonCodeAndPropertiesVariableHeader.REASON_CODE_OK);
    }

    @Override
    public void publish(Msg msg) {
        logger.debug("Do publish message from card {}.", msg.getId());
        this.publishManager.publish(msg);
    }

    @Override
    public void launch(Msg msg) {
        logger.debug("Do run local task from card {}.", msg.getId());
        this.display(this.localTaskManager.run(msg), msg.getId());
    }

    @Async("processExecutor")
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
