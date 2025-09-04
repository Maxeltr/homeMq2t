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
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.maxeltr.homeMq2t.Service.UI.HasSubscription;

@Entity
@Table(name = "command_settings")
public class CommandEntity extends BaseEntity implements HasSubscription {

    public static final String JSON_FIELD_ID = "ID";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(JSON_FIELD_ID)
    private long id;
    @JsonProperty("NAME")
    private String name;
    private String subscriptionTopic;
    private String subscriptionQos;
    private String publicationTopic;
    private String publicationQos;
    private Boolean publicationRetain;
    private String publicationDataType;
    private String path;
    private String arguments;
    @Column(name = "number", insertable = false)
    private Integer number;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubscriptionTopic() {
        return subscriptionTopic;
    }

    public void setSubscriptionTopic(String subscriptionTopic) {
        this.subscriptionTopic = subscriptionTopic;
    }

    public String getSubscriptionQos() {
        return subscriptionQos;
    }

    public void setSubscriptionQos(String subscriptionQos) {
        this.subscriptionQos = subscriptionQos;
    }

    public String getPublicationTopic() {
        return publicationTopic;
    }

    public void setPublicationTopic(String publicationTopic) {
        this.publicationTopic = publicationTopic;
    }

    public String getPublicationQos() {
        return publicationQos;
    }

    public void setPublicationQos(String publicationQos) {
        this.publicationQos = publicationQos;
    }

    public boolean getPublicationRetain() {
        return publicationRetain;
    }

    public void setPublicationRetain(boolean publicationRetain) {
        this.publicationRetain = publicationRetain;
    }

    public String getPublicationDataType() {
        return publicationDataType;
    }

    public void setPublicationDataType(String publicationDataType) {
        this.publicationDataType = publicationDataType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    @Override
    public Integer getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

}
