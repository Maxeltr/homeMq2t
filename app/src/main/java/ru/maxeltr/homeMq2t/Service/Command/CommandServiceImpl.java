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
package ru.maxeltr.homeMq2t.Service.Command;

import io.netty.handler.codec.mqtt.MqttQoS;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import ru.maxeltr.homeMq2t.Config.AppProperties;
import ru.maxeltr.homeMq2t.Model.Msg;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.apache.commons.lang3.StringUtils;
import ru.maxeltr.homeMq2t.Model.MsgImpl;
import ru.maxeltr.homeMq2t.Service.ServiceMediator;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class CommandServiceImpl implements CommandService {

    private static final Logger logger = LoggerFactory.getLogger(CommandServiceImpl.class);

    private long timeout = 5_000;

    private ServiceMediator mediator;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private ObjectMapper mapper;

    @Override
    public void setMediator(ServiceMediator mediator) {
        this.mediator = mediator;
    }

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
    private String extractCommandName(Msg msg) {
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

        return command;
    }

    /**
     * Asynchronously executes a command based on the provided message and
     * command number.
     *
     * The method processes the message to extract the command and its
     * associated parameters, executes the command and send the result to the
     * specified Mqtt topic.
     *
     * @param builder containing the data to be processed. Must not be null.
     * @param commandNumber A unique identifier for the command being executed.
     * Must not be null.
     */
    @Async("processExecutor")
    @Override
    public void execute(Msg.Builder builder, String commandNumber) {
        String command = this.extractCommandName(builder.build());
        if (StringUtils.isEmpty(command)) {
            logger.warn("Command is empty. Command number={}", commandNumber);
            return;
        }

        String commandPath = this.appProperties.getCommandPath(command);
        if (StringUtils.isEmpty(commandPath)) {
            logger.warn("Command path is empty. Command={}, commandNumber={}", command, commandNumber);
            return;
        }

        String arguments = this.appProperties.getCommandArguments(command);

        logger.info("Execute command. name={}, commandNumber={}, commandPath={}, arguments={}.", command, commandNumber, commandPath, arguments);

        String result = this.execute(commandPath, arguments);

        String topic = appProperties.getCommandPubTopic(command);
        if (StringUtils.isEmpty(topic)) {
            logger.info("Could not send reply. Command {} publication topic is empty.", command);
            return;
        }

        MqttQoS qos = this.convertToMqttQos(appProperties.getCommandPubQos(command));

        boolean retain = Boolean.parseBoolean(appProperties.getCommandPubRetain(command));

        this.sendReply(result, command, topic, qos, retain);
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

    private void sendReply(String data, String commandName, String topic, MqttQoS qos, boolean retain) {
        String type = this.appProperties.getCommandPubDataType(commandName);
        if (StringUtils.isEmpty(type)) {
            logger.info("Property type is empty for command={}. Set text/plain.", commandName);
            type = MediaType.TEXT_PLAIN_VALUE;
        }

        Msg.Builder builder = new MsgImpl.MsgBuilder("onExecuteCommand")
                .type(type)
                .timestamp(String.valueOf(Instant.now().toEpochMilli()))
                .data(data);

        Msg msg = builder.build();
        this.mediator.publish(builder.build(), topic, qos, retain);

        logger.info("Reply on command={} has been sent. Msg={}, topic={}, qos={}, retain={}.", commandName, msg, topic, qos, retain);
    }

    /**
     * Execute a command in the system shell and return the output as a string.
     *
     * This method uses ProcessBuilder to start a process with the specified
     * command path and arguments. It captures the output of the command and
     * handles any potential errors that may occur during the execution. if the
     * process does not complete within the specified timeout it will be
     * forcibly terminated.
     *
     * @param commandPath the path to the command to be executed
     * @param arguments the arguments to be passed to the command. This should
     * be a single string containing all arguments separated by space.
     * @return the output of the command as a string. If error occursduring
     * execution, an error message will be returned instead.
     */
    @Override
    public String execute(String commandPath, String arguments) {
        logger.debug("Start command task. commandPath={}, arguments={}.", commandPath, arguments);

        ProcessBuilder pb = new ProcessBuilder(commandPath, arguments);
        pb.redirectErrorStream(true);

        Process process;
        try {
            process = pb.start();
        } catch (IOException ex) {
            logger.warn("ProcessBuilder cannot start. commandPath={}, arguments={}. {}", commandPath, arguments, ex.getMessage());
            return "Error. Could execute command.";
        }

        int character;
        StringBuilder result = new StringBuilder();
        long startTime = System.currentTimeMillis();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while ((character = br.read()) != -1) {
                result.append((char) character);
                if (System.currentTimeMillis() - startTime > timeout) {
                    logger.warn("Reading process output timed out. commandPath={}, arguments={}. {}", commandPath, arguments);
                    break;
                }
            }
        } catch (IOException ex) {
            logger.warn("Can not read process output of command. commandPath={}, arguments={}. {}", commandPath, arguments, ex.getMessage());
            result.append("Error. Could not read output.");
        }

        boolean finished = false;
        try {
            finished = process.waitFor(this.timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.warn("Waiting for process was interrupted.", ex.getMessage());
        }

        if (!finished) {
            logger.warn("Process did not finish in time={}. Destroy process. commandPath={}, arguments={}.", this.timeout, commandPath, arguments);
            process.destroyForcibly();
        } else {
            logger.info("Command has been executed. commandPath={}, arguments={}. exitCode={}", commandPath, arguments, process.exitValue());
        }

        return result.toString();
    }

}
