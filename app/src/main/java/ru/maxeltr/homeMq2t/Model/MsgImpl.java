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

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class MsgImpl implements Msg {

    private final String topic;

    private final String payload;

    private final String type;

    private final String timestamp;

    MsgImpl(String topic, String payload, String type, String timestamp) {
        this.topic = topic;
        this.payload = payload;
        this.type = type;
        this.timestamp = timestamp;
    }

//    MsgImpl(Msg.Builder builder) {
//        this.topic = builder.topic;
//        this.payload = builder.payload;
//        this.type = builder.type;
//        this.timestamp = builder.timestamp;
//    }

    @Override
    public String getTopic() {
        return this.topic;
    }

    @Override
    public String getPayload() {
        return this.payload;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public String getTimestamp() {
        return this.timestamp;
    }

}
