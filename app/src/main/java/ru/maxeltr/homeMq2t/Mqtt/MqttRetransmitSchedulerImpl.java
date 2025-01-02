/*
 * The MIT License
 *
 * Copyright 2025 Dev.
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

import io.netty.handler.codec.mqtt.MqttMessage;
import java.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

/**
 *
 * @author Dev
 */
public class MqttRetransmitSchedulerImpl implements MqttRetransmitScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(MqttRetransmitSchedulerImpl.class);
    
    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;
    
    @Autowired
    private PeriodicTrigger retransmitPeriodicTrigger;
    
    @Autowired
    private MqttAckMediator ackMediator;
    
    private ScheduledFuture<?> retransmitScheduledFuture;

//    public MqttRetransmitSchedulerImpl(MqttAckMediator ackMediator) {
//        this.ackMediator = ackMediator;
//    }

    @Override
    public void start() {
        if (this.retransmitScheduledFuture == null) {
            logger.info("Start retransmit task");
            this.retransmitScheduledFuture = this.threadPoolTaskScheduler.schedule(new RetransmitTask(), this.retransmitPeriodicTrigger);
        } else {
            logger.warn("Could not start retransmit task. Previous retransmit task was not stopped.");
        }
    }

    @Override
    public void stop() {
        if (this.retransmitScheduledFuture != null && !this.retransmitScheduledFuture.isCancelled()) {
            this.retransmitScheduledFuture.cancel(false);
            this.retransmitScheduledFuture = null;
            logger.info("Retransmit task has been stopped");
        }
    }
    
    class RetransmitTask implements Runnable {
        
        @Override
        public void run() {
            logger.info("Strart retransmission");
            for (MqttMessage message: ackMediator) {
                logger.info("message={}", message.toString());
            }
        }
    }
}
