/*
 * The MIT License
 *
 * Copyright 2025 Maxim Eltratov <<Maxim.Eltratov@ya.ru>>..
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
package ru.maxeltr.homeMq2t.Service;

import io.netty.handler.codec.mqtt.MqttQoS;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import ru.maxeltr.homeMq2t.Model.Msg;
import ru.maxeltr.homeMq2t.Config.AppProperties;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class ComponentServiceImpl implements ComponentService {

    private static final Logger logger = LoggerFactory.getLogger(ComponentServiceImpl.class);

    private ServiceMediator mediator;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private ComponentLoader loader;

    @Autowired
    @Qualifier("plugins")
    private List<Object> components;

    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @Autowired
    private PeriodicTrigger pollingPeriodicTrigger;

    private ScheduledFuture<?> pollingScheduledFuture;

    @Override
    public void setMediator(ServiceMediator mediator) {
        this.mediator = mediator;
    }

    @PostConstruct
    public void postConstruct() {
        //this.components = this.loader.loadComponents(this.appProperties.getComponentPath());

         logger.debug("Postconstruc ComponentService = {}", this.components);

        for (Object component : this.components) {
            logger.debug("Loaded {}", component);
            if (component instanceof CallbackComponent) {
                logger.debug("Callback {} ", component);
            }
        }
System.out.println(components);
        this.startPolling();

        //this.future = taskScheduler.schedule(new RunnableTask(), periodicTrigger);
    }

    @Override
    public void process(Msg msg, String componentNumber) {
        logger.debug("Process message. Component number={}, msg={} ", componentNumber, msg);
        if (msg.getType() == MediaType.TEXT_PLAIN_VALUE) {
            if (msg.getData().equalsIgnoreCase("updateAll")) {
                logger.debug("Update readings of all components");
                this.stopPolling();
                this.startPolling();
            }
        }
    }

    public void startPolling() {
        if (this.pollingScheduledFuture == null) {
            logger.info("Start polling components task.");
            this.pollingScheduledFuture = this.threadPoolTaskScheduler.schedule(new PollingTask(), this.pollingPeriodicTrigger);
        } else {
            logger.warn("Could not start polling components task. Previous polling components task was not stopped.");
        }
    }

    public void stopPolling() {
        if (this.pollingScheduledFuture != null && !this.pollingScheduledFuture.isCancelled()) {
            this.pollingScheduledFuture.cancel(false);
            this.pollingScheduledFuture = null;
            logger.info("Polling components task has been stopped");
        }
    }

    class PollingTask implements Runnable {

        @Override
        public void run() {
            logger.debug("Start/resume polling");
            Msg.Builder builder;
            for (Object ob : components) {
                if (ob instanceof Component component) {
                    logger.debug("Component in polling task {}", component);
//                    String data = component.getData();
//                    logger.info("Component={}. Get data={}.", component.getName(), data);
//
//                    builder = new Msg.Builder("onPolling");
//                    builder.data(data);
//                    builder.type(appProperties.getComponentPubDataType(component.getName()));
//                    builder.timestamp(String.valueOf(Instant.now().toEpochMilli()));
//
//                    String topic = appProperties.getComponentPubTopic(component.getName());
//                    MqttQoS qos = MqttQoS.valueOf(appProperties.getComponentPubQos(component.getName()));
//                    boolean retain = Boolean.getBoolean(appProperties.getComponentPubRetain(component.getName()));
//                    publish(builder, topic, qos, retain);
                } else {
                     logger.warn("There is unknown object in components collection. {}.", ob.getClass());
                }
            }

            logger.debug("Pause polling");
        }
    }

    @Async("processExecutor")
    private void publish(Msg.Builder msg, String topic, MqttQoS qos, boolean retain) {
        logger.info("Message passes to publish. Message={}, topic={}, qos={}, retain={}", msg, topic, qos, retain);
        this.mediator.publish(msg.build(), topic, qos, retain);
    }

    /* private Set<Object> loadComponents(String pathJar) {
        if (pathJar.trim().isEmpty()) {
            logger.warn("Component path is empty.");
            return new HashSet<>();
        }

        Set<Path> paths = listFiles(pathJar);

        Set<Object> componentSet = new HashSet<>();
        for (Path path : paths) {
            try {
                componentSet = this.loadClassesFromJar(path);
            } catch (IOException | ClassNotFoundException ex) {
                logger.warn("Can not load classes", ex.getMessage());
            }
        }

        return componentSet;
    }

    private Set listFiles(String dir) {
        Set<Path> pathSet = new HashSet();

        try (Stream<Path> paths = Files.walk(Paths.get(dir))) {
            pathSet = paths
                    .filter(Files::isRegularFile)
                    .peek((file) -> {
                        logger.info("There is {} in component directory {}.", file.getFileName(), dir);
                    })
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            logger.info("Error to list files in component directory {}. {}", dir, e.getMessage());
        }

        return pathSet;
    }

    private Set<Object> loadClassesFromJar(Path path) {
        Set<Class> classes = getClassesFromJarFile(path.toFile());
		Set<Object> components = new HashSet<>();
        for (Class clazz : classes) {
            logger.debug("There is {} in {}", clazz, path);
            if (clazz.isInterface()) {
                continue;
            }

            for (Class i : ClassUtils.getAllInterfaces(clazz)) {
				if (i.getSimpleName().equals(Component.class.getSimpleName())) {
					logger.debug("Class to instantiate {}", clazz);
					this.instantiateClass(i).ifPresent(instance -> components.add(instance));
				}
            }
        }

        return components;
    }

    private Set<String> getClassNamesFromJarFile(File givenFile) {
        Set<String> classNames = new HashSet<>();
        try (JarFile jarFile = new JarFile(givenFile)) {
            Enumeration<JarEntry> e = jarFile.entries();
            while (e.hasMoreElements()) {
                JarEntry jarEntry = e.nextElement();
                if (jarEntry.getName().endsWith(".class")) {
                    String className = jarEntry.getName().replace("/", ".").replace(".class", "");
                    classNames.add(className);
                }
            }
        } catch (IOException ex) {
			logger.warn("Cannot get classes from jar={}.", givenFile, ex.getMessage());
		}

		return classNames;
    }

    private Set<Class> getClassesFromJarFile(File jarFile) {
        Set<String> classNames = getClassNamesFromJarFile(jarFile);
        Set<Class> classes = new HashSet<>(classNames.size());
        try (URLClassLoader cl = URLClassLoader.newInstance(
                new URL[]{new URL("jar:file:" + jarFile + "!/")})
		) {
            for (String name : classNames) {
                classes.add(cl.loadClass(name));
                logger.debug("Loads class {} from jar {}", name, jarFile);
            }
        } catch (IOException | ClassNotFoundException ex) {
			logger.warn("Cannot load class={} from jar={}.", name, jarFile, ex.getMessage());
		}

        return classes;
    }

    private Optional<?> instantiateClass(Class<?> clazz) {
        Object result;
		try {
			Constructor<?> constructor = clazz.getConstructor();
			result = constructor.newInstance();
			logger.debug("{} has been instantiated.", clazz);
		} catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
			logger.warn("Can not instantiate class={}.", clazz, ex.getMessage());
		}

		return Optional.ofNullable(result);
    } */
}
