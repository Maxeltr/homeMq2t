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
package ru.maxeltr.homeMq2t.Model;

import java.util.Objects;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import ru.maxeltr.homeMq2t.Entity.MqttSettingsEntity;

public class MqttSettingsImpl extends ViewModel<MqttSettingsEntity> {

    public MqttSettingsImpl(MqttSettingsEntity mqttSettingsEntity, String pathname) {
        super(mqttSettingsEntity, pathname);
    }

    @Override
    void configureTemplate(Document document) {
        Element el = document.getElementById("settingsMqtt-number");
        if (el != null) {
            el.attr("value", this.getNumber());
        }

        el = document.getElementById("settingsMqtt-id");
        if (el != null) {
            el.attr("value", String.valueOf(this.getEntity().getId()));
        }

        el = document.getElementById("settingsMqtt-name");
        if (el != null) {
            el.attr("value", Objects.requireNonNullElse(this.getEntity().getName(), ""));
        }

        el = document.getElementById("settingsMqtt-host");
        if (el != null) {
            el.attr("value", Objects.requireNonNullElse(this.getEntity().getHost(), ""));
        }

        el = document.getElementById("settingsMqtt-port");
        if (el != null) {
            el.attr("value", Objects.requireNonNullElse(this.getEntity().getPort(), ""));
        }

        el = document.getElementById("settingsMqtt-mq2tPassword");
        if (el != null) {
            el.attr("value", Objects.requireNonNullElse(this.getEntity().getMq2tPassword(), ""));
        }

        el = document.getElementById("settingsMqtt-mq2tUsername");
        if (el != null) {
            el.attr("value", Objects.requireNonNullElse(this.getEntity().getMq2tUsername(), ""));
        }

        el = document.getElementById("settingsMqtt-clientId");
        if (el != null) {
            el.attr("value", Objects.requireNonNullElse(this.getEntity().getClientId(), ""));
        }

        el = document.getElementById("settingsMqtt-hasUsername");
        if (el != null) {
            for (Element option : el.getElementsByTag("option")) {
                if (String.valueOf(this.getEntity().getHasUsername()).equals(option.val())) {
                    option.attr("selected", "selected");
                } else {
                    option.removeAttr("selected");
                }
            }
        }

        el = document.getElementById("settingsMqtt-hasPassword");
        if (el != null) {
            for (Element option : el.getElementsByTag("option")) {
                if (String.valueOf(this.getEntity().getHasPassword()).equals(option.val())) {
                    option.attr("selected", "selected");
                } else {
                    option.removeAttr("selected");
                }
            }
        }

        el = document.getElementById("settingsMqtt-willQos");
        if (el != null) {
            el.attr("value", Objects.requireNonNullElse(this.getEntity().getWillQos(), ""));
        }

        el = document.getElementById("settingsMqtt-willRetain");
        if (el != null) {
            for (Element option : el.getElementsByTag("option")) {
                if (String.valueOf(this.getEntity().getWillRetain()).equals(option.val())) {
                    option.attr("selected", "selected");
                } else {
                    option.removeAttr("selected");
                }
            }
        }

        el = document.getElementById("settingsMqtt-willFlag");
        if (el != null) {
            for (Element option : el.getElementsByTag("option")) {
                if (String.valueOf(this.getEntity().getWillFlag()).equals(option.val())) {
                    option.attr("selected", "selected");
                } else {
                    option.removeAttr("selected");
                }
            }
        }

        el = document.getElementById("settingsMqtt-cleanSession");
        if (el != null) {
            for (Element option : el.getElementsByTag("option")) {
                if (String.valueOf(this.getEntity().getCleanSession()).equals(option.val())) {
                    option.attr("selected", "selected");
                } else {
                    option.removeAttr("selected");
                }
            }
        }

        el = document.getElementById("settingsMqtt-autoConnect");
        if (el != null) {
            for (Element option : el.getElementsByTag("option")) {
                if (String.valueOf(this.getEntity().getAutoConnect()).equals(option.val())) {
                    option.attr("selected", "selected");
                } else {
                    option.removeAttr("selected");
                }
            }
        }

        el = document.getElementById("settingsMqtt-willTopic");
        if (el != null) {
            el.attr("value", Objects.requireNonNullElse(this.getEntity().getWillTopic(), ""));
        }

        el = document.getElementById("settingsMqtt-willMessage");
        if (el != null) {
            el.attr("value", Objects.requireNonNullElse(this.getEntity().getWillMessage(), ""));
        }

        el = document.getElementById("settingsMqtt-reconnect");
        if (el != null) {
            for (Element option : el.getElementsByTag("option")) {
                if (String.valueOf(this.getEntity().getReconnect()).equals(option.val())) {
                    option.attr("selected", "selected");
                } else {
                    option.removeAttr("selected");
                }
            }
        }
    }

}
