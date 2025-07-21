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
package ru.maxeltr.homeMq2t.Model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class DashboardImpl implements Dashboard {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DashboardImpl.class);

//    static final int MAX_CHAR_TO_PRINT = 256;
    private final String pathname;

    private String dashboardNumber = "";

    static final String CARD_ELEMENT_ID = "cards";

    private final List<CardModel> dashboardCards;

    private String name = "";

    private final Document view;

    public DashboardImpl(String dashboardNumber, String name, List<CardModel> dashboardCards, String pathname) {
        this.dashboardNumber = Objects.requireNonNullElse(dashboardNumber, "");
        this.name = Objects.requireNonNullElse(name, "");
        this.dashboardCards = Objects.requireNonNullElse(dashboardCards, new ArrayList<>());
        this.pathname = Objects.requireNonNull(pathname);
        this.view = this.getViewTemplate();

    }

    @Override
    public String getDashboardNumber() {
        return this.dashboardNumber;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public List<CardModel> getCards() {
        return this.dashboardCards;
    }

    @Override
    public String getHtml() {
        return this.view.body().html();
    }

    private Document getViewTemplate() {
        Document document;
        document = this.getTemplateFromFile()
                .orElse(Jsoup.parse("<div style=\"color:red;\"><h3>Error</h3><h5>Cannot get dashboard view template.</h5></div>"));
        this.configureTemplate(document);

        return document;
    }

    private void configureTemplate(Document document) {
        Element el = document.getElementById(CARD_ELEMENT_ID);
        if (el != null) {
            for (CardModel card : this.getCards()) {
                el.append(card.getHtml());
            }
        } else {
            logger.warn("Element with id={} not found in the document.", CARD_ELEMENT_ID);
        }
    }

//    private Document getTemplateFromResource() throws IOException {
//        //InputStream is = DashboardImpl.class.getClassLoader().getResourceAsStream(pathname);
//        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(pathname);
//
//        if (is == null) {
//            logger.error("Can not get dashboard from resource. InputStream is null.");
//        }
//        Document doc = Jsoup.parse(is, "utf-8", "");
//        return doc;
//    }
    private Optional<Document> getTemplateFromFile() {
        String path = System.getProperty("user.dir") + this.pathname;
        logger.info("Load template from={}", path);
        Document doc = null;
        File initialFile = new File(path);

        if (!initialFile.exists()) {
            logger.error("Dashboard template file not found: {}", path);
            return Optional.empty();
        }

        try (InputStream is = new FileInputStream(initialFile)) {
            doc = Jsoup.parse(is, "utf-8", "");
        } catch (IOException ex) {
            logger.error("Error reading or parsing dashboard template.", ex);
        }

        return Optional.ofNullable(doc);
    }

//    @Override
//    public String toString() {
//        StringBuilder sb = new StringBuilder();
//        sb.append(this.getClass().getName())
//                .append("name=").append(this.name)
//                .append(", dashboardCards=");
//        for (CardModel card : this.getCards()) {
//            sb.append(card.toString());
//            sb.append(", ");
//        }
//        sb.append("view=");
//        String strView = this.view.toString();
//        if (strView.length() > MAX_CHAR_TO_PRINT) {
//            sb.append(strView.substring(0, MAX_CHAR_TO_PRINT));
//            sb.append("...");
//        } else {
//            sb.append(strView);
//        }
//        sb.append("}");
//
//        return sb.toString();
//    }
}
