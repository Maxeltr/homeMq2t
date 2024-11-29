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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.maxeltr.homeMq2t.Config.AppProperties;
import ru.maxeltr.homeMq2t.Service.ServiceMediator;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class HmMq2tImpl implements HmMq2t {

    private static final Logger logger = LoggerFactory.getLogger(HmMq2tImpl.class);

    private EventLoopGroup workerGroup;

    private Channel channel;

    private MqttAckMediator mqttAckMediator;
    private ServiceMediator serviceMediator;

    @Autowired
    private MqttChannelInitializer mqttChannelInitializer;

    @Autowired
    private AppProperties appProperties;

    @Value("${host:127.0.0.1}")
    private String host;

    @Value("${port:1883}")
    private Integer port;

    @Value("${connect-timeout:5000}")
    private Integer connectTimeout;

    @Override
    public Promise<MqttConnAckMessage> connect() {
        workerGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(mqttChannelInitializer);

        Promise<MqttConnAckMessage> authFuture = new DefaultPromise<>(workerGroup.next());
        mqttAckMediator.setConnectFuture(authFuture);

        bootstrap.remoteAddress(host, port);

        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout);
        ChannelFuture future = bootstrap.connect();
        future.addListener((ChannelFutureListener) f -> HmMq2tImpl.this.channel = f.channel());
        logger.info("Connecting to {} via port {}.", host, port);

        return authFuture;

    }

    @Override
    public void disconnect() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Promise<MqttSubAckMessage> subscribe() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Promise<MqttUnsubAckMessage> unsubscribe() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Promise<?> publish() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void setMediator(MqttAckMediator mqttAckMediator) {
        this.mqttAckMediator = mqttAckMediator;
    }

    @Override
    public void setMediator(ServiceMediator serviceMediator) {
        this.serviceMediator = serviceMediator;
    }
}
