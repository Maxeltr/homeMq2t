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

import java.util.List;
import ru.maxeltr.homeMq2t.Entity.StartupTaskEntity;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public interface StartupTaskPropertiesProvider {

    public List<StartupTaskEntity> getAllStartupTasks();

    /**
     * Retrieves the startup task number associated with the specified name.
     *
     * @param name the name associated with the startup task
     * @return the startup task number if found, or an empty string.
     */
    public String getStartupTaskNumber(String name);

    /**
     * Retrieves the startup task arguments associated with the specified
     * startup task.
     *
     * @param name the name associated with the startup task
     * @return the startup task arguments if found, or an empty string.
     */
    public String getStartupTaskArguments(String name);

    /**
     * Retrieves the startup task path associated with the specified startup
     * task.
     *
     * @param name the name associated with the startup task
     * @return the startup task path if found, or an empty string.
     */
    public String getStartupTaskPath(String name);

}
