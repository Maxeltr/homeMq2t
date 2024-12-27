/*
 * The MIT License
 *
 * Copyright 2021 Maxim Eltratov <<Maxim.Eltratov@ya.ru>>.
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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttReasonCodeAndPropertiesVariableHeader;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import ru.maxeltr.homeMq2t.Service.ServiceMediator;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class MqttPingHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MqttPingHandler.class);

    @Autowired
    private Environment env;

    private ScheduledFuture<?> pingRespTimeout;
    
    private final MqttMessage pingReqMsg;
    
    private final MqttMessage pingRespMsg;
    
    private final ServiceMediator serviceMediator;

    MqttPingHandler(ServiceMediator serviceMediator) {
        this.serviceMediator = serviceMediator;
        MqttFixedHeader fixedHeaderReqMsg = new MqttFixedHeader(MqttMessageType.PINGREQ, false, MqttQoS.AT_MOST_ONCE, false, 0);
        pingReqMsg = new MqttMessage(fixedHeaderReqMsg);
        
        MqttFixedHeader fixedHeaderRespMsg = new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0);
        pingRespMsg = new MqttMessage(fixedHeaderRespMsg);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof MqttMessage)) {
            ctx.fireChannelRead(msg);
            return;
        }

        MqttMessage message = (MqttMessage) msg;
        if (message.fixedHeader().messageType() == MqttMessageType.PINGREQ) {
            ctx.channel().writeAndFlush(pingRespMsg);
            logger.info("Received ping request. Sent ping response. {}.", msg);
//            ReferenceCountUtil.release(msg);
        } else if (message.fixedHeader().messageType() == MqttMessageType.PINGRESP) {
            logger.info("Received ping response {}.", msg);
            if (this.pingRespTimeout != null && !this.pingRespTimeout.isCancelled() && !this.pingRespTimeout.isDone()) {
                this.pingRespTimeout.cancel(true);
                this.pingRespTimeout = null;
            }
//            ReferenceCountUtil.release(msg);
        } else {
            ctx.fireChannelRead(msg);   //ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent event) {
            switch (event.state()) {
                case READER_IDLE -> {
                }
                case WRITER_IDLE -> {
                    ctx.writeAndFlush(pingReqMsg);
                    logger.info("Sent ping request {}.", pingReqMsg);

                    if (this.pingRespTimeout == null) {
                        this.pingRespTimeout = ctx.channel().eventLoop().schedule(() -> {
                            logger.info("Ping response was not received for keepAlive time.");
                            this.serviceMediator.disconnect(MqttReasonCodeAndPropertiesVariableHeader.REASON_CODE_OK);
                            //this.publishPingTimeoutEvent(); //TODO ?
                        }, Integer.parseInt(this.env.getProperty("keep-alive-timer", "20")), TimeUnit.MILLISECONDS);
                    }
                }
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }

    }

}
