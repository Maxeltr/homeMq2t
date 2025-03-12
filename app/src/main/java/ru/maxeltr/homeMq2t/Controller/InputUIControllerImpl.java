/*
 * The MIT License
 *
 * Copyright 2024 Maxim Eltratov <<Maxim.Eltratov@ya.ru>>.
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
package ru.maxeltr.homeMq2t.Controller;

import io.netty.handler.codec.mqtt.MqttQoS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import ru.maxeltr.homeMq2t.Model.Msg;
import ru.maxeltr.homeMq2t.Service.UIService;
import io.netty.handler.codec.mqtt.MqttReasonCodeAndPropertiesVariableHeader;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
@Controller
public class InputUIControllerImpl implements InputUIController {

    private static final Logger logger = LoggerFactory.getLogger(InputUIControllerImpl.class);

    @Autowired
    private UIService uiService;

    @Override
    @MessageMapping("/connect")
    public void connect(Msg.Builder msg) {
        logger.info("Do connect. Msg.Builder was received - {}.", msg);
        uiService.connect();
    }

    @Override
    @MessageMapping("/disconnect")
    public void disconnect(Msg.Builder msg) {
        logger.info("Do disconnect. Msg.Builder was received - {}.", msg);
        uiService.disconnect(MqttReasonCodeAndPropertiesVariableHeader.REASON_CODE_OK);
    }

    @Override
    @MessageMapping("/shutdownApp")
    public void shutdownApp(Msg.Builder msg) {
        logger.info("Do shutdown. Msg.Builder was received - {}.", msg);
        uiService.shutdownApp();
    }

    @MessageMapping("/publish")
    public void publish(Msg.Builder msg) {
        logger.info("Do publish. Msg.Builder was received - {}.", msg);
        uiService.publish(msg);
        uiService.launch(msg);
    }
}
