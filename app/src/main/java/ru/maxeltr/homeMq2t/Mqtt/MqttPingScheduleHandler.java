/*
 * The MIT License
 *
 * Copyright 2024 Dev.
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
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import ru.maxeltr.homeMq2t.Service.ServiceMediator;
import java.util.concurrent.ScheduledFuture;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author Dev
 */
public class MqttPingScheduleHandler extends ChannelInboundHandlerAdapter  {
    
    private static final Logger logger = LoggerFactory.getLogger(MqttPingScheduleHandler.class);
    
    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;
    
    @Autowired
    private PeriodicTrigger pingPeriodicTrigger;
    
    @Value("${reconnect:true}")
    private boolean reconnect;
    
    private ChannelHandlerContext ctx;
    
    private final ServiceMediator serviceMediator;
    
    private ScheduledFuture<?> future;

    private final MqttMessage pingReqMsg;
    
    private final MqttMessage pingRespMsg;
    
    private boolean pingRespTimeout;
    
    public MqttPingScheduleHandler(ServiceMediator serviceMediator) {
        this.serviceMediator = serviceMediator;

        MqttFixedHeader fixedHeaderReqMsg = new MqttFixedHeader(MqttMessageType.PINGREQ, false, MqttQoS.AT_MOST_ONCE, false, 0);
        pingReqMsg = new MqttMessage(fixedHeaderReqMsg);
        
        MqttFixedHeader fixedHeaderRespMsg = new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0);
        pingRespMsg = new MqttMessage(fixedHeaderRespMsg);
    }
    
    //@PostConstruct
    public void start() {
        this.future = threadPoolTaskScheduler.schedule(new RunnableTask(), pingPeriodicTrigger);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }
    
    public void stop() {
        this.future.cancel(false);
        logger.info("The ping was canceled. {}", this);
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
            logger.info("Received ping request={}. Sent ping response={}.", msg, pingRespMsg);
//            ReferenceCountUtil.release(msg);
        } else if (message.fixedHeader().messageType() == MqttMessageType.PINGRESP) {
            logger.info("Received ping response={}.", msg);
            this.pingRespTimeout = false;
//            ReferenceCountUtil.release(msg);
        } else {
            ctx.fireChannelRead(msg);   //ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
        }
    }
    
    class RunnableTask implements Runnable {

        @Override
        public void run() {
            if (pingRespTimeout) {
                logger.info("Ping response was not received for keep-alive time. {}", this);
//                MqttPingScheduleHandler.this.future.cancel(false);
                //publishPingTimeoutEvent();
                if (reconnect) {
                    serviceMediator.reconnect();
                    logger.debug("Reconnection attempt.");
                } else {
                    serviceMediator.disconnect(MqttReasonCodeAndPropertiesVariableHeader.REASON_CODE_OK);
                    future.cancel(false);
                    logger.debug("Disconnection.");
                }
                
                return;
            }

            ctx.writeAndFlush(pingReqMsg);
            pingRespTimeout = true;
            logger.info("Sent ping request. {}. Message {}.", this, pingReqMsg);

        }
    }
}
