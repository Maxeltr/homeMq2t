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
import java.io.File;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import ru.maxeltr.homeMq2t.AppShutdownManager;
import ru.maxeltr.homeMq2t.Mqtt.HmMq2t;
import ru.maxeltr.homeMq2t.Mqtt.HmMq2tImpl;
import ru.maxeltr.homeMq2t.Mqtt.MqttAckMediator;
import ru.maxeltr.homeMq2t.Mqtt.MqttAckMediatorImpl;
import ru.maxeltr.homeMq2t.Mqtt.MqttChannelInitializer;
import ru.maxeltr.homeMq2t.Repository.CardRepository;
import ru.maxeltr.homeMq2t.Service.Command.CommandService;
import ru.maxeltr.homeMq2t.Service.Command.CommandServiceImpl;
import ru.maxeltr.homeMq2t.Service.ComponentServiceImpl;
import ru.maxeltr.homeMq2t.Service.ComponentService;
import ru.maxeltr.homeMq2t.Service.ServiceMediator;
import ru.maxeltr.homeMq2t.Service.ServiceMediatorImpl;
import ru.maxeltr.homeMq2t.Service.UI.UIService;
import ru.maxeltr.homeMq2t.Service.UI.UIServiceImpl;
//import ru.maxeltr.homeMq2t.Service.Mq2tCallbackComponent;
import ru.maxeltr.mq2tLib.Mq2tComponent;
import ru.maxeltr.homeMq2t.Repository.DashboardRepository;
import ru.maxeltr.homeMq2t.Service.Command.CommandParser;
import ru.maxeltr.homeMq2t.Service.Command.CommandParserImpl;
import ru.maxeltr.homeMq2t.Service.Command.ProcessExecutor;
import ru.maxeltr.homeMq2t.Service.Command.ProcessExecutorImpl;
import ru.maxeltr.homeMq2t.Service.Command.ReplySender;
import ru.maxeltr.homeMq2t.Service.Command.ReplySenderImpl;
import ru.maxeltr.homeMq2t.Service.UI.ConnectManager;
import ru.maxeltr.homeMq2t.Service.UI.ConnectManagerImpl;
import ru.maxeltr.homeMq2t.Service.UI.DashboardItemManager;
import ru.maxeltr.homeMq2t.Service.UI.DashboardItemManagerImpl;
import ru.maxeltr.homeMq2t.Service.UI.HtmlSanitizer;
import ru.maxeltr.homeMq2t.Service.UI.HtmlSanitizerImpl;
import ru.maxeltr.homeMq2t.Service.UI.Base64HtmlJsonFormatterImpl;
import ru.maxeltr.homeMq2t.Service.UI.LocalTaskManager;
import ru.maxeltr.homeMq2t.Service.UI.LocalTaskManagerImpl;
import ru.maxeltr.homeMq2t.Service.UI.PublishManager;
import ru.maxeltr.homeMq2t.Service.UI.PublishManagerImpl;
import ru.maxeltr.homeMq2t.Service.UI.UIJsonFormatter;

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
    @Primary
    public S getAppProperty() {
        logger.info("Current user dir={}", System.getProperty("user.dir"));
        String[] classpathes = System.getProperty("java.class.path").split(File.pathSeparator);
        for (String classpath : classpathes) {
            logger.info("Classpath={}", classpath);
        }
        logger.info("Java home={}", System.getenv("JAVA_HOME"));
        logger.info("Java version={}", System.getProperty("java.version"));
        logger.info("Java vendor={}", System.getProperty("java.vendor"));
        logger.info("Java VM name={}", System.getProperty("java.vm.name"));
        logger.info("OS name={}", System.getProperty("os.name"));
        logger.info("OS architecture={}", System.getProperty("os.arch"));

        Runtime runtime = Runtime.getRuntime();
        logger.info("Available memory={}", runtime.freeMemory());
        logger.info("Total memory={}", runtime.totalMemory());
        logger.info("Max heap size={}", String.valueOf(runtime.maxMemory()));

        File root = new File("/");
        logger.info("Free space on root={}", root.getFreeSpace());

        logger.info("Default locale={}", Locale.getDefault());
        logger.info("Environment variables={}", env);

        Enumeration<NetworkInterface> networkInterfaces;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                logger.info("Network interface: {}, IP adressea: {}", ni.getName(), Collections.list(ni.getInetAddresses()));
            }
        } catch (SocketException ex) {
            logger.info("Could not get network interfaces {}", ex);
        }

        return new S();
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
        List<Mq2tComponent> providers = new ArrayList<>();
        logger.info("Starting to collect components implement Mq2tComponent.");
        ServiceLoader<Mq2tComponent> loader = ServiceLoader.load(Mq2tComponent.class);
        for (Mq2tComponent provider : loader) {
            logger.info("Add {} as {} provider.", provider.getClass().getName(), Mq2tComponent.class.getName());
            providers.add(provider);
        }

        return new ComponentServiceImpl(providers);
    }

    @Bean
    public ObjectMapper getObjectMapper() {
        return new ImmutableObjectMapper();
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

    @Bean
    public ReplySender getReplySender() {
        return new ReplySenderImpl();
    }

    @Bean
    public ProcessExecutor getProcessExecutor() {
        return new ProcessExecutorImpl();
    }

    @Bean
    public CommandParser getCommandParser() {
        return new CommandParserImpl();
    }

    @Bean
    public ConnectManager getConnectManager() {
        return new ConnectManagerImpl();
    }

    @Bean
    public DashboardItemManager getDashboardItemManager() {
        return new DashboardItemManagerImpl();
    }

    @Bean
    public HtmlSanitizer getHtmlSanitizer() {
        return new HtmlSanitizerImpl();
    }

    @Bean
    public UIJsonFormatter getJsonCreator() {
        return new Base64HtmlJsonFormatterImpl();
    }

    @Bean
    public LocalTaskManager getLocalTaskManager() {
        return new LocalTaskManagerImpl();
    }

    @Bean
    public PublishManager getPublishManager() {
        return new PublishManagerImpl();
    }

    @Bean
    public UIPropertiesProvider getUIPropertiesProvider() {
        return getAppProperty();
    }

    @Bean
    public CardPropertiesProvider getCardPropertiesProvider() {
        return getAppProperty();
    }

    @Bean
    public CommandPropertiesProvider getCommandPropertiesProvider() {
        return getAppProperty();
    }

    @Bean
    public ComponentPropertiesProvider getComponentPropertiesProvider() {
        return getAppProperty();
    }

    @Bean
    public StartupTaskPropertiesProvider getStartupTaskPropertiesProvider() {
        return getAppProperty();
    }
}
