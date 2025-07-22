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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.Promise;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import ru.maxeltr.homeMq2t.Config.AppProperties;
import ru.maxeltr.homeMq2t.Controller.OutputUIController;
import ru.maxeltr.homeMq2t.Model.Dashboard;
import ru.maxeltr.homeMq2t.Model.Msg;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import ru.maxeltr.homeMq2t.Entity.CardEntity;
import ru.maxeltr.homeMq2t.Entity.DashboardEntity;
import ru.maxeltr.homeMq2t.Model.CardModel;
import ru.maxeltr.homeMq2t.Model.MsgImpl;

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

//    @Autowired
//    private ObjectMapper mapper;

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
        Msg.Builder msg = new MsgImpl.MsgBuilder("onConnect").type(MediaType.APPLICATION_JSON_VALUE);

        Promise<MqttConnAckMessage> authFuture = this.mediator.connect();
        authFuture.awaitUninterruptibly(this.connectTimeout);

        if (authFuture.isCancelled()) {
            logger.info("Connection attempt to remote server was canceled.");
            msg.data(this.createJsonResponse(this.getStartDashboard(), "onConnect", "fail"));
        } else if (!authFuture.isSuccess()) {
            logger.info("Connection established failed.");
            msg.data(this.createJsonResponse(this.getStartDashboard(), "onConnect", "fail"));
        } else {
            logger.info("Connection established successfully.");
            msg.data(this.createJsonResponse(this.getStartDashboard(), "onConnect", "ok"));
        }

        msg.timestamp(String.valueOf(Instant.now().toEpochMilli()));
        this.display(msg, "");
    }

    @Override
    public void editCardSettings(Msg.Builder msg) {
        logger.debug("Do edit settings {}.", msg);

        msg.type(MediaType.APPLICATION_JSON_VALUE);

        Optional<CardModel> cardSettingsOpt = this.appProperties.getCardSettings(msg.getId());
        if (cardSettingsOpt.isPresent()) {
            logger.info("Settings retrieved successfully. Card={}", msg.getId());
            msg.data(this.createJsonResponse(cardSettingsOpt.get().getHtml(), "onEditCardSettings", "ok"));
        } else {
            logger.warn("Could not get settings for card={}.", msg.getId());
            msg.data(this.createJsonResponse("", "onEditCardSettings", "fail"));
        }

        msg.timestamp(String.valueOf(Instant.now().toEpochMilli()));
        this.display(msg, "");
    }

    @Override
    public void saveCardSettings(Msg.Builder msg) {
        logger.debug("Do save settings {}.", msg);

        CardEntity cardEntity;
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        JsonNode root;

        try {

            cardEntity = mapper.readValue(msg.getData(), CardEntity.class);

            root = mapper.readTree(msg.getData());

            DashboardEntity dashboardEntity = appProperties.getDashboardEntity(root.path("dashboardNumber").asText()).orElseThrow();

            cardEntity.setDashboard(dashboardEntity);
            ;
            logger.info("saveCardSettings={}", this.appProperties.saveCardEntity(cardEntity));
        } catch (JsonProcessingException | NoSuchElementException ex) {
            logger.warn("Could not convert json data={} to map. {}", msg, ex.getMessage());

        }

    }

    /**
     * Construct a Json-romatted response string containing the given HTML,
     * event name, and status. The HTML is optionally prefixed with an error or
     * unknown status message, then Base64 encoded and embedded in the JSON
     * payload. If the status is "ok", the HTML is included as is. If the status
     * is "fail", an error message is prepended. For any other status value, an
     * "undefined status" message is prepended.
     *
     * <p>
     * The generated JSON has the following structure
     * <pre>{@code
     * {
     * 		"name": "<event>",
     * 		"status": "<status>",
     * 		"type": "<text/html;base64>",
     * 		"data": "<base64-encoded HTML with opitonal prefix>"
     * }
     * }</pre>
     * </p>
     *
     * @param dashboard the raw HTML content to include in the response
     * @param event the name of the event assosiated with this response
     * @param status the status of the last action, expected values are "ok" or
     * "fail".
     *
     * @return a Json string with the event name, status, content type and
     * Base64-encoded HTML with optional error or unknown status prefix.
     */
    private String createJsonResponse(String dashboard, String event, String status) {
        String errorCaption = "<div style=\"color:red;\">There was an error while loading the dashboard. Please check the logs for more details.</div>";
        String unknownStatusCaption = "<div style=\"color:red;\">The last action completed with an undefined status. Please check the logs for more details.</div>";

        String form = switch (status) {
            case "ok" ->
                dashboard;
            case "fail" ->
                errorCaption + dashboard;
            default ->
                unknownStatusCaption + dashboard;
        };

        String data = "{\"name\": \"" + event + "\", \"status\": \""
                + status
                + "\", \"type\": \"text/html;base64\", \"data\": \""
                + Base64.getEncoder().encodeToString(form.getBytes(StandardCharsets.UTF_8))
                + "\"}";

        logger.debug("Send ui data={}.", data);

        return data;
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

    private String getStartDashboard() {
        List<Dashboard> dashboards = appProperties.getDashboards();

        if (dashboards == null || dashboards.isEmpty()) {
            logger.info("Dashboard list is empty or null.");
            return "<div style=\"color:red;\">No dashboards available.</div>";
        }

        if (dashboards.get(0).getCards().isEmpty()) {
            logger.info("Card list is empty.");
            return "<div style=\"color:red;\">No cards available.</div>";
        }

        return dashboards.get(0).getHtml();
    }

    /**
     * Convert the given qos value from string to MqttQos enum instance. If the
     * qos value is invalid, it defaults to qos level 0.
     *
     * @param qosString The qos value as a string. Must not be null.
     * @return The qos level as a MqttQos enum value.
     */
    private MqttQoS convertToMqttQos(String qosString) {
        MqttQoS qos;
        try {
            qos = MqttQoS.valueOf(qosString);
        } catch (IllegalArgumentException ex) {
            logger.error("Invalid QoS value for the given qos string={}: {}. Set QoS=0.", qosString, ex.getMessage());
            qos = MqttQoS.AT_MOST_ONCE;
        }

        return qos;
    }

    @Override
    public void publish(Msg.Builder msg) {
        String topic = this.appProperties.getCardPubTopic(msg.getId());
        if (StringUtils.isEmpty(topic)) {
            logger.info("Could not publish. There is no topic for card={}", msg.getId());
            return;
        }

        MqttQoS qos = this.convertToMqttQos(this.appProperties.getCardPubQos(msg.getId()));

        boolean retain = Boolean.parseBoolean(this.appProperties.getCardPubRetain(msg.getId()));

        String type = this.appProperties.getCardPubDataType(msg.getId());
        if (StringUtils.isEmpty(type)) {
            logger.info("Type is empty for card={}. Set text/plain.", msg.getId());
            type = MediaType.TEXT_PLAIN_VALUE;
        }

        msg.type(type);
        msg.data(this.appProperties.getCardPubData(msg.getId()));
        msg.timestamp(String.valueOf(Instant.now().toEpochMilli()));

        logger.info("Creating publish message for card={}, topic={}, QoS={}, retain={}.", msg.getId(), topic, qos, retain);

        this.mediator.publish(msg.build(), topic, qos, retain);
    }

    @Override
    public void launch(Msg.Builder msg) {
        String path = this.appProperties.getCardLocalTaskPath(msg.getId());
        if (StringUtils.isEmpty(path)) {
            logger.info("There is no local task to launch for msg={}.", msg.getId());
            return;
        }

        String arguments = this.appProperties.getCardLocalTaskArguments(msg.getId());
        logger.info("Launch local task for msg={}. commandPath={}, arguments={}.", msg.getId(), path, arguments);
        String data = this.mediator.execute(path, arguments);
        if (data == null) {
            data = "";
        }

        Msg.Builder builder = new MsgImpl.MsgBuilder()
                .data(data)
                .timestamp(String.valueOf(Instant.now().toEpochMilli()))
                .type(this.appProperties.getCardLocalTaskDataType(msg.getId()));

        logger.info("Display result of local task for msg={}.", msg.getId());
        this.display(builder, msg.getId());
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
                logger.info("Type is undefined for card={}. Set application/json.", cardNumber);
                builder.type(MediaType.APPLICATION_JSON_VALUE);
            }
        }

        if (builder.getType().equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE)) {
            String jsonPathExpression = this.appProperties.getCardJsonPathExpression(cardNumber);
            if (StringUtils.isNotEmpty(jsonPathExpression)) {
                String parsedValue = this.parseJson(builder.getData(), jsonPathExpression);
                logger.debug("Parsed data by using jsonPath. Parsed value={}.", parsedValue);
                String dataName = this.parseJson(builder.getData(), "name");
                String status = this.parseJson(builder.getData(), "status");
                builder.data("{\"name\": \"" + dataName + "\", \"type\": \"" + MediaType.TEXT_PLAIN_VALUE + "\", \"data\": \"" + parsedValue + "\", \"status\": \"" + status + "\"}");
            } else {
                logger.debug("JsonPath expression is empty for card={}.", cardNumber);
            }
        }

        builder.data(Jsoup.clean(builder.getData(), Safelist.basic()));
        logger.debug("Display data={}. Card={}", builder, cardNumber);
        this.uiController.display(builder.build(), cardNumber);
    }

    private String parseJson(String json, String jsonPathExpression) {
        String parsedValue = "";
        try {
            parsedValue = JsonPath.parse(json).read(jsonPathExpression, String.class);
        } catch (Exception ex) {
            logger.info("Could not parse json. {}", ex.getMessage());
        }

        return parsedValue;
    }
}
