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

    @Override
    void configureTemplate(Document document) {
        Element el = document.getElementById("card1");
        if (el != null) {
            el.attr("id", this.getCardNumber());
        }

        el = document.getElementById("editCardSettings");
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
}
