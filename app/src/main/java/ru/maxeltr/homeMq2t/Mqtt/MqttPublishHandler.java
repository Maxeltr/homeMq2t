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
//@Sharable
public class MqttPublishHandler extends SimpleChannelInboundHandler<MqttMessage> {

    private static final Logger logger = LoggerFactory.getLogger(MqttPublishHandler.class);

    private ChannelHandlerContext ctx;


    public MqttPublishHandler(PromiseBroker promiseBroker) {
        this.promiseBroker = promiseBroker;
        
    }

    public static MqttPublishHandler newInstance(PromiseBroker promiseBroker, MessageHandler messageHandler, Config config, ThreadPoolTaskScheduler taskScheduler, PeriodicTrigger periodicTrigger, ApplicationEventPublisher applicationEventPublisher) {
        return new MqttPublishHandler(promiseBroker, messageHandler, config, taskScheduler, periodicTrigger, applicationEventPublisher);

    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        this.ctx = ctx;
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
                MqttPubAckMessage pubAckmessage = (MqttPubAckMessage) msg;
                
                logger.info("Received PUBACK. Message id: {}, d: {}, q: {}, r: {}.",
                        pubAckmessage.variableHeader().messageId(),
                        pubAckmessage.fixedHeader().isDup(),
                        pubAckmessage.fixedHeader().qosLevel(),
                        pubAckmessage.fixedHeader().isRetain()
                ));
                Promise future = (Promise<MqttPublishMessage>) this.promiseBroker.get(pubAckmessage.variableHeader().messageId());
                if (!future.isDone()) {
                    future.setSuccess(pubAckmessage);
                }
                break;
            case PUBREC:
                
                this.handlePubrec(ctx.channel(), msg);

                break;
            case PUBREL:
                MqttMessage pubrelMessage = (MqttMessage) msg;
                MqttMessageIdVariableHeader pubrelVariableHeader = (MqttMessageIdVariableHeader) pubrelMessage.variableHeader();
                
                logger.info("Received PUBREL. Message id: {}, d: {}, q: {}, r: {}.",
                        pubrelVariableHeader.messageId(),
                        msg.fixedHeader().isDup(),
                        msg.fixedHeader().qosLevel(),
                        msg.fixedHeader().isRetain()
                ));
                this.handlePubrel(ctx.channel(), msg);
                break;
            case PUBCOMP:
                MqttMessage pubcompMessage = (MqttMessage) msg;
                MqttMessageIdVariableHeader pubcompVariableHeader = (MqttMessageIdVariableHeader) pubcompMessage.variableHeader();
                
                logger.info("Received PUBCOMP. Message id: {}, d: {}, q: {}, r: {}.",
                        pubcompVariableHeader.messageId(),
                        msg.fixedHeader().isDup(),
                        msg.fixedHeader().qosLevel(),
                        msg.fixedHeader().isRetain()
                ));
                this.handlePubcomp(ctx.channel(), msg);
                break;

        }
    }

    private void handlePubcomp(Channel channel, MqttMessage message) {
        MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) message.variableHeader();
        MqttMessage pubRecMessage = this.pendingPubComp.get(variableHeader.messageId());
        if (pubRecMessage == null) {
            logger.warn("Collection of waiting confirmation PUBREC messages returned null instead saved pubRecMessage");
            
        } else {
            this.pendingPubComp.remove(variableHeader.messageId());
            logger.info("Remove (from pending PUBCOMP) saved pubRecMessage id: {}", variableHeader.messageId());
            
        }
    }

    private void handlePubrec(Channel channel, MqttMessage message) {
        MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) message.variableHeader();
        Promise future = (Promise<MqttMessage>) this.mqttAckMediator.getFuture(variableHeader.messageId());
        //if (!future.isDone()) {
            future.setSuccess(message);
        //}
        if (!this.mqttAckMediator.contains(variableHeader.messageId())) {
            this.pendingPubComp.put(variableHeader.messageId(), message);
            logger.info("Add (to pending PUBCOMP collection) PUBREC message id: {}.", variableHeader.messageId());
        } else {
            
            logger.warn("Received PUBREC message is repeated. Message id: {}.", variableHeader.messageId());
        }

        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_LEAST_ONCE, false, 0);
        MqttMessage pubrelMessage = new MqttMessage(fixedHeader, variableHeader);

        channel.writeAndFlush(pubrelMessage);

        logger.info("Received PUBREC. Message id: {}, d: {}, q: {}, r: {}.",
				pubrecVariableHeader.messageId(),
				msg.fixedHeader().isDup(),
				msg.fixedHeader().qosLevel(),
				msg.fixedHeader().isRetain()
		));
        logger.info("Sent PUBREL message id: {}, d: {}, q: {}, r: {}.",
                variableHeader.messageId(),
                pubrelMessage.fixedHeader().isDup(),
                pubrelMessage.fixedHeader().qosLevel(),
                pubrelMessage.fixedHeader().isRetain()
        ));
    }

    private void handlePubrel(Channel channel, MqttMessage message) throws InterruptedException {
        MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) message.variableHeader();
        MqttPublishMessage publishMessage = this.pendingPubRel.get(variableHeader.messageId());
        if (publishMessage == null) {
            logger.warn("Collection of waiting confirmation publish QoS2 messages returned null instead saved publishMessage");
            
        } else {
            //TODO handle publish Message
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

    private void handlePublish(Channel channel, MqttPublishMessage message) throws InterruptedException {
        MqttFixedHeader fixedHeader;
        MqttMessageIdVariableHeader variableHeader;
        switch (message.fixedHeader().qosLevel()) {
            case AT_MOST_ONCE:
                ReferenceCountUtil.retain(message);
                this.messageHandler.handleMessage(message);
                
                break;
            case AT_LEAST_ONCE:
                ReferenceCountUtil.retain(message);
                this.messageHandler.handleMessage(message);

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
                
                if (!this.pendingPubRel.containsKey(message.variableHeader().packetId())) {
                    ReferenceCountUtil.retain(message);
                    this.pendingPubRel.put(message.variableHeader().packetId(), message);
                    
                    logger.info("Add (to pending PUBREL collection) publish message id: {}.", message.variableHeader().packetId());
                } else {
                    
                    logger.info("Received publish message with QoS2 is repeated id: {}.", message.variableHeader().packetId());
                }

                fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0);
                variableHeader = MqttMessageIdVariableHeader.from(message.variableHeader().packetId());
                MqttMessage pubrecMessage = new MqttMessage(fixedHeader, variableHeader);

                channel.writeAndFlush(pubrecMessage);

                
                logger.info("Sent PUBREC message id: {}, d: {}, q: {}, r: {}.",
                        variableHeader.messageId(),
                        pubrecMessage.fixedHeader().isDup(),
                        pubrecMessage.fixedHeader().qosLevel(),
                        pubrecMessage.fixedHeader().isRetain()
                ));
                break;
        }
    }

    public ChannelHandlerContext getChannelHandlerContext() {
        return this.ctx;
    }



}
