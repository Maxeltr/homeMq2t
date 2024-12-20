package ru.maxeltr.homeMq2t.Mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.util.concurrent.Promise;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import java.util.List;
import ru.maxeltr.homeMq2t.Service.ServiceMediator;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public interface HmMq2t {

    public Promise<MqttConnAckMessage> connect();

    public void disconnect(byte reasonCode);

    public Promise<MqttSubAckMessage> subscribe(List<MqttTopicSubscription> subscriptions);

    public Promise<MqttUnsubAckMessage> unsubscribe(List<String> topics);

    public void publish(String topic, ByteBuf payload, MqttQoS qos, boolean retain);

//    public void setMediator(MqttAckMediator mqttAckMediator);

    public void setMediator(ServiceMediator serviceMediator);
}
