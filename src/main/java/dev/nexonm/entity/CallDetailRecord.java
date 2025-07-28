package dev.nexonm.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Сущность CallDetailRecord представляет запись деталей звонка.
 * Соответствует таблице call_detail_records в базе данных.
 */

@Entity
@Table(name = "call_detail_records")
public class CallDetailRecord extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "call_time", nullable = false)
    public LocalDateTime time;

    @Column(name = "duration", nullable = false)
    public Integer duration; // Продолжительность звонка в секундах

    @Column(name = "number_a", nullable = false, length = 15)
    public String numberA; // Номер A (инициатор звонка)

    @Column(name = "number_b", nullable = false, length = 15)
    public String numberB; // Номер B (получатель звонка)

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "agent_id", referencedColumnName = "agent_id", nullable = false)
    public Agent agent; // Агент предоставляющий услугу связи номеру A

    public CallDetailRecord() {
        // Default constructor
    }

    public CallDetailRecord(LocalDateTime time, String numberA, String numberB, Integer duration, Agent agent) {
        this.time = time;
        this.duration = duration;
        this.numberA = numberA;
        this.numberB = numberB;
        this.agent = agent;
    }

    // Методы базы данных Panache

    public static List<CallDetailRecord> findByAgent(Agent agent) {
        return list("agent", agent);
    }

    public static List<CallDetailRecord> findByTimeRange(LocalDateTime start, LocalDateTime end) {
        return list("callTimestamp >= ?1 and callTimestamp <= ?2", start, end);
    }

    public static List<CallDetailRecord> findByAgentAndTimeRange(Agent agent, LocalDateTime start, LocalDateTime end) {
        return list("agent = ?1 and callTimestamp >= ?2 and callTimestamp <= ?3", agent, start, end);
    }

    // Object методы

    @Override
    public String toString() {
        return String.format("CallDetailRecord{id=%d, timestamp=%s, %s->%s, duration=%d, agent=%s}",
                id, time, numberA, numberB, duration, agent != null ? agent.agentId : "null");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CallDetailRecord cdr)) return false;
        return id != null && id.equals(cdr.id);
    }

}
