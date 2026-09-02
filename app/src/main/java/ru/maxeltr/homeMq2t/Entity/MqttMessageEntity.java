package ru.maxeltr.homeMq2t.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pending_mqtt_messages")
public class MqttMessageEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String topic;

    @Lob
    @Column(nullable = false)
    private byte[] payload;

    @Column(nullable = false)
    private int qos;

    private boolean retain;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public MqttMessageEntity() {
    }
    
    public MqttMessageEntity(String topic, byte[] payload, int qos, boolean retain) {
        this.topic = topic;
        this.payload = payload;
        this.qos = qos;
        this.retain = retain;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public byte[] getPayload() { return payload; }
    public void setPayload(byte[] payload) { this.payload = payload; }

    public int getQos() { return qos; }
    public void setQos(int qos) { this.qos = qos; }

    public boolean isRetain() { return retain; }
    public void setRetain(boolean retain) { this.retain = retain; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
