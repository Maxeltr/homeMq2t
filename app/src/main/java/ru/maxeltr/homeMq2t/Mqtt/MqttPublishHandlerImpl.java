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
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.maxeltr.homeMq2t.Service.ServiceMediator;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class MqttPublishHandlerImpl extends SimpleChannelInboundHandler<MqttMessage> implements MqttPublishHandler {

    private static final Logger logger = LoggerFactory.getLogger(MqttPublishHandlerImpl.class);

    private ChannelHandlerContext ctx;

    private final MqttAckMediator mqttAckMediator;

    private final ServiceMediator serviceMediator;

    public MqttPublishHandlerImpl(MqttAckMediator mqttAckMediator, ServiceMediator serviceMediator) {
        this.mqttAckMediator = mqttAckMediator;
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
                Promise future = (Promise<MqttPublishMessage>) this.mqttAckMediator.getFuture(id);
                if (future == null) {
                    logger.warn("There is no stored future of PUBLISH message for PUBACK message id={}. May be it was acknowledged already. ", id);
                    return;
                }
                future.setSuccess(pubAckMessage);

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
                int pubRelId = ((MqttMessageIdVariableHeader) pubRelMessage.variableHeader()).messageId();
                logger.info("Received PUBREL. Message id: {}, d: {}, q: {}, r: {}.",
                        pubRelId,
                        pubRelMessage.fixedHeader().isDup(),
                        pubRelMessage.fixedHeader().qosLevel(),
                        pubRelMessage.fixedHeader().isRetain()
                );
                Promise pubRelFuture = (Promise<MqttMessage>) this.mqttAckMediator.getFuture(pubRelId);
                if (pubRelFuture == null) {
                    logger.warn("There is no stored future of PUBLISH message for PUBREL message id={}. May be it was acknowledged already.", pubRelId);
                    return;
                }
                pubRelFuture.setSuccess(pubRelMessage);

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

    private void handlePubComp(MqttMessage message) {
        int id = ((MqttMessageIdVariableHeader) message.variableHeader()).messageId();
        MqttMessage storedMessage = this.mqttAckMediator.getMessage(id);
        if (storedMessage.fixedHeader().messageType() != MqttMessageType.PUBREL) {
            logger.warn("Error. There is no stored PUBREL message for received PUBCOMP id={}.", id);
            return;
        }
        Promise future = (Promise<MqttMessage>) this.mqttAckMediator.getFuture(id);
        if (future == null) {
            logger.warn("There is no stored future of PUBREL message for PUBCOMP message id={}. May be it was acknowledged already.", id);
            return;
        }
        future.setSuccess(message);
    }

    private void handlePubRec(MqttMessage message) {
        int id = ((MqttMessageIdVariableHeader) message.variableHeader()).messageId();
        MqttMessage storedMessage = this.mqttAckMediator.getMessage(id);
        if (!(storedMessage instanceof MqttPublishMessage)) {
            logger.warn("Error. There is no stored publish message for received PUBREC message id={}.", id);
            return;
        }
        Promise future = (Promise<MqttMessage>) this.mqttAckMediator.getFuture(id);
        if (future == null) {
            logger.warn("There is no stored future of PUBLISH message for PUBREC message id={}.May be it was acknowledged already.", id);
            return;
        }
        future.setSuccess(message);
    }

    private void handlePubRel(Channel channel, MqttMessage pubRelMessage) {
        MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) pubRelMessage.variableHeader();
        int id = variableHeader.messageId();
        MqttMessage storedMessage = this.mqttAckMediator.getMessage(id);
        if (!(storedMessage instanceof MqttPublishMessage)) {
            logger.warn("Error. There is no stored publish message for received PUBREL message id={}.", id);
            return;
        }

        this.mqttAckMediator.remove(id);
        logger.info("Publish message QoS2 id={} has been acknowledged.", id);

        this.serviceMediator.handleMessage((MqttPublishMessage) storedMessage);
        ReferenceCountUtil.release(storedMessage);

        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessage pubCompMessage = new MqttMessage(fixedHeader, variableHeader);

        channel.writeAndFlush(pubCompMessage);

        logger.info("Sent PUBCOMP message id: {}, d: {}, q: {}, r: {}.",
                id,
                pubCompMessage.fixedHeader().isDup(),
                pubCompMessage.fixedHeader().qosLevel(),
                pubCompMessage.fixedHeader().isRetain()
        );

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
            case EXACTLY_ONCE:
                if (!this.mqttAckMediator.isContainId(message.variableHeader().packetId())) {
                    ReferenceCountUtil.retain(message);
                    Promise<? extends MqttMessage> publishFuture = channel.eventLoop().newPromise();
                    this.mqttAckMediator.add(message.variableHeader().packetId(), publishFuture, message);
                    publishFuture.addListener((FutureListener) (Future f) -> {
                        MqttPublishHandlerImpl.this.handlePubRel(channel, (MqttMessage) f.get());
                    });
                    logger.info("Publish message with QoS2 id={} has been stored.", message.variableHeader().packetId());
                } else {
                    logger.info("Received publish message with QoS2 id={} is repeated.", message.variableHeader().packetId());
                    //TODO
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
