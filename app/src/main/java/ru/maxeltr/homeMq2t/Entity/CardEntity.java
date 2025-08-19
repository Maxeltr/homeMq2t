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

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "card_settings")
public class CardEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("ID")
    private long id;
    @JsonProperty("NAME")
    private String name;
    private String subscriptionTopic;
    private String subscriptionQos;
    private String subscriptionDataName;
    private String subscriptionDataType;
    @JsonProperty("displayDataJsonPath")
    private String displayDataJsonpath;
    private String publicationTopic;
    private String publicationQos;
    private Boolean publicationRetain;
    private String publicationData;
    private String publicationDataType;
    private String localTaskPath;
    private String localTaskArguments;
    private String localTaskDataType;
    @Column(name = "number", insertable = false)
    private Integer number;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "dashboard_id", nullable = false)
    private DashboardEntity dashboard;

    public Long getId() {
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

    public String getSubscriptionDataName() {
        return subscriptionDataName;
    }

    public void setSubscriptionDataName(String subscriptionDataName) {
        this.subscriptionDataName = subscriptionDataName;
    }

    public String getSubscriptionDataType() {
        return subscriptionDataType;
    }

    public void setSubscriptionDataType(String subscriotionDataType) {
        this.subscriptionDataType = subscriotionDataType;
    }

    public String getDisplayDataJsonpath() {
        return displayDataJsonpath;
    }

    public void setDisplayDataJsonpath(String displayDataJsonpath) {
        this.displayDataJsonpath = displayDataJsonpath;
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

    public Boolean getPublicationRetain() {
        return publicationRetain;
    }

    public void setPublicationRetain(boolean publicationRetain) {
        this.publicationRetain = publicationRetain;
    }

    public String getPublicationData() {
        return publicationData;
    }

    public void setPublicationData(String publicationData) {
        this.publicationData = publicationData;
    }

    public String getPublicationDataType() {
        return publicationDataType;
    }

    public void setPublicationDataType(String publicationDataType) {
        this.publicationDataType = publicationDataType;
    }

    public String getLocalTaskPath() {
        return localTaskPath;
    }

    public void setLocalTaskPath(String localTaskPath) {
        this.localTaskPath = localTaskPath;
    }

    public String getLocalTaskArguments() {
        return localTaskArguments;
    }

    public void setLocalTaskArguments(String localTaskArguments) {
        this.localTaskArguments = localTaskArguments;
    }

    public String getLocalTaskDataType() {
        return localTaskDataType;
    }

    public void setLocalTaskDataType(String localTaskDataType) {
        this.localTaskDataType = localTaskDataType;
    }

    @Override
    public Integer getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public DashboardEntity getDashboard() {
        return dashboard;
    }

    public void setDashboard(DashboardEntity dashboard) {
        this.dashboard = dashboard;
    }

    @Override
    public String toString() {
        return "CardEntity{" + "id=" + id + ", name=" + name + ", subscriptionTopic=" + subscriptionTopic + ", subscriptionQos=" + subscriptionQos + ", subscriptionDataName=" + subscriptionDataName + ", subscriptionDataType=" + subscriptionDataType + ", displayDataJsonpath=" + displayDataJsonpath + ", publicationTopic=" + publicationTopic + ", publicationQos=" + publicationQos + ", publicationRetain=" + publicationRetain + ", publicationData=" + publicationData + ", publicationDataType=" + publicationDataType + ", localTaskPath=" + localTaskPath + ", localTaskArguments=" + localTaskArguments + ", localTaskDataType=" + localTaskDataType + ", number=" + number + ", dashboard=" + dashboard.getName() + '}';
    }

}
