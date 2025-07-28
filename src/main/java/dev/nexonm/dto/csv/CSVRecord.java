package dev.nexonm.dto.csv;

import com.opencsv.bean.CsvBindByPosition;
import jakarta.validation.constraints.*;

import java.util.Objects;

/**
 * DTO для парсинга одной записи звонка из CSV файла.
 * <p>
 * Поля CSV файла:
 * <ul>
 *   <li><b>time</b>: время регистрации звонка без даты (HH:mm:ss)</li>
 *   <li><b>numberA</b>: исходящий телефонный номер</li>
 *   <li><b>numberB</b>: входящий телефонный номер</li>
 *   <li><b>duration</b>: продолжительность в секундах</li>
 *   <li><b>account</b>: лицевой счет агента</li>
 * </ul>
 */
public class CSVRecord {

    @CsvBindByPosition(position = 0)
    @NotBlank(message = "Время звонка не может быть пустым")
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$",
            message = "Время должно быть в формате HH:mm:ss")
    private String time;

    @CsvBindByPosition(position = 1)
    @NotBlank(message = "Номер A не может быть пустым")
    @Pattern(regexp = "^[1-9]\\d{10,14}$",
            message = "Номер A должен быть в формате E.164 без плюса (10-15 цифр, не начинается с 0)")
    private String numberA;

    @CsvBindByPosition(position = 2)
    @NotBlank(message = "Номер B не может быть пустым")
    @Pattern(regexp = "^[1-9]\\d{10,14}$",
            message = "Номер B должен быть в формате E.164 без плюса (10-15 цифр, не начинается с 0)")
    private String numberB;

    @CsvBindByPosition(position = 3)
    @NotNull(message = "Продолжительность обязательна")
    @Min(value = 1, message = "Продолжительность должна быть больше нуля")
    @Max(value = 86400, message = "Продолжительность не может превышать сутки")
    private Integer duration;

    @CsvBindByPosition(position = 4)
    @NotBlank(message = "Лицевой счет не может быть пустым")
    @Size(max = 100, message = "Лицевой счет не может быть длиннее 100 символов")
    @Pattern(regexp = "^[a-zA-Z0-9]+$",
            message = "Лицевой счет может содержать только буквы и цифры")
    private String account;

    // Конструкторы

    /**
     * Конструктор по умолчанию для OpenCSV
     */
    public CSVRecord() {
    }

    /**
     * Конструктор со всеми параметрами
     */
    public CSVRecord(String time, String numberA, String numberB,
                     Integer duration, String account) {
        this.time = time;
        this.numberA = numberA;
        this.numberB = numberB;
        this.duration = duration;
        this.account = account;
    }

    // Геттеры и сеттеры

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getNumberA() {
        return numberA;
    }

    public void setNumberA(String numberA) {
        this.numberA = numberA;
    }

    public String getNumberB() {
        return numberB;
    }

    public void setNumberB(String numberB) {
        this.numberB = numberB;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    // Бизнес методы

    /**
     * Фабричный метод для создания из массива CSV строк
     */
    public static CSVRecord fromArray(String[] fields) {
        if (fields.length != 5) {
            throw new IllegalArgumentException(
                    String.format("Ожидается 5 полей в CSV, получено: %d", fields.length));
        }

        try {
            return new CSVRecord(
                    fields[0].trim(),
                    fields[1].trim(),
                    fields[2].trim(),
                    Integer.parseInt(fields[3].trim()),
                    fields[4].trim()
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Ошибка парсинга duration: " + fields[3], e);
        }
    }

    /**
     * Проверка бизнес-правил
     */
    public boolean isValidNumbers() {
        return hasValidPhoneNumber(numberA) && hasValidPhoneNumber(numberB) && !numberA.equals(numberB);
    }

    public boolean hasValidPhoneNumber(String number) {
        if (number == null) return false;
        // E.164: от 10 до 15 цифр, не начинается с 0
        return number.matches("^[1-9]\\d{10,14}$") &&
                number.length() >= 11 && number.length() <= 15;
    }

    public boolean hasValidDuration() {
        return duration != null && duration > 0 && duration <= 86400;
    }

    public boolean hasValidTime() {
        return time != null && time.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$");
    }


    @Override
    public String toString() {
        return String.format("CSVRecord{time='%s', %s->%s, duration=%d, account='%s'}",
                time, numberA, numberB, duration, account);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CSVRecord that = (CSVRecord) obj;
        return Objects.equals(time, that.time) &&
                Objects.equals(numberA, that.numberA) &&
                Objects.equals(numberB, that.numberB) &&
                Objects.equals(duration, that.duration) &&
                Objects.equals(account, that.account);
    }

    @Override
    public int hashCode() {
        return Objects.hash(time, numberA, numberB, duration, account);
    }
}
