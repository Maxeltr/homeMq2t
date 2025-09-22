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
package ru.maxeltr.homeMq2t.Mqtt;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MqttUtils {

    public static final int MQTT_SUBACK_FAILURE = 0x80;

    private MqttUtils() {

    }

    /**
     * Convert the given qos value from string (AT_MOST_ONCE, AT_LEAST_ONCE or
     * EXACTLY_ONCE) to MqttQos enum instance. If the qos value is invalid, it
     * defaults to qos level 0.
     *
     * @param qosString The qos value as a string. Must not be null.
     * @return The qos level as a MqttQos enum value.
     */
    public static MqttQoS convertToMqttQos(String qosString) {
        MqttQoS qos;
        try {
            qos = MqttQoS.valueOf(qosString);
        } catch (IllegalArgumentException ex) {
            qos = MqttQoS.AT_MOST_ONCE;
        }

        return qos;
    }

    public static List<MqttTopicSubscription> removeCopiesAndSelectMaxQos(List<MqttTopicSubscription> subscriptions) {
        if (subscriptions == null || subscriptions.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, MqttTopicSubscription> merged = new LinkedHashMap<>(subscriptions.size());
        for (MqttTopicSubscription s : subscriptions) {
            if (s == null) {
                continue;
            }
            String topic = s.topicName();
            int qos = s.qualityOfService().value();

            MqttTopicSubscription existing = merged.get(topic);
            if (existing == null) {
                merged.put(topic, s);
            } else {
                if (qos > existing.qualityOfService().value()) {
                    merged.put(topic, s);
                }
            }
        }

        return new ArrayList<>(merged.values());
    }
}
