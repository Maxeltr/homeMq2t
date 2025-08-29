/*
 * The MIT License
 *
 * Copyright 2025 Maxim Eltratov <<Maxim.Eltratov@ya.ru>>.
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import ru.maxeltr.homeMq2t.Model.Msg;

public enum ServiceType {

    UI("ui", ServiceMediatorImpl::display, ServiceMediatorImpl::getCardNumbersByTopic),
    COMMAND("command", ServiceMediatorImpl::execute, ServiceMediatorImpl::getCommandNumbersByTopic),
    COMPONENT("component", ServiceMediatorImpl::process, ServiceMediatorImpl::getComponentNumbersByTopic);

    private final TriConsumer<ServiceMediatorImpl, Msg, String> action;

    private final BiFunction<ServiceMediatorImpl, String, List<String>> numbersProvider;

    private final String name;

    ServiceType(String name, TriConsumer<ServiceMediatorImpl, Msg, String> action, BiFunction<ServiceMediatorImpl, String, List<String>> numbersProvider) {
        this.name = name;
        this.action = action;
        this.numbersProvider = numbersProvider;
    }

    public String getName() {
        return name;
    }

    public static Optional<ServiceType> fromString(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(st -> st.name.equalsIgnoreCase(name)).findFirst();
    }

    public void dispatch(ServiceMediatorImpl serviceMediator, Msg msg, String topic) {
        for (String number : numbersProvider.apply(serviceMediator, topic)) {
            action.accept(serviceMediator, msg, number);
        }
    }

    @FunctionalInterface
    public interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }
}
