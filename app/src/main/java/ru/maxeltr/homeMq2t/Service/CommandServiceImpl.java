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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import ru.maxeltr.homeMq2t.Model.Msg;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class CommandServiceImpl implements CommandService {

    private static final Logger logger = LoggerFactory.getLogger(CommandServiceImpl.class);

    private ServiceMediator mediator;

    @Autowired
    private Map<String, String> CommandsAndNumbers;

    @Autowired
    private Environment env;

    @Override
    public void setMediator(ServiceMediator mediator) {
        this.mediator = mediator;
    }

    @Async("processExecutor")
    @Override
    public void execute(Msg.Builder command) {
        logger.info("Start command task. Msg={}.", msg);

        String command = "";
        if (msg.getType().equalsIgnoreCase("text/plain")) {
            command = msg.getData();
        } else if (msg.getType().equalsIgnoreCase("application/json")) {
            //TODO
            throw new UnsupportedOperationException("Not supported yet.");
        }

        if (command.trim().isEmpty()) {
            logger.warn("Command is empty.");
            return "";
        }

        String commandNumber = CommandsAndNumbers.get(command);

        String commandPath = env.getProperty("command[" + commandNumber + "]." + "path", "");   //TODO move to app prop class. left here only - getCommandPath(command)/ the rest likewise
        if (commandPath.trim().isEmpty()) {
            logger.warn("Command path is empty. Command={}, commandNumber={}", command, commandNumber);
            return "";
        }

        String arguments = env.getProperty("command[" + commandNumber + "]." + "arguments", "");

        Msg.Builder msg = new Msg.Builder("onExecuteCommand").type("text/plain");   //TODO create ENUM fot mime types
        msg.timestamp(String.valueOf(Instant.now().toEpochMilli()));
        msg.data(this.executeCommand(command.build()));

        logger.info("Create message {}.", msg);

        this.mediator.publish(msg.build(), topic, qos, retain);
    }

    private String executeCommand(Msg msg) {


        logger.info("Start execute command. name={}, commandNumber={}, commandPath={}, arguments={}.", command, commandNumber, commandPath, arguments);

        String line;
        ProcessBuilder pb = new ProcessBuilder(commandPath, arguments);
        pb.redirectErrorStream(true);
        Process p;
        try {
            p = pb.start();
        } catch (IOException ex) {
            logger.warn("ProcessBuilder cannot start. Command name={}. {}", command, ex.getMessage());
            return "";
        }

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
            logger.warn("Can not read process output of command name={}. {}", command, ex.getMessage());
            return "";
        }

        logger.info("End command task. Msg={}.", msg);

        return result;
    }

}
