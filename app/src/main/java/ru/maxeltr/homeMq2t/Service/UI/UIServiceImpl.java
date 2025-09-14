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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import ru.maxeltr.homeMq2t.Model.Msg;
import org.springframework.beans.factory.annotation.Qualifier;
import ru.maxeltr.homeMq2t.Config.AppProperties;
import ru.maxeltr.homeMq2t.Service.ServiceMediator;

public class UIServiceImpl implements UIService {

    private static final Logger logger = LoggerFactory.getLogger(UIServiceImpl.class);

    private ServiceMediator mediator;

    @Autowired
    private AppProperties appProperties;

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
    @Qualifier("getDashboardItemMqttSettingManager")
    private DashboardItemManager mqttSettingManager;

    @Autowired
    private MqttManager publishManager;

    @Autowired
    private LocalTaskManager localTaskManager;

    @Autowired
    @Qualifier("getDisplayManager")
    private DisplayManager displayManager;

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
        this.display(this.connectManager.connect(), "dashboard");
    }

    @Override
    public void displayCardDashboard(Msg msg) {
        logger.debug("Do display dashboard {}.", msg);
        this.display(this.cardManager.getItemsByDashboard(msg), "dashboard");
    }

    public void displayStartDashboard(Msg msg) {
        logger.debug("Do display start dashboard {}.", msg);
        this.display(this.cardManager.getItemsByDashboard(msg.toBuilder().id("").build()), "dashboard");
    }

    @Override
    public void displayCommandDashboard(Msg msg) {
        logger.debug("Do display command dashboard {}.", msg);
        this.display(this.commandManager.getItemsByDashboard(msg), "dashboard");
    }

    @Override
    public void displayComponentDashboard(Msg msg) {
        logger.debug("Do display component dashboard {}.", msg);
        this.display(this.componentManager.getItemsByDashboard(msg), "dashboard");
    }

    @Override
    public void displayCardSettings(Msg msg) {
        logger.debug("Do edit card settings {}.", msg);
        this.display(this.cardManager.getItem(msg), "dashboard");
    }

    @Override
    public void displayCommandSettings(Msg msg) {
        logger.debug("Do edit command settings {}.", msg);
        this.display(this.commandManager.getItem(msg), "dashboard");
    }

    @Override
    public void displayComponentSettings(Msg msg) {
        logger.debug("Do edit component settings {}.", msg);
        this.display(this.componentManager.getItem(msg), "dashboard");
    }

    @Override
    public void displayMqttSettings(Msg msg) {
        logger.debug("Do edit mqtt settings {}.", msg);
        this.display(this.mqttSettingManager.getItem(msg), "dashboard");
    }

    @Override
    public void saveCardSettings(Msg msg) {
        logger.debug("Do save card settings {}.", msg.getData());
        this.cardManager.saveItem(msg);
    }

    @Override
    public void saveCommandSettings(Msg msg) {
        logger.debug("Do save command settings {}.", msg.getData());
        this.commandManager.saveItem(msg);
    }

    @Override
    public void saveComponentSettings(Msg msg) {
        logger.debug("Do save component settings {}.", msg.getData());
        this.componentManager.saveItem(msg);
    }

    @Override
    public void saveMqttSettings(Msg msg) {
        logger.debug("Do save component settings {}.", msg.getData());
        this.mqttSettingManager.saveItem(msg);
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
    public void deleteComponent(Msg msg) {
        logger.debug("Do delete component {}.", msg.getData());
        this.componentManager.deleteItem(msg);
    }

    @Override
    public void deleteMqttSettings(Msg msg) {
        logger.debug("Do delete mqtt settings {}.", msg.getData());
        //this.componentManager.deleteItem(msg);
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
        Msg result = this.localTaskManager.run(msg);
        if (StringUtils.isNotBlank(result.getData())) {
            this.display(result, msg.getId());
        }
    }

    @Async("processExecutor")
    @Override
    public void display(Msg msg, String cardNumber) {
        logger.debug("Do display message id={} to card {}.", msg.getId(), cardNumber);
        this.displayManager.display(msg, cardNumber);
    }

}
