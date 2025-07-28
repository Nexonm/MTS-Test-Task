package dev.nexonm.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;


public class CSVFileReader {

    private static final Pattern FILE_DATE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})\\.csv$");
    public static final String[] EXPECTED_HEADERS = {"time", "numberA", "numberB", "duration", "account"};

    private FileReader fileReader;
    private CSVReader csvReader;

    /**
     * Конструктор CSVFileReader.
     * @param fileReader FileReader для файла
     * @param csvReader CSVReader для чтения строк
     */
    public CSVFileReader(FileReader fileReader, CSVReader csvReader) {
        this.fileReader = fileReader;
        this.csvReader = csvReader;
    }

    /**
     * Инициализирует CSVFileReader для указанного пути.
     * @param path путь к файлу
     * @return экземпляр CSVFileReader
     * @throws IOException если не удалось открыть файл
     */
    public static CSVFileReader init(Path path) throws IOException {
        FileReader fileReader = new FileReader(path.toFile());
        CSVReader csvReader = new CSVReader(fileReader);
        return new CSVFileReader(fileReader, csvReader);
    }

    /**
     * Закрывает ресурсы чтения файла.
     * @throws IOException если не удалось закрыть
     */
    public void close() throws IOException{
        this.csvReader.close();
        this.fileReader.close();
    }

    /**
     * Читает batch строк из CSV-файла в список.
     * @param list список для добавления строк
     * @param batch максимальное количество строк
     * @throws IOException ошибка ввода-вывода
     * @throws CsvException ошибка формата CSV
     */
    public void read(List<String[]> list, int batch) throws IOException, CsvException {
        while(list.size() < batch) {
            String[] row = csvReader.readNext();
            if (row == null){
                break;
            }
            list.add(row);
        }
    }

    /**
     * Читает одну строку из CSV-файла.
     * @return массив строк (ячейки)
     * @throws IOException ошибка ввода-вывода
     * @throws CsvException ошибка формата CSV
     */
    public String[] readLine() throws IOException, CsvException {
        return csvReader.readNext();
    }

    /**
     * Ищет CSV-файл по указанному пути и возвращает Path.
     * <ul>
     *   <li>Проверяет существование файла</li>
     *   <li>Проверяет расширение .csv</li>
     *   <li>Проверяет шаблон имени</li>
     * </ul>
     * @param filePath путь к файлу
     * @return Path к найденному файлу
     * @throws IllegalArgumentException если файл не найден или невалиден
     */
    public static Path findFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Путь к файлу не может быть пустым");
        }
        Path path = Paths.get(filePath);
        if (path.isAbsolute()) {
            return validateFile(path);
        }
        String projectRootString = getProjectRootPath();
        Path projectRoot = Paths.get(projectRootString);
        Path resolvedPath = projectRoot.resolve(path);
        if (!Files.exists(resolvedPath)) {
            throw new IllegalArgumentException(
                    String.format("Файл не существует: %s (относительно корня проекта: %s)",
                            resolvedPath, projectRoot));
        }
        return validateFile(resolvedPath);
    }

    /**
     * Получает путь к корню проекта (где лежит build.gradle)
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

    /**
     * Проверяет, что путь указывает на обычный файл с расширением .csv и корректным именем.
     * @param path путь к файлу
     * @return Path если файл валиден
     * @throws IllegalArgumentException если файл невалиден
     */
    private static Path validateFile(Path path) {
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Путь не является обычным файлом: " + path);
        }
        if (!path.toString().toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Файл должен иметь расширение .csv: " + path);
        }
        if (!FILE_DATE_PATTERN.matcher(path.getFileName().toString()).matches()) {
            throw new IllegalArgumentException("Имя файла должно соответствовать шаблону YYYY-MM-DD.csv: " + path);
        }
        return path;
    }

    /**
     * Проверяет, что заголовки CSV-файла соответствуют ожидаемому формату.
     * <ul>
     *   <li>Проверяет количество столбцов</li>
     *   <li>Проверяет имена столбцов (без учета регистра и пробелов)</li>
     * </ul>
     * @param headers массив строк — заголовки
     * @throws IllegalArgumentException если заголовки невалидны
     */
    public static void validateCSVHeaders(String[] headers) {
        if (headers.length != EXPECTED_HEADERS.length) {
            throw new IllegalArgumentException(
                    String.format("Ожидалось %d столбцов, найдено %d", EXPECTED_HEADERS.length, headers.length));
        }
        for (int i = 0; i < EXPECTED_HEADERS.length; i++) {
            if (!EXPECTED_HEADERS[i].equalsIgnoreCase(headers[i].trim())) {
                throw new IllegalArgumentException(
                        String.format("Некорректный заголовок в позиции %d. Ожидалось '%s', найдено '%s'",
                                i, EXPECTED_HEADERS[i], headers[i]));
            }
        }
    }

    /**
     * Извлекает дату из имени файла по шаблону YYYY-MM-DD.csv
     * @param file имя файла
     * @return дата
     */
    public static LocalDate extractFileDate(String file) {
        return LocalDate.parse(file.substring(0, 10), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
