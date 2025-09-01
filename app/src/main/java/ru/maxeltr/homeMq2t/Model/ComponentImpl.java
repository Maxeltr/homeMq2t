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
import ru.maxeltr.homeMq2t.Entity.ComponentEntity;

public class ComponentImpl extends ViewModel<ComponentEntity> {

    public ComponentImpl(ComponentEntity componentEntity, String pathname) {
        super(componentEntity, pathname);
    }

    @Override
    void configureTemplate(Document document) {
        Element el = document.getElementById("component1");
        if (el != null) {
            el.attr("id", this.getNumber());
        }

        el = document.getElementById("editComponentSettings");
        if (el != null) {
            el.attr("value", this.getNumber());
        }

        el = document.getElementById("component1-payload");
        if (el != null) {
            el.attr("id", this.getNumber() + "-payload");
        }

        el = document.getElementById("component1-timestamp");
        if (el != null) {
            el.attr("id", this.getNumber() + "-timestamp");
        }

        el = document.getElementById("component1-status");
        if (el != null) {
            el.attr("id", this.getNumber() + "-status");
        }

        el = document.getElementById("component1-title");
        if (el != null) {
            el.text(this.getName());
            el.attr("id", this.getNumber() + "-title");
        }

    }

}
