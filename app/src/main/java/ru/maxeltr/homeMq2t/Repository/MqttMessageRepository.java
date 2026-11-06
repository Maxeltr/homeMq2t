package ru.maxeltr.homeMq2t.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.maxeltr.homeMq2t.Entity.MqttMessageEntity;

@Repository
public interface MqttMessageRepository extends JpaRepository<MqttMessageEntity, Long> {

    Optional<MqttMessageEntity> findByPacketId(int packetId);

    /**
     * Проверяет, занят ли packetId в базе данных (т.е. существует ли
     * сообщение с таким packetId, ожидающее подтверждения от брокера).
     *
     * Spring Data генерирует для этого метода эффективный запрос
     * "SELECT 1 ... LIMIT 1" (EXISTS) вместо COUNT, поэтому метод
     * выполняется быстро даже на больших таблицах.
     *
     * @param packetId идентификатор MQTT-пакета
     * @return true, если packetId уже занят в БД, иначе false
     */
    boolean existsByPacketId(int packetId);

    List<MqttMessageEntity> findByStatus(String status);

    @Transactional
    void deleteByPacketId(int packetId);

    @Transactional
    @Modifying
    @Query("update MqttMessageEntity m set m.status = :status where m.packetId = :packetId")
    int updateStatusByPacketId(@Param("packetId") int packetId, @Param("status") String status);
}
