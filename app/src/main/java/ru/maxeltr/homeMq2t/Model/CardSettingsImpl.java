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

import java.util.List;
import java.util.Objects;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.slf4j.LoggerFactory;
import ru.maxeltr.homeMq2t.Entity.CardEntity;
import ru.maxeltr.homeMq2t.Entity.DashboardEntity;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class CardSettingsImpl extends ViewModel<CardEntity> {

    private final List<ViewModel<DashboardEntity>> dashboards;

    private final List<String> mediaTypes;

    public CardSettingsImpl(CardEntity cardEntity, String pathname, List<ViewModel<DashboardEntity>> dashboards, List<String> mediaTypes) {
        super(cardEntity, pathname);
        this.dashboards = dashboards;
        this.mediaTypes = mediaTypes;
    }

    @Override
    void configureTemplate(Document document) {
        Element el = document.getElementById("settingsCard-number");
        if (el != null) {
            el.attr("value", this.getNumber());
        }

        el = document.getElementById("settingsCard-id");
        if (el != null) {
            el.attr("value", String.valueOf(this.getEntity().getId()));
        }

        el = document.getElementById("settingsCard-dashboardNumber");
        if (el != null) {
            el.attr("value", String.valueOf(this.getEntity().getDashboard().getNumber()));
        }

        el = document.getElementById("settingsCard-name");
        if (el != null) {
            el.attr("value", Objects.requireNonNullElse(this.getEntity().getName(), ""));
        }

        el = document.getElementById("settingsCard-subscriptionTopic");
        if (el != null) {
            el.attr("value", Objects.requireNonNullElse(this.getEntity().getSubscriptionTopic(), ""));
        }

        el = document.getElementById("settingsCard-subscriptionQos");
        if (el != null) {
            for (Element option : el.getElementsByTag("option")) {
                if (Objects.requireNonNullElse(this.getEntity().getSubscriptionQos(), "").equals(option.val())) {
                    option.attr("selected", "selected");
                } else {
                    option.removeAttr("selected");
                }
            }
        }

        el = document.getElementById("settingsCard-subscriptionDataName");
        if (el != null) {
            el.attr("value", Objects.requireNonNullElse(this.getEntity().getSubscriptionDataName(), ""));
        }

        el = document.getElementById("settingsCard-subscriptionDataType");
        if (el != null) {
            for (String mediaType : mediaTypes) {
                Element option = new Element(Tag.valueOf("option"), "").attr("value", mediaType).text(mediaType);
                if (Objects.requireNonNullElse(this.getEntity().getSubscriptionDataType(), "").equals(option.val())) {
                    option.attr("selected", "selected");
                }
                el.appendChild(option);
            }
        }

        el = document.getElementById("settingsCard-displayDataJsonPath");
        if (el != null) {
            el.attr("value", Objects.requireNonNullElse(this.getEntity().getDisplayDataJsonpath(), ""));
        }

        el = document.getElementById("settingsCard-publicationTopic");
        if (el != null) {
            el.attr("value", Objects.requireNonNullElse(this.getEntity().getPublicationTopic(), ""));
        }

        el = document.getElementById("settingsCard-publicationQos");
        if (el != null) {
            for (Element option : el.getElementsByTag("option")) {
                if (Objects.requireNonNullElse(this.getEntity().getPublicationQos(), "").equals(option.val())) {
                    option.attr("selected", "selected");
                } else {
                    option.removeAttr("selected");
                }
            }
        }

        el = document.getElementById("settingsCard-publicationRetain");
        if (el != null) {
            for (Element option : el.getElementsByTag("option")) {
                if (String.valueOf(this.getEntity().getPublicationRetain()).equals(option.val())) {
                    option.attr("selected", "selected");
                } else {
                    option.removeAttr("selected");
                }
            }
        }

        el = document.getElementById("settingsCard-publicationData");
        if (el != null) {
            el.attr("value", Objects.requireNonNullElse(this.getEntity().getPublicationData(), ""));
        }

        el = document.getElementById("settingsCard-publicationDataType");
        if (el != null) {
            for (String mediaType : mediaTypes) {
                Element option = new Element(Tag.valueOf("option"), "").attr("value", mediaType).text(mediaType);
                if (Objects.requireNonNullElse(this.getEntity().getPublicationDataType(), "").equals(option.val())) {
                    option.attr("selected", "selected");
                }
                el.appendChild(option);
            }
        }

        el = document.getElementById("settingsCard-localTaskPath");
        if (el != null) {
            el.attr("value", Objects.requireNonNullElse(this.getEntity().getLocalTaskPath(), ""));
        }

        el = document.getElementById("settingsCard-localTaskArguments");
        if (el != null) {
            el.attr("value", Objects.requireNonNullElse(this.getEntity().getLocalTaskArguments(), ""));
        }

        el = document.getElementById("settingsCard-localTaskDataType");
        if (el != null) {
            for (String mediaType : mediaTypes) {
                Element option = new Element(Tag.valueOf("option"), "").attr("value", mediaType).text(mediaType);
                if (Objects.requireNonNullElse(this.getEntity().getLocalTaskDataType(), "").equals(option.val())) {
                    option.attr("selected", "selected");
                }
                el.appendChild(option);
            }
        }

        el = document.getElementById("settingsCard-title");
        if (el != null) {
            el.text("Settings for card " + this.getNumber());
            el.attr("id", this.getNumber() + "-title");
        }

        el = document.getElementById("settingsCard-dashboardNumber");
        if (el != null) {
            for (ViewModel dashboard : this.dashboards) {
                Element option = new Element(Tag.valueOf("option"), "").attr("value", String.valueOf(dashboard.getNumber())).text(dashboard.getName());
                if (String.valueOf(this.getEntity().getDashboard().getNumber()).equals(option.val())) {
                    option.attr("selected", "selected");
                }
                el.appendChild(option);
            }
        }
    }

}
