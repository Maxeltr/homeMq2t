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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Objects;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
@JsonDeserialize(as = Msg.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface Msg {

    static final int MAX_CHAR_TO_PRINT = 256;

    public String getType();

    public String getTopic();

    public String getPayload();

    public String getTimestamp();

    public static class Builder {

        @JsonProperty("id")
        protected String id = "";

        @JsonProperty("topic")
        protected String topic = "";

        @JsonProperty("payload")
        protected String payload = "";

        @JsonProperty("type")
        protected String type = "";

        @JsonProperty("timestamp")
        protected String timestamp = "";

        public Builder() {
            
        }
        
        public Builder(String id) {
            this.id = id;
        }

        public Builder topic(String topic) {
            this.topic = Objects.requireNonNullElse(topic, "");
            return this;
        }

        public Builder payload(String payload) {
            this.payload = Objects.requireNonNullElse(payload, "");
            return this;
        }

        public Builder type(String type) {
            this.type = Objects.requireNonNullElse(type, "");
            return this;
        }

        public Builder timestamp(String timestamp) {
            this.timestamp = Objects.requireNonNullElse(timestamp, "");
            return this;
        }

        public MsgImpl build() {
            return new MsgImpl(this.topic, this.payload, this.type, this.timestamp);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Msg.Builder{")
                    .append("id=").append(this.id)
                    .append(", topic=").append(this.topic)
                    .append(", type=").append(this.type)
                    .append(", timestamp=").append(this.timestamp)
                    .append(", payload=");
            if (this.payload.length() > MAX_CHAR_TO_PRINT) {
                sb.append(this.payload.substring(0, MAX_CHAR_TO_PRINT));
                sb.append("...");
            } else {
                sb.append(this.payload);
            }
            sb.append("}");

            return sb.toString();
        }
    }
}
