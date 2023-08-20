package ru.maxeltr.homeMq2t.Mqtt;

import io.netty.util.concurrent.Promise;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public interface HmMq2t {

    public Promise<MqttConnAckMessage> connect();

    public void disconnect();

    public Promise<MqttSubAckMessage> subscribe();

    public Promise<MqttUnsubAckMessage> unsubscribe();

    public Promise<?> publish();

    public void setMediator(MqttAckMediator mqttAckMediator);
}
