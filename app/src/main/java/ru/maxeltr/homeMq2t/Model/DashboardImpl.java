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
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import ru.maxeltr.homeMq2t.Service.UIServiceImpl;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.util.ResourceUtils;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
//@Configurable
public class DashboardImpl implements Dashboard {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DashboardImpl.class);

    @Autowired
    private Environment env;

    @Autowired
ResourceLoader resourceLoader;

    //@Value("${dashboard-template-path:dashboard.html}")
    //@Value("classpath:Static/dashboard.html")
    @Value("classpath:dashboard.html")
    private String dashboardPath;

    private List<Card> dashboardCards;

    private String name = "";

    public DashboardImpl(String name, List<Card> dashboardCards) {
        this.name = name;
        this.dashboardCards = dashboardCards;
    }

    public String getName() {
        return this.name;
    }

    public List<Card> getCards() {
        return this.dashboardCards;
    }

    public String getHtml() {
        Document document;
        try {
            document = this.getTemplate();
        } catch (IOException ex) {
            logger.error("Cannot get dashboard template.", ex);
            return "<div><h3>Error</h3><h5>Cannot get dashboard template.</h5></div>";
        }
        this.modifyTemplate(document);

        return document.body().html();
    }

    private void modifyTemplate(Document document) {
        Element el = document.getElementById("cards");
        if (el != null) {
            for (Card card : this.getCards()) {
                el.append(card.getHtml());
            }
        }
    }

    private Document getTemplate() throws IOException {
        String pathname = File.separator + "Static" + File.separator + "dashboard.html";
        InputStream is = DashboardImpl.class.getClassLoader().getResourceAsStream(pathname);
        return Jsoup.parse(is, "utf-8", "");
    }
}
