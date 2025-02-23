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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.ClassUtils;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class ComponentLoader {

    private static final Logger logger = LoggerFactory.getLogger(ComponentLoader.class);

    public List<Object> loadClassesFromJar(String path) {
        List<Object> components = new ArrayList<>();
        if (path.trim().isEmpty()) {
            logger.warn("Component path is empty.");
            return components;
        }
        Set<Class> classes = getClassesFromJarFile(new File(path));

        for (Class clazz : classes) {
            if (clazz.isInterface()) {
                logger.info("Found interface={} in jar={}", clazz, path);
                continue;
            }

            logger.info("Found class={} in jar={}", clazz, path);
            for (Class clazzInterface : ClassUtils.getAllInterfaces(clazz)) {
                logger.info("Class={} implements interface={}", clazz, clazzInterface);
                if (clazzInterface.getSimpleName().equals(Mq2tComponent.class.getSimpleName())) {
                    logger.info("Class to instantiate={}", clazz);
                    this.instantiateClass(clazz).ifPresent(instance -> components.add(instance));
                }
            }
        }

        if (components.isEmpty()) {
            logger.warn("No valid classes found in jar={}", path);
        } else {
            logger.info("Successfully instantiated {} classes from jar={}", components.size(), path);
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
            logger.warn("Cannot get classes from jar={}. {}", givenFile, ex.getMessage());
        }

        return classNames;
    }

    private Set<Class> getClassesFromJarFile(File jarFile) {
        Set<String> classNames = getClassNamesFromJarFile(jarFile);
        Set<Class> classes = new HashSet<>(classNames.size());
        try (URLClassLoader cl = URLClassLoader.newInstance(
                new URL[]{new URL("jar:file:" + jarFile + "!/")})) {
            for (String name : classNames) {
                classes.add(cl.loadClass(name));
                logger.debug("Loads class {} from jar {}", name, jarFile);
            }
        } catch (IOException | ClassNotFoundException | NoClassDefFoundError ex) {
            logger.warn("Cannot load class from jar={}.", jarFile, ex);
        }

        return classes;
    }

    private Optional<?> instantiateClass(Class<?> clazz) {
        Object result = null;
        try {
            Constructor<?> constructor = clazz.getConstructor();
            result = constructor.newInstance();
            logger.debug("{} has been instantiated.", clazz);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            logger.warn("Can not instantiate class={}.", clazz, ex.getMessage());
        }

        return Optional.ofNullable(result);
    }

}
