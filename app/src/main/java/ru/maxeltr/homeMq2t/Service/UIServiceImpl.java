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
package ru.maxeltr.homeMq2t.Service;

import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.util.concurrent.Promise;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import ru.maxeltr.homeMq2t.Controller.OutputUIController;
import ru.maxeltr.homeMq2t.Model.Dashboard;
import ru.maxeltr.homeMq2t.Model.Msg;
import ru.maxeltr.homeMq2t.Model.MsgImpl;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public class UIServiceImpl implements UIService {

    private static final Logger logger = LoggerFactory.getLogger(UIServiceImpl.class);

    private ServiceMediator mediator;

    @Autowired
    private Environment env;

    @Autowired
    private OutputUIController uiController;

    @Autowired
    private List<Dashboard> dashboards;

    @Override
    public void setMediator(ServiceMediator mediator) {
        this.mediator = mediator;
    }

    @Override
    public void connect() {
        logger.info("Do connect.");
        Promise<MqttConnAckMessage> authFuture = mediator.connect();

        authFuture.awaitUninterruptibly();
        if (authFuture.isCancelled()) {
            logger.info("Connection attempt cancelled.");

            Msg msg = new Msg.Builder("")
                    .type("application/json")
                    .payload("{'name': \"connect\", 'status': \"fail\", 'data': \"<div>Connection attempt cancelled.</div>\"}")
                    .timestamp(String.valueOf(Instant.now().toEpochMilli()))
                    .build();
            this.display(msg);
        } else if (!authFuture.isSuccess()) {
            logger.info("Connection established failed {}", authFuture.cause());
            Msg msg = new MsgImpl.Builder("")
                    .type("application/json")
                    .payload("{'name': \"connect\", 'status': \"fail\", 'data': \"<div>Connection established failed.</div>\"}")
                    .timestamp(String.valueOf(Instant.now().toEpochMilli()))
                    .build();
            this.display(msg);
        } else {
            logger.info("Connection established successfully.");
            Msg msg = new MsgImpl.Builder("")
                    .type("application/json")
                    .payload("{'name': \"connect\", 'status': \"ok\", 'data':" + this.getStartDashboard() + "}")
                    .timestamp(String.valueOf(Instant.now().toEpochMilli()))
                    .build();
            this.display(msg);
        }
    }

    @Override
    public void disconnect() {
        logger.info("Do disconnect.");

    }

    private String getStartDashboard() {
        return this.dashboards.get(0).getHtml();
    }

    /* private String getCardHtml(String id) {

        return """
               <div class="card text-center text-white bg-secondary shadow-sm " id="card1">
                   <div class="align-items-center" id="card1-payload">
                       <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-display" viewBox="0 0 16 16">
                       <path d="M0 4s0-2 2-2h12s2 0 2 2v6s0 2-2 2h-4c0 .667.083 1.167.25 1.5H11a.5.5 0 0 1 0 1H5a.5.5 0 0 1 0-1h.75c.167-.333.25-.833.25-1.5H2s-2 0-2-2V4zm1.398-.855a.758.758 0 0 0-.254.302A1.46 1.46 0 0 0 1 4.01V10c0 .325.078.502.145.602.07.105.17.188.302.254a1.464 1.464 0 0 0 .538.143L2.01 11H14c.325 0 .502-.078.602-.145a.758.758 0 0 0 .254-.302 1.464 1.464 0 0 0 .143-.538L15 9.99V4c0-.325-.078-.502-.145-.602a.757.757 0 0 0-.302-.254A1.46 1.46 0 0 0 13.99 3H2c-.325 0-.502.078-.602.145z"/>
                       </svg>
                   </div>
                   <div class="card-body">
                       <h5 class="card-title">Screenshot</h5>
                       <p class="card-text">Get screenshot from hm</p>
                       <div class="btn-group">
                           <button type="button" class="btn btn-sm btn-outline-light" id="sendCommand" value="1">Get</button>
                           <a href="" download="image.jpg" role="button" class="btn btn-sm btn-outline-light disabled" id="card1-save">Save</a>
                       </div>
                       <small class="text-dark">9 mins</small>
                   </div>
                   <div class="card-footer text-dark" id="card1-timestamp">
                       2 days ago
                   </div>
               </div>""";
    } */
    @Override
    public void publish(Msg msg) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void display(Msg msg) {
      this.uiController.display(msg);
    }
}
