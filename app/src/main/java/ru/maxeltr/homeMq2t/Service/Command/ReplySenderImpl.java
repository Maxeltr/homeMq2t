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

import io.netty.handler.codec.mqtt.MqttQoS;
import java.time.Instant;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import ru.maxeltr.homeMq2t.Config.AppProperties;
import ru.maxeltr.homeMq2t.Model.Msg;
import ru.maxeltr.homeMq2t.Model.MsgImpl;
import ru.maxeltr.homeMq2t.Service.ServiceMediator;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class ReplySenderImpl implements ReplySender {

    private static final Logger logger = LoggerFactory.getLogger(ReplySenderImpl.class);

    @Autowired
    private AppProperties appProperties;

    @Autowired
    @Lazy               //TODO
    private ServiceMediator mediator;

    @Override
    public void sendReply(String data, String commandName) {
        String topic = appProperties.getCommandPubTopic(commandName);
        if (StringUtils.isEmpty(topic)) {
            logger.info("Could not send reply. Command {} publication topic is empty.", commandName);
            return;
        }

        MqttQoS qos = this.convertToMqttQos(appProperties.getCommandPubQos(commandName));

        boolean retain = Boolean.parseBoolean(appProperties.getCommandPubRetain(commandName));

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
        logger.info("Sending reply on command={}. Msg={}, topic={}, qos={}, retain={}.", commandName, msg, topic, qos, retain);
        this.mediator.publish(builder.build(), topic, qos, retain);
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
}
