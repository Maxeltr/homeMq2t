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
package ru.maxeltr.homeMq2t.Controller;


import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.util.concurrent.Promise;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import ru.maxeltr.homeMq2t.Model.Data;
import ru.maxeltr.homeMq2t.Model.DataImpl;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import ru.maxeltr.homeMq2t.Model.Data;

import ru.maxeltr.homeMq2t.Model.Reply;
import ru.maxeltr.homeMq2t.Service.ServiceMediator;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */

@Controller
public class UIControllerImpl implements UIController {

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    Logger logger = LoggerFactory.getLogger(UIControllerImpl.class);

    private ServiceMediator mediator;

    
    @MessageMapping("/connect")
    public void connect(Data msg) {
        logger.info("{} command received." , msg.getName());
        
        /*Promise<MqttConnAckMessage> authFuture = mediator.connect();

        authFuture.awaitUninterruptibly();
        if (authFuture.isCancelled()) {
            // Connection attempt cancelled by user
            logger.info("Connection attempt was cancelled.");
            Data data = new DataImpl("connect", "TEXT/PLAIN", "Connection attempt was cancelled.", "fail", String.valueOf(Instant.now().toEpochMilli()));
            mediator.display(data);
        } else if (!authFuture.isSuccess()) {
            logger.info("Connection established failed {}", authFuture.cause());
        } else {
            // Connection established successfully
            logger.info("connectFuture. Connection established successfully.");
        }*/


    }


    @Override
    public void setMediator(ServiceMediator mediator) {
        this.mediator = mediator;
    }

    @Override
    public void display(Data data) {

        simpMessagingTemplate.convertAndSend("/topic/data", data, Map.of("card", "card1"));
        logger.debug("Data was sent to display {}." , data);
    }

    @Override
    public void display(Reply reply) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
