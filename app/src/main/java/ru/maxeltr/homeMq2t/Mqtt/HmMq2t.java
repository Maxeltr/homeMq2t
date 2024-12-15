package ru.maxeltr.homeMq2t.Mqtt;

import io.netty.util.concurrent.Promise;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
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

    public void subscribe(List<MqttTopicSubscription> subscriptions);

    public void unsubscribe();

    public void publish();

    public void setMediator(MqttAckMediator mqttAckMediator);

    public void setMediator(ServiceMediator serviceMediator);
}
