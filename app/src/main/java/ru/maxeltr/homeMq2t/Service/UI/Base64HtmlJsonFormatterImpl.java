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
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import ru.maxeltr.homeMq2t.Model.Msg;
import ru.maxeltr.homeMq2t.Model.MsgImpl;
import ru.maxeltr.homeMq2t.Model.Status;

public class Base64HtmlJsonFormatterImpl implements UIJsonFormatter {

    private static final Logger logger = LoggerFactory.getLogger(Base64HtmlJsonFormatterImpl.class);

    @Autowired
    private ObjectMapper mapper;

    private static final String ERROR_CAPTION = "<div style=\"color:red;\">There was an error while loading the dashboard. Please check the logs for more details.</div>";

    private static final String UNKNOWN_STATUS_CAPTION = "<div style=\"color:red;\">The last action completed with an undefined status. Please check the logs for more details.</div>";

    private static final String ERROR_JSON_PROCESSING = "{\"type\": \"" + MediaType.TEXT_PLAIN_VALUE + "\", \"data\": \"Could not serialize json.\"}";

    private static final String SEPARATOR_EQ = " = ";

    /**
     * Parses a value from the provided JSON message using the given JSONPath
     * expression and returns a JSON string that contains the parsed value with
     * a media type.
     *
     * @param msg the JSON message to parse
     * @param jsonPathExpression the JSONPath expression used to extract a
     * single value from msg
     * @return a JSON string produced by {@link #buildJson(String, String)}
     * containing the parsed value
     */
    @Override
    public String parseAndCreateJson(String msg, String jsonPathExpression) {
        String parsedValue = this.parseJson(msg, jsonPathExpression);
        logger.debug("Parsed data by using jsonPath. Parsed value={}.", parsedValue);

        return buildJson(MediaType.TEXT_PLAIN_VALUE, parsedValue);
    }

    /**
     * Parses multiple values from the provided JSON message using the given
     * list of JSONPath expressions and returns a JSON string that contains the
     * concatenated results with a media type.
     *
     * @param msg the JSON message to parse
     * @param jsonPathExpressions list of JSONPath expressions
     * @return a JSON string produced by {@link #buildJson(String, String)}
     * containing the concatenated expression=value lines (each pair separated
     * by the platform line separator)
     */
    @Override
    public String parseAndCreateJson(String msg, List<String> jsonPathExpressions) {
        StringBuilder sb = new StringBuilder();
        for (String exp : jsonPathExpressions) {
            sb.append(exp)
                    .append(SEPARATOR_EQ)
                    .append(this.parseJson(msg, exp))
                    .append(System.lineSeparator());

        }
        logger.debug("Parsed data by using jsonPath. {}.", sb.toString());

        return buildJson(MediaType.TEXT_PLAIN_VALUE, sb.toString());
    }

    /**
     * Encode the given HTML. The HTML is optionally prefixed with an error or
     * unknown status message. If the status is "ok", the HTML is included as
     * is. If the status is "fail", an error message is prepended. For any other
     * status value, an "undefined status" message is prepended.
     *
     * @param dashboard the raw HTML content to include in the response
     * @param status the status of the last action, expected values are "ok" or
     * "fail".
     *
     * @return Base64-encoded HTML with optional error or unknown status prefix.
     */
    @Override
    public String createAndEncodeHtml(String dashboard, Status status) {
        String form = switch (status) {
            case OK ->
                dashboard;
            case FAIL ->
                ERROR_CAPTION + dashboard;
            case UNKNOWN ->
                UNKNOWN_STATUS_CAPTION + dashboard;
        };

        return Base64.getEncoder().encodeToString(form.getBytes(StandardCharsets.UTF_8));
    }

    private String buildJson(String type, String data) {
        Msg msg = MsgImpl.newBuilder()
                .data(data)
                .type(type)
                .build();

        String result;
        try {
            result = mapper.writeValueAsString(msg);
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
