/*
 * The MIT License
 *
 * Copyright 2023 Maxim Eltratov <<Maxim.Eltratov@ya.ru>>.
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
package ru.maxeltr.homeMq2t.Service;

import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttReasonCodeAndPropertiesVariableHeader;
import io.netty.util.concurrent.Promise;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import ru.maxeltr.homeMq2t.AppShutdownManager;
import ru.maxeltr.homeMq2t.Controller.OutputUIController;
import ru.maxeltr.homeMq2t.Model.Dashboard;
import ru.maxeltr.homeMq2t.Model.Msg;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class UIServiceImpl implements UIService {

    private static final Logger logger = LoggerFactory.getLogger(UIServiceImpl.class);

    private ServiceMediator mediator;

    @Autowired
    private Environment env;

    @Value("${connect-timeout:5000}")
    private Integer connectTimeout;
    
    @Value("${wait-disconnect-while-shutdown:1000}")
    private Integer waitDisconnect;

    @Autowired
    private OutputUIController uiController;

    @Autowired
    private List<Dashboard> dashboards;

    @Autowired
    AppShutdownManager appShutdownManager;
    
    @Override
    public void setMediator(ServiceMediator mediator) {
        this.mediator = mediator;
    }

    @Override
    public void connect() {
        logger.info("Do connect.");
        Msg.Builder msg = new Msg.Builder("onConnect").type("application/json");

        Promise<MqttConnAckMessage> authFuture = this.mediator.connect();
        authFuture.awaitUninterruptibly(this.connectTimeout);

        if (authFuture.isCancelled()) {
            logger.info("Connection attempt to remote server was failed.");
            String startDashboardWithError = "<div style=\"color:red;\">Connection attempt to remote server was failed.</div>" + this.getStartDashboard();
            msg.data("{\"name\": \"onConnect\", \"status\": \"fail\", \"type\": \"text/html;base64\", \"data\": \""
                    + Base64.getEncoder().encodeToString(startDashboardWithError.getBytes())
                    + "\"}");
        } else if (!authFuture.isSuccess()) {
            logger.info("Connection established failed {}", authFuture.cause());
            String startDashboardWithError = "<div style=\"color:red;\">Connection attempt to remote server was failed.</div>" + this.getStartDashboard();
            msg.data("{\"name\": \"onConnect\", \"status\": \"fail\", \"type\": \"text/html;base64\", \"data\": \""
                    + Base64.getEncoder().encodeToString(startDashboardWithError.getBytes())
                    + "\"}");
        } else {
            logger.info("Connection established successfully.");
            msg.data("{\"name\": \"onConnect\", \"status\": \"ok\", \"type\": \"text/html;base64\", \"data\": \""
                    + Base64.getEncoder().encodeToString(this.getStartDashboard().getBytes())
                    + "\"}");
        }
        msg.timestamp(String.valueOf(Instant.now().toEpochMilli()));
        this.display(msg, "");
    }

    @Override
    public void disconnect(byte reasonCode) {
        logger.info("Do disconnect with reason code {}.", reasonCode);
        this.mediator.disconnect(reasonCode);
    }
    
    @Override
    public void shutdownApp() {
        logger.info("Do shutdown aplication.");
        this.disconnect(MqttReasonCodeAndPropertiesVariableHeader.REASON_CODE_OK);
        
        try {
            TimeUnit.MILLISECONDS.sleep(waitDisconnect);
        } catch (InterruptedException ex) {
            logger.info("Shutdown. InterruptedException while disconnect timeout.", ex);
        }
        
        this.appShutdownManager.shutdownApp(0);
    }

    private String getStartDashboard() {
        return this.dashboards.get(0).getHtml();
    }

    @Override
    public void publish(Msg.Builder msg) {
        String topic = env.getProperty("card[" + msg.getId() + "].publication.topic", "");
        MqttQoS qos = MqttQoS.valueOf(env.getProperty("card[" + msg.getId() + "].publication.qos", "AT_MOST_ONCE"));
        boolean retain = Boolean.getBoolean(env.getProperty("card[" + msg.getId() + "].publication.retain", "false"));
        msg.data(env.getProperty("card[" + msg.getId() + "].publication.data", ""));
        msg.type(env.getProperty("card[" + msg.getId() + "].publication.data.type", ""));
        msg.timestamp(String.valueOf(Instant.now().toEpochMilli()));
        
        logger.info("Create message {}.", msg);
        
        this.mediator.publish(msg.build(), topic, qos, retain);
    }

    @Override
    public void display(Msg.Builder msg, String cardNumber) {

        //TODO sanitize name, payload, timestamp...
        this.uiController.display(msg.build(), cardNumber);
    }
}