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
import org.slf4j.LoggerFactory;
import ru.maxeltr.homeMq2t.Entity.CardEntity;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class CardSettingsImpl extends CardModel {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(CardSettingsImpl.class);

    public CardSettingsImpl(CardEntity cardEntity, String pathname) {
        super(cardEntity, pathname);
    }

    @Override
    void configureTemplate(Document document) {
        Element el = document.getElementById("settingsCard");
        if (el != null) {
            el.attr("id", this.getCardNumber());
        }

        el = document.getElementById("settingsCard-name");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-name");
            el.attr("value", Objects.requireNonNullElse(this.getCardEntity().getName(), ""));
        }

        el = document.getElementById("settingsCard-subscriptionTopic");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-subscriptionTopic");
            el.attr("value", Objects.requireNonNullElse(this.getCardEntity().getSubscriptionTopic(), ""));
        }

        el = document.getElementById("settingsCard-subscriptionQos");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-subscriptionQos");
            for (Element option : el.getElementsByTag("opiton")) {
                if (option.val().equals(Objects.requireNonNullElse(this.getCardEntity().getSubscriptionQos(), ""))) {
                    option.attr("selected", "selected");
                } else {
                    option.removeAttr("selected");
                }
            }
        }

        el = document.getElementById("settingsCard-subscriptionDataName");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-subscriptionDataName");
            el.attr("value", Objects.requireNonNullElse(this.getCardEntity().getSubscriptionDataName(), ""));
        }

        el = document.getElementById("settingsCard-subscriptionDataType");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-subscriptionDataType");
            el.attr("value", Objects.requireNonNullElse(this.getCardEntity().getSubscriptionDataType(), ""));
        }

        el = document.getElementById("settingsCard-displayDataJsonPath");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-displayDataJsonPath");
            el.attr("value", Objects.requireNonNullElse(this.getCardEntity().getDisplayDataJsonpath(), ""));
        }

        el = document.getElementById("settingsCard-publicationTopic");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-publicationTopic");
            el.attr("value", Objects.requireNonNullElse(this.getCardEntity().getPublicationTopic(), ""));
        }

        el = document.getElementById("settingsCard-publicationQos");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-publicationQos");
            for (Element option : el.getElementsByTag("opiton")) {
                if (option.val().equals(Objects.requireNonNullElse(this.getCardEntity().getPublicationQos(), ""))) {
                    option.attr("selected", "selected");
                } else {
                    option.removeAttr("selected");
                }
            }
        }

        el = document.getElementById("settingsCard-publicationRetain");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-publicationRetain");
            for (Element option : el.getElementsByTag("opiton")) {
                if (option.val().equals(String.valueOf(this.getCardEntity().getPublicationRetain()))) {
                    option.attr("selected", "selected");
                } else {
                    option.removeAttr("selected");
                }
            }
        }

        el = document.getElementById("settingsCard-publicationData");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-publicationData");
            el.attr("value", Objects.requireNonNullElse(this.getCardEntity().getPublicationData(), ""));
        }

        el = document.getElementById("settingsCard-publicationDataType");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-publicationDataType");
            el.attr("value", Objects.requireNonNullElse(this.getCardEntity().getPublicationDataType(), ""));
        }

        el = document.getElementById("settingsCard-localTaskPath");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-localTaskPath");
            el.attr("value", Objects.requireNonNullElse(this.getCardEntity().getLocalTaskPath(), ""));
        }

        el = document.getElementById("settingsCard-localTaskArguments");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-localTaskArguments");
            el.attr("value", Objects.requireNonNullElse(this.getCardEntity().getLocalTaskArguments(), ""));
        }

        el = document.getElementById("settingsCard-localTaskDataType");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-localTaskDataType");
            el.attr("value", Objects.requireNonNullElse(this.getCardEntity().getLocalTaskDataType(), ""));
        }



        el = document.select(".card-title").first();
        if (el != null) {
            el.text("Settings for card " + this.getCardNumber());
        }
    }

}
