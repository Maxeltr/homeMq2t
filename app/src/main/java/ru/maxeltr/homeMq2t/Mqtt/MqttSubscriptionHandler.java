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
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttProperties.MqttProperty;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import ru.maxeltr.homeMq2t.Mqtt.MqttAckMediator;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class MqttSubscriptionHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MqttSubscriptionHandler.class);

    private AppProperties appProperties;

    MqttSubscriptionHandler(AppProperties appProperties) {
		this.appProperties = appProperties;
        logger.debug("Create {}.", this.getClass());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof MqttMessage message)) {
            ctx.fireChannelRead(msg);
            return;
        }

		switch (message.fixedHeader().messageType()) {
            case SUBACK -> {
                this.handleSubAck(ctx.channel(), (MqttSubAckMessage) message);
                ReferenceCountUtil.release(msg);
            }
            case UNSUBACK -> {
                this.handleUnsuback(ctx.channel(), (MqttUnsubAckMessage) message);
                ReferenceCountUtil.release(msg);
            }
            default -> {
                ctx.fireChannelRead(msg); 
            }
		}
    }

    private void handleSubAck(Channel channel, MqttSubAckMessage message) {
		int id = message.variableHeader().messageId();
        logger.info("Received SUBACK for subscription with id={}.", id);
		
        var pendingSubs = channel.attr(HmMq2tImpl.PENDING_SUBSCRIBES).get();
        if (pendingSubs == null) {
            logger.warn("No pending subscriptions map found in channel attributes for SUBACK id={}", id);
            return;
        }

        Promise<MqttSubAckMessage> future = pendingSubs.get(id);
        if (future == null) {
            logger.warn("There is no stored future of SUBSCRIBE message for SUBACK message id={}. Maybe it timed out already.", id);
            return;
        }

        if (!future.trySuccess(message)) {
            logger.debug("SUBACK id={} arrived but promise was already done (e.g., timed out).", id);
        }
    }

    private void handleUnsuback(Channel channel, MqttUnsubAckMessage message) {
		int id = message.variableHeader().messageId();
        logger.info("Received UNSUBACK for subscription with id={}.", id);

		var pendingUnsubs = channel.attr(HmMq2tImpl.PENDING_UNSUBSCRIBES).get();
        if (pendingUnsubs == null) {
            logger.warn("No pending unsubscriptions map found in channel attributes for UNSUBACK id={}", id);
            return;
        }

        Promise<MqttUnsubAckMessage> future = pendingUnsubs.get(id);
        if (future == null) {
            logger.warn("There is no stored future of UNSUBSCRIBE message for UNSUBACK message id={}. Maybe it timed out already.", id);
            return;
        }

        if (!future.trySuccess(message)) {
            logger.debug("UNSUBACK id={} arrived but promise was already done.", id);
		}
        
    }
}
