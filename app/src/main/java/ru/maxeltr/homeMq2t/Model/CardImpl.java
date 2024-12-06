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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import ru.maxeltr.homeMq2t.Service.UIServiceImpl;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class CardImpl implements Card {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(CardImpl.class);
    
	private String pathname = File.separator + "Static" + File.separator + "card.html";

    private String name = "";

    private String text = "";
	
	private Document view;

    public CardImpl(String name) {
        this.name = name;
		this.view = this.getViewTemplate();
    }

    public CardImpl(String name, String text) {
        this.name = name;
        this.text = text;
		this.view = this.getViewTemplate();
    }

    public String getName() {
        return this.name;
    }

    public String getText() {
        return this.text;
    }

    @Override
    public String getHtml() {
		return this.view.body().html();
    }

	private Document getViewTemplate() {
		Document document;
        try {
            document = this.getTemplateFromResource();
        } catch (IOException ex) {
            logger.error("Cannot get card template.", ex);
            return Jsoup.parse("<div><h3>Error</h3><h5>Cannot get card view template.</h5></div>");
        }
        this.configureTemplate(document);
		
		return document;
	}
	
    private void configureTemplate(Document document) {
        Element el = document.getElementById("card1");
        //el.removeAttr("id");
        if (el != null) el.attr("id", this.getName());

        el = document.getElementById("card1-payload");
        if (el != null) el.attr("id", this.getName() + "-payload");

        el = document.getElementById("card1-save");
        if (el != null) el.attr("id", this.getName() + "-save");

        el = document.getElementById("card1-timestamp");
        if (el != null) el.attr("id", this.getName() + "-timestamp");

		el = document.getElementById("sendCommand");
		if (el != null) el.attr("value", this.getName());
		
        el = document.select(".card-title").first();
        if (el != null) el.text(this.getName());
    }

    private Document getTemplateFromResource() throws IOException {
        InputStream is = DashboardImpl.class.getClassLoader().getResourceAsStream(pathname);
        return Jsoup.parse(is, "utf-8", "");
    }
}
