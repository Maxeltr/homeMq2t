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

import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.util.concurrent.Promise;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import ru.maxeltr.homeMq2t.Config.UIPropertiesProvider;
import ru.maxeltr.homeMq2t.Model.Dashboard;
import ru.maxeltr.homeMq2t.Model.Msg;
import ru.maxeltr.homeMq2t.Model.MsgImpl;
import ru.maxeltr.homeMq2t.Service.ServiceMediator;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class ConnectManagerImpl implements ConnectManager {

    private static final Logger logger = LoggerFactory.getLogger(ConnectManagerImpl.class);

    @Value("${connect-timeout:5000}")
    private Integer connectTimeout;

    @Autowired
    @Lazy               //TODO
    private ServiceMediator mediator;

    @Autowired
    private Base64HtmlJsonFormatter jsonFormatter;

    @Autowired
    @Qualifier("getUIPropertiesProvider")
    private UIPropertiesProvider appProperties;

    @Override
    public Msg.Builder connect() {
        Msg.Builder msg = new MsgImpl.MsgBuilder("onConnect").type(MediaType.APPLICATION_JSON_VALUE);

        Promise<MqttConnAckMessage> authFuture = this.mediator.connect();
        authFuture.awaitUninterruptibly(this.connectTimeout);
        String dashboardHtml = this.appProperties.getStartDashboard().map(Dashboard::getHtml).orElse("");
        if (authFuture.isCancelled()) {
            logger.info("Connection attempt to remote server was canceled.");
            msg.data(this.jsonFormatter.createJson(dashboardHtml, "onConnect", "fail"));
        } else if (!authFuture.isSuccess()) {
            logger.info("Connection established failed.");
            msg.data(this.jsonFormatter.createJson(dashboardHtml, "onConnect", "fail"));
        } else {
            logger.info("Connection established successfully.");
            msg.data(this.jsonFormatter.createJson(dashboardHtml, "onConnect", "ok"));
        }

        msg.timestamp(String.valueOf(Instant.now().toEpochMilli()));

        return msg;
    }

    @Override
    public void disconnect(byte reasonCode) {
        logger.info("Do disconnect with reason code {}.", reasonCode);
        this.mediator.disconnect(reasonCode);
    }

    @Override
    public void shutdownApp() {
        logger.info("Do shutdown application.");
        this.mediator.shutdown();   //disconnect(MqttReasonCodeAndPropertiesVariableHeader.REASON_CODE_OK);
    }
}
