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

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class CommandServiceImpl implements CommandService {

    private static final Logger logger = LoggerFactory.getLogger(CommandServiceImpl.class);

    private ServiceMediator mediator;

    @Autowired
    private AppProperties appProperties;

    @Override
    public void setMediator(ServiceMediator mediator) {
        this.mediator = mediator;
    }

    @Async("processExecutor")
    @Override
    public void execute(Msg.Builder builder, String commandNumber) {
        String command = "";
        Msg msg = builder.build();
        if (msg.getType().equalsIgnoreCase(MediaType.TEXT_PLAIN_VALUE)) {
            command = msg.getData();
        } else if (msg.getType().equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE)) {
            //TODO
            throw new UnsupportedOperationException("Not supported yet.");
        }

        if (command.trim().isEmpty()) {
            logger.warn("Command is empty.");
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

        this.sendReply(this.executeCommand(commandPath, arguments), command, topic, qos, retain);
    }

    private void sendReply(String data, String commandName, String topic, MqttQoS qos, boolean retain) {
        Msg.Builder builder = new Msg.Builder("onExecuteCommand").type(this.appProperties.getCommandPubDataType(commandName));
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
            return "";
        }

        String line;
        String result = "";

        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            while (true) {
                line = br.readLine();
                if (line == null) {
                    break;
                }
                result += line;
            }
        } catch (IOException ex) {
            logger.warn("Can not read process output of command. commandPath={}, arguments={}. {}", commandPath, arguments, ex.getMessage());
            return "";
        }

        logger.debug("End command task. commandPath={}, arguments={}.", commandPath, arguments);

        return result;
    }

}
