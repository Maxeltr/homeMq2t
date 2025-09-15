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

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import ru.maxeltr.homeMq2t.Entity.CommandEntity;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class CommandImpl extends ViewModel<CommandEntity> {

    public CommandImpl(CommandEntity commandEntity, String pathname) {
        super(commandEntity, pathname);
    }

    @Override
    void configureTemplate(Document document) {
        Element el = document.getElementById("command1");
        if (el != null) {
            el.attr("id", this.getNumber());
        }

        el = document.getElementById("editCommandSettings");
        if (el != null) {
            el.attr("value", this.getNumber());
        }

        el = document.getElementById("command1-title");
        if (el != null) {
            el.text(this.getName());
            el.attr("id", this.getNumber() + "-title");
        }

        el = document.getElementById("command1-subscriptionTopic");
        if (el != null) {
            el.text(this.getEntity().getSubscriptionTopic());
            el.attr("id", this.getNumber() + "-subscriptionTopic");
        }

        el = document.getElementById("command1-publicationTopic");
        if (el != null) {
            el.text(this.getEntity().getPublicationTopic());
            el.attr("id", this.getNumber() + "-publicationTopic");
        }

    }
}
