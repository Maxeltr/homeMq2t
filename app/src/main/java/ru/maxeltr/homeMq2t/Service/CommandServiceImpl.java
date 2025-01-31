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
import ru.maxeltr.homeMq2t.Model.MsgImpl;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class CommandServiceImpl implements CommandService {

    private static final Logger logger = LoggerFactory.getLogger(CommandServiceImpl.class);

    private int waitForProcess = 60_000;

    private ServiceMediator mediator;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private ObjectMapper mapper;

    @Override
    public void setMediator(ServiceMediator mediator) {
        this.mediator = mediator;
    }

    @Async("processExecutor")
    @Override
    public void execute(Msg.Builder builder, String commandNumber) {
        String command = "";
        Msg msg = builder.build();
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
                return;
            }
            command = Optional.ofNullable(dataMap.get("name")).orElse("");
        } else {
            logger.warn("Unsupported type={}. Command number={}. Data={}", msgType, commandNumber, msg.getData());
        }

        if (command.trim().isEmpty()) {
            logger.warn("Command is empty. Command number={}", commandNumber);
            return;
        }

        String topic = appProperties.getCommandPubTopic(command);
        MqttQoS qos = MqttQoS.valueOf(appProperties.getCommandPubQos(command));
        boolean retain = Boolean.parseBoolean(appProperties.getCommandPubRetain(command));

        if (topic.trim().isEmpty()) {
            logger.warn("Command {} publication topic is empty.", command);
            return;
        }

        String commandPath = this.appProperties.getCommandPath(command);
        if (commandPath.trim().isEmpty()) {
            logger.warn("Command path is empty. Command={}, commandNumber={}", command, commandNumber);
            return;
        }

        String arguments = this.appProperties.getCommandArguments(command);

        logger.info("Execute command. name={}, commandNumber={}, commandPath={}, arguments={}.", command, commandNumber, commandPath, arguments);

        String result = this.executeCommand(commandPath, arguments);

        this.sendReply(result, command, topic, qos, retain);
    }

    private void sendReply(String data, String commandName, String topic, MqttQoS qos, boolean retain) {
        Msg.Builder builder = new MsgImpl.MsgBuilder("onExecuteCommand").type(this.appProperties.getCommandPubDataType(commandName));
        builder.timestamp(String.valueOf(Instant.now().toEpochMilli()));
        builder.data(data);
        Msg msg = builder.build();
        this.mediator.publish(msg, topic, qos, retain);

        logger.info("Reply on command={} has been sent. Msg={}, topic={}, qos={}, retain={}.", commandName, msg, topic, qos, retain);
    }

    private String executeCommand(String commandPath, String arguments) {
        logger.debug("Start command task. commandPath={}, arguments={}.", commandPath, arguments);

        ProcessBuilder pb = new ProcessBuilder(commandPath, arguments);
        pb.redirectErrorStream(true);

        Process p;
        try {
            p = pb.start();
        } catch (IOException ex) {
            logger.warn("ProcessBuilder cannot start. commandPath={}, arguments={}. {}", commandPath, arguments, ex.getMessage());
            return "Error. Could not start.";
        }

        String line;
        StringBuilder result = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            while (true) {
                line = br.readLine();
                if (line == null) {
                    break;
                }
                result.append(line);
            }
        } catch (IOException ex) {
            logger.warn("Can not read process output of command. commandPath={}, arguments={}. {}", commandPath, arguments, ex.getMessage());
            return "Error. Could not read output.";
        }

        int exitCode = 0;
        try {
            boolean finished = p.waitFor(this.waitForProcess, TimeUnit.MILLISECONDS);
            if (!finished) {
                logger.warn("Process did not finish in time={}. commandPath={}, arguments={}.", this.waitForProcess, commandPath, arguments);
                p.destroy();
                return "Error. Process timed out.";
            }
            exitCode = p.exitValue();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.warn("Waiting for process was interrupted.", ex.getMessage());
        }

        if (exitCode != 0) {
            logger.warn("Command executed with error. commandPath={}, arguments={}. exitCode={}", commandPath, arguments, exitCode);
        }

        logger.debug("End command task. commandPath={}, arguments={}.", commandPath, arguments);

        return result.toString();
    }

}
