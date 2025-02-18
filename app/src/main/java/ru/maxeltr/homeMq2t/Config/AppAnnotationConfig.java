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

    /**
     * Retrives a mapping of card topics to their corresponding card numbers.
     *
     * This method iterates through the card properties defined in the
     * environment, collecting topics and their associated card indices. Each
     * topic can be assotiated with multiple card numbers.
     *
     * @return a map where the key is the card topic and the value is a list of
     * card numbers assotiated with that topic.
     *
     * @throw IllegalArgumentException if a card topic is not defined for a
     * card.
     */
    @Bean
    public Map<String, List<String>> topicsAndCards() {
        Map<String, List<String>> map = new HashMap<>();
        int i = 0;
        logger.info("Starting to collect topics and their corresponding card numbers.");
        while (true) {
            String card = env.getProperty(String.format("card[%d].name", i), "");
            if (card == null || card.isEmpty()) {
                break;
            }

            String cardTopic = env.getProperty(String.format("card[%d].subscription.topic", i), "");
            if (cardTopic == null || cardTopic.isEmpty()) {
                throw new IllegalArgumentException("No topic defined for subscription of card=" + i);
            }

            List<String> cardNumbers = map.getOrDefault(cardTopic, new ArrayList<>());
            cardNumbers.add(String.valueOf(i));

            map.put(cardTopic, cardNumbers);
            logger.info("Add topic={} and card numbers={}.", cardTopic, cardNumbers);
            ++i;
        }

        logger.info("Topics and cards collection completed. Found {} topics.", map.size());

        return map;
    }

    /**
     * Retrives a mapping of command topics to their corresponding command
     * numbers.
     *
     * This method iterates through the command properties defined in the
     * environment, collecting topics and their associated command indices. Each
     * topic can be assotiated with multiple command numbers.
     *
     * @return a map where the key is the command topic and the value is a list
     * of command numbers assotiated with that topic.
     *
     * @throw IllegalArgumentException if a command topic is not defined for a
     * command.
     */
    @Bean
    public Map<String, List<String>> topicsAndCommands() {
        Map<String, List<String>> map = new HashMap<>();
        int i = 0;
        logger.info("Starting to collect topics and their corresponding command numbers.");
        while (true) {
            String command = env.getProperty(String.format("command[%d].name", i), "");
            if (command == null || command.isEmpty()) {
                break;
            }

            String commandTopic = env.getProperty(String.format("command[%d].subscription.topic", i), "");
            if (commandTopic == null || commandTopic.isEmpty()) {
                throw new IllegalArgumentException("No topic defined for subscription of command=" + i);
            }

            List<String> commandNumbers = map.getOrDefault(commandTopic, new ArrayList<>());
            commandNumbers.add(String.valueOf(i));

            map.put(commandTopic, commandNumbers);
            logger.info("Add topic={} and commands numbers={}.", commandTopic, commandNumbers);
            ++i;
        }

        logger.info("Topics and commands collection completed. Found {} topics.", map.size());

        return map;
    }

    /**
     * Retrives a mapping of command names to their corresponding numbers.
     *
     * This method iterates through the command properties defined in the
     * environment, collecting command names and their associated indexes.
     *
     * @return a map where the key is the command name and the value is the
     * index of the command.
     */
    @Bean
    public Map<String, String> commandsAndNumbers() {
        Map<String, String> map = new HashMap<>();
        int i = 0;
        String commandName;
        logger.info("Starting to collect commands and their corresponding command numbers.");
        while (!(commandName = env.getProperty(String.format("command[%d].name", i), "")).isEmpty()) {
            map.put(commandName, String.valueOf(i));
            logger.info("Add command={} with number={}.", commandName, i);
            ++i;
        }

        logger.info("Commands and numbers collection completed. Found {} commands.", map.size());

        return map;
    }

    /**
     * Retrives a mapping of component topics to their corresponding component
     * numbers.
     *
     * This method iterates through the component properties defined in the
     * environment, collecting topics and their associated component indices.
     * Each topic can be assotiated with multiple component numbers.
     *
     * @return a map where the key is the component topic and the value is a
     * list of component numbers assotiated with that topic.
     *
     * @throw IllegalArgumentException if a component topic is not defined for a
     * component.
     */
    @Bean
    public Map<String, List<String>> topicsAndComponents() {
        Map<String, List<String>> map = new HashMap<>();
        int i = 0;
        logger.info("Starting to collect topics and their corresponding component numbers.");
        while (true) {
            String component = env.getProperty(String.format("component[%d].name", i), "");
            if (component == null || component.isEmpty()) {
                break;
            }

            String componentTopic = env.getProperty(String.format("component[%d].subscription.topic", i), "");
            if (componentTopic == null || componentTopic.isEmpty()) {
                throw new IllegalArgumentException("No topic defined for subscription of the component=" + i);
            }

            List<String> componentNumbers = map.getOrDefault(componentTopic, new ArrayList<>());
            componentNumbers.add(String.valueOf(i));

            map.put(componentTopic, componentNumbers);
            logger.info("Add topic={} and component numbers={}.", componentTopic, componentNumbers);
            ++i;
        }

        logger.info("Topics and components collection completed. Found {} topics.", map.size());

        return map;
    }

    /**
     * Retrives a mapping of component names to their corresponding numbers.
     *
     * This method iterates through the component properties defined in the
     * environment, collecting component names and their associated indexes.
     *
     * @return a map where the key is the component name and the value is the
     * index of the component.
     */
    @Bean
    public Map<String, String> componentsAndNumbers() {
        Map<String, String> map = new HashMap<>();
        int i = 0;
        String componentName;
        logger.info("Starting to collect components and their corresponding component numbers.");
        while (!(componentName = env.getProperty(String.format("component[%d].name", i), "")).isEmpty()) {
            map.put(componentName, String.valueOf(i));
            logger.info("Add component={} with number={}.", componentName, i);
            ++i;
        }

        logger.info("Components and numbers collection completed. Found {} components.", map.size());

        return map;
    }

    @Bean
    public List<Dashboard> dashboards(AppProperties appProperties) {
        int i = 0;
        List<Card> cards = new ArrayList<>();
        List<Dashboard> dashboards = new ArrayList<>();

        String dashboardPathname = env.getProperty("dashboard-template-path", "");
        if (dashboardPathname.isEmpty()) {
            logger.info("No value defined for dashboard template pathname.");
            return dashboards;
        }

        String cardPathname = env.getProperty("card-template-path", "");
        if (cardPathname.isEmpty()) {
            logger.info("No value defined for card template pathname.");
            return dashboards;
        }

        while (!env.getProperty("dashboard[" + i + "].name", "").isEmpty()) {
            List<String> listOfCards = (List<String>) env.getProperty("dashboard[" + i + "].cards", List.class);
            logger.info("Dashboard={} has cards={}", i, listOfCards);
            if (listOfCards == null || listOfCards.isEmpty()) {
                logger.info("No cards defined for dashboard number={}", i);
                return dashboards;
            }

            for (String cardNumber : listOfCards) {
                String cardName = env.getProperty("card[" + cardNumber + "].name", "");
                if (cardName.isEmpty()) {
                    logger.info("No name defined for card={}", cardNumber);
                    continue;
                }
                Card card = new CardImpl(cardNumber, cardName, cardPathname, appProperties);
                cards.add(card);
                logger.info("Card={} has been created and added to card list.", card.getName());
            }

            String dashboardName = env.getProperty("dashboard[" + i + "].name", "");
            if (dashboardName.isEmpty()) {
                logger.info("No name defined for dashboard={}", i);
                continue;
            }

            Dashboard dashboard = new DashboardImpl(String.valueOf(i), dashboardName, cards, dashboardPathname);
            dashboards.add(dashboard);

            logger.info("Dashboard={} has been created and added to dashboard list.", dashboard.getName());
            ++i;
        }

        if (dashboards.isEmpty()) {
            logger.info("Dashboard list is empty.");
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
