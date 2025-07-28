---- Создание таблицы справочника агентов
--CREATE TABLE agents (
--    agent_id BIGINT PRIMARY KEY,
--    name VARCHAR(1024) NOT NULL,
--    account VARCHAR(100) NOT NULL UNIQUE
--);

-- Создание индекса для быстрого поиска по лицевому счету
CREATE INDEX idx_agents_account ON agents(account);

---- Создание таблицы звонков (CDR)
--CREATE TABLE call_detail_records (
--    id BIGINT AUTO_INCREMENT PRIMARY KEY,
--    call_time TIMESTAMP NOT NULL,
--    number_a VARCHAR(15) NOT NULL,
--    number_b VARCHAR(15) NOT NULL,
--    duration INTEGER NOT NULL,
--    agent_id BIGINT NOT NULL,
--
--    CONSTRAINT fk_cdr_agent FOREIGN KEY (agent_id) REFERENCES agents(agent_id),
--    CONSTRAINT chk_duration_positive CHECK (duration > 0),
--    CONSTRAINT chk_numbers_different CHECK (number_a != number_b)
--);

-- Создание индексов для оптимизации запросов
CREATE INDEX idx_cdr_time ON call_detail_records(call_time);
CREATE INDEX idx_cdr_agent_id ON call_detail_records(agent_id);
CREATE INDEX idx_cdr_time_agent ON call_detail_records(call_time, agent_id);

-- Предзаполняем данные по агентам
INSERT INTO agents (agent_id, name, account) VALUES
(1, 'ООО "Байкалвестком"', 'account1'),
(2, 'ООО "СБЕР Мобайл"', 'account2'),
(3, 'ООО "Вайнах Телеком"', 'account3'),
(4, 'ООО "Волга Телеком"', 'account4'),
(5, 'ООО "Дельта Телеком"', 'account5'),
(6, 'ООО "ЕТК"', 'account6'),
(7, 'ООО "Мегафон"', 'account7'),
(8, 'ООО "Т-Мобайл"', 'account8'),
(9, 'ООО "Мотив"', 'account9');

-- Тестовые данные CDR для разработки (опционально)
--INSERT INTO call_detail_records (call_time, number_a, number_b, duration, agent_id) VALUES
--('2024-01-15 09:30:15', '79161234567', '79267654321', 180, 1),
--('2024-01-15 10:15:30', '79161234568', '79267654322', 240, 2),
--('2024-01-15 11:45:00', '79161234569', '79267654323', 90, 1),
--('2024-01-15 14:20:45', '79161234570', '79267654324', 300, 3),
--('2024-01-15 16:10:15', '79161234571', '79267654325', 150, 2);
