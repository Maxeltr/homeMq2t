package ru.maxeltr.homeMq2t.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.maxeltr.homeMq2t.Entity.MqttMessageEntity;

@Repository
public interface MqttMessageRepository extends JpaRepository<MqttMessageEntity, Long> {
    
}
