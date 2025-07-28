package dev.nexonm.dto.request;

import jakarta.validation.constraints.*;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import java.time.LocalDateTime;

/**
 * <p><b>DTO для запроса данных CDR через REST API</b></p>
 * <ul>
 *   <li>Фильтрация по диапазону времени: <code>timeFrom</code>, <code>timeTo</code></li>
 *   <li>Фильтрация по агенту: <code>agentId</code></li>
 *   <li>Пагинация: <code>page</code>, <code>size</code></li>
 *   <li>Сортировка: <code>sortBy</code>, <code>sortDirection</code></li>
 * </ul>
 * <p><b>Пример запроса:</b></p>
 * <pre>
 * GET /api/cdr?timeFrom=2024-01-01T00:00:00&amp;timeTo=2024-01-31T23:59:59&amp;agentId=1&amp;page=0&amp;size=20&amp;sortBy=callTimestamp&amp;sortDirection=DESC
 * </pre>
 */
public record CDRQueryRequest(

        /**
         * Начало временного диапазона для фильтрации
         * Формат: ISO LocalDateTime (2024-01-15T10:30:00)
         */
        @QueryParam("timeFrom")
        LocalDateTime timeFrom,

        /**
         * Конец временного диапазона для фильтрации
         * Формат: ISO LocalDateTime (2024-01-15T23:59:59)
         */
        @QueryParam("timeTo")
        LocalDateTime timeTo,

        /**
         * ID агента для фильтрации записей
         * Должен быть положительным числом
         */
        @QueryParam("agentId")
        @Positive(message = "ID агента должен быть положительным числом")
        Long agentId,

        /**
         * Номер страницы (начинается с 0)
         * По умолчанию: 0
         */
        @QueryParam("page")
        @DefaultValue("0")
        @Min(value = 0, message = "Номер страницы не может быть отрицательным")
        Integer page,

        /**
         * Размер страницы (количество записей)
         * По умолчанию: 20, максимум: 100
         */
        @QueryParam("size")
        @DefaultValue("100")
        @Min(value = 1, message = "Размер страницы должен быть больше нуля")
        @Max(value = 1000, message = "Размер страницы не может превышать 1000")
        Integer size,

        /**
         * Поле для сортировки
         * Доступные значения: callTimestamp, numberA, numberB, duration, agentName
         * По умолчанию: callTimestamp
         */
        @QueryParam("sortBy")
        @DefaultValue("callTimestamp")
        @Pattern(regexp = "^(callTimestamp|numberA|numberB|duration|agentName)$",
                message = "Доступные поля для сортировки: callTimestamp, numberA, numberB, duration, agentName")
        String sortBy,

        /**
         * Направление сортировки
         * Доступные значения: ASC, DESC
         * По умолчанию: DESC
         */
        @QueryParam("sortDirection")
        @DefaultValue("ASC")
        @Pattern(regexp = "^(ASC|DESC)$",
                message = "Направление сортировки должно быть ASC или DESC")
        String sortDirection

) {
    /**
     * Компактный конструктор для дополнительной валидации и нормализации
     * Вызывается автоматически при создании record
     */
    public CDRQueryRequest {
        if (timeFrom != null && timeTo != null && timeFrom.isAfter(timeTo)) {
            throw new IllegalArgumentException(
                    "Дата начала не может быть позже даты окончания");
        }

        // Проверка максимального диапазона
        if (timeFrom != null && timeTo != null &&
                timeFrom.plusYears(1).isBefore(timeTo)) {
            throw new IllegalArgumentException(
                    "Временной диапазон не может превышать один год");
        }
    }

    // Методы для работы с фильтрами

    /**
     * Проверяет есть ли фильтры в запросе
     * @return true если есть хотя бы один фильтр
     */
    public boolean hasFilters() {
        return hasTimeFilter() || hasAgentFilter();
    }

    /**
     * Проверяет есть ли временной фильтр
     * @return true если указаны timeFrom и timeTo
     */
    public boolean hasTimeFilter() {
        return timeFrom != null && timeTo != null;
    }

    /**
     * Проверяет есть ли фильтр по агенту
     * @return true если указан agentId
     */
    public boolean hasAgentFilter() {
        return agentId != null;
    }

    /**
     * Возвращает направление сортировки как boolean
     * @return true для DESC, false для ASC
     */
    public boolean isSortDescending() {
        return "DESC".equals(sortDirection);
    }

    /**
     * Возвращает offset для SQL запросов
     * @return количество записей для пропуска
     */
    public int getOffset() {
        return page * size;
    }

    /**
     * Проверяет валидность всех параметров
     * @return true если все параметры корректны
     */
    public boolean isValid() {
        try {
            return isValidSortField() && isValidSortDirection();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Проверяет корректность поля сортировки
     * @return true если поле допустимо
     */
    public boolean isValidSortField() {
        if (sortBy == null) return false;
        return sortBy.matches("^(callTimestamp|numberA|numberB|duration|agentName)$");
    }

    /**
     * Проверяет корректность направления сортировки
     * @return true если направление допустимо
     */
    public boolean isValidSortDirection() {
        if (sortDirection == null) return false;
        return sortDirection.matches("^(ASC|DESC)$");
    }

    // Helper методы

    /**
     * Возвращает строковое представление фильтров для логирования
     * @return строка с активными фильтрами
     */
    public String getFiltersDescription() {
        StringBuilder sb = new StringBuilder();
        if (hasTimeFilter()) {
            sb.append(String.format("time[%s to %s]", timeFrom, timeTo));
        }
        if (hasAgentFilter()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(String.format("agent[%d]", agentId));
        }
        if (sb.isEmpty()) {
            sb.append("no filters");
        }
        sb.append(String.format(", page[%d/%d], sort[%s %s]",
                page, size, sortBy, sortDirection));
        return sb.toString();
    }
}

