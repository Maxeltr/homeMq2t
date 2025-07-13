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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import ru.maxeltr.homeMq2t.Config.AppProperties;
import ru.maxeltr.homeMq2t.Entity.CardEntity;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class CardSettingsImpl {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(CardSettingsImpl.class);

    static final int MAX_CHAR_TO_PRINT = 256;

    private final String pathname;

    private final Document view;

    private final CardEntity cardEntity;

    private String cardNumber = "";

    public CardSettingsImpl(String cardNumber, String pathname, CardEntity cardEntity) {
        this.cardNumber = Objects.requireNonNull(cardNumber);
        this.cardEntity = cardEntity;
        this.pathname = pathname;

        this.view = this.getViewTemplate();
    }

    public String getHtml() {
        return this.view.body().html();
    }

    private Document getViewTemplate() {
        Document document;

        document = this.getTemplateFromFile()
                .orElse(Jsoup.parse("<div style=\"color:red;\"><h3>Error</h3><h5>Cannot get card view template.</h5></div>"));
        this.configureTemplate(document);

        return document;
    }

    public String getCardNumber() {
        return this.cardNumber;
    }

    private void configureTemplate(Document document) {
        Element el = document.getElementById("settingsCard");
        if (el != null) {
            el.attr("id", this.getCardNumber());
        }

        el = document.getElementById("settingsCard-name");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-name");
        }

        el = document.getElementById("settingsCard-subscriptionTopic");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-subscriptionTopic");
        }

        el = document.getElementById("settingsCard-subscriptionQos");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-subscriptionQos");
        }

        el = document.getElementById("settingsCard-subscriptionDataName");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-subscriptionDataName");
        }

        el = document.getElementById("settingsCard-subscriptionDataType");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-subscriptionDataType");
        }

        el = document.getElementById("settingsCard-displayDataJsonPath");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-displayDataJsonPath");
        }

        el = document.getElementById("settingsCard-publicationTopic");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-publicationTopic");
        }

        el = document.getElementById("settingsCard-publicationQos");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-publicationQos");
        }

        el = document.getElementById("settingsCard-publicationRetain");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-publicationRetain");
        }

        el = document.getElementById("settingsCard-publicationData");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-publicationData");
        }

        el = document.getElementById("settingsCard-publicationDataType");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-publicationDataType");
        }

        el = document.getElementById("settingsCard-localTaskPath");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-localTaskPath");
        }

        el = document.getElementById("settingsCard-localTaskArguments");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-localTaskArguments");
        }

        el = document.getElementById("settingsCard-localTaskDataType");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-localTaskDataType");
        }



        el = document.select(".card-title").first();
        if (el != null) {
            el.text("Settings for card " + this.getCardNumber());
        }
    }

    private Optional<Document> getTemplateFromFile() {
        String path = System.getProperty("user.dir") + pathname;
        logger.info("Load template from={}", path);
        Document doc = null;
        File initialFile = new File(path);

        if (!initialFile.exists()) {
            logger.error("Card settings template file not found: {}", path);
            return Optional.empty();
        }

        try (InputStream is = new FileInputStream(initialFile)) {
            doc = Jsoup.parse(is, "utf-8", "");
        } catch (IOException ex) {
            logger.error("Error reading or parsing card settings template file.", ex);
        }

        return Optional.ofNullable(doc);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(CardSettingsImpl.class.getCanonicalName())
                .append("name=").append("Settings of card number=").append(this.getCardNumber())
                .append(", view=");
        String strView = this.view.toString();
        if (strView.length() > MAX_CHAR_TO_PRINT) {
            sb.append(strView.substring(0, MAX_CHAR_TO_PRINT));
            sb.append("...");
        } else {
            sb.append(strView);
        }
        sb.append("}");

        return sb.toString();
    }
}
