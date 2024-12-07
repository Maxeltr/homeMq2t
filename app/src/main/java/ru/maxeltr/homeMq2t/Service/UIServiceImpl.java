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
import io.netty.util.concurrent.Promise;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import ru.maxeltr.homeMq2t.Controller.OutputUIController;
import ru.maxeltr.homeMq2t.Model.Dashboard;
import ru.maxeltr.homeMq2t.Model.Msg;
import ru.maxeltr.homeMq2t.Model.MsgImpl;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class UIServiceImpl implements UIService {

    private static final Logger logger = LoggerFactory.getLogger(UIServiceImpl.class);

    private ServiceMediator mediator;

    @Autowired
    private Environment env;

    @Autowired
    private OutputUIController uiController;

    @Autowired
    private List<Dashboard> dashboards;

    @Override
    public void setMediator(ServiceMediator mediator) {
        this.mediator = mediator;
    }

    @Override
    public void connect() {
        logger.info("Do connect.");
        Promise<MqttConnAckMessage> authFuture = mediator.connect();

        authFuture.awaitUninterruptibly();
        if (authFuture.isCancelled()) {
            logger.info("Connection attempt cancelled.");

            Msg msg = new Msg.Builder("")
                    .type("application/json")
                    .payload("{\"name\": \"connect\", \"status\": \"fail\", \"data\": \""
                            + Base64.getEncoder().encodeToString("<div style=\"color:red;\">Connection attempt cancelled.</div>".getBytes())
                            + "\"}")
                    .timestamp(String.valueOf(Instant.now().toEpochMilli()))
                    .build();
            this.display(msg);
        } else if (!authFuture.isSuccess()) {
            logger.info("Connection established failed {}", authFuture.cause());
            Msg msg = new MsgImpl.Builder("")
                    .type("application/json")
                    .payload("{\"name\": \"connect\", \"status\": \"fail\", \"data\": \""
                            + Base64.getEncoder().encodeToString("<div style=\"color:red;\">Connection established failed.</div>".getBytes())
                            + "\"}")
                    .timestamp(String.valueOf(Instant.now().toEpochMilli()))
                    .build();
            this.display(msg);
        } else {
            logger.info("Connection established successfully.");
            Msg msg = new MsgImpl.Builder("")
                    .type("application/json")
                    .payload("{\"name\": \"connect\", \"status\": \"ok\", \"data\":\""
                            + Base64.getEncoder().encodeToString(this.getStartDashboard().getBytes())
                            + "\"}")
                    .timestamp(String.valueOf(Instant.now().toEpochMilli()))
                    .build();
            this.display(msg);
        }
    }

    @Override
    public void disconnect() {
        logger.info("Do disconnect.");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private String getStartDashboard() {
        return this.dashboards.get(0).getHtml();
    }

    @Override
    public void publish(Msg msg) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void display(Msg msg) {
        this.uiController.display(msg);
    }
}
