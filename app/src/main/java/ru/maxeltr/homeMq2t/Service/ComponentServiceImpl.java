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

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ClassUtils;
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

    private Set<Object> components;

    @Override
    public void setMediator(ServiceMediator mediator) {
        this.mediator = mediator;
    }

    @PostConstruct
    public void postConstruct() {
        this.components = this.loadComponents();

        for (Object component : this.components) {
            logger.debug("Loads {}", component);
        }

        //this.future = taskScheduler.schedule(new RunnableTask(), periodicTrigger);
    }

    private Set<Object> loadComponents() {
//        String pathToJar = "c:\\java\\mqttClient\\Components\\app.jar";
        String pathJar = this.appProperties.getComponentPath();
        if (pathJar.trim().isEmpty()) {
            logger.warn("Component path is empty.");
            return new HashSet<>();
        }

        Set<Path> paths = listFiles(pathJar);

        Set<Object> components = new HashSet<>();
        for (Path path : paths) {
            try {
                components = this.loadClassesFromJar(path);
            } catch (IOException | ClassNotFoundException ex) {
                logger.warn("Can not load classes", ex.getMessage());
            }
        }

        return components;
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

    private Set<Object> loadClassesFromJar(Path path) throws IOException, ClassNotFoundException {
        Set<Class> classes = getClassesFromJarFile(path.toFile());

        Set<Class> classesToInstantiate = new HashSet<>();
        Set<Class> classesToSetCallback = new HashSet<>();

        for (Class clazz : classes) {
            logger.debug("Class to instantiate or to set callback {}", clazz.getSimpleName());
            if (clazz.isInterface()) {
                continue;
            }

            for (Class i : ClassUtils.getAllInterfaces(clazz)) {
                if (i.getSimpleName().equals(Component.class.getSimpleName())) {
                    classesToInstantiate.add(clazz);
                    logger.debug("Class to instantiate {}", clazz);
                }
                if (i.getSimpleName().equals(CallbackComponent.class.getSimpleName())) {
                    classesToSetCallback.add(clazz);
                    logger.debug("Class to set callback {}", clazz);
                }
            }
        }

        Set<Object> components = new HashSet<>();
        Object instance;
        for (Class i : classesToInstantiate) {
            try {
                instance = instantiateClass(i);
                logger.debug("Instantiate class{}", i);
                if (classesToSetCallback.contains(i)) {
                    Method method = i.getMethod("setCallback", Consumer.class);
                    method.invoke(instance, (Consumer<String>) (String val) -> {
                        System.out.println("setcallback " + val);   //TODO
                    });
                    logger.debug("Set callback to {}", i);
                }
                components.add(instance);
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                logger.warn("Can not load class.", ex.getMessage());
            }

        }

        return components;
    }

    private Set<String> getClassNamesFromJarFile(File givenFile) throws IOException {
        Set<String> classNames = new HashSet<>();
        try (JarFile jarFile = new JarFile(givenFile)) {
            Enumeration<JarEntry> e = jarFile.entries();
            while (e.hasMoreElements()) {
                JarEntry jarEntry = e.nextElement();
                if (jarEntry.getName().endsWith(".class")) {
                    String className = jarEntry.getName()
                            .replace("/", ".")
                            .replace(".class", "");
                    classNames.add(className);
                }
            }
            return classNames;
        }
    }

    private Set<Class> getClassesFromJarFile(File jarFile) throws IOException, ClassNotFoundException {
        Set<String> classNames = getClassNamesFromJarFile(jarFile);
        Set<Class> classes = new HashSet<>(classNames.size());
        try (URLClassLoader cl = URLClassLoader.newInstance(
                new URL[]{new URL("jar:file:" + jarFile + "!/")})) {
            for (String name : classNames) {
                Class clazz = cl.loadClass(name); // Load the class by its name
                classes.add(clazz);
                logger.debug("Loads class from jar {}", clazz);
            }
        }
        return classes;
    }

    private Object instantiateClass(Class<?> clazz) throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Constructor<?> constructor = clazz.getConstructor();
        Object result = constructor.newInstance();
        logger.info("Instantiate class {}.", clazz);
        return result;
    }

    @Override
    public void update() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void update(Component component) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void update(String component) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
