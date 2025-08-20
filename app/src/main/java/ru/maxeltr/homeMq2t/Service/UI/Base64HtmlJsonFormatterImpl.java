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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import ru.maxeltr.homeMq2t.Model.Status;
import ru.maxeltr.homeMq2t.Model.ViewModel;

public class Base64HtmlJsonFormatterImpl implements UIJsonFormatter {

    private static final Logger logger = LoggerFactory.getLogger(Base64HtmlJsonFormatterImpl.class);

    @Autowired
    private ObjectMapper mapper;

    private static final String MEDIA_TYPE_BASE64_HTML = "text/html;base64";

    private static final String ERROR_CAPTION = "<div style=\"color:red;\">There was an error while loading the dashboard. Please check the logs for more details.</div>";

    private static final String UNKNOWN_STATUS_CAPTION = "<div style=\"color:red;\">The last action completed with an undefined status. Please check the logs for more details.</div>";

    private static final String ERROR_JSON_PROCESSING = "{\"name\": \"\", \"status\": \"fail\", \"type\": \"" + MediaType.TEXT_PLAIN_VALUE + "\", \"data\": \"Could not serialize json.\"}";

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
    @Override
    public String createJson(String dashboard, String event, Status status) {

        String form = switch (status) {
            case OK ->
                dashboard;
            case FAIL ->
                ERROR_CAPTION + dashboard;
            case UNKNOWN ->
                UNKNOWN_STATUS_CAPTION + dashboard;
        };

        return buildJson(event, status.getValue(), MEDIA_TYPE_BASE64_HTML, Base64.getEncoder().encodeToString(form.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public String createJson(String msg, String jsonPathExpression) {
        String parsedValue = this.parseJson(msg, jsonPathExpression);
        logger.debug("Parsed data by using jsonPath. Parsed value={}.", parsedValue);
        String dataName = this.parseJson(msg, "name");
        String status = this.parseJson(msg, "status");

        return buildJson(dataName, status, MediaType.TEXT_PLAIN_VALUE, parsedValue);
    }

    public <T extends ViewModel> String createJson(Optional<T> modelOpt, String name) {
        if (modelOpt.isPresent()) {
            return this.createJson(modelOpt.get().getHtml(), name, Status.OK);
        }

        return this.createJson("", name, Status.FAIL);
    }

    private String buildJson(String name, String status, String type, String data) {

        ObjectNode root = mapper.createObjectNode();
        root.put("name", name);
        root.put("status", status);
        root.put("type", type);
        root.put("data", data);

        String result;
        try {
            result = mapper.writeValueAsString(root);
        } catch (JsonProcessingException ex) {
            logger.error("Could not serialize json.", ex);
            result = ERROR_JSON_PROCESSING;
        }

        return result;
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
