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

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import ru.maxeltr.homeMq2t.Config.UIPropertiesProvider;
import ru.maxeltr.homeMq2t.Model.Msg;
import ru.maxeltr.homeMq2t.Service.ServiceMediator;

public class Base64HtmlJsonFormatterImpl implements Base64HtmlJsonFormatter {

    private static final Logger logger = LoggerFactory.getLogger(Base64HtmlJsonFormatterImpl.class);

    @Autowired
    @Lazy               //TODO
    private ServiceMediator mediator;

    @Autowired
    @Qualifier("getUIPropertiesProvider")
    private UIPropertiesProvider appProperties;

    private static String BASE64_HTML_MEDIA_TYPE = "text/html;base64";

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
    public String createJson(String dashboard, String event, String status) {
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

        StringBuilder sb = new StringBuilder();
        sb.append("{\"name\": \"")
                .append(event)
                .append("\", \"status\": \"")
                .append(status)
                .append("\", \"type\": \"")
                .append(BASE64_HTML_MEDIA_TYPE)
                .append("\", \"data\": \"")
                .append(Base64.getEncoder().encodeToString(form.getBytes(StandardCharsets.UTF_8)))
                .append("\"}");

        return sb.toString();
    }

    @Override
    public String createJson(String msg, String jsonPathExpression) {
        String parsedValue = this.parseJson(msg, jsonPathExpression);
        logger.debug("Parsed data by using jsonPath. Parsed value={}.", parsedValue);
        String dataName = this.parseJson(msg, "name");
        String status = this.parseJson(msg, "status");

        StringBuilder sb = new StringBuilder();
        sb.append("{\"name\": \"")
                .append(dataName)
                .append("\", \"status\": \"")
                .append(status)
                .append("\", \"type\": \"")
                .append(MediaType.TEXT_PLAIN_VALUE)
                .append("\", \"data\": \"")
                .append(parsedValue)
                .append("\"}");

        return sb.toString();
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
