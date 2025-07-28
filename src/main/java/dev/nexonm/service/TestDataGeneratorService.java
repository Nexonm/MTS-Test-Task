package dev.nexonm.service;

import com.opencsv.CSVWriter;
import dev.nexonm.dto.csv.CSVRecord;
import dev.nexonm.entity.Agent;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Сервис для генерации тестовых CSV файлов с данными звонков
 * <p>Возможности:
 * <ul>
 * <li>Генерация реалистичных данных CDR</li>
 * <li>Сохранение в CSV формате</li>
 * <li>Правильное именование файлов с датой</li>
 * <li>Создание папки data/ автоматически</li>
 * </ul>
 */
@Slf4j
@ApplicationScoped
public class TestDataGeneratorService {

    private static final String DATA_DIRECTORY = getProjectRootPath() + "/data";
    ;
    private static final String[] CSV_HEADER = {"time", "numberA", "numberB", "duration", "account"};

    /**
     * Получает путь к корню проекта (где лежит build.gradle)
     *
     * @return путь к корню проекта
     */
    private static String getProjectRootPath() {
        // Получаем текущий рабочий каталог
        String userDir = System.getProperty("user.dir");
        // Если мы в build/ каталоге, поднимаемся на уровень выше
        if (userDir.endsWith("build") || userDir.contains("build/classes")) {
            Path currentPath = Paths.get(userDir);
            // Ищем корень проекта (где есть build.gradle)
            while (currentPath != null && currentPath.getParent() != null) {
                if (Files.exists(currentPath.resolve("build.gradle"))) {
                    return currentPath.toString();
                }
                currentPath = currentPath.getParent();
            }
        }
        // По умолчанию используем текущий каталог
        return userDir;
    }

    // Генератор случайных чисел
    private final RandomGenerator random = RandomGenerator.getDefault();
    // Шаблоны для генерации реалистичных номеров
    private final String[] phoneAreaCodes = {"916", "926", "903", "905", "906", "909", "963", "996", "997", "999",
            "929", "922", "930", "910", "343"};

    /**
     * Генерирует CSV-файл с тестовыми данными за указанную дату.
     * <ul>
     *   <li>Генерирует записи звонков</li>
     *   <li>Сохраняет в папку data/</li>
     *   <li>Имя файла соответствует шаблону</li>
     * </ul>
     *
     * @param date        дата для генерации
     * @param recordCount количество записей
     * @return путь к созданному файлу
     */
    public String generateCSVFile(LocalDate date, int recordCount) {
        validateInput(date, recordCount);
        createDataDirectory();
        String fileName = generateFileName(date);
        Path filePath = Paths.get(DATA_DIRECTORY, fileName);
        try {
            List<CSVRecord> records = generateCallRecords(recordCount);
            writeCSVFile(filePath, records);
            log.info("Сгенерирован файл: {} с {} записями", filePath.toString(), recordCount);
            return filePath.toString();
        } catch (IOException e) {
            log.error("Ошибка при создании CSV файла: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при создании CSV файла: " + e.getMessage(), e);
        }
    }

    // ===================================
    // Приватные методы генерации данных
    // ===================================

