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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import ru.maxeltr.homeMq2t.Config.AppProperties;
import ru.maxeltr.homeMq2t.Model.Msg;
import org.apache.commons.lang3.StringUtils;
import ru.maxeltr.homeMq2t.Service.ServiceMediator;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class CommandServiceImpl implements CommandService {

    private static final Logger logger = LoggerFactory.getLogger(CommandServiceImpl.class);

    private ServiceMediator mediator;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private CommandParser commandParser;

    @Autowired
    private ProcessExecutor processExecutor;

    @Autowired
    private ReplySender replySender;

//    @Autowired
//    private CommandPropertiesProvider appProperties;

    @Override
    public void setMediator(ServiceMediator mediator) {
        this.mediator = mediator;
    }

    /**
     * Asynchronously executes a command based on the provided message and
     * command number.
     *
     * The method processes the message to extract the command and its
     * associated parameters, executes the command and send the result to the
     * specified Mqtt topic.
     *
     * @param msg containing the data to be processed. Must not be null.
     * @param commandNumber A unique identifier for the command being executed.
     * Must not be null.
     */
    @Async("processExecutor")
    @Override
    public void execute(Msg msg, String commandNumber) {
        String command = this.commandParser.parseCommandName(msg);
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

        logger.info("Executing command. name={}, commandNumber={}, commandPath={}, arguments={}.", command, commandNumber, commandPath, arguments);

        String result = this.processExecutor.execute(commandPath, arguments);

        this.replySender.sendReply(result, command);
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
    @Async("processExecutor")
    @Override
    public String execute(String commandPath, String arguments) {
        return this.processExecutor.execute(commandPath, arguments);
    }
}
