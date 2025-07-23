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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.maxeltr.homeMq2t.Model.Msg;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class ProcessExecutorImpl implements ProcessExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ProcessExecutorImpl.class);

    private long timeout = 5_000;

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
