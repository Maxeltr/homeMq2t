/*
 * The MIT License
 *
 * Copyright Maxim Eltratov <<Maxim.Eltratov@ya.ru>>.
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
package ru.maxeltr.homeMq2t.Utils;

import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

public final class AppUtils {

    private AppUtils() {

    }

    /**
     * Attempts to parse the given string to Integer.
     *
     * @param number value the string to parse
     * @return Optional containing the parsed Integer or empty Optional if
     * parsing fails
     */
    public static Optional<Integer> safeParseInt(String number) {
        try {
            return Optional.ofNullable(number).filter(StringUtils::isNotBlank).map(StringUtils::trim).map(Integer::valueOf);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

}
