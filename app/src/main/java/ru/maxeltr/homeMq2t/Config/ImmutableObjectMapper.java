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
package ru.maxeltr.homeMq2t.Config;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.Module;

public class ImmutableObjectMapper extends ObjectMapper {

    public ImmutableObjectMapper() {
        super();
        init();
    }

    private void init() {
        super.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    @Override
    public ObjectMapper registerModule(Module module) {
        throw new UnsupportedOperationException("Module registration is denied.");
    }

    @Override
    public ObjectMapper registerModules(Module... modules) {
        throw new UnsupportedOperationException("Module registration is denied.");
    }

    @Override
    public ObjectMapper setSerializationInclusion(Include include) {
        throw new UnsupportedOperationException("Setting changing is denied.");
    }

    @Override
    public ObjectMapper configure(SerializationFeature feature, boolean state) {
        throw new UnsupportedOperationException("Setting changing is denied.");
    }

    @Override
    public ObjectMapper configure(DeserializationFeature feature, boolean state) {
        throw new UnsupportedOperationException("Setting changing is denied.");
    }

    @Override
    public ObjectMapper configure(JsonParser.Feature feature, boolean state) {
        throw new UnsupportedOperationException("Setting changing is denied.");
    }

    @Override
    public ObjectMapper configure(JsonGenerator.Feature feature, boolean state) {
        throw new UnsupportedOperationException("Setting changing is denied.");
    }
}
