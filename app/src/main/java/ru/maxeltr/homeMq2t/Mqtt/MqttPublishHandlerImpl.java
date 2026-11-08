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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import ru.maxeltr.homeMq2t.Entity.MqttMessageEntity;
import ru.maxeltr.homeMq2t.Repository.MqttMessageRepository;
import ru.maxeltr.homeMq2t.Service.ServiceMediator;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class MqttPublishHandlerImpl extends SimpleChannelInboundHandler<MqttMessage> implements MqttPublishHandler {

    private static final Logger logger = LoggerFactory.getLogger(MqttPublishHandlerImpl.class);

    private ChannelHandlerContext ctx;

    private final ServiceMediator serviceMediator;

    @Autowired
    @Qualifier("processExecutor")
    private TaskExecutor processExecutor;

    @Autowired
    private MqttMessageRepository messageRepository;

    public MqttPublishHandlerImpl(ServiceMediator serviceMediator) {
        this.serviceMediator = serviceMediator;
        logger.debug("Create {}.", this.getClass());
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
                );
                this.handlePublish(ctx.channel(), pubMessage);

                break;
            case PUBACK:
                MqttPubAckMessage pubAckMessage = (MqttPubAckMessage) msg;
                int id = pubAckMessage.variableHeader().messageId();
                logger.info("Received PUBACK. Message id: {}, d: {}, q: {}, r: {}.",
                        id,
                        pubAckMessage.fixedHeader().isDup(),
                        pubAckMessage.fixedHeader().qosLevel(),
                        pubAckMessage.fixedHeader().isRetain()
                );
                
                var pendingPubAckAttr = ctx.channel().attr(HmMq2tImpl.PENDING_PUBACK);
                ConcurrentHashMap<Integer, Promise<MqttPubAckMessage>> pendingPubAck = pendingPubAckAttr.get();
                if (pendingPubAck == null) {
                    logger.warn("There is no pending pub ack map for this channel. Message id={} dropped.", id);
                    return;
                }

                Promise<MqttPubAckMessage> future = pendingPubAck.remove(id);
                if (future == null) {
                    logger.warn("There is no stored future for PUBACK id={}. Maybe it timed out and map was cleaned.", id);
                    return;
                }

                if (!future.trySuccess(pubAckMessage)) {
                    logger.warn("PUBACK for id={} arrived, but the promise was already completed (likely timeout). Cleaning DB anyway.", id);
                }

                break;
            case PUBREC:
                MqttMessage pubRecMessage = (MqttMessage) msg;
                MqttMessageIdVariableHeader pubRecVariableHeader = (MqttMessageIdVariableHeader) pubRecMessage.variableHeader();
                logger.info("Received PUBREC. Message id: {}, d: {}, q: {}, r: {}.",
                        pubRecVariableHeader.messageId(),
                        pubRecMessage.fixedHeader().isDup(),
                        pubRecMessage.fixedHeader().qosLevel(),
                        pubRecMessage.fixedHeader().isRetain()
                );
                this.handlePubRec(msg);

                break;
            case PUBREL:
                MqttMessage pubRelMessage = (MqttMessage) msg;
                this.handlePubRel(ctx.channel(), pubRelMessage);

                break;
            case PUBCOMP:
                MqttMessage pubCompMessage = (MqttMessage) msg;
                logger.info("Received PUBCOMP. Message id: {}, d: {}, q: {}, r: {}.",
                        ((MqttMessageIdVariableHeader) pubCompMessage.variableHeader()).messageId(),
                        pubCompMessage.fixedHeader().isDup(),
                        pubCompMessage.fixedHeader().qosLevel(),
                        pubCompMessage.fixedHeader().isRetain()
                );
                this.handlePubComp(msg);

                break;
        }
    }

    /**
     * Handles the final stage of the outbound QoS2 publish handshake (PUBLISH -> PUBREC -> PUBREL -> PUBCOMP).
     * This is the point where the message is finally removed from the DB:
     * it is NOT deleted on PUBREC and NOT deleted on PUBREL — only here, on PUBCOMP,
     * which means the broker has confirmed full delivery (analogous to PUBACK for QoS1, see HmMq2tImpl).
     */
    private void handlePubComp(MqttMessage message) {
        int id = ((MqttMessageIdVariableHeader) message.variableHeader()).messageId();

        // At this moment the message in the DB still has status STATUS_SEND_PUBREL
        // (set in HmMq2tImpl after PUBREC was received and PUBREL was sent).
        // PUBCOMP completes the QoS2 handshake, so the message is fully acknowledged
        // and can be safely removed from the DB; no retransmission will be needed anymore.
        CompletableFuture.runAsync(() -> {
            this.messageRepository.deleteByPacketId(id);
        }, this.processExecutor).whenComplete((v, ex) -> {
            if (ex != null) {
                logger.error("Failed to delete acknowledged QoS2 message id={} from DB.", id, ex);
            } else {
                logger.debug("Acknowledged QoS2 message id={} (status=STATUS_SEND_PUBREL) has been deleted from DB.", id);
            }
        });

        var pendingPubCompAttr = ctx.channel().attr(HmMq2tImpl.PENDING_PUBCOMP);
        ConcurrentHashMap<Integer, Promise<MqttMessage>> pendingPubComp = pendingPubCompAttr.get();
    
        if (pendingPubComp == null) {
            logger.warn("There is no pending pub comp map for this channel. Message id={} dropped.", id);
            return;
        }
    
        Promise<MqttMessage> future = pendingPubComp.remove(id);
        if (future == null) {
            logger.warn("There is no stored future for PUBCOMP id={}. Maybe it timed out.", id);
            return;
        }
        if (!future.trySuccess(message)) {
            logger.warn("PUBCOMP for id={} arrived, but the promise was already completed (likely timeout).", id);
        }
    }

    
    private void handlePubRec(MqttMessage message) {
        // Note: this message has already been logged in channelRead0() (case PUBREC).
        // Only the QoS2 handshake logic is handled here to avoid duplicated log entries.
        MqttMessage pubRecMessage = (MqttMessage) message;
        int id = ((MqttMessageIdVariableHeader) pubRecMessage.variableHeader()).messageId();

        var pendingPubRecAttr = ctx.channel().attr(HmMq2tImpl.PENDING_PUBREC);
        ConcurrentHashMap<Integer, Promise<MqttMessage>> pendingPubRec = pendingPubRecAttr.get();
        if (pendingPubRec == null) {
            logger.warn("There is no pending pub rec map for this channel. Message id={} dropped.", id);
            return;
        }

        Promise<MqttMessage> future = pendingPubRec.remove(id);
       if (future == null) {
            logger.warn("There is no stored future for PUBREC id={}. Maybe it timed out and map was cleaned.", id);
            return;
        }

        if (!future.trySuccess(pubRecMessage)) {
            logger.warn("PUBREC for id={} arrived, but the promise was already completed (likely timeout).", id);
        }
    }
    
    private void handlePubRel(Channel channel, MqttMessage pubRelMessage) {
        MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) pubRelMessage.variableHeader();
        int id = variableHeader.messageId();
        logger.info("Received PUBREL. Message id: {}, d: {}, q: {}, r: {}.",
                id,
                pubRelMessage.fixedHeader().isDup(),
                pubRelMessage.fixedHeader().qosLevel(),
                pubRelMessage.fixedHeader().isRetain()
        );

        var pendingPubRelAttr = ctx.channel().attr(HmMq2tImpl.PENDING_PUBREL);
        ConcurrentHashMap<Integer, MqttPublishMessage> pendingPubRel = pendingPubRelAttr.get();

        MqttPublishMessage storedMessage = (pendingPubRel != null) ? pendingPubRel.remove(id) : null;
        if (storedMessage == null) {
            logger.warn("There is no stored publish message for received PUBREL message id={}. "
                    + "Sending PUBCOMP anyway (may be duplicate or already acknowledged).", id);
        } else {
            logger.info("Publish message QoS2 id={} has been acknowledged.", id);
            this.serviceMediator.handleMessage(storedMessage);
            ReferenceCountUtil.release(storedMessage);
        }

        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 0);
        channel.writeAndFlush(new MqttMessage(fixedHeader, variableHeader));
        logger.info("Sent PUBCOMP message id: {}.", id);
    }

    private void handlePublish(Channel channel, MqttPublishMessage message) {
        MqttFixedHeader fixedHeader;
        MqttMessageIdVariableHeader variableHeader;
        switch (message.fixedHeader().qosLevel()) {
            case AT_MOST_ONCE:
                //ReferenceCountUtil.retain(message);		//TODO del?	message does not save
                this.serviceMediator.handleMessage(message);

                break;
            case AT_LEAST_ONCE:
                //ReferenceCountUtil.retain(message);		//TODO del? message does not save
                this.serviceMediator.handleMessage(message);	//TODO check DUP first!

                fixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
                variableHeader = MqttMessageIdVariableHeader.from(message.variableHeader().packetId());

                MqttPubAckMessage pubAckMessage = new MqttPubAckMessage(fixedHeader, variableHeader);
                channel.writeAndFlush(pubAckMessage);

                logger.info("Sent PUBACK message id: {}, d: {}, q: {}, r: {}.",
                        pubAckMessage.variableHeader().messageId(),
                        pubAckMessage.fixedHeader().isDup(),
                        pubAckMessage.fixedHeader().qosLevel(),
                        pubAckMessage.fixedHeader().isRetain()
                );

                break;
            case FAILURE:
                logger.warn("Received PUBLISH message with invalid QoS ({}). Message is skipped.",
                        message.fixedHeader().qosLevel());

                return;
            case EXACTLY_ONCE:
                var pendingPubRelAttr = ctx.channel().attr(HmMq2tImpl.PENDING_PUBREL);
                ConcurrentHashMap<Integer, MqttPublishMessage> pendingPubRel = pendingPubRelAttr.get();
                if (pendingPubRel == null) {
                    pendingPubRel = new ConcurrentHashMap<>();
                    ConcurrentHashMap<Integer, MqttPublishMessage> oldMap = pendingPubRelAttr.setIfAbsent(pendingPubRel);
                    if (oldMap != null) {
                        pendingPubRel = oldMap;
                    }
                }

                if (pendingPubRel.putIfAbsent(message.variableHeader().packetId(), message) == null) {
                    ReferenceCountUtil.retain(message);
                    logger.info("Publish message with QoS2 id={} has been stored.", message.variableHeader().packetId());
                } else {
                    logger.info("Received publish message with QoS2 id={} is repeated.", message.variableHeader().packetId());
                }
                fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0);
                variableHeader = MqttMessageIdVariableHeader.from(message.variableHeader().packetId());
                MqttMessage pubRecMessage = new MqttMessage(fixedHeader, variableHeader);

                channel.writeAndFlush(pubRecMessage);

                logger.info("Sent PUBREC message id: {}, d: {}, q: {}, r: {}.",
                        variableHeader.messageId(),
                        pubRecMessage.fixedHeader().isDup(),
                        pubRecMessage.fixedHeader().qosLevel(),
                        pubRecMessage.fixedHeader().isRetain()
                );

                break;
        }
    }

    public ChannelHandlerContext getChannelHandlerContext() {
        return this.ctx;
    }

}
