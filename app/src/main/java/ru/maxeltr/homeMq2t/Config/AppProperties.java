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
package ru.maxeltr.homeMq2t.Config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
@ConfigurationProperties
public class AppProperties {

    @Autowired
    private Environment env;

    private String host = "127.0.0.22";
    private String port = "1883";
    private String localServerPort = "8030";
    private String password = "defaultPassword";
    private String userName = "defaultUsername";
    private String clientId = "clientIdClientId";
    private Boolean hasUserName = true;
    private Boolean hasPassword = true;
    private int willQos = 0;
    private Boolean willRetain = false;
    private Boolean willFlag = false;
    private Boolean cleanSession = true;
    private int keepAliveTimer = 20;                //sec
    private int retransmitMqttMessageTimer = 60;    //sec
    private int reconnectDelay = 3;                 //sec
    private int maxBytesInMessage = 8092000;        //bytes
    private int measurementPeriodicTrigger = 10;    //sec
    private int CONNECT_TIMEOUT = 2000;             //ms

    public String getProperty(String key, String defaultValue) {
        return env.getProperty(key, defaultValue);
    }

    public Environment getEnv() {
        return this.env;
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public String getLocalServerPort() {
        return localServerPort;
    }

    public String getPassword() {
        return password;
    }

    public String getUserName() {
        return userName;
    }

    public String getClientId() {
        return clientId;
    }

    public Boolean getHasUserName() {
        return hasUserName;
    }

    public Boolean getHasPassword() {
        return hasPassword;
    }

    public int getWillQos() {
        return willQos;
    }

    public Boolean getWillRetain() {
        return willRetain;
    }

    public Boolean getWillFlag() {
        return willFlag;
    }

    public Boolean getCleanSession() {
        return cleanSession;
    }

    public int getKeepAliveTimer() {
        return keepAliveTimer;
    }

    public int getRetransmitMqttMessageTimer() {
        return retransmitMqttMessageTimer;
    }

    public int getReconnectDelay() {
        return reconnectDelay;
    }

    public int getMaxBytesInMessage() {
        return maxBytesInMessage;
    }

    public int getMeasurementPeriodicTrigger() {
        return measurementPeriodicTrigger;
    }

    public void setEnv(Environment env) {
        this.env = env;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public void setLocalServerPort(String localServerPort) {
        this.localServerPort = localServerPort;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setHasUserName(Boolean hasUserName) {
        this.hasUserName = hasUserName;
    }

    public void setHasPassword(Boolean hasPassword) {
        this.hasPassword = hasPassword;
    }

    public void setWillQos(int willQos) {
        this.willQos = willQos;
    }

    public void setWillRetain(Boolean willRetain) {
        this.willRetain = willRetain;
    }

    public void setWillFlag(Boolean willFlag) {
        this.willFlag = willFlag;
    }

    public void setCleanSession(Boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public void setKeepAliveTimer(int keepAliveTimer) {
        this.keepAliveTimer = keepAliveTimer;
    }

    public void setRetransmitMqttMessageTimer(int retransmitMqttMessageTimer) {
        this.retransmitMqttMessageTimer = retransmitMqttMessageTimer;
    }

    public void setReconnectDelay(int reconnectDelay) {
        this.reconnectDelay = reconnectDelay;
    }

    public void setMaxBytesInMessage(int maxBytesInMessage) {
        this.maxBytesInMessage = maxBytesInMessage;
    }

    public void setMeasurementPeriodicTrigger(int measurementPeriodicTrigger) {
        this.measurementPeriodicTrigger = measurementPeriodicTrigger;
    }

    @Override
    public String toString() {
        return "AppProperty{" + "env=" + env + ", host=" + host + ", port=" + port + ", localServerPort=" + localServerPort + ", password=" + password + ", userName=" + userName + ", clientId=" + clientId + ", hasUserName=" + hasUserName + ", hasPassword=" + hasPassword + ", willQos=" + willQos + ", willRetain=" + willRetain + ", willFlag=" + willFlag + ", cleanSeesion=" + cleanSession + ", keepAliveTimer=" + keepAliveTimer + ", retransmitMqttMessageTimer=" + retransmitMqttMessageTimer + ", reconnectDelay=" + reconnectDelay + ", maxBytesInMessage=" + maxBytesInMessage + ", measurementPeriodicTrigger=" + measurementPeriodicTrigger + '}';
    }

}
