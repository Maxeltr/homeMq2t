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
import ru.maxeltr.homeMq2t.Mqtt.MqttConnectHandler;
import ru.maxeltr.homeMq2t.Mqtt.MqttPublishHandlerImpl;
import ru.maxeltr.homeMq2t.Mqtt.MqttSubscriptionHandler;
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

//    public AppAnnotationConfig() {
//        try {
//            LogManager.getLogManager().readConfiguration(AppAnnotationConfig.class.getResourceAsStream("/logging.properties")
//            );
//        } catch (IOException | SecurityException ex) {
//            System.err.println("Could not setup logger configuration: " + ex.toString());
//        }
//    }

    @Bean
    public AppProperties appProperty() {
        return new AppProperties();
    }

    @Bean
    public HmMq2t hmMq2t() {
        return new HmMq2tImpl();
    }

    @Bean
    public MqttChannelInitializer mqttChannelInitializer() {
        return new MqttChannelInitializer();
    }

    @Bean
    public MqttAckMediator mqttAckMediator() {
        return new MqttAckMediatorImpl();
    }

    @Bean
    public ServiceMediator serviceMediator() {
        return new ServiceMediatorImpl();
    }

    @Bean
    public CommandService commandService() {
        return new CommandServiceImpl();
    }

    @Bean
    public UIService getUIService() {
        return new UIServiceImpl();
    }
    
    @Bean
    public MqttPublishHandlerImpl getMqttPublishHandler() {
        return new MqttPublishHandlerImpl();
    }
    
//    @Bean
//    public MqttSubscriptionHandler getMqttSubscriptionHandler() {
//        return new MqttSubscriptionHandler();
//    }
    
//    @Bean
//    public MqttConnectHandler getMqttConnectHandler() {
//        return new MqttConnectHandler();
//    }

    @Bean
    public List<Dashboard> dashboards() {
        List<Dashboard> dashboards = new ArrayList<>();
        List<Card> cards = new ArrayList<>();
        List<String> listOfDashboardNames = (List<String>) env.getProperty("dashboards", List.class);
        for (String dashboardName : listOfDashboardNames) {
            List<String> listOfCardsNames = (List<String>) env.getProperty(dashboardName + ".cards", List.class);
            for (String cardName : listOfCardsNames) {
                Card card = new CardImpl(cardName);
                cards.add(card);
            }
            Dashboard dashboard = new DashboardImpl(dashboardName, cards);
            dashboards.add(dashboard);
        }

        return dashboards;
    }


}
