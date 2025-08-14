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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class MsgImpl implements Msg {

    private final String id;

    private final String data;

    private final String type;

    private final String timestamp;

    private MsgImpl(MsgBuilder builder) {
        this.id = Objects.requireNonNullElse(builder.id, "");
        this.data = Objects.requireNonNullElse(builder.data, "");
        this.type = Objects.requireNonNullElse(builder.type, "");
        this.timestamp = Objects.requireNonNullElse(builder.timestamp, "");
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getData() {
        return this.data;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public String getTimestamp() {
        return this.timestamp;
    }

    @Override
    public Msg.Builder toBuilder() {
        return newBuilder()
                .id(this.id)
                .data(this.data)
                .type(this.type)
                .timestamp(this.timestamp);
    }

    public static Msg.Builder newBuilder() {
        return new MsgImpl.MsgBuilder();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MsgImpl{")
                .append("id=").append(this.id)
                .append(", type=").append(this.type)
                .append(", timestamp=").append(this.timestamp)
                .append(", data=");
        if (this.data.length() > MAX_CHAR_TO_PRINT) {
            sb.append(this.data.substring(0, MAX_CHAR_TO_PRINT));
            sb.append("...");
        } else {
            sb.append(this.data);
        }
        sb.append("}");

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof MsgImpl)) {
            return false;
        }

        MsgImpl that = (MsgImpl) o;

        return this.id.equals(that.id)
                && this.type.equals(that.type)
                && this.timestamp.equals(that.timestamp)
                && this.data.equals(that.data);
    }

    @Override
    public int hashCode() {
        int result = this.id.hashCode();
        result = 31 * result + this.type.hashCode();
        result = 31 * result + this.timestamp.hashCode();
        result = 31 * result + this.data.hashCode();

        return result;
    }

    public static class MsgBuilder implements Msg.Builder {

        @JsonProperty("id")
        protected String id = "";

        @JsonProperty("data")
        protected String data = "";

        @JsonProperty("type")
        protected String type = "";

        @JsonProperty("timestamp")
        protected String timestamp = "";

        public MsgBuilder() {

        }

        public MsgBuilder(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getData() {
            return data;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public String getTimestamp() {
            return timestamp;
        }

        @Override
        public MsgBuilder id(String id) {
            this.id = Objects.requireNonNullElse(id, "");
            return this;
        }

        @Override
        public MsgBuilder data(String data) {
            this.data = Objects.requireNonNullElse(data, "");
            return this;
        }

        @Override
        public MsgBuilder type(String type) {
            this.type = Objects.requireNonNullElse(type, "");
            return this;
        }

        @Override
        public MsgBuilder timestamp(String timestamp) {
            this.timestamp = Objects.requireNonNullElse(timestamp, "");
            return this;
        }

        @Override
        public MsgImpl build() {
            return new MsgImpl(this);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("MsgBuilder{")
                    .append("id=").append(this.id)
                    .append(", type=").append(this.type)
                    .append(", timestamp=").append(this.timestamp)
                    .append(", data=");
            if (this.data.length() > MAX_CHAR_TO_PRINT) {
                sb.append(this.data.substring(0, MAX_CHAR_TO_PRINT));
                sb.append("...");
            } else {
                sb.append(this.data);
            }
            sb.append("}");

            return sb.toString();
        }
    }
}
