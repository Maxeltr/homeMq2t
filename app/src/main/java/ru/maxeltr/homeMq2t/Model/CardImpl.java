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

import java.util.Objects;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.maxeltr.homeMq2t.Entity.CardEntity;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class CardImpl extends CardModel {

    private static final Logger logger = LoggerFactory.getLogger(CardModel.class);

    public CardImpl(CardEntity cardEntity, String pathname) {
        super(cardEntity, pathname);
    }

//    static final int MAX_CHAR_TO_PRINT = 256;
//
//    private String pathname;
//
//    private String cardNumber = "";
//
//    private String name = "";
//
//    private final Document view;
//
//    private String cardSubDataName;
//    public CardImpl(String cardNumber, String name, String pathname, String cardSubDataName) {
//        this.cardNumber = Objects.requireNonNullElse(cardNumber, "");
//        this.name = Objects.requireNonNullElse(name, "");
//        this.pathname = pathname;
//        this.cardSubDataName = Objects.requireNonNullElse(cardSubDataName, "");
//
//        this.view = this.getViewTemplate();
//    }
//    private Document getViewTemplate() {
//        Document document;
//
//        document = this.getTemplateFromFile()
//                .orElse(Jsoup.parse("<div style=\"color:red;\"><h3>Error</h3><h5>Cannot get card view template.</h5></div>"));
//        this.configureTemplate(document);
//
//        return document;
//    }

    @Override
    void configureTemplate(Document document) {
        Element el = document.getElementById("card1");
        if (el != null) {
            el.attr("id", this.getCardNumber());
        }

        el = document.getElementById("editSettings");
        if (el != null) {
            el.attr("value", this.getCardNumber());
        }

        el = document.getElementById("card1-payload");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-payload");
        }

        el = document.getElementById("card1-save");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-save");
        }

        el = document.getElementById("card1-timestamp");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-timestamp");
        }

        el = document.getElementById("sendCommand");
        if (el != null) {
            el.attr("value", this.getCardNumber());
        }

        el = document.getElementById("card1-save");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-save");
        }

        el = document.getElementById("card1-text");
        if (el != null) {
            el.text(Objects.requireNonNullElse(this.getCardEntity().getSubscriptionDataName(), ""));
            el.attr("id", this.getCardNumber() + "-text");
        }

        el = document.getElementById("card1-status");
        if (el != null) {
            el.attr("id", this.getCardNumber() + "-status");
        }

        el = document.select(".card-title").first();
        if (el != null) {
            el.text(this.getName());
        }
    }

//    private Optional<Document> getTemplateFromFile() {
//        String path = System.getProperty("user.dir") + this.getPathname();
//        logger.info("Load template from={}", path);
//        Document doc = null;
//        File initialFile = new File(path);
//
//        if (!initialFile.exists()) {
//            logger.error("Card template file not found: {}", path);
//            return Optional.empty();
//        }
//
//        try (InputStream is = new FileInputStream(initialFile)) {
//            doc = Jsoup.parse(is, "utf-8", "");
//        } catch (IOException ex) {
//            logger.error("Error reading or parsing card template.", ex);
//        }
//
//        return Optional.ofNullable(doc);
//    }

//    private Document getTemplateFromResource() throws IOException {
//        //InputStream is = DashboardImpl.class.getClassLoader().getResourceAsStream(pathname);
//        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(pathname);
//        return Jsoup.parse(is, "utf-8", "");
//    }
//    @Override
//    public String toString() {
//        StringBuilder sb = new StringBuilder();
//        sb.append("CardImpl{")
//                .append("name=").append(this.getName())
//                .append(", view=");
//        String strView = this.getView().toString();
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
