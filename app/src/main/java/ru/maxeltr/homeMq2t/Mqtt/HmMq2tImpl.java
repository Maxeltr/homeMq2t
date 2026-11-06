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

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttSubscribePayload;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttProperties;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttReasonCodeAndPropertiesVariableHeader;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribePayload;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.maxeltr.homeMq2t.Service.ServiceMediator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.task.TaskExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import java.util.concurrent.CompletableFuture;
import ru.maxeltr.homeMq2t.Config.AppProperties;
import ru.maxeltr.homeMq2t.Entity.MqttMessageEntity;
import ru.maxeltr.homeMq2t.Repository.MqttMessageRepository;
import ru.maxeltr.homeMq2t.Utils.AppUtils;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class HmMq2tImpl implements HmMq2t, CommandLineRunner { // TODO separate to ConnectionManager,
                                                               // ReconnectionStrategy, PingScheduler,
                                                               // SubscriptionManager, PublishManager

    private static final Logger logger = LoggerFactory.getLogger(HmMq2tImpl.class);

    private EventLoopGroup workerGroup = new NioEventLoopGroup();

    private Channel channel;

    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @Autowired
    private MqttChannelInitializer mqttChannelInitializer;

    @Autowired
    @Qualifier("processExecutor")
    private TaskExecutor processExecutor;

    @Autowired
    private MqttMessageRepository messageRepository;

    @Value("${connect-timeout:5000}")
    private Integer connectTimeout;

    @Value("${wait-disconnect-while-shutdown:1000}")
    private Integer waitDisconnect;

    @Value("${reconnect-delay:3000}")
    private int reconnectDelay;

    @Value("${reconnect-delay-max:1800000}")
    private int reconnectDelayMax;

    @Value("${subscribe-timeout:1000}")
    private int subscribeTimeout;

    private int publishQos1Timeout = 5;

    private int publishQos2Timeout = 5;

    private int pubRelQos2Timeout = 5;

    @Autowired
    private AppProperties appProperties;

    private final AtomicInteger nextMessageId = new AtomicInteger(1);

    // private final Map<String, MqttTopicSubscription> subscribedTopics =
    // Collections
    // .synchronizedMap(new LinkedHashMap<>());

    private final Map<String, MqttQoS> subscribedTopics = new ConcurrentHashMap<>();

    private final static AtomicBoolean connecting = new AtomicBoolean();

    private final static AtomicBoolean connected = new AtomicBoolean();

    private final static AtomicBoolean reconnecting = new AtomicBoolean();

    private final AtomicLong writeFailureCount = new AtomicLong(0);

    private final AtomicInteger consecutiveWriteFailerCount = new AtomicInteger(0);

    private static int reconnectAttempts = 0;

    private Promise<MqttConnAckMessage> authFuture;

    private static final class TimeoutWithMessage extends TimeoutException {

        TimeoutWithMessage(String message) {
            super(message);
        }
    }

    public static final AttributeKey<ConcurrentHashMap<Integer, Promise<MqttSubAckMessage>>> PENDING_SUBSCRIBES = AttributeKey
            .valueOf(AppProperties.NAME_PENDING_SUBSCRIBES);

    public static final AttributeKey<ConcurrentHashMap<Integer, Promise<MqttUnsubAckMessage>>> PENDING_UNSUBSCRIBES = AttributeKey
            .valueOf(AppProperties.NAME_PENDING_UNSUBSCRIBES);

    public static final AttributeKey<Promise<MqttConnAckMessage>> CONNACK_PROMISE = AttributeKey
            .valueOf(AppProperties.NAME_CONNACK_PROMISE);

    public static final AttributeKey<ConcurrentHashMap<Integer, Promise<MqttPubAckMessage>>> PENDING_PUBACK = AttributeKey
            .valueOf(AppProperties.NAME_PENDING_PUBACK);

    public static final AttributeKey<ConcurrentHashMap<Integer, Promise<MqttMessage>>> PENDING_PUBREC = AttributeKey
            .valueOf(AppProperties.NAME_PENDING_PUBREC);

    public static final AttributeKey<ConcurrentHashMap<Integer, Promise<MqttMessage>>> PENDING_PUBCOMP = AttributeKey
            .valueOf(AppProperties.NAME_PENDING_PUBCOMP);

    public static final AttributeKey<ConcurrentHashMap<Integer, MqttPublishMessage>> PENDING_PUBREL = AttributeKey
            .valueOf(AppProperties.NAME_PENDING_PUBREL);
  
    @Override
    public void run(String... args) {
        logger.info("Start app with args={}.", Arrays.toString(args));
        if (this.appProperties.getAutoConnect()) {
            logger.info("Start auto connect.");
            this.connect();
        }
    }

    @Override
    public Promise<MqttConnAckMessage> connect() {
        logger.debug("Start connect method.");
        if (connected.get() || !connecting.compareAndSet(false, true)) {
            logger.warn("Connecting or connected already. connecting={}. connected={}. auhtFuture={}", connecting.get(),
                    connected.get(), authFuture);
            return authFuture;
        }

        logger.info("Start connection attempt.");
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(mqttChannelInitializer);

        authFuture = new DefaultPromise<>(workerGroup.next());
        authFuture.addListener(f -> {
            connecting.set(false);
            if (f.isSuccess()) {
                connected.set(true);
                logger.debug("Connection accepted. CONNACK message has been received {}.",
                        ((MqttConnAckMessage) f.get()).variableHeader());
                reconnectAttempts = 0;
            } else {
                logger.error("Connection attempt failed or timed out: {}", f.cause().getMessage());
                this.cancelConnect();
            }

            logger.debug("Connection attempt completed. authFuture isDone={}, isSuccess={}, isCancelled={}, future={}",
                    f.isDone(), f.isSuccess(), f.isCancelled(), f);
        });

        bootstrap.attr(CONNACK_PROMISE, authFuture);

        bootstrap.remoteAddress(this.appProperties.getHost(),
                AppUtils.safeParseInt(this.appProperties.getPort()).orElse(1883));
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.connectTimeout);
        ChannelFuture channelFuture = bootstrap.connect();
        channelFuture.addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                HmMq2tImpl.this.channel = f.channel();
                logger.info("TCP connected to {} via port {}. Waiting for ConnAckMessage.",
                        this.appProperties.getHost(), this.appProperties.getPort());
            } else {
                logger.error("TCP connection failed to establish (Host: {}, Port: {}). Cause: {}",
                        this.appProperties.getHost(), this.appProperties.getPort(), f.cause().getMessage());
                authFuture.tryFailure(f.cause());
            }
        });

        logger.info("Connecting to {} via port {}.", this.appProperties.getHost(), this.appProperties.getPort());

        return authFuture;
    }

    private void cancelConnect() {
        logger.debug("Executing cancelConnect to clean up resources.");

        if (this.authFuture != null && !this.authFuture.isDone()) {
            this.authFuture.cancel(true);
            logger.debug("Auth future has been cancelled.");
        }

        if (this.channel != null && this.channel.isOpen()) {
            logger.info("Closing active Netty channel to prevent leaks.");
            this.channel.close();
            this.channel = null;
        }

        connecting.set(false);
        connected.set(false);
    }

    @Override
    public void reconnect() {
        logger.debug("Start reconnect method.");
        if (!this.appProperties.getReconnect()) {
            logger.info("Reconnect is not allowed by config.");
            return;
        }

        if (reconnecting.get() || connecting.get()) {
            logger.info("Unable to start reconnecting. The connection is being reconnected.");
            return;
        }

        reconnecting.set(true);
        reconnectAttempts = reconnectAttempts + 1;
        logger.info("Start reconnect! Attempt {}.", reconnectAttempts);

        this.disconnect(MqttReasonCodeAndPropertiesVariableHeader.REASON_CODE_OK);

        int timeout = this.reconnectDelay * reconnectAttempts;
        if (timeout > this.reconnectDelayMax) {
            timeout = this.reconnectDelayMax;
        }

        try {
            TimeUnit.MILLISECONDS.sleep(timeout);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.info("InterruptedException while reconnect delay.", ex);
        }

        Promise<MqttConnAckMessage> reconnectFuture = this.connect();
        reconnectFuture.awaitUninterruptibly(this.connectTimeout);
        if (reconnectFuture.isCancelled()) {
            logger.info("Reconnection canceled.");
        } else if (!reconnectFuture.isSuccess()) {
            logger.info("Reconnection failed");
        } else {
            logger.info("Reconnection is successful.");
        }
        reconnecting.set(false);
    }

    private Optional<MqttPingScheduleHandler> getPingHandler() {
        return Optional.ofNullable(((MqttPingScheduleHandler) channel.pipeline().get(MqttPingScheduleHandler.NAME)));
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }

    @Override
    public void disconnect(byte reasonCode) {
        logger.info("Initiating disconnect sequence. Reason code: {}", reasonCode);

        if (authFuture != null && !authFuture.isDone()) {
            authFuture.cancel(true);
        }
        connecting.set(false);

        this.getPingHandler().ifPresent(pingHandler -> pingHandler.stopPing());

        if (this.appProperties.getCleanSession()) {
            this.subscribedTopics.clear();
            CompletableFuture.runAsync(() -> this.messageRepository.deleteAll(), this.processExecutor)
                    .whenComplete((v, ex) -> {
                        if (ex != null) {
                            logger.error("Failed to clear pending messages from DB on disconnect.", ex);
                        }
                    });
        }

        connected.set(false);
        consecutiveWriteFailerCount.set(0);
        writeFailureCount.set(0);

        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.DISCONNECT, false, MqttQoS.AT_MOST_ONCE,
                false, 0);
        MqttReasonCodeAndPropertiesVariableHeader mqttDisconnectVariableHeader = new MqttReasonCodeAndPropertiesVariableHeader(
                reasonCode, MqttProperties.NO_PROPERTIES);
        MqttMessage message = new MqttMessage(mqttFixedHeader, mqttDisconnectVariableHeader);

        if (this.channel != null && this.channel.isActive()) {
            this.writeAndFlush(message).addListener((ChannelFutureListener) writeFuture -> {
                logger.info("Sent disconnection message reason={}, d={}, q={}, r={}.",
                        mqttDisconnectVariableHeader.reasonCode(),
                        message.fixedHeader().isDup(),
                        message.fixedHeader().qosLevel(),
                        message.fixedHeader().isRetain());

                this.channel.close().addListener(f -> {
                    logger.debug("Netty channel closed after sending DISCONNECT.");
                    this.channel = null;
                });
            });

        } else {
            logger.warn("Channel is not active. Skipping DISCONNECT message packet.");
            if (this.channel != null) {
                this.channel.close();
                this.channel = null;
            }
        }
    }

    public void shutdown() {
        logger.info("Deactivating MQTT client completely. Shutting down worker group.");
        if (this.channel != null) {
            this.channel.close().awaitUninterruptibly();
            this.channel = null;
        }
        
        if (this.workerGroup != null) {
            this.workerGroup.shutdownGracefully().awaitUninterruptibly();
            logger.info("Netty worker group shutdown gracefully completed.");
        }
    }

    @Override
    public Promise<MqttSubAckMessage> subscribe(List<MqttTopicSubscription> subscriptions) {
        int id = getNewMessageId();

        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.SUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE,
                false, 0);
        MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(id);
        MqttSubscribePayload payload = new MqttSubscribePayload(subscriptions);
        MqttSubscribeMessage message = new MqttSubscribeMessage(fixedHeader, variableHeader, payload);

        Promise<MqttSubAckMessage> subscribeFuture = this.channel.eventLoop().newPromise();

        var pendingSubsAttr = this.channel.attr(PENDING_SUBSCRIBES);
        ConcurrentHashMap<Integer, Promise<MqttSubAckMessage>> pendingSubs = pendingSubsAttr.get();
        if (pendingSubs == null) {
            pendingSubs = new ConcurrentHashMap<>();
            var oldMap = pendingSubsAttr.setIfAbsent(pendingSubs);
            if (oldMap != null) {
                pendingSubs = oldMap;
            }
        }
        pendingSubs.put(id, subscribeFuture);

        final ConcurrentHashMap<Integer, Promise<MqttSubAckMessage>> finalPendingSubs = pendingSubs;
        final List<MqttTopicSubscription> topics = message.payload().topicSubscriptions();

        ScheduledFuture<?> scheduledTask = this.channel.eventLoop().schedule(() -> {
            if (subscribeFuture != null && !subscribeFuture.isDone()) {
                logger.warn("Timeout SUBACK for id={} is over.", id);
                subscribeFuture.tryFailure(new TimeoutWithMessage("Broker did not answer for subscribe message id=" + id));
            }
        }, this.subscribeTimeout, TimeUnit.SECONDS);

        subscribeFuture.addListener((Promise<MqttSubAckMessage> f) -> {
            finalPendingSubs.remove(id);
            if (scheduledTask != null && !scheduledTask.isDone()) {
                boolean cancelled = scheduledTask.cancel(false);
                if (!cancelled) {
                    logger.warn("Failed to cancel timeout task for message id={}, it might be executing right now", id);
                } else {
                    logger.debug("Timeout task for message id={} cancelled", id);
                }
            }
            if (f.isSuccess()) {
                var ack = f.getNow();
                logger.info("Subscribe message id={} has been acknowledged", ack.variableHeader().messageId());

                List<Integer> grantedQosLevels = ack.payload().grantedQoSLevels();
                for (int i = 0; i < topics.size(); i++) {
                    String topicName = topics.get(i).topicFilter();
                    int qosCode = grantedQosLevels.get(i);
                    if (qosCode >= 0 && qosCode <= 2) {
                        MqttQoS grantedQos = MqttQoS.valueOf(qosCode);
                        this.subscribedTopics.put(topicName, grantedQos);
                        logger.info("Successfully subscribed to topic: {} with QoS: {}", topicName, grantedQos);
                    } else {
                        logger.warn("Subscription failed for topic: {} with error code: {}", topicName, qosCode);
                    }
                }
            } else {
                logger.warn("Subscribe message id={} failed: {}", id, f.cause().getMessage());
            }
        });

        this.writeAndFlush(message);
        logger.info("Sent SUBSCRIBE message id={}.", message.variableHeader().messageId());

        return subscribeFuture;
    }

    @Override
    public Promise<MqttUnsubAckMessage> unsubscribe(List<String> topics) {
        int id = getNewMessageId();
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE,
                false, 0);
        MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(id);
        MqttUnsubscribePayload payload = new MqttUnsubscribePayload(topics);
        MqttUnsubscribeMessage unsubscribeMessage = new MqttUnsubscribeMessage(fixedHeader, variableHeader, payload);

        Promise<MqttUnsubAckMessage> unsubscribeFuture = this.channel.eventLoop().newPromise();

        var pendingUnsubsAttr = this.channel.attr(PENDING_UNSUBSCRIBES);
        ConcurrentHashMap<Integer, Promise<MqttUnsubAckMessage>> pendingUnsubs = pendingUnsubsAttr.get();
        if (pendingUnsubs == null) {
            pendingUnsubs = new ConcurrentHashMap<>();
            var oldMap = pendingUnsubsAttr.setIfAbsent(pendingUnsubs);
            if (oldMap != null) {
                pendingUnsubs = oldMap;
            }
        }

        pendingUnsubs.put(id, unsubscribeFuture);

        final ConcurrentHashMap<Integer, Promise<MqttUnsubAckMessage>> finalPendingUnsubs = pendingUnsubs;

        ScheduledFuture<?> scheduledTask = this.channel.eventLoop().schedule(() -> {
            if (unsubscribeFuture != null && !unsubscribeFuture.isDone()) {
                logger.warn("Timeout UNSUBACK for id={} is over.", id);
                unsubscribeFuture.tryFailure(
                        new TimeoutWithMessage("Broker did not answer for unsubscribe message id=" + id));
            }
        }, this.subscribeTimeout, TimeUnit.SECONDS);

        unsubscribeFuture.addListener((Promise<MqttUnsubAckMessage> f) -> {
            finalPendingUnsubs.remove(id);
            if (scheduledTask != null && !scheduledTask.isDone()) {
                scheduledTask.cancel(false);
            }
            if (f.isSuccess()) {
                var ack = f.getNow();
                logger.info("Unsubscribe message id={} has been acknowledged", ack.variableHeader().messageId());
                this.subscribedTopics.keySet().removeAll(topics);
            } else {
                logger.warn("Unsubscribe message id={} failed: {}", id, f.cause().getMessage());
            }
        });

        this.writeAndFlush(unsubscribeMessage);
        logger.info("Sent UNSUBSCRIBE message id={}.", unsubscribeMessage.variableHeader().messageId());

        return unsubscribeFuture;
    }

    @Override
    public void publish(String topic, ByteBuf payload, MqttQoS qos, boolean retain) {
        switch (qos) {
            case MqttQoS.AT_MOST_ONCE -> {
                this.publishAtMostOnce(topic, payload, retain);
            }
            case MqttQoS.AT_LEAST_ONCE -> {
                this.publishAtLeastOnce(topic, payload, retain);
            }
            case MqttQoS.EXACTLY_ONCE -> {
                this.publishExactlyOnce(topic, payload, retain);
            }
            default -> {
                logger.warn("Invalid publish QoS={} for topic={}. Message will not be sent.", qos, topic);
                throw new IllegalArgumentException("Invalid publish QoS=" + qos + " for topic=" + topic);
            }
        }
    }

    public void publishAtMostOnce(String topic, ByteBuf payload, boolean retain) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, retain,
                0);
        MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(topic, -1);
        MqttPublishMessage message = new MqttPublishMessage(fixedHeader, variableHeader, payload);

        this.writeAndFlush(message);
        logger.info("Sent publish message id={}, t={}, d={}, q={}, r={}.",
                message.variableHeader().packetId(),
                message.variableHeader().topicName(),
                message.fixedHeader().isDup(),
                message.fixedHeader().qosLevel(),
                message.fixedHeader().isRetain());
    }

    private Promise<MqttPubAckMessage> publishAtLeastOnce(String topic, ByteBuf payload, boolean retain) {
        int id = this.getNewMessageId();

        byte[] bytes = new byte[payload.readableBytes()];
        payload.getBytes(payload.readerIndex(), bytes);

        CompletableFuture.runAsync(() -> {
            logger.debug("Saving pending PUBLISH message (QoS=1) to DB: id={}, topic={}, qos={}, retain={}, status={}, payloadSize={}.",
                    id, topic, MqttQoS.AT_LEAST_ONCE.value(), retain, MqttMessageEntity.STATUS_SEND_PUBLISH, bytes.length);
            this.messageRepository.save(new MqttMessageEntity(id, topic, bytes,
                    MqttQoS.AT_LEAST_ONCE.value(), retain, MqttMessageEntity.STATUS_SEND_PUBLISH));
            logger.debug("Pending PUBLISH message (QoS=1) id={} has been saved to DB.", id);
        }, this.processExecutor).whenComplete((v, ex) -> {
            if (ex != null) {
                logger.error("Failed to save pending PUBLISH message id={} to DB.", id, ex);
            }
        });

        return this.doPublishAtLeastOnce(id, topic, Unpooled.wrappedBuffer(bytes), retain, false);
    }

    /**
     * Отправляет PUBLISH с QoS=1 и ожидает PUBACK. Используется как при обычной
     * отправке (dup=false, id сгенерирован заново), так и при ретрансляции
     * pending-сообщения из БД (dup=true, сохранённый packetId). Подтверждение
     * обрабатывается одинаково: при получении PUBACK строка удаляется из БД.
     */
    private Promise<MqttPubAckMessage> doPublishAtLeastOnce(int id, String topic, ByteBuf payload, boolean retain,
            boolean dup) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, dup, MqttQoS.AT_LEAST_ONCE, retain,
                0);
        MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(topic, id);
        MqttPublishMessage message = new MqttPublishMessage(fixedHeader, variableHeader, payload);

        Promise<MqttPubAckMessage> publishFuture = this.channel.eventLoop().newPromise();

        var pendingPubAckAttr = this.channel.attr(PENDING_PUBACK);
        ConcurrentHashMap<Integer, Promise<MqttPubAckMessage>> pendingPubAck = pendingPubAckAttr.get();
        if (pendingPubAck == null) {
            pendingPubAck = new ConcurrentHashMap<>();
            ConcurrentHashMap<Integer, Promise<MqttPubAckMessage>> oldMap = pendingPubAckAttr.setIfAbsent(pendingPubAck);
            if (oldMap != null) {
                pendingPubAck = oldMap;
            }
        }

        pendingPubAck.put(id, publishFuture);
        
        final ConcurrentHashMap<Integer, Promise<MqttPubAckMessage>> finalPendingPubAck = pendingPubAck;

        ScheduledFuture<?> scheduledTask = this.channel.eventLoop().schedule(() -> {
            if (publishFuture != null && !publishFuture.isDone()) {
                logger.warn("Timeout PUBLISH with QoS=1 for id={} is over.", id);
                publishFuture.tryFailure(
                        new TimeoutWithMessage("Broker did not answer for publish message."));
            }
        }, this.publishQos1Timeout, TimeUnit.SECONDS);

        publishFuture.addListener((Promise<MqttPubAckMessage> f) -> {
            finalPendingPubAck.remove(id);
            if (scheduledTask != null && !scheduledTask.isDone()) {
                scheduledTask.cancel(false);
            }
            if (f.isSuccess()) {
                var ack = f.getNow();
                logger.info("Publish message id={} has been acknowledged", ack.variableHeader().messageId());
                CompletableFuture.runAsync(() -> {
                    this.messageRepository.deleteByPacketId(id);
                }, this.processExecutor).whenComplete((v, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to delete acknowledged PUBLISH message id={} from DB.", id, ex);
                    }
                });
            } else {
                logger.warn("Publish message with QoS=1 id={} failed: {}", id, f.cause().getMessage());
            }
        });

        this.writeAndFlush(message);
        logger.info("Sent publish message id={}, t={}, d={}, q={}, r={}.",
                message.variableHeader().packetId(),
                message.variableHeader().topicName(),
                message.fixedHeader().isDup(),
                message.fixedHeader().qosLevel(),
                message.fixedHeader().isRetain());

        return publishFuture;
    }
    
    private Promise<MqttMessage> publishExactlyOnce(String topic, ByteBuf payload, boolean retain) {
        int id = this.getNewMessageId();

        byte[] bytes = new byte[payload.readableBytes()];
        payload.getBytes(payload.readerIndex(), bytes);

        CompletableFuture.runAsync(() -> {
            logger.debug("Saving pending PUBLISH message (QoS=2) to DB: id={}, topic={}, qos={}, retain={}, status={}, payloadSize={}.",
                    id, topic, MqttQoS.EXACTLY_ONCE.value(), retain, MqttMessageEntity.STATUS_SEND_PUBLISH, bytes.length);
            this.messageRepository.save(new MqttMessageEntity(id, topic, bytes,
                    MqttQoS.EXACTLY_ONCE.value(), retain, MqttMessageEntity.STATUS_SEND_PUBLISH));
            logger.debug("Pending PUBLISH message (QoS=2) id={} has been saved to DB.", id);
        }, this.processExecutor).whenComplete((v, ex) -> {
            if (ex != null) {
                logger.error("Failed to save pending PUBLISH message id={} to DB.", id, ex);
            }
        });

        return this.doPublishExactlyOnce(id, topic, Unpooled.wrappedBuffer(bytes), retain, false);
    }

    /**
     * Отправляет PUBLISH с QoS=2 и ожидает PUBREC. Используется как при обычной
     * отправке (dup=false, id сгенерирован заново), так и при ретрансляции
     * pending-сообщения из БД (dup=true, сохранённый packetId). Подтверждение
     * обрабатывается одинаково: при получении PUBREC строка переводится в статус
     * SEND_PUBREL и отправляется PUBREL.
     */
    private Promise<MqttMessage> doPublishExactlyOnce(int id, String topic, ByteBuf payload, boolean retain,
            boolean dup) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, dup, MqttQoS.EXACTLY_ONCE, retain,
                0);
        MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(topic, id);
        MqttPublishMessage message = new MqttPublishMessage(fixedHeader, variableHeader, payload);

        Promise<MqttMessage> publishFuture = this.channel.eventLoop().newPromise();

        var pendingPubRecAttr = this.channel.attr(PENDING_PUBREC);
        ConcurrentHashMap<Integer, Promise<MqttMessage>> pendingPubRec = pendingPubRecAttr.get();
        if (pendingPubRec == null) {
            pendingPubRec = new ConcurrentHashMap<>();
            ConcurrentHashMap<Integer, Promise<MqttMessage>> oldMap = pendingPubRecAttr.setIfAbsent(pendingPubRec);
            if (oldMap != null) {
                pendingPubRec = oldMap;
            }
        }

        pendingPubRec.put(id, publishFuture);
        
        final ConcurrentHashMap<Integer, Promise<MqttMessage>> finalPendingPubRec = pendingPubRec;

        ScheduledFuture<?> scheduledTask = this.channel.eventLoop().schedule(() -> {
            if (publishFuture != null && !publishFuture.isDone()) {
                logger.warn("Timeout PUBLISH with QoS=2 for id={} is over.", id);
                publishFuture.tryFailure(
                        new TimeoutWithMessage("Broker did not answer for publish message id=" + id));
            }
        }, this.publishQos2Timeout, TimeUnit.SECONDS);

        publishFuture.addListener((Promise<MqttMessage> f) -> {
            finalPendingPubRec.remove(id);
            if (scheduledTask != null && !scheduledTask.isDone()) {
                scheduledTask.cancel(false);
            }
            if (f.isSuccess()) {
                var ack = f.getNow();
                int packetId = ((MqttMessageIdVariableHeader) ack.variableHeader()).messageId();
                logger.info("Publish message id={} has been acknowledged", packetId);
                CompletableFuture.runAsync(() -> {
                    this.messageRepository.updateStatusByPacketId(packetId, MqttMessageEntity.STATUS_SEND_PUBREL);
                }, this.processExecutor).whenComplete((v, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to update status of pending PUBLISH message id={} to {} in DB.",
                                packetId, MqttMessageEntity.STATUS_SEND_PUBREL, ex);
                    }
                });
                this.sendPubRelMessage(packetId);
            } else {
                logger.warn("Publish message with QoS=2 id={} failed: {}", id, f.cause().getMessage());
            }
        });

        this.writeAndFlush(message);
        logger.info("Sent publish message id={}, t={}, d={}, q={}, r={}.",
                message.variableHeader().packetId(),
                message.variableHeader().topicName(),
                message.fixedHeader().isDup(),
                message.fixedHeader().qosLevel(),
                message.fixedHeader().isRetain());

        return publishFuture;
    }

    @Override
    public void retransmitPublish(String topic, byte[] payload, MqttQoS qos, boolean retain, int packetId) {
        if (this.channel == null) {
            logger.error("Cannot retransmit PUBLISH message id={}: channel is null.", packetId);
            return;
        }

        switch (qos) {
            case MqttQoS.AT_LEAST_ONCE ->
                this.doPublishAtLeastOnce(packetId, topic, Unpooled.wrappedBuffer(payload), retain, true);
            case MqttQoS.EXACTLY_ONCE ->
                this.doPublishExactlyOnce(packetId, topic, Unpooled.wrappedBuffer(payload), retain, true);
            default ->
                logger.warn("Invalid retransmit QoS={} for message id={}. Message is skipped.", qos, packetId);
        }
    }

    @Override
    public void retransmitPubRel(int packetId) {
        if (this.channel == null) {
            logger.error("Cannot retransmit PUBREL message id={}: channel is null.", packetId);
            return;
        }
        this.sendPubRelMessage(packetId);
    }

    /**
     * Отправляет PUBREL для сообщения с QoS=2 после получения PUBREC
     * и ожидает PUBCOMP. Удаление pending-сообщения из БД выполняется
     * в {@link MqttPublishHandlerImpl#handlePubComp} при получении PUBCOMP.
     * Для ретрансляции PUBREL достаточно строки со статусом SEND_PUBREL:
     * PUBREL восстанавливается из packet_id. Метод также используется
     * {@link MqttRetransmitServiceImpl} при переподключении.
     */
    private Promise<MqttMessage> sendPubRelMessage(int id) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_LEAST_ONCE, false,
                0);
        MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(id);
        MqttMessage pubrelMessage = new MqttMessage(fixedHeader, variableHeader);

        Promise<MqttMessage> pubRelFuture = this.channel.eventLoop().newPromise();

        var pendingPubCompAttr = this.channel.attr(PENDING_PUBCOMP);
        ConcurrentHashMap<Integer, Promise<MqttMessage>> pendingPubComp = pendingPubCompAttr.get();
        if (pendingPubComp == null) {
            pendingPubComp = new ConcurrentHashMap<>();
            ConcurrentHashMap<Integer, Promise<MqttMessage>> oldMap = pendingPubCompAttr.setIfAbsent(pendingPubComp);
            if (oldMap != null) {
                pendingPubComp = oldMap;
            }
        }

        pendingPubComp.put(id, pubRelFuture);

        final ConcurrentHashMap<Integer, Promise<MqttMessage>> finalPendingPubComp = pendingPubComp;

        ScheduledFuture<?> scheduledTask = this.channel.eventLoop().schedule(() -> {
            if (pubRelFuture != null && !pubRelFuture.isDone()) {
                logger.warn("Timeout PUBREL with QoS=2 for id={} is over.", id);
                pubRelFuture.tryFailure(
                        new TimeoutWithMessage("Broker did not answer with PUBCOMP for message id=" + id));
            }
        }, this.pubRelQos2Timeout, TimeUnit.SECONDS);

        pubRelFuture.addListener((Promise<MqttMessage> f) -> {
            finalPendingPubComp.remove(id);
            if (scheduledTask != null && !scheduledTask.isDone()) {
                scheduledTask.cancel(false);
            }
            if (f.isSuccess()) {
                var ack = f.getNow();
                int packetId = ((MqttMessageIdVariableHeader) ack.variableHeader()).messageId();
                logger.info("PUBREL message id={} has been acknowledged with PUBCOMP", packetId);
            } else {
                logger.warn("PUBREL message with QoS=2 id={} failed or timed out: {}", id, f.cause().getMessage());
            }
        });

        this.writeAndFlush(pubrelMessage);
        logger.info("Sent PUBREL message id={}, d={}, q={}, r={}.",
                variableHeader.messageId(),
                pubrelMessage.fixedHeader().isDup(),
                pubrelMessage.fixedHeader().qosLevel(),
                pubrelMessage.fixedHeader().isRetain());

        return pubRelFuture;
    }

    private ChannelFuture writeAndFlush(Object message) {
        if (this.channel == null) {
            logger.error("Cannot write and flush message. Channel is null. Total failures={}, consecutive failures={}.",
                    writeFailureCount.incrementAndGet(),
                    consecutiveWriteFailerCount.incrementAndGet());
            ReferenceCountUtil.release(message);
            return null;
        }
        
        ChannelFuture future = this.channel.writeAndFlush(message);
        future.addListener((ChannelFutureListener) f -> {
            if (!f.isSuccess()) {
              ReferenceCountUtil.release(message);
                logger.error("Write failed. Total failures={}, consecutive failures={}. {}",
                        writeFailureCount.incrementAndGet(),
                        consecutiveWriteFailerCount.incrementAndGet(),
                        f.cause());
            } else {
                consecutiveWriteFailerCount.set(0);
            }
        });
        
        return future;
    }

    /**
     * Максимальное число попыток найти свободный packetId. Ограничивает цикл,
     * чтобы не зациклиться, если в БД скопилось много необработанных сообщений.
     */
    private static final int MAX_ID_ALLOCATION_ATTEMPTS = 100;

    /**
     * Генерирует новый уникальный packetId для MQTT-пакета.
     *
     * Многопоточность:
     * 1. Метод может одновременно вызываться из нескольких потоков Netty
     *    (event loop) и из потоков processExecutor. Значение nextMessageId
     *    изменяется только через AtomicInteger.updateAndGet, который атомарен,
     *    поэтому параллельные потоки НИКОГДА не получат один и тот же id —
     *    каждый вызывающий поток гарантированно получает уникальный кандидат.
     * 2. Занятость кандидата проверяется по БД (pending-сообщения, ожидающие
     *    подтверждения брокера). Если id занят — берём следующий. Так как
     *    каждый поток получает свой уникальный кандидат от AtomicInteger,
     *    конкуренция за один и тот же id между потоками исключена.
     * 3. Метод синхронный (возвращает int), поэтому проверка БД выполняется
     *    блокирующе. Вызовы идут через быстрый EXISTS-запрос
     *    {@link MqttMessageRepository#existsByPacketId(int)} и число попыток
     *    ограничено {@link #MAX_ID_ALLOCATION_ATTEMPTS}, чтобы не блокировать
     *    event loop надолго.
     *
     * @return свободный (не занятый в БД) packetId в диапазоне 1..0xffff
     */
    private int getNewMessageId() {
        int id = 1;

        for (int attempt = 0; attempt < MAX_ID_ALLOCATION_ATTEMPTS; attempt++) {
            // Атомарно увеличиваем. Если вышли за 0xffff или ушли в минус/ноль — сбрасываем в 1
            id = this.nextMessageId.updateAndGet(current ->
                    (current >= 0xffff || current <= 0) ? 1 : current + 1
            );

            try {
                // Проверяем, не занят ли id в БД необработанным (pending) сообщением
                if (!this.messageRepository.existsByPacketId(id)) {
                    return id;
                }
                logger.warn("Packet id={} is already occupied in DB (pending message). Trying next id, attempt={}/{}.",
                        id, attempt + 1, MAX_ID_ALLOCATION_ATTEMPTS);
            } catch (Exception ex) {
                // БД недоступна — не блокируем отправку сообщения, считаем id свободным.
                // Ошибка при сохранении pending-сообщения в БД будет залогирована отдельно.
                logger.error("Failed to check packet id={} occupancy in DB. Returning it as free.", id, ex);
                return id;
            }
        }

        // Все попытки исчерпаны — возвращаем последний сгенерированный id,
        // чтобы не блокировать отправку. Дубликат возможен только в том
        // случае, если предыдущее сообщение с таким же id всё ещё ожидает
        // подтверждения в БД.
        logger.error("Could not find a free packet id after {} attempts. Returning id={} anyway.",
                MAX_ID_ALLOCATION_ATTEMPTS, id);
        return id;
    }

    @Override
    public void setMediator(ServiceMediator serviceMediator) {
    }

    public String getSubscribedTopicAndQosAsString() {
        return this.subscribedTopics.keySet().stream().map(key -> this.subscribedTopics.get(key).toString())
                .collect(Collectors.joining("\\n ", "", ""));
    }

}