    /**
     * Генерирует список случайных записей звонков.
     *
     * @param count количество записей
     * @return список CSVRecord
     */
    private List<CSVRecord> generateCallRecords(int count) {
        List<Agent> agents = Agent.listAll();
        if (agents.isEmpty()) {
            log.error("Нет агентов в БД! Убедитесь что таблица agents заполнена.");
            throw new IllegalStateException("Нет агентов в БД! Убедитесь что таблица agents заполнена.");
        }
        List<CSVRecord> records = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            records.add(generateRandomCallRecord(agents));
        }
        records.sort(Comparator.comparing(CSVRecord::getTime));
        return records;
    }

    /**
     * Генерирует одну случайную запись звонка.
     *
     * @param agents список агентов
     * @return CSVRecord
     */
    private CSVRecord generateRandomCallRecord(List<Agent> agents) {
        return new CSVRecord(generateRandomTime(), generateRandomPhoneNumber(), generateRandomPhoneNumber(),
                generateRandomDuration(), selectRandomAgent(agents).account);
    }

    /**
     * Генерирует случайное время в течение дня.
     *
     * @return строка времени
     */
    private String generateRandomTime() {
        // Больше звонков в рабочие часы (9-18)
        int hour;
        if (random.nextDouble() < 0.7) { // 70% в рабочие часы
            hour = 9 + random.nextInt(9); // 9-17
        } else {
            hour = random.nextInt(24); // любое время
        }
        int minute = random.nextInt(60);
        int second = random.nextInt(60);
        return LocalTime.of(hour, minute, second).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    /**
     * Генерирует реалистичный российский номер телефона.
     *
     * @return строка номера
     */
    private String generateRandomPhoneNumber() {
        String areaCode = phoneAreaCodes[random.nextInt(phoneAreaCodes.length)];
        StringBuilder number = new StringBuilder("7");
        number.append(areaCode);
        for (int i = 0; i < 7; i++) {
            number.append(random.nextInt(10));
        }
        return number.toString();
    }

    /**
     * Генерирует реалистичную продолжительность звонка.
     *
     * @return продолжительность в секундах
     */
    private Integer generateRandomDuration() {
        // 80% коротких звонков, 20% длинных
        if (random.nextDouble() < 0.8) {
            return 30 + random.nextInt(300); // 30 секунд - 5 минут
        } else {
            return 300 + random.nextInt(3300); // 5-60 минут
        }
    }

    /**
     * Выбирает случайного агента.
     *
     * @param agents список агентов
     * @return Agent
     */
    private Agent selectRandomAgent(List<Agent> agents) {
        return agents.get(random.nextInt(agents.size()));
    }

    // Утилитарные методы

    /**
     * Валидация входных параметров.
     *
     * @param date        дата
     * @param recordCount количество записей
     */
    private void validateInput(LocalDate date, long recordCount) {
        if (date == null) {
            throw new IllegalArgumentException("Дата не может быть null");
        }
        if (date.isAfter(LocalDate.now().plusDays(1))) {
            throw new IllegalArgumentException("Нельзя генерировать данные для будущих дат");
        }
        if (recordCount <= 0) {
            throw new IllegalArgumentException("Количество записей должно быть больше 0");
        }
        if (recordCount > 2000000000) {
            throw new IllegalArgumentException("Слишком много записей (макс. 2.000.000.000)");
        }
    }

    /**
     * Создаёт папку data/ если не существует.
     */
    private void createDataDirectory() {
        try {
            Path dataPath = Paths.get(DATA_DIRECTORY);
            if (!Files.exists(dataPath)) {
                Files.createDirectories(dataPath);
                log.info("Создана папка: {}", dataPath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Не удалось создать папку {}: {}", DATA_DIRECTORY, e.getMessage(), e);
            throw new RuntimeException("Не удалось создать папку " + DATA_DIRECTORY, e);
        }
    }

    /**
     * Генерирует имя файла по шаблону YYYY-MM-DD.csv
     *
     * @param date дата
     * @return имя файла
     */
    private String generateFileName(LocalDate date) {
        return String.format("%s.csv", date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    }

    /**
     * Записывает данные в CSV-файл.
     *
     * @param filePath путь к файлу
     * @param records  список записей
     * @throws IOException ошибка записи
     */
    private void writeCSVFile(Path filePath, List<CSVRecord> records) throws IOException {
        try (
                FileWriter fileWriter = new FileWriter(filePath.toFile());
                CSVWriter csvWriter = new CSVWriter(fileWriter)
        ) {
            csvWriter.writeNext(CSV_HEADER, false);
            for (CSVRecord record : records) {
                String[] csvRecord = {record.getTime(), record.getNumberA(), record.getNumberB(),
                        record.getDuration().toString(), record.getAccount()};
                csvWriter.writeNext(csvRecord, false);
            }
            csvWriter.flush();
            log.info("Данные успешно записаны в файл: {}", filePath.toAbsolutePath());
        }
    }

    /**
     * Очиcтка всех сгенерированных файлов.
     */
    public void cleanupGeneratedFiles() {
        Path dataPath = Paths.get(DATA_DIRECTORY);
        if (!Files.exists(dataPath)) {
            log.warn("Папка data/ не существует");
            return;
        }
        log.info("Начинается процесс очистки данных");
        try {
            Files.list(dataPath)
                    .filter(path -> path.toString().endsWith(".csv"))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            log.info("Удалён файл: {}", path);
                        } catch (IOException e) {
                            log.error("Ошибка удаления {}: {}", path, e.getMessage(), e);
                        }
                    });
        } catch (IOException e) {
            log.error("Ошибка при очистке файлов: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при очистке файлов", e);
        }
    }
}

