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
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.maxeltr.homeMq2t.Entity.CardEntity;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public abstract class CardModel {

    private static final Logger logger = LoggerFactory.getLogger(CardModel.class);

    static final int MAX_CHAR_TO_PRINT = 256;

    private final String pathname;

    private final Document view;

    private final CardEntity cardEntity;

    public CardModel(CardEntity cardEntity, String pathname) {
        this.cardEntity = cardEntity;
        this.pathname = pathname;
        this.view = this.getViewTemplate();
    }

    public String getCardNumber() {
        return String.valueOf(cardEntity.getNumber());
    }

    public String getName() {
        return cardEntity.getName();
    }

    public String getHtml() {
        return this.view.body().html();
    }

    protected String getPathname() {
        return pathname;
    }

    protected Document getView() {
        return view;
    }

    protected CardEntity getCardEntity() {
        return cardEntity;
    }

    abstract void configureTemplate(Document document);

    private Document getViewTemplate() {
        Document document;

        document = this.getTemplateFromFile()
                .orElse(Jsoup.parse("<div style=\"color:red;\"><h3>Error</h3><h5>Cannot get card view template.</h5></div>"));
        this.configureTemplate(document);

        return document;
    }

    private Optional<Document> getTemplateFromFile() {
        String path = System.getProperty("user.dir") + this.getPathname();
        logger.info("Load template from={}", path);
        Document doc = null;
        File initialFile = new File(path);

        if (!initialFile.exists()) {
            logger.error("Card template file not found: {}", path);
            return Optional.empty();
        }

        try (InputStream is = new FileInputStream(initialFile)) {
            doc = Jsoup.parse(is, "utf-8", "");
        } catch (IOException ex) {
            logger.error("Error reading or parsing card template.", ex);
        }

        return Optional.ofNullable(doc);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getName())
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
