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
package ru.maxeltr.homeMq2t.Mqtt;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class MqttChannelInitializer extends ChannelInitializer<SocketChannel> implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(MqttChannelInitializer.class);

    private ApplicationContext appContext;

    private MqttAckMediator mqttAckMediator;

	@Value("${max-bytes-in-message:8092000}")
	private int maxBytesInMessage;

	@Value("${keep-alive-timer:20}")
	private int keepAliveTimer;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast("mqttDecoder", this.createMqttDecoder());
        ch.pipeline().addLast("mqttEncoder", this.createMqttEncoder());
        ch.pipeline().addLast("idleStateHandler", this.createIdleStateHandler());
        //ch.pipeline().addLast("mqttPingHandler", this.createMqttPingHandler());
        ch.pipeline().addLast("mqttConnectHandler", this.createMqttConnectHandler());
        //ch.pipeline().addLast("mqttSubscriptionHandler", this.createMqttSubscriptionHandler());
        ch.pipeline().addLast("mqttPublishHandler", this.createMqttPublishHandler());
//        ch.pipeline().addLast(new LoggingHandler(LogLevel.WARN));
        //ch.pipeline().addLast("exceptionHandler", this.createExceptionHandler());
    }

    @Override
    public void setApplicationContext(ApplicationContext appContext) throws BeansException {
        this.appContext = appContext;
    }

    private MqttDecoder createMqttDecoder() {
        return new MqttDecoder(maxBytesInMessage);
    }

    private MqttEncoder createMqttEncoder() {
        return MqttEncoder.INSTANCE;
    }

    private IdleStateHandler createIdleStateHandler() {
        return new IdleStateHandler(0, keepAliveTimer, 0, TimeUnit.SECONDS);
    }

    private MqttConnectHandler createMqttConnectHandler() {
        var mqttConnectHandler = new MqttConnectHandler();

        AutowireCapableBeanFactory autowireCapableBeanFactory = this.appContext.getAutowireCapableBeanFactory();
        autowireCapableBeanFactory.autowireBean(mqttConnectHandler);
        autowireCapableBeanFactory.initializeBean(mqttConnectHandler, "mqttConnectHandler");

        return mqttConnectHandler;
    }
	
	private MqttPublishHandler createMqttPublishHandler() {
        MqttPublishHandler mqttPublishHandler = new MqttPublishHandler();
		
        AutowireCapableBeanFactory autowireCapableBeanFactory = this.appContext.getAutowireCapableBeanFactory();
        autowireCapableBeanFactory.autowireBean(mqttPublishHandler);
        autowireCapableBeanFactory.initializeBean(mqttPublishHandler, "mqttPublishHandler");

        return mqttPublishHandler;
    }
}
