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
import java.util.ServiceLoader;
import org.apache.commons.lang3.StringUtils;
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
import ru.maxeltr.homeMq2t.Repository.CardRepository;
import ru.maxeltr.homeMq2t.Service.CommandService;
import ru.maxeltr.homeMq2t.Service.CommandServiceImpl;
import ru.maxeltr.homeMq2t.Service.ComponentServiceImpl;
import ru.maxeltr.homeMq2t.Service.ComponentService;
import ru.maxeltr.homeMq2t.Service.ServiceMediator;
import ru.maxeltr.homeMq2t.Service.ServiceMediatorImpl;
import ru.maxeltr.homeMq2t.Service.UIService;
import ru.maxeltr.homeMq2t.Service.UIServiceImpl;
//import ru.maxeltr.homeMq2t.Service.Mq2tCallbackComponent;
import ru.maxeltr.mq2tLib.Mq2tComponent;
import ru.maxeltr.homeMq2t.Repository.DashboardRepository;

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

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Bean
    public AppProperties getAppProperty() {
        logger.info("Current user dir={}", System.getProperty("user.dir"));
        String[] classpathes = System.getProperty("java.class.path").split(File.pathSeparator);
        for (String classpath : classpathes) {
            logger.info("Classpath={}", classpath);
        }
        logger.info("Java home={}", System.getenv("JAVA_HOME"));
        logger.info("OS name={}", System.getProperty("os.name"));

        logger.info("cardRepository={}", cardRepository.findByNumber(1).get());
        logger.info("dashboardRepository={}", dashboardRepository.findByNumber(0).get());

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

//    @Bean
//    public List<Mq2tComponent> mq2tComponents() {
//        List<Mq2tComponent> providers = new ArrayList<>();
//        logger.info("Starting to collect components implement Mq2tComponent.");
//        ServiceLoader<Mq2tComponent> loader = ServiceLoader.load(Mq2tComponent.class);
//        Iterator<Mq2tComponent> iterator = loader.iterator();
//        while (iterator.hasNext()) {
//            Mq2tComponent provider = iterator.next();
//            logger.info("Add {} as {} provider.", provider.getClass().getName(), Mq2tComponent.class.getName());
//            providers.add(provider);
//        }
//
//        return providers;
//    }

    @Bean
    public ComponentService getComponentService() {
        List<Mq2tComponent> providers = new ArrayList<>();
        logger.info("Starting to collect components implement Mq2tComponent.");
        ServiceLoader<Mq2tComponent> loader = ServiceLoader.load(Mq2tComponent.class);
        for (Mq2tComponent provider : loader) {
            logger.info("Add {} as {} provider.", provider.getClass().getName(), Mq2tComponent.class.getName());
            providers.add(provider);
        }

        return new ComponentServiceImpl(providers);
    }

    /**
     * Retrives a mapping of card topics to their corresponding card numbers.
     *
     * This method iterates through the card properties defined in the
     * environment, collecting topics and their associated card indices. Each
     * topic can be assotiated with multiple card numbers.
     *
     * This method is linked to the cardsAndNumbers() method through the
     * indexing of cards, meaning that the index used here corresponds to the
     * same card in the cardsAndNumbers() method. Refactoring of these methods
     * should be done together to maintain consistency.
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
            if (StringUtils.isEmpty(card)) {
                break;
            }

            String cardTopic = env.getProperty(String.format("card[%d].subscription.topic", i), "");
            if (StringUtils.isEmpty(cardTopic)) {
                logger.warn("No topic defined for subscription of card={}", i);
                i++;
                continue;
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
     * Retrives a mapping of card names to their corresponding numbers.
     *
     * This method iterates through the card properties defined in the
     * environment, collecting card names and their associated indexes.
     *
     * This method is linked to the topicsAndCards() method through the indexing
     * of cards, meaning that the index used here corresponds to the same card
     * in the topicsAndCards() method. Refactoring of these methods should be
     * done together to maintain consistency.
     *
     * @return a map where the key is the card name and the value is the index
     * of the card.
     */
    @Bean
    public Map<String, String> cardsAndNumbers() {
        Map<String, String> map = new HashMap<>();
        int i = 0;
        String cardName;
        logger.info("Starting to collect cards and their corresponding card numbers.");
        while (StringUtils.isNotEmpty(cardName = env.getProperty(String.format("card[%d].name", i), ""))) {
            map.put(cardName, String.valueOf(i));
            logger.info("Add card={} with number={}.", cardName, i);
            ++i;
        }

        logger.info("Cards and numbers collection completed. Found {} cards.", map.size());

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
     * This method is linked to the commandsAndNumbers() method through the
     * indexing of commands, meaning that the index used here corresponds to the
     * same command in the commandsAndNumbers() method. Refactoring of these
     * methods should be done together to maintain consistency.
     *
     * @return a map where the key is the command topic and the value is a list
     * of command numbers assotiated with that topic.
     */
    @Bean
    public Map<String, List<String>> topicsAndCommands() {
        Map<String, List<String>> map = new HashMap<>();
        int i = 0;
        logger.info("Starting to collect topics and their corresponding command numbers.");
        while (true) {
            String command = env.getProperty(String.format("command[%d].name", i), "");
            if (StringUtils.isEmpty(command)) {
                break;
            }

            String commandTopic = env.getProperty(String.format("command[%d].subscription.topic", i), "");
            if (StringUtils.isEmpty(commandTopic)) {
                logger.warn("No topic defined for subscription of command={}", i);
                i++;
                continue;
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
     * This method is linked to the topicsAndCommands() method through the
     * indexing of commands, meaning that the index used here corresponds to the
     * same command in the topicsAndCommands() method. Refactoring of these
     * methods should be done together to maintain consistency.
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
        while (StringUtils.isNotEmpty(commandName = env.getProperty(String.format("command[%d].name", i), ""))) {
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
     * This method is linked to the componentsAndNumbers() method through the
     * indexing of commands, meaning that the index used here corresponds to the
     * same command in the componentsAndNumbers() method. Refactoring of these
     * methods should be done together to maintain consistency.
     *
     * @return a map where the key is the component topic and the value is a
     * list of component numbers assotiated with that topic.
     */
    @Bean
    public Map<String, List<String>> topicsAndComponents() {
        Map<String, List<String>> map = new HashMap<>();
        int i = 0;
        logger.info("Starting to collect topics and their corresponding component numbers.");
        while (true) {
            String component = env.getProperty(String.format("component[%d].name", i), "");
            if (StringUtils.isEmpty(component)) {
                break;
            }

            String componentTopic = env.getProperty(String.format("component[%d].subscription.topic", i), "");
            if (StringUtils.isEmpty(componentTopic)) {
                logger.warn("No topic defined for subscription of the component={}", i);
                i++;
                continue;
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
     * This method is linked to the topicsAndComponents() method through the
     * indexing of commands, meaning that the index used here corresponds to the
     * same command in the topicsAndComponents() method. Refactoring of these
     * methods should be done together to maintain consistency.
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
        while (StringUtils.isNotEmpty(componentName = env.getProperty(String.format("component[%d].name", i), ""))) {
            map.put(componentName, String.valueOf(i));
            logger.info("Add component={} with number={}.", componentName, i);
            ++i;
        }

        logger.info("Components and numbers collection completed. Found {} components.", map.size());

        return map;
    }

    @Bean(name = "startupTasks")
    public Map<String, String> startupTasksAndNumbers() {
        Map<String, String> map = new HashMap<>();
        int i = 0;
        String taskName;
        logger.info("Starting to collect startup tasks and their corresponding task numbers.");
        while (true) {
            taskName = env.getProperty(String.format("startup.task[%d].name", i), "");
            if (StringUtils.isEmpty(taskName)) {
                break;
            }
            map.put(taskName, String.valueOf(i));
            logger.info("Add startup task={} with number={}.", taskName, i);
            ++i;
        }

        logger.info("Startup tasks and numbers collection completed. Found {} task.", map.size());

        return map;
    }

    /**
     * Creates a list of dashboards based on the configuration properties
     * defined in the environment. Each dashboard can contain multiple cards,
     * which are defined in the configuration.
     *
     * @param appProperties the application properties used for card creation.
     *
     * @return a list of dashboards created from configuration. If no dashboards
     * are defined, an empty list is returned.
     */
    @Bean
    public List<Dashboard> dashboards(AppProperties appProperties) {
        int i = 0;
        List<Dashboard> dashboards = new ArrayList<>();

        String dashboardPathname = env.getProperty("dashboard-template-path", "");
        if (StringUtils.isEmpty(dashboardPathname)) {
            logger.info("No value defined for dashboard template pathname.");
            return dashboards;
        }

        String cardPathname = env.getProperty("card-template-path", "");
        if (StringUtils.isEmpty(cardPathname)) {
            logger.info("No value defined for card template pathname.");
            return dashboards;
        }

        while (StringUtils.isNotEmpty(env.getProperty("dashboard[" + i + "].name", ""))) {
            List<String> listOfCards = (List<String>) env.getProperty("dashboard[" + i + "].cards", List.class);
            logger.info("Dashboard={} has cards={}", i, listOfCards);
            if (listOfCards == null || listOfCards.isEmpty()) {
                logger.info("No cards defined for dashboard number={}", i);
                i++;
                continue;   //Move to the next dashboard
            }

            List<Card> cards = new ArrayList<>();
            for (String cardNumber : listOfCards) {
                String cardName = env.getProperty("card[" + cardNumber + "].name", "");
                if (StringUtils.isEmpty(cardName)) {
                    logger.info("No name defined for card={}", cardNumber);
                    continue;   //Move to the next card
                }
                Card card = new CardImpl(cardNumber, cardName, cardPathname, appProperties);
                cards.add(card);
                logger.info("Card={} has been created and added to card list.", card.getName());
            }

            String dashboardName = env.getProperty("dashboard[" + i + "].name", "");
            if (StringUtils.isEmpty(dashboardName)) {
                logger.info("No name defined for dashboard={}", i);
                i++;
                continue;   //Move to the next dashboard
            }

            Dashboard dashboard = new DashboardImpl(String.valueOf(i), dashboardName, cards, dashboardPathname);
            dashboards.add(dashboard);

            logger.info("Dashboard={} has been created and added to dashboard list.", dashboard.getName());
            i++;
        }

        logger.info("Create dashbord list with size={}.", dashboards.size());

        return dashboards;
    }

    /**
     * Adds Mqtt topic subscriptions for the specified prefix.
     *
     * @param subscriptions the list to which subscriptions will be added.
     * @param prefix the prefix for retrieving subscription properties.
     */
    private void addSubscriptions(List<MqttTopicSubscription> subscriptions, String prefix) {
        int i = 0;
        MqttQoS topicQos;
        while (StringUtils.isNotEmpty(env.getProperty(String.format("%s[%d].name", prefix, i), ""))) {
            String topic = env.getProperty(String.format("%s[%d].subscription.topic", prefix, i), "");
            if (StringUtils.isEmpty(topic)) {
                logger.warn("Topic for {} subscription={} is not defined", prefix, i);
                i++;
                continue;
            }

            try {
                topicQos = MqttQoS.valueOf(env.getProperty(String.format("%s[%d].subscription.qos", prefix, i), MqttQoS.AT_MOST_ONCE.toString()));
            } catch (IllegalArgumentException ex) {
                logger.error("Invalid QoS value for {} subscription={}: {}. Set QoS=0.", prefix, i, ex.getMessage());
                topicQos = MqttQoS.valueOf(0);
            }

            subscriptions.add(new MqttTopicSubscription(topic, topicQos));
            i++;
        }
    }

    /**
     * Creates a list of Mqtt topic subscriptions.
     *
     * @return a list of Mqtt topic subscriptions.
     */
    @Bean
    public List<MqttTopicSubscription> subscriptions() {
        List<MqttTopicSubscription> subscriptions = new ArrayList<>();

        this.addSubscriptions(subscriptions, "card");
        this.addSubscriptions(subscriptions, "command");
        this.addSubscriptions(subscriptions, "component");

        return subscriptions;
    }

    @Bean
    public ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        return mapper;
    }

    @Bean
    public AppShutdownManager getAppShutdownManager() {
        return new AppShutdownManager();
    }

    @Bean(name = "mq2tTaskScheduler")
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
