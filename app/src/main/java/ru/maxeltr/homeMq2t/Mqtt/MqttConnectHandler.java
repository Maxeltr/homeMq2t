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
package ru.maxeltr.homeMq2t.Mqtt;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttProperties;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Promise;
import java.nio.charset.Charset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class MqttConnectHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MqttConnectHandler.class);

//    @Autowired
    private final MqttAckMediator mqttAckMediator;

    @Value("${protocol-name:MQTT}")
    private String protocolName = "MQTT";

    @Value("${protocol-version:4}")
    private int version;

    @Value("${has-user-name:true}")
    private Boolean hasUserName;

    @Value("${has-password:true}")
    private Boolean hasPassword;

    @Value("${will-retain:false}")
    private Boolean willRetain;

    @Value("${will-qos:0}")
    private int willQos;

    @Value("${will-flag:false}")
    private Boolean willFlag;

    @Value("${clean-session:true}")
    private Boolean cleanSession;

    @Value("${keep-alive-timer:20}")
    private int keepAliveTimer;

    @Value("${client-id:defaultClientId}")
    private String clientId;

    @Value("${will-topic:defaultWillTopic}")
    private String willTopic;

    @Value("${will-message:defaultWillMessage}")
    private String willMessage;

    @Value("${mq2t-username:defaultUserName}")
    private String username;

    @Value("${mq2t-password:defaultPassword}")
    private String password;

    MqttConnectHandler(MqttAckMediator mqttAckMediator) {
        this.mqttAckMediator = mqttAckMediator;
    }
//    public void setMediator(MqttAckMediator mqttAckMediator) {
//        this.mqttAckMediator = mqttAckMediator;
//    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof MqttMessage)) {
            logger.debug("Received non Mqtt message=[{}]", msg);
            ctx.fireChannelRead(msg);
            return;
        }

        MqttMessage message = (MqttMessage) msg;
        if (message.fixedHeader().messageType() == MqttMessageType.CONNACK) {
            handleConnackMessage(ctx.channel(), (MqttConnAckMessage) message);
            ReferenceCountUtil.release(msg);
        } else if (message.fixedHeader().messageType() == MqttMessageType.DISCONNECT) {
            logger.info("Received disconnect message={}. Close channel.", msg);
            ctx.close();
        } else {
            ctx.fireChannelRead(msg);   //ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        MqttFixedHeader connectFixedHeader = new MqttFixedHeader(MqttMessageType.CONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0);

        MqttConnectVariableHeader connectVariableHeader = new MqttConnectVariableHeader(
                protocolName,
                version,
                hasUserName,
                hasPassword,
                willRetain,
                willQos,
                willFlag,
                cleanSession,
                keepAliveTimer,
                MqttProperties.NO_PROPERTIES
        );

        MqttConnectPayload connectPayload = new MqttConnectPayload(
                clientId,
                MqttProperties.NO_PROPERTIES,
                willTopic,
                willMessage.getBytes(Charset.forName("UTF-8")),
                username,
                password.getBytes(Charset.forName("UTF-8"))
        );

        MqttConnectMessage connectMessage = new MqttConnectMessage(connectFixedHeader, connectVariableHeader, connectPayload);
        ctx.writeAndFlush(connectMessage);

        logger.debug("Sent connect message {}.", connectMessage.variableHeader());

    }

    private void handleConnackMessage(Channel channel, MqttConnAckMessage message) {
        logger.debug("Handle ConnAckMessage {}.", message.variableHeader());
        MqttConnectReturnCode returnCode = message.variableHeader().connectReturnCode();
        Promise<MqttConnAckMessage> future = this.mqttAckMediator.getConnectFuture();
        switch (returnCode) {
            case CONNECTION_ACCEPTED -> {
                if (!future.isDone()) {
                    future.setSuccess(message);
                }
                logger.info("Received CONNACK message. Connection accepted. Message={}.", message);

                channel.flush();
            }

            case CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, CONNECTION_REFUSED_IDENTIFIER_REJECTED, CONNECTION_REFUSED_NOT_AUTHORIZED, CONNECTION_REFUSED_SERVER_UNAVAILABLE, CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION -> {
                if (!future.isDone()) {
                    future.cancel(true);
                }
                logger.info("Received CONNACK message. Connection refused. Message={}.", message);
                channel.close();
                // Don't start reconnect logic here
            }
        }

    }
}
