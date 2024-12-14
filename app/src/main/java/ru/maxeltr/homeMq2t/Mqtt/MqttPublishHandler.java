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
package ru.maxeltr.mqttClient.Mqtt;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Promise;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;
import ru.maxeltr.mqttClient.Config.Config;
import ru.maxeltr.mqttClient.Service.MessageHandler;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class MqttPublishHandler extends SimpleChannelInboundHandler<MqttMessage> {

    private static final Logger logger = LoggerFactory.getLogger(MqttPublishHandler.class);

    private ChannelHandlerContext ctx;
	
	private MqttAckMediator mqttAckMediator;

    public MqttPublishHandler() {
        
    }

    public static MqttPublishHandler newInstance() {
        return new MqttPublishHandler();

    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }
	
	public void setMediator(MqttAckMediator mqttAckMediator) {
        this.mqttAckMediator = mqttAckMediator;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) throws Exception {
        switch (msg.fixedHeader().messageType()) {
            case PUBLISH:
                MqttPublishMessage pubMessage = (MqttPublishMessage) msg;
                logger.info("Received PUBLISH. Message id: {}, t: {}, d: {}, q: {}, r: {}.",
                        pubMessage.variableHeader().packetId(),
                        pubMessage.variableHeader().topicName(),
                        pubMessage.fixedHeader().isDup(),
                        pubMessage.fixedHeader().qosLevel(),
                        pubMessage.fixedHeader().isRetain()
                ));
                this.handlePublish(ctx.channel(), pubMessage);
				
                break;
            case PUBACK:
                MqttPubAckMessage pubAckMessage = (MqttPubAckMessage) msg;
                logger.info("Received PUBACK. Message id: {}, d: {}, q: {}, r: {}.",
                        pubAckMessage.variableHeader().messageId(),
                        pubAckMessage.fixedHeader().isDup(),
                        pubAckMessage.fixedHeader().qosLevel(),
                        pubAckMessage.fixedHeader().isRetain()
                ));
                Promise future = (Promise<MqttPublishMessage>) this.mqttAckMediator.get(pubAckMessage.variableHeader().messageId());
                if (future == null ) {
					logger.warn("There is no stored future of PUBLISH message for PUBACK message. May be it was acknowledged already. [{}]. ", pubAckMessage);
					return;
				}
                future.setSuccess(pubAckMessage);
                
                break;
            case PUBREC:
				MqttMessage pubRecmessage = (MqttMessage) msg;
                logger.info("Received PUBREC. Message id: {}, d: {}, q: {}, r: {}.",
                        pubRecmessage.variableHeader().messageId(),
                        pubRecmessage.fixedHeader().isDup(),
                        pubRecmessage.fixedHeader().qosLevel(),
                        pubRecmessage.fixedHeader().isRetain()
                this.handlePubrec(ctx.channel(), msg);

                break;
            case PUBREL:
                MqttMessage pubrelMessage = (MqttMessage) msg;
                logger.info("Received PUBREL. Message id: {}, d: {}, q: {}, r: {}.",
                        pubrelMessage.variableHeader().messageId(),
                        pubrelMessage.fixedHeader().isDup(),
                        pubrelMessage.fixedHeader().qosLevel(),
                        pubrelMessage.fixedHeader().isRetain()
                ));
                this.handlePubrel(ctx.channel(), msg);
				
                break;
            case PUBCOMP:
                MqttMessage pubcompMessage = (MqttMessage) msg;
                logger.info("Received PUBCOMP. Message id: {}, d: {}, q: {}, r: {}.",
                        pubcompMessage.variableHeader().messageId(),
                        pubcompMessage.fixedHeader().isDup(),
                        pubcompMessage.fixedHeader().qosLevel(),
                        pubcompMessage.fixedHeader().isRetain()
                ));
                this.handlePubcomp(ctx.channel(), msg);
				
                break;
        }
    }

    private void handlePubcomp(Channel channel, MqttMessage message) {
        int id = message.variableHeader().messageId();
		Promise future = (Promise<MqttMessage>) this.mqttAckMediator.get(id);
		if (future == null ) {
			logger.warn("There is no stored future of PUBREL message for PUBCOMP message. May be it was acknowledged already. [{}]. ", message);
			return;
		}
		future.setSuccess(message);
    }

    private void handlePubrec(Channel channel, MqttMessage message) {
        MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) message.variableHeader();
        Promise future = (Promise<MqttMessage>) this.mqttAckMediator.getFuture(variableHeader.messageId());
		if (future == null ) {
			logger.warn("There is no stored future of PUBLISH message for PUBREC message.May be it was acknowledged already. [{}].", message);
			return;
		}
        future.setSuccess(message);
    }

    private void handlePubrel(Channel channel, MqttMessage message) throws InterruptedException {
        MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) message.variableHeader();
        MqttPublishMessage publishMessage = this.pendingPubRel.get(variableHeader.messageId());
        if (publishMessage == null) {
            logger.warn("Collection of waiting confirmation publish QoS2 messages returned null instead saved publishMessage");
            
        } else {
            //TODO handle publish Message. delete message and future
            this.messageHandler.handleMessage(publishMessage);
            this.pendingPubRel.remove(variableHeader.messageId());
//            publishMessage.release();	//???
            logger.info("Remove (from pending PUBREL) publish message id: {}", variableHeader.messageId());
            
        }

        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessage pubCompMessage = new MqttMessage(fixedHeader, variableHeader);

        channel.writeAndFlush(pubCompMessage);

        
        logger.info("Sent PUBCOMP message id: {}, d: {}, q: {}, r: {}.",
                variableHeader.messageId(),
                pubCompMessage.fixedHeader().isDup(),
                pubCompMessage.fixedHeader().qosLevel(),
                pubCompMessage.fixedHeader().isRetain()
        ));
    }

    private void handlePublish(Channel channel, MqttPublishMessage message) {
        MqttFixedHeader fixedHeader;
        MqttMessageIdVariableHeader variableHeader;
        switch (message.fixedHeader().qosLevel()) {
            case AT_MOST_ONCE:
                ReferenceCountUtil.retain(message);
                this.messageHandler.handleMessage(message);
                
                break;
            case AT_LEAST_ONCE:
                ReferenceCountUtil.retain(message);
                this.messageHandler.handleMessage(message);	//TODO check DUP first!

                fixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
                variableHeader = MqttMessageIdVariableHeader.from(message.variableHeader().packetId());

                MqttPubAckMessage pubAckMessage = new MqttPubAckMessage(fixedHeader, variableHeader);
                channel.writeAndFlush(pubAckMessage);
                
                logger.info("Sent PUBACK message id: {}, d: {}, q: {}, r: {}.",
                        pubAckMessage.variableHeader().messageId(),
                        pubAckMessage.fixedHeader().isDup(),
                        pubAckMessage.fixedHeader().qosLevel(),
                        pubAckMessage.fixedHeader().isRetain()
                ));

                break;
            case EXACTLY_ONCE:
                if (!this.mqttAckMediator.contains(message.variableHeader().packetId())) {
                    ReferenceCountUtil.retain(message);
					//save message and future
					Promise<? extends MqttMessage> pubRecFuture = new DefaultPromise<>(this.workerGroup.next());
                    this.mqttAckMediator.put(message.variableHeader().packetId(), pubRecFuture, message);
                    logger.info("Publish message with QoS2 has been stored - [{}].", message);
                } else {
                    logger.info("Received publish message with QoS2 is repeated - [{}].", message);
					//TODO
                }
				//send pubRec
                fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0);
                variableHeader = MqttMessageIdVariableHeader.from(message.variableHeader().packetId());
                MqttMessage pubRecMessage = new MqttMessage(fixedHeader, variableHeader);

                channel.writeAndFlush(pubRecMessage);
                
                logger.info("Sent PUBREC message id: {}, d: {}, q: {}, r: {}.",
                        variableHeader.messageId(),
                        pubRecMessage.fixedHeader().isDup(),
                        pubRecMessage.fixedHeader().qosLevel(),
                        pubRecMessage.fixedHeader().isRetain()
                ));
				
                break;
        }
    }

    public ChannelHandlerContext getChannelHandlerContext() {
        return this.ctx;
    }



}
