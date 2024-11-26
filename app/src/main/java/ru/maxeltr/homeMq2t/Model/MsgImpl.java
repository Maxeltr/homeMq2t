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

import io.netty.util.internal.StringUtil;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class MsgImpl implements Msg {

    private final String topic;

    private final String payload;

    private final String type;

    private final String timestamp;

    public static clacc Builder {
		
		private final String id;
		
		private String topic = "";

        private String payload = "";

        private String type = "";

        private String timestamp = "";
		
		public Builder (String id) {
		    this.id = id;	
		}
		
		public Builder (String id, String topic, String payload, String type, String timestamp) {
		    this.id = id;
			this.topic = topic;
			this.payload = payload;
			this.type = type;
			this.timestamp = timestamp;
		}
		
		public Builder topic(String topic) {
			this.topic = topic;
			return this;
		}
		
		public Builder payload(String payload) {
			this.payload = payload;
			return this;
		}
		
		public Builder type(String type) {
			this.type = type;
			return this;
		}
		
		public Builder timestamp(String timestamp) {
		    this.timestamp = timestamp;
			return this;
		}
		
		public MsgImpl build() {
			return new MsgImpl(this);
		}
	}
	
	private MsgImpl (Builder builder) {
		topic = builder.topic;
		payload = builder.payload;
		type = builder.type;
		timestamp = builder.timestamp;
	}
	
    @Override
    public String getTopic() {
        return id;
    }

    @Override
    public String getPayload() {
        return name;
    }

    @Override
    public String getType() {
        return arguments;
    }

    @Override
    public String getTimestamp() {
        return timestamp;
    }

}
