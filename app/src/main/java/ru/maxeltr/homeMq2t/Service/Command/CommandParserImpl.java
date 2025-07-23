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
package ru.maxeltr.homeMq2t.Service.Command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import ru.maxeltr.homeMq2t.Model.Msg;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class CommandParserImpl implements CommandParser {

    private static final Logger logger = LoggerFactory.getLogger(CommandParserImpl.class);

    @Autowired
    private ObjectMapper mapper;

    /**
     * Extracts the command from the given message. The method checks the
     * message type and retrieves the command accordingly. If the message type
     * is Json, it attempts to parse the data to extract the command name.
     *
     * @param msg The message from which to extract the command. Must not be
     * null.
     * @return The extraxted command as a String, or empty string if command
     * could not be determined.
     */
    @Override
    public String parseCommandName(Msg msg) {
        String command = "";
        String msgType = msg.getType();
        if (msgType.equalsIgnoreCase(MediaType.TEXT_PLAIN_VALUE)) {
            command = msg.getData();
        } else if (msgType.equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE)) {
            HashMap<String, String> dataMap;
            try {
                dataMap = mapper.readValue(msg.getData(), new TypeReference<HashMap<String, String>>() {
                });
            } catch (JsonProcessingException ex) {
                logger.warn("Could not convert json data={} to map. {}", msg.getData(), ex.getMessage());
                return "";
            }
            command = dataMap.getOrDefault("name", "");
        } else {
            logger.warn("Unsupported type={}. Data={}", msgType, msg.getData());
        }
        logger.info("Parsed command name={}.", command);

        return command;
    }

}
