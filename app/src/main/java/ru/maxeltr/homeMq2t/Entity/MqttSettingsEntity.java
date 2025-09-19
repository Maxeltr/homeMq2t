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
package ru.maxeltr.homeMq2t.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import static ru.maxeltr.homeMq2t.Entity.ComponentEntity.JSON_FIELD_ID;

@Entity
@Table(name = "mqtt_settings")
public class MqttSettingsEntity extends BaseEntity {

    public static final String TABLE_NAME = "Mqtt Settings";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(JSON_FIELD_ID)
    private long id;
    @JsonProperty(JSON_FIELD_NAME)
    private String name;
    private String host;
    private String port;
    private String mq2tPassword;
    private String mq2tUsername;
    private String clientId;
    private Boolean hasUsername;
    private Boolean hasPassword;
    private String willQos;
    private Boolean willRetain;
    private Boolean willFlag;
    private Boolean cleanSession;
    private Boolean autoConnect;
    private String willTopic;
    private String willMessage;
    private Boolean reconnect;
    @Column(name = "number", insertable = false)
    private Integer number;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Nullable
    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Nullable
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Nullable
    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    @Nullable
    public String getMq2tPassword() {
        return mq2tPassword;
    }

    public void setMq2tPassword(String mq2tPassword) {
        this.mq2tPassword = mq2tPassword;
    }

    @Nullable
    public String getMq2tUsername() {
        return mq2tUsername;
    }

    public void setMq2tUsername(String mq2tUsername) {
        this.mq2tUsername = mq2tUsername;
    }

    @Nullable
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Nullable
    public Boolean getHasUsername() {
        return hasUsername;
    }

    public void setHasUsername(Boolean hasUsername) {
        this.hasUsername = hasUsername;
    }

    @Nullable
    public Boolean getHasPassword() {
        return hasPassword;
    }

    public void setHasPassword(Boolean hasPassword) {
        this.hasPassword = hasPassword;
    }

    @Nullable
    public String getWillQos() {
        return willQos;
    }

    public void setWillQos(String willQos) {
        this.willQos = willQos;
    }

    @Nullable
    public Boolean getWillRetain() {
        return willRetain;
    }

    public void setWillRetain(Boolean willRetain) {
        this.willRetain = willRetain;
    }

    @Nullable
    public Boolean getWillFlag() {
        return willFlag;
    }

    public void setWillFlag(Boolean willFlag) {
        this.willFlag = willFlag;
    }

    @Nullable
    public Boolean getCleanSession() {
        return cleanSession;
    }

    public void setCleanSession(Boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    @Nullable
    public Boolean getAutoConnect() {
        return autoConnect;
    }

    public void setAutoConnect(Boolean autoConnect) {
        this.autoConnect = autoConnect;
    }

    @Nullable
    public String getWillTopic() {
        return willTopic;
    }

    public void setWillTopic(String willTopic) {
        this.willTopic = willTopic;
    }

    @Nullable
    public String getWillMessage() {
        return willMessage;
    }

    public void setWillMessage(String willMessage) {
        this.willMessage = willMessage;
    }

    @Nullable
    public Boolean getReconnect() {
        return reconnect;
    }

    public void setReconnect(Boolean reconnect) {
        this.reconnect = reconnect;
    }

    @Nullable
    @Override
    public Integer getNumber() {
        return this.number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

}
