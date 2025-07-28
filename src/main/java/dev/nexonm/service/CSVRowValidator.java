package dev.nexonm.service;

import dev.nexonm.dto.csv.CSVRecord;
import dev.nexonm.dto.response.ValidationError;
import dev.nexonm.mapper.CallDetailRecordMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Validator;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class CSVRowValidator {

    @Inject
    Validator validator;

    /**
     * Валидирует список строк CSV, добавляет ошибки в список.
     * <ul>
     *   <li>Пустые строки пропускаются с ошибкой</li>
     *   <li>Ошибки парсинга и бизнес-валидации добавляются в errors</li>
     *   <li>Проверяется наличие account в справочнике</li>
     *   <li>Проверяется, что numberA и numberB не совпадают</li>
     * </ul>
     * @param allRows список строк из CSV
     * @param errors список ошибок для пополнения
     * @param accounts множество валидных account
     * @param baseRow базовый номер строки (для корректной нумерации)
     */
    public void validate(List<String[]> allRows, List<ValidationError> errors, HashSet<String> accounts, long baseRow) {
        for (int i = 0; i < allRows.size(); i++) {
            String[] row = allRows.get(i);
            if (isEmptyRow(row)) {
                errors.add(new ValidationError(baseRow + i, "Ошибка парсинга: Пустая строка, пропуск"));
                continue;
            }
            try {
                CSVRecord record = CallDetailRecordMapper.mapRowToRecord(row);
                ValidationError error = validateCSVRecord(record, accounts, baseRow + i);
                if (error != null) {
                    errors.add(error);
                }
            } catch (Exception e) {
                // Логируем ошибку парсинга, но продолжаем обработку
                errors.add(new ValidationError(baseRow + i, "Ошибка парсинга: " + e.getMessage()));
            }
        }
    }

    /**
     * Проверяет, пуста ли строка CSV.
     * <p>
     * Строка считается пустой, если все значения null или пустые.
     * @param row строка CSV
     * @return true если строка пуста, иначе false
     */
    private boolean isEmptyRow(String[] row) {
        if (row == null) return true;
        for (String cell : row) {
            if (cell != null && !cell.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Валидирует CSVRecord с помощью Bean Validation и бизнес-правил.
     * <ul>
     *   <li>Проверяет наличие account в справочнике</li>
     *   <li>Проверяет, что номера не совпадают</li>
     *   <li>Собирает сообщения об ошибках</li>
     * </ul>
     * @param record запись для проверки
     * @param accounts множество валидных account
     * @param row номер строки
     * @return ValidationError или null, если ошибок нет
     */
    private ValidationError validateCSVRecord(CSVRecord record, HashSet<String> accounts, long row) {
        var violations = validator.validate(record);
        String errorMessage = violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        if (!accounts.contains(record.getAccount())) {
            errorMessage += "; account: Аккаунт агента не найден";
        }
        if (!record.isValidNumbers()) {
            errorMessage += "numbers; account: Нарушение бизнес-правила: numberA и numberB не могут совпадать";
        }
        if (errorMessage.isBlank()){
            return null;
        }
        return new ValidationError(row, errorMessage);
    }

}