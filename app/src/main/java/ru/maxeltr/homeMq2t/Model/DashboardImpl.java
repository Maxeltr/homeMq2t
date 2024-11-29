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

import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class DashboardImpl implements Dashboard {

    @Autowired
    private Environment env;

    private List<Card> dashboardCards;

    private String name = "";

    public DashboardImpl(String name, List<Card> dashboardCards) {
        this.name = name;
        this.dashboardCards = dashboardCards;
    }

    public String getName() {
        return this.name;
    }

    public List<Card> getDashboardCards() {
        return this.dashboardCards;
    }

    public String getHtml() {
        StringBuilder builder = new StringBuilder();
        builder.append("<div class=\"row row-cols-1 row-cols-sm-2 row-cols-md-3 g-3\" id=\"cards\">");
        int i = 1;
        for (Card card : this.dashboardCards) {
            builder.append(card.getHtml());
            ++i;
        }
        builder.append("</div>");

        return builder.toString();
    }

    @Override
    public String getCards() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
