package ru.maxeltr.homeMq2t.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pending_mqtt_messages", indexes = {
    @Index(name = "idx_pending_packet_id", columnList = "packet_id"),
    @Index(name = "idx_pending_status", columnList = "status")
})
public class MqttMessageEntity {

    /** Сообщение отправлено, подтверждение (PUBACK/PUBREC) не получено. */
    public static final String STATUS_SEND_PUBLISH = "SEND_PUBLISH";

    /** PUBREC получен, ожидается PUBCOMP (сообщение в статусе PUBREL). */
    public static final String STATUS_SEND_PUBREL = "SEND_PUBREL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Идентификатор пакета MQTT (packet id) в рамках текущей сессии. */
    @Column(name = "packet_id", nullable = false)
    private int packetId;

    @Column(nullable = false)
    private String topic;

    @Lob
    @Column(nullable = false)
    private byte[] payload;

    @Column(nullable = false)
    private int qos;

    private boolean retain;

    /** Статус ожидания подтверждения: SEND_PUBLISH или SEND_PUBREL. */
    @Column(length = 20)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public MqttMessageEntity() {
    }

    public MqttMessageEntity(int packetId, String topic, byte[] payload, int qos, boolean retain, String status) {
        this.packetId = packetId;
        this.topic = topic;
        this.payload = payload;
        this.qos = qos;
        this.retain = retain;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getPacketId() { return packetId; }
    public void setPacketId(int packetId) { this.packetId = packetId; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public byte[] getPayload() { return payload; }
    public void setPayload(byte[] payload) { this.payload = payload; }

    public int getQos() { return qos; }
    public void setQos(int qos) { this.qos = qos; }

    public boolean isRetain() { return retain; }
    public void setRetain(boolean retain) { this.retain = retain; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
