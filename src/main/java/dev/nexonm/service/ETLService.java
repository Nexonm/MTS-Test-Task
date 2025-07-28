package dev.nexonm.service;

import dev.nexonm.dto.response.ETLReport;
import dev.nexonm.dto.response.ValidationError;
import dev.nexonm.entity.Agent;
import dev.nexonm.entity.CallDetailRecord;
import dev.nexonm.mapper.CallDetailRecordMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ETL-сервис для обработки CSV-файлов и загрузки данных в базу данных.
 * <p>
 * Процесс:
 * <ul>
 *   <li>Извлечение: чтение CSV-файла из файловой системы.</li>
 *   <li>Трансформация: парсинг и валидация данных, преобразование в сущности.</li>
 *   <li>Загрузка: сохранение данных в базу данных пакетами.</li>
 * </ul>
 * </p>
 */
@Slf4j
@ApplicationScoped
public class ETLService {

    private static final int BATCH_SIZE = 1000000; // Количество записей в одном батче для единовременной обработки

    @Inject
    CSVRowValidator validator;

    /**
     * Главный ETL-метод: обрабатывает CSV-файл и загружает данные в базу данных.
     * <p>
     * Этапы:
     * <ul>
     *   <li>Извлечение: поиск и чтение файла</li>
     *   <li>Валидация: проверка структуры и данных</li>
     *   <li>Трансформация и загрузка: преобразование строк в сущности и сохранение</li>
     * </ul>
     * </p>
     * @param path путь к CSV-файлу (относительный или абсолютный)
     * @return результат ETL-обработки со статистикой
     */
    public ETLReport processCSVFile(String path) {
        log.info("Запуск обработки файла {}", path);
        ETLReport.ETLReportBuilder report = ETLReport.builder()
                .fileName(Paths.get(path).getFileName().toString())
                .startTime(LocalDateTime.now());
        try {
            // Этап 1: ИЗВЛЕЧЕНИЕ - Валидация файла, извлечение даты, чтение
            log.debug("Поиск файла");
            Path file = CSVFileReader.findFile(path);
            log.debug("Чтение и валидация файла {}", file.getFileName());
            List<ValidationError> errors = validateFile(file);
            log.debug("Завершена валидация файла");
            // Проверяем наличие ошибок до загрузки
            if (!errors.isEmpty()) {
                log.warn("Обнаружено {} ошибок при валидации!", errors.size());
                return report
                        .status(ETLReport.ETLStatus.FAILED)
                        .endTime(LocalDateTime.now())
                        .validationErrors(errors)
                        .errorMessage("Обнаружены ошибки валидации при трансформации")
                        .build();
            }
            // Этап 2: ТРАНСФОРМАЦИЯ и ЗАГРУЗКА
            log.debug("Начало трансформации и сохранения данных");
            long count = transformAndLoad(file);
            log.info("Успешно преобразовано и загружено {} записей", count);
            // Формируем успешный результат
            return report
                    .status(ETLReport.ETLStatus.COMPLETED)
                    .endTime(LocalDateTime.now())
                    .totalRecords(count)
                    .fileDate(CSVFileReader.extractFileDate(file.getFileName().toString()))
                    .validationErrors(errors)
                    .build();
        } catch (Exception e) {
            log.error("Ошибка ETL-процесса: {}", e.getMessage(), e);
            return report
                    .status(ETLReport.ETLStatus.FAILED)
                    .endTime(LocalDateTime.now())
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * Валидирует CSV-файл построчно, собирает ошибки валидации.
     * <ul>
     *   <li>Проверяет заголовки</li>
     *   <li>Проверяет каждую строку на корректность</li>
     *   <li>Проверяет наличие account в справочнике агентов</li>
     * </ul>
     * @param path путь к файлу
     * @return список ошибок валидации
     */
    public List<ValidationError> validateFile(Path path) {
        List<ValidationError> errors = new LinkedList<>();
        try {
            CSVFileReader reader = CSVFileReader.init(path);
            String[] headers = reader.readLine();
            try {
                log.debug("Валидация заголовков: {}", Arrays.stream(headers).toList());
                CSVFileReader.validateCSVHeaders(headers);
            } catch (IllegalArgumentException e) {
                log.error("Ошибка валидации заголовков: {}", e.getMessage());
                errors.add(new ValidationError(1L, e.getMessage()));
            }
            HashSet<String> accounts = loadAgentsAccounts();
            List<String[]> list = new ArrayList<>(BATCH_SIZE);
            int count = 0;
            do {
                list.clear();
                reader.read(list, BATCH_SIZE);
                validator.validate(list, errors, accounts, (long) BATCH_SIZE * count + 2);
                log.debug("Прочитано {} строк CSV", list.size());
                count++;
            } while (list.size() == BATCH_SIZE);
            reader.close();
            log.info("Завершено чтение CSV-файла, всего итераций: {}",  count);
            return errors;
        } catch (Exception e) {
            log.error("Ошибка при валидации файла: {}", e.getMessage(), e);
            errors.add(new ValidationError(-1L, e.getMessage()));
        }
        return errors;
    }

    /**
     * Трансформирует строки CSV в сущности и загружает их в базу данных пакетами.
     * <ul>
     *   <li>Загружает справочник агентов</li>
     *   <li>Парсит дату из имени файла</li>
     *   <li>Пакетно сохраняет записи</li>
     * </ul>
     * @param path путь к файлу
     * @return количество успешно загруженных записей
     */
    public long transformAndLoad(Path path) {
        long count = 0L;
        try {
            Map<String, Agent> agents = loadAgentMap();
            LocalDate date = CSVFileReader.extractFileDate(path.getFileName().toString());
            CSVFileReader reader = CSVFileReader.init(path);
            reader.readLine();
            List<String[]> rows = new ArrayList<>(BATCH_SIZE);
            do {
                rows.clear();
                reader.read(rows, BATCH_SIZE);
                log.debug("Чтение {} новых строк", rows.size());
                List<CallDetailRecord> entities = CallDetailRecordMapper.mapSCVToEntities(rows, date, agents);
                saveToDatabaseInBatches(entities);
                count += rows.size();
            } while (rows.size() == BATCH_SIZE);
            reader.close();
            log.info("Завершена загрузка {} строк", count);
            return count;
        } catch (Exception e) {
            log.error("Ошибка при трансформации и загрузке: {}", e.getMessage(), e);
            return count;
        }
    }

    /**
     * Предзагружает всех агентов в map для быстрого поиска по account.
     * @return map account -> Agent
     */
    private Map<String, Agent> loadAgentMap() {
        List<Agent> agents = Agent.listAll();
        if (agents.isEmpty()) {
            log.error("В базе данных не найдено ни одного агента!");
            throw new IllegalStateException("Нет агентов в базе данных. Заполните таблицу agents.");
        }
        return agents.stream()
                .collect(Collectors.toMap(
                        agent -> agent.account,
                        agent -> agent,
                        (existing, replacement) -> existing // При дубликатах оставляем первого
                ));
    }

    /**
     * Загружает все account агентов в HashSet для быстрой проверки.
     * @return множество account
     */
    private HashSet<String> loadAgentsAccounts() {
        List<Agent> agents = Agent.listAll();
        if (agents.isEmpty()) {
            log.error("В базе данных не найдено ни одного агента!");
            throw new IllegalStateException("Нет агентов в базе данных. Заполните таблицу agents.");
        }
        return new HashSet<>(agents.stream().map(agent -> agent.account).toList());
    }

    /**
     * Сохраняет сущности в базу данных пакетами для оптимальной производительности.
     * <ul>
     *   <li>Пакетная вставка</li>
     *   <li>Логирование успешных и неуспешных батчей</li>
     * </ul>
     * @param entities список сущностей для сохранения
     * @return количество успешно сохранённых записей
     */
    @Transactional
    public void saveToDatabaseInBatches(List<CallDetailRecord> batch) {
            try {
                CallDetailRecord.persist(batch);
                log.debug("Сохранён батч ({} записей)", batch.size());
            } catch (Exception e) {
                log.error("Ошибка при сохранении батча ({} записей), \"{}\": {}", batch.size(), e.getMessage(), e);
            }
        log.info("Сохранено {} записей в батче",batch.size());
    }

}
