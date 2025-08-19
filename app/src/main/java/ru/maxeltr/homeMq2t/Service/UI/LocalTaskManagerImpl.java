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
package ru.maxeltr.homeMq2t.Service.UI;

import java.time.Instant;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import ru.maxeltr.homeMq2t.Config.CardPropertiesProvider;
import ru.maxeltr.homeMq2t.Config.UIPropertiesProvider;
import ru.maxeltr.homeMq2t.Model.Msg;
import ru.maxeltr.homeMq2t.Model.MsgImpl;
import ru.maxeltr.homeMq2t.Service.ServiceMediator;

public class LocalTaskManagerImpl implements LocalTaskManager {

    private static final Logger logger = LoggerFactory.getLogger(LocalTaskManagerImpl.class);

    @Autowired
    @Lazy               //TODO
    private ServiceMediator mediator;

    @Autowired
    @Qualifier("getCardPropertiesProvider")
    private CardPropertiesProvider appProperties;

    @Override
    public Msg run(Msg msg) {
        String data = "";

        var builder = msg.toBuilder()
                .data(data)
                .timestamp(String.valueOf(Instant.now().toEpochMilli()))
                .type(this.appProperties.getCardLocalTaskDataType(msg.getId()));

        String path = this.appProperties.getCardLocalTaskPath(msg.getId());
        if (StringUtils.isEmpty(path)) {
            logger.info("There is no local task to launch for msg={}.", msg.getId());
            return builder.build();
        }

        String arguments = this.appProperties.getCardLocalTaskArguments(msg.getId());
        logger.info("Launch local task for msg={}. commandPath={}, arguments={}.", msg.getId(), path, arguments);
        data = this.mediator.execute(path, arguments);
        if (data != null) {
            builder.data(data);
        }

        return builder.build();
    }
}
