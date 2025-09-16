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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.LoggerFactory;
import ru.maxeltr.homeMq2t.Entity.DashboardEntity;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class DashboardImpl extends ViewModel<DashboardEntity> implements Dashboard {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DashboardImpl.class);

    static final String CARD_ELEMENT_ID = "dashboard-cards";

    private final List<ViewModel<?>> dashboardCards;

    public <T extends ViewModel<?>> DashboardImpl(DashboardEntity dashboardEntity, List<T> dashboardCards, String pathname) {
        super(dashboardEntity, pathname);
        this.dashboardCards = Objects.requireNonNullElse(new ArrayList<>(dashboardCards), new ArrayList<>());
    }

    @Override
    public List<ViewModel<?>> getItems() {
        return this.dashboardCards;
    }

    @Override
    void configureTemplate(Document document) {
        Element el = document.getElementById(CARD_ELEMENT_ID);
        if (el != null) {
            for (ViewModel card : this.getItems()) {
                el.append(card.getHtml());
            }
            el.attr("data-dashboardName", getName());
        } else {
            logger.warn("Element with id={} not found in the document.", CARD_ELEMENT_ID);
        }
    }
}
