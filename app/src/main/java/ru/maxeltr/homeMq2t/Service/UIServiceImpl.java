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

import com.jayway.jsonpath.InvalidPathException;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttReasonCodeAndPropertiesVariableHeader;
import io.netty.util.concurrent.Promise;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import ru.maxeltr.homeMq2t.AppShutdownManager;
import ru.maxeltr.homeMq2t.Config.AppProperties;
import ru.maxeltr.homeMq2t.Controller.OutputUIController;
import ru.maxeltr.homeMq2t.Model.Dashboard;
import ru.maxeltr.homeMq2t.Model.Msg;
import com.jayway.jsonpath.JsonPath;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class UIServiceImpl implements UIService {

    private static final Logger logger = LoggerFactory.getLogger(UIServiceImpl.class);

    private ServiceMediator mediator;

    @Autowired
    private AppProperties appProperties;

    @Value("${connect-timeout:5000}")
    private Integer connectTimeout;

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
        Msg.Builder msg = new Msg.Builder("onConnect").type(MediaType.APPLICATION_JSON_VALUE);

        Promise<MqttConnAckMessage> authFuture = this.mediator.connect();
        authFuture.awaitUninterruptibly(this.connectTimeout);

        if (authFuture.isCancelled()) {
            logger.info("Connection attempt to remote server was canceled.");
            String startDashboardWithError = "<div style=\"color:red;\">Connection attempt to remote server was failed.</div>" + this.getStartDashboard();
            msg.data("{\"name\": \"onConnect\", \"status\": \"fail\", \"type\": \"text/html;base64\", \"data\": \""
                    + Base64.getEncoder().encodeToString(startDashboardWithError.getBytes())
                    + "\"}");
        } else if (!authFuture.isSuccess()) {
            logger.info("Connection established failed.");
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
        this.mediator.shutdown();   //disconnect(MqttReasonCodeAndPropertiesVariableHeader.REASON_CODE_OK);
    }

    private String getStartDashboard() {
        return this.dashboards.get(0).getHtml();
    }

    @Override
    public void publish(Msg.Builder msg) {
        String topic = this.appProperties.getCardPubTopic(msg.getId());
        MqttQoS qos = MqttQoS.valueOf(this.appProperties.getCardPubQos(msg.getId()));
        boolean retain = Boolean.getBoolean(this.appProperties.getCardPubRetain(msg.getId()));
        msg.data(this.appProperties.getCardPubData(msg.getId()));
        msg.type(this.appProperties.getCardPubDataType(msg.getId()));
        msg.timestamp(String.valueOf(Instant.now().toEpochMilli()));

        logger.info("Create message {}.", msg);

        this.mediator.publish(msg.build(), topic, qos, retain);
    }

    @Async("processExecutor")
    @Override
    public void display(Msg.Builder builder, String cardNumber) {
        builder.type(this.appProperties.getCardSubDataType(cardNumber));
        if (builder.getType().equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE)) {
            String dataName = this.appProperties.getCardSubDataName(cardNumber);
            String jsonPathExpression = this.appProperties.getCardSubJsonPathExpression(cardNumber);
            if (!jsonPathExpression.isEmpty()) {
                String parsedValue = this.parseJson(builder.getData(), jsonPathExpression);
                logger.debug("Parse data. Parsed value={}.", parsedValue);
                builder.data("{\"name\": \"" + dataName + "\", \"type\": \"" + MediaType.TEXT_PLAIN_VALUE + "\", \"data\": \"" + parsedValue + "\"}");
            } else {
                logger.debug("JsonPath expression is empty.");
            }
        }
        builder.data(Jsoup.clean(builder.getData(), Safelist.basic()));
        logger.debug("Display data={}.", builder);
        this.uiController.display(builder.build(), cardNumber);
    }

    private String parseJson(String json, String jsonPathExpression) {
        String parsedValue = "";
        try {
            parsedValue = JsonPath.parse(json).read(jsonPathExpression, String.class);
        } catch (InvalidPathException ex) {
            logger.warn("Could not parse json. {}", ex.getMessage());
        }

        return parsedValue;
    }
}
