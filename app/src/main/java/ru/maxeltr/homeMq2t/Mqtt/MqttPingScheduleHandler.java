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
    
	private final AtomicBoolean pingRespTimeout = new AtomicBoolean();

    public MqttPingScheduleHandler(ServiceMediator serviceMediator) {
        this.serviceMediator = serviceMediator;

        MqttFixedHeader fixedHeaderReqMsg = new MqttFixedHeader(MqttMessageType.PINGREQ, false, MqttQoS.AT_MOST_ONCE, false, 0);
        pingReqMsg = new MqttMessage(fixedHeaderReqMsg);
        
        MqttFixedHeader fixedHeaderRespMsg = new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0);
        pingRespMsg = new MqttMessage(fixedHeaderRespMsg);
        
        logger.debug("Create {}.", this);
    }
    
    @PostConstruct
    public void startPing() {
        logger.info("Start ping. {}", this);
        this.future = threadPoolTaskScheduler.schedule(new RunnableTask(), pingPeriodicTrigger);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }
    
    public void stopPing() {
		if (this.future != null && !this.future.isDone()) {
			this.future.cancel(false);
			logger.info("The ping was canceled. {}", this);
			this.future = null;
		} else {
			logger.warn("Attempted to cancel ping, but it was not started or already canceled.");
		}
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
            logger.info("Received ping request={}. Sent ping response.", msg);
//            ReferenceCountUtil.release(msg);
        } else if (message.fixedHeader().messageType() == MqttMessageType.PINGRESP) {
            logger.info("Received ping response={}.", msg);
            this.pingRespTimeout.set(false);
//            ReferenceCountUtil.release(msg);
        } else {
            ctx.fireChannelRead(msg);   //ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
        }
    }
    
    class RunnableTask implements Runnable {

        @Override
        public void run() {
			if (ctx == null) {
				logger.warn("ChannelHandlerContext is not initialized. Skipping ping request.");
				return;
			}
			
            if (pingRespTimeout.get()) {
                logger.info("Ping response was not received within the keep-alive period. {}", this);
                stopPing();
                if (reconnect) {
                    logger.info("Start the reconnection attempt.");
                    serviceMediator.reconnect();
                } else {
					logger.info("Disconnect without the reconnection.");
                    serviceMediator.disconnect(MqttReasonCodeAndPropertiesVariableHeader.REASON_CODE_OK);
                }
                
                return;
            }

            ctx.writeAndFlush(pingReqMsg);
            pingRespTimeout.set(true);
            logger.info("Sent ping request. {}.", this);

        }
    }
}
