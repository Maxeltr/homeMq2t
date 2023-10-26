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

import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.util.concurrent.Promise;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.maxeltr.homeMq2t.Model.Command;
import ru.maxeltr.homeMq2t.Model.Data;
import ru.maxeltr.homeMq2t.Model.DataImpl;
import ru.maxeltr.homeMq2t.Model.Reply;
import ru.maxeltr.homeMq2t.Mqtt.HmMq2t;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class CommandServiceImpl implements CommandService {

    private static final Logger logger = LoggerFactory.getLogger(CommandServiceImpl.class);

    private ServiceMediator mediator;

    @Override
    public void setMediator(ServiceMediator mediator) {
        this.mediator = mediator;
    }

    @Override
    public void execute(Command command) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void execute(Reply reply) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public void connect() {
        logger.debug("Do connect.");
        Promise<MqttConnAckMessage> authFuture = mediator.connect();

        authFuture.awaitUninterruptibly();
        if (authFuture.isCancelled()) {
            // Connection attempt cancelled by user
            logger.debug("Connection attempt cancelled.");
            Data data = new DataImpl("CONNECT", "TEXT/PLAIN", "Connection attempt cancelled.", "fail", String.valueOf(Instant.now().toEpochMilli()));
            mediator.display(data);
        } else if (!authFuture.isSuccess()) {
            logger.debug("Connection established failed {}", authFuture.cause());
        } else {
            // Connection established successfully
            logger.debug("connectFuture. Connection established successfully.");
        }
    }

    private buildHtml() {
        
    }
}
