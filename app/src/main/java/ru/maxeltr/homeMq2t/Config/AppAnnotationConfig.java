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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import ru.maxeltr.homeMq2t.AppShutdownManager;
import ru.maxeltr.homeMq2t.Model.Card;
import ru.maxeltr.homeMq2t.Model.CardImpl;
import ru.maxeltr.homeMq2t.Model.Dashboard;
import ru.maxeltr.homeMq2t.Model.DashboardImpl;
import ru.maxeltr.homeMq2t.Mqtt.HmMq2t;
import ru.maxeltr.homeMq2t.Mqtt.HmMq2tImpl;
import ru.maxeltr.homeMq2t.Mqtt.MqttAckMediator;
import ru.maxeltr.homeMq2t.Mqtt.MqttAckMediatorImpl;
import ru.maxeltr.homeMq2t.Mqtt.MqttChannelInitializer;
import ru.maxeltr.homeMq2t.Service.CommandService;
import ru.maxeltr.homeMq2t.Service.CommandServiceImpl;
import ru.maxeltr.homeMq2t.Service.ComponentLoader;
import ru.maxeltr.homeMq2t.Service.ComponentServiceImpl;
import ru.maxeltr.homeMq2t.Service.ComponentService;
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

    private static final Logger logger = LoggerFactory.getLogger(AppAnnotationConfig.class);

    @Autowired
    private Environment env;

    @Bean
    public AppProperties getAppProperty() {
        logger.info("Current user dir={}", System.getProperty("user.dir"));
        String[] classpathes = System.getProperty("java.class.path").split(File.pathSeparator);
        for (String classpath : classpathes) {
            logger.info("Classpath={}", classpath);
        }
        logger.info("Java home={}", System.getenv("JAVA_HOME"));
        logger.info("OS name={}", System.getProperty("os.name"));

        return new AppProperties();
    }

    @Bean(name = "processExecutor")
    public TaskExecutor workExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setThreadNamePrefix("Async-");
        threadPoolTaskExecutor.setCorePoolSize(10);
        threadPoolTaskExecutor.setMaxPoolSize(20);
        threadPoolTaskExecutor.setQueueCapacity(600);
        threadPoolTaskExecutor.afterPropertiesSet();
        return threadPoolTaskExecutor;
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
    public ComponentService getComponentService() {
        int i = 0;
        List<Object> components = new ArrayList<>();
        ComponentLoader componentLoader = new ComponentLoader();
        while (!env.getProperty("component[" + i + "].name", "").isEmpty()) {
            String path = env.getProperty("component[" + i + "].path", "");
            ++i;

            List<Object> instances = componentLoader.loadClassesFromJar(path);
            if (instances == null || instances.isEmpty()) {
                logger.warn("Failed to load classes from path={}", path);
                continue;
            }
            components.addAll(instances);
        }

        return new ComponentServiceImpl(components);
    }

    @Bean
    public Map<String, String> topicsAndCards() {
        Map<String, String> map = new HashMap<>();
        int i = 0;
        while (!env.getProperty("card[" + i + "].name", "").isEmpty()) {
            String cardTopic = env.getProperty("card[" + i + "].subscription.topic", "");
            if (cardTopic.isEmpty()) {
                throw new IllegalArgumentException("No topic defined for subscription of card=" + i);
            }
            map.put(cardTopic, String.valueOf(i));
            ++i;
        }

        return map;
    }

    @Bean
    public Map<String, String> topicsAndCommands() {
        Map<String, String> map = new HashMap<>();
        int i = 0;
        while (!env.getProperty("command[" + i + "].name", "").isEmpty()) {
            String commandTopic = env.getProperty("command[" + i + "].subscription.topic", "");
            if (commandTopic.isEmpty()) {
                throw new IllegalArgumentException("No topic defined for subscription of command=" + i);
            }
            map.put(commandTopic, String.valueOf(i));
            ++i;
        }

        return map;
    }

    @Bean
    public Map<String, String> commandsAndNumbers() {
        Map<String, String> map = new HashMap<>();
        int i = 0;
        while (!env.getProperty("command[" + i + "].name", "").isEmpty()) {
            String commandName = env.getProperty("command[" + i + "].name", "");
            if (commandName.isEmpty()) {
                throw new IllegalArgumentException("No name defined for command=" + i);
            }
            map.put(commandName, String.valueOf(i));
            ++i;
        }

        return map;
    }

    @Bean
    public Map<String, String> topicsAndComponents() {
        Map<String, String> map = new HashMap<>();
        int i = 0;
        while (!env.getProperty("component[" + i + "].name", "").isEmpty()) {
            String componentTopic = env.getProperty("component[" + i + "].subscription.topic", "");
            if (componentTopic.isEmpty()) {
                throw new IllegalArgumentException("No topic defined for subscription of component=" + i);
            }
            map.put(componentTopic, String.valueOf(i));
            ++i;
        }

        return map;
    }

    @Bean
    public Map<String, String> componentsAndNumbers() {
        Map<String, String> map = new HashMap<>();
        int i = 0;
        while (!env.getProperty("component[" + i + "].name", "").isEmpty()) {
            String componentName = env.getProperty("component[" + i + "].name", "");
            if (componentName.isEmpty()) {
                throw new IllegalArgumentException("No name defined for component=" + i);
            }
            map.put(componentName, String.valueOf(i));
            ++i;
        }

        return map;
    }

    @Bean
    public List<Dashboard> dashboards() {
        int i = 0;
        List<Card> cards = new ArrayList<>();
        List<Dashboard> dashboards = new ArrayList<>();

        String dashboardPathname = env.getProperty("dashboard-template-path", "");
        if (dashboardPathname.isEmpty()) {
            throw new IllegalArgumentException("No name defined for dashboard template pathname.");
        }

        String cardPathname = env.getProperty("card-template-path", "");
        if (cardPathname.isEmpty()) {
            throw new IllegalArgumentException("No name defined for card template pathname.");
        }

        while (!env.getProperty("dashboard[" + i + "].name", "").isEmpty()) {
            List<String> listOfCards = (List<String>) env.getProperty("dashboard[" + i + "].cards", List.class);
            logger.info("Dashboard={} has cards={}", i, listOfCards);
            if (listOfCards == null || listOfCards.isEmpty()) {
                throw new IllegalArgumentException("No cards defined for dashboard=" + i);
            }

            for (String cardNumber : listOfCards) {
                String cardName = env.getProperty("card[" + cardNumber + "].name", "");
                if (cardName.isEmpty()) {
                    throw new IllegalArgumentException("No name defined for card=" + cardNumber);
                }
                Card card = new CardImpl(cardNumber, cardName, cardPathname);
                cards.add(card);
                logger.info("Card={} has been created and added to card list.", card.getName());
            }

            String dashboardName = env.getProperty("dashboard[" + i + "].name", "");
            if (dashboardName.isEmpty()) {
                throw new IllegalArgumentException("No name defined for dashboard=" + i);
            }

            Dashboard dashboard = new DashboardImpl(dashboardName, cards, dashboardPathname);
            dashboards.add(dashboard);

            logger.info("Dashboard={} has been created and added to dashboard list.", dashboard.getName());
            ++i;
        }

        if (dashboards.isEmpty()) {
            throw new IllegalArgumentException("Dashboard list is empty.");
        }

        logger.info("Create dashbord list with size={}.", dashboards.size());

        return dashboards;
    }

    @Bean
    public List<MqttTopicSubscription> subscriptions() {
        int i;
        String topic;
        MqttQoS topicQos;
        List<MqttTopicSubscription> subscriptions = new ArrayList<>();
        MqttTopicSubscription subscription;

        i = 0;
        while (!env.getProperty("card[" + i + "].name", "").isEmpty()) {
            topic = env.getProperty("card[" + i + "].subscription.topic", "");
            if (topic == null || topic.isEmpty()) {
                logger.warn("Topic for card subscrition={} is not defined", i);
                continue;
            }
            topicQos = MqttQoS.valueOf(env.getProperty("card[" + i + "].subscription.qos", MqttQoS.AT_MOST_ONCE.toString()));
            subscription = new MqttTopicSubscription(topic, topicQos);
            subscriptions.add(subscription);
            ++i;
        }

        i = 0;
        while (!env.getProperty("command[" + i + "].name", "").isEmpty()) {
            topic = env.getProperty("command[" + i + "].subscription.topic", "");
            if (topic == null || topic.isEmpty()) {
                logger.warn("Topic for command subscrition={} is not defined", i);
                continue;
            }
            topicQos = MqttQoS.valueOf(env.getProperty("command[" + i + "].subscription.qos", MqttQoS.AT_MOST_ONCE.toString()));
            subscription = new MqttTopicSubscription(topic, topicQos);
            subscriptions.add(subscription);
            ++i;
        }

        i = 0;
        while (!env.getProperty("component[" + i + "].name", "").isEmpty()) {
            topic = env.getProperty("component[" + i + "].subscription.topic", "");
            if (topic == null || topic.isEmpty()) {
                logger.warn("Topic for component subscrition={} is not defined", i);
                continue;
            }
            topicQos = MqttQoS.valueOf(env.getProperty("component[" + i + "].subscription.qos", MqttQoS.AT_MOST_ONCE.toString()));
            subscription = new MqttTopicSubscription(topic, topicQos);
            subscriptions.add(subscription);
            ++i;
        }

        return subscriptions;
    }

    @Bean
    public ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        //mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    @Bean
    public AppShutdownManager getAppShutdownManager() {
        return new AppShutdownManager();
    }

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(10);
        threadPoolTaskScheduler.setThreadNamePrefix("Mq2tThreadPoolTaskScheduler");
        return threadPoolTaskScheduler;
    }

    @Bean(name = "pingPeriodicTrigger")
    public PeriodicTrigger pingPeriodicTrigger() {
        Duration duration = Duration.ofMillis(Integer.parseInt(this.env.getProperty("keep-alive-timer", "20000")));
        PeriodicTrigger periodicTrigger = new PeriodicTrigger(duration);
        periodicTrigger.setInitialDelay(duration);
        return periodicTrigger;
    }

    @Bean(name = "retransmitPeriodicTrigger")
    public PeriodicTrigger retransmitPeriodicTrigger() {
        Duration duration = Duration.ofMillis(Integer.parseInt(this.env.getProperty("retransmit-delay", "60000")));
        PeriodicTrigger periodicTrigger = new PeriodicTrigger(duration);
        periodicTrigger.setInitialDelay(duration);
        return periodicTrigger;
    }

    @Bean(name = "pollingPeriodicTrigger")
    public PeriodicTrigger pollingPeriodicTrigger() {
        Duration duration = Duration.ofMillis(Integer.parseInt(this.env.getProperty("polling-sensors-delay", "10000")));
        PeriodicTrigger periodicTrigger = new PeriodicTrigger(duration);
        return periodicTrigger;
    }
}
