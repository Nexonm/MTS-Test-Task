package dev.nexonm.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.util.List;

/**
 * Сущность Agent представляет базовую модель агента связи.
 * Соответствует таблице agents в базе данных.
 * Является справочиком для агентов.
 */

@Entity
@Table(name = "agents")
public class Agent extends PanacheEntityBase {

    @Id
    @Column(name = "agent_id", nullable = false)
    public Long agentId;

    @Column(name = "name", nullable = false, length = 1024)
    public String name;

    @Column(name = "account", nullable = false, length = 100, unique = true)
    public String account;

    @OneToMany(mappedBy = "agent", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    public List<CallDetailRecord> callDetails;

    public Agent() {
        // Default constructor
    }

    public Agent(Long agentId, String name, String account) {
        this.agentId = agentId;
        this.name = name;
        this.account = account;
    }

    // Методы базы данных Panache

    public static Agent findByAccount(String account) {
        return find("account", account).firstResult();
    }

    public static boolean existsByAccount(String account) {
        return count("account", account) > 0;
    }

    // Object методы

    @Override
    public String toString() {
        return String.format("Agent{agentId=%d, name='%s', account='%s'}", agentId, name, account);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Agent agent)) return false;
        return account != null && account.equals(agent.account);
    }

    @Override
    public int hashCode() {
        return account != null ? account.hashCode() : 0;
    }

}
