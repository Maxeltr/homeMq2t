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
package ru.maxeltr.homeMq2t.Config;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.maxeltr.homeMq2t.Model.Card;
import ru.maxeltr.homeMq2t.Model.CardImpl;
import ru.maxeltr.homeMq2t.Model.Dashboard;
import ru.maxeltr.homeMq2t.Model.DashboardImpl;
import ru.maxeltr.homeMq2t.Mqtt.HmMq2t;
import ru.maxeltr.homeMq2t.Mqtt.HmMq2tImpl;
import ru.maxeltr.homeMq2t.Mqtt.MqttAckMediator;
import ru.maxeltr.homeMq2t.Mqtt.MqttAckMediatorImpl;
import ru.maxeltr.homeMq2t.Mqtt.MqttChannelInitializer;
import ru.maxeltr.homeMq2t.Mqtt.MqttPublishHandlerImpl;
import ru.maxeltr.homeMq2t.Service.CommandService;
import ru.maxeltr.homeMq2t.Service.CommandServiceImpl;
import ru.maxeltr.homeMq2t.Service.ServiceMediator;
import ru.maxeltr.homeMq2t.Service.ServiceMediatorImpl;
import ru.maxeltr.homeMq2t.Service.UIService;
import ru.maxeltr.homeMq2t.Service.UIServiceImpl;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AppAnnotationConfig {

    @Autowired
    private Environment env;

    @Bean
    public AppProperties appProperty() {
        return new AppProperties();
    }

    @Bean
    public HmMq2t getHmMq2t() {
        return new HmMq2tImpl();
    }

    @Bean
    public MqttChannelInitializer getMqttChannelInitializer() {
        return new MqttChannelInitializer();
    }

    @Bean
    public MqttAckMediator getMqttAckMediator() {
        return new MqttAckMediatorImpl();
    }

    @Bean
    public ServiceMediator getServiceMediator() {
        return new ServiceMediatorImpl();
    }

    @Bean
    public CommandService getCommandService() {
        return new CommandServiceImpl();
    }

    @Bean
    public UIService getUIService() {
        return new UIServiceImpl();
    }

    @Bean
    public MqttPublishHandlerImpl getMqttPublishHandler(MqttAckMediator mqttAckMediator) {
        return new MqttPublishHandlerImpl(mqttAckMediator);
    }

    @Bean
    public List<Dashboard> dashboards() {
        int i = 0;
        List<Card> cards = new ArrayList<>();
        List<Dashboard> dashboards = new ArrayList<>();
        while (!env.getProperty("dashboard[" + i + "].name", "").isEmpty()) {
            List<String> listOfCards = (List<String>) env.getProperty("dashboard[" + i + "].cards", List.class);
            for (String cardNumber : listOfCards) {
                String cardName = env.getProperty("card[" + cardNumber + "].name", "");
                String sub = env.getProperty("card[" + cardNumber + "].subTopic", "");
                String pub = env.getProperty("card[" + cardNumber + "].pubTopic", "");
                Card card = new CardImpl(cardName, sub, pub);
                cards.add(card);
            }
            String dashboardName = env.getProperty("dashboard[" + i + "].name", "");
            Dashboard dashboard = new DashboardImpl(dashboardName, cards);
            dashboards.add(dashboard);
            ++i;
        }

        return dashboards;
    }
    
    @Bean
    public List<MqttTopicSubscription> subscriptions() {
        int i = 0;
        String topic;
        MqttQoS topicQos;
        List<MqttTopicSubscription> subscriptions = new ArrayList<>();
        while (!env.getProperty("card[" + i + "].name", "").isEmpty()) {
            topic = env.getProperty("card[" + i + "].subscription.topic", "");
            topicQos = MqttQoS.valueOf(env.getProperty("card[" + i + "].subscription.qos", MqttQoS.AT_MOST_ONCE.toString()));
            MqttTopicSubscription subscription = new MqttTopicSubscription(topic, topicQos);
            subscriptions.add(subscription);
            ++i;
        }
        
        return subscriptions;
    }
}
