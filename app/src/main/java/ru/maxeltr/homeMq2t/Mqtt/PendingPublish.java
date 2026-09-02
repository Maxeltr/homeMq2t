package ru.maxeltr.homeMq2t.Mqtt;

import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.util.concurrent.Promise;

public record PendingPublish(
    Promise<MqttPubAckMessage> future,
    MqttPublishMessage message,
    Long dbId
) {}
