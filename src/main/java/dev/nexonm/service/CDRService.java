package dev.nexonm.service;

import dev.nexonm.dto.request.CDRQueryRequest;
import dev.nexonm.dto.response.CDRResponse;
import dev.nexonm.entity.CallDetailRecord;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class CDRService {

    /**
     * Выполняет поиск CDR-записей с динамической фильтрацией, пагинацией и сортировкой.
     * <ul>
     *   <li>Фильтрация по времени и agentId</li>
     *   <li>Пагинация</li>
     *   <li>Сортировка по выбранному полю</li>
     * </ul>
     * @param request параметры запроса (фильтры, пагинация, сортировка)
     * @return список CDR-записей, соответствующих критериям поиска
     */
    public List<CDRResponse> search(CDRQueryRequest request) {
        StringBuilder queryBuilder = new StringBuilder();
        Map<String, Object> parameters = new HashMap<>();
        buildWhereClause(queryBuilder, parameters, request);
        Sort sort = buildSort(request);
        var query = CallDetailRecord.find(queryBuilder.toString(), sort, parameters);
        List<CallDetailRecord> entities = query
                .page(Page.of(request.page(), request.size()))
                .list();
        if (entities.isEmpty()) {
            log.warn("Результат поиска пустой по заданным фильтрам");
        } else {
            log.info("Найдено {} записей на странице {}", entities.size(), request.page());
        }
        return entities.stream()
                .map(CDRResponse::fromEntity)
                .toList();
    }

    /**
     * Формирует WHERE-условие динамически на основе фильтров запроса.
     * <ul>
     *   <li>Добавляет фильтр по времени, если задан</li>
     *   <li>Добавляет фильтр по agentId, если задан</li>
     * </ul>
     * @param queryBuilder билдер SQL-запроса
     * @param parameters параметры для подстановки
     * @param request параметры фильтрации
     */
    private void buildWhereClause(StringBuilder queryBuilder, Map<String, Object> parameters, CDRQueryRequest request) {
        queryBuilder.append("1=1"); // Нужен для упрощения добавления условий
        if (request.hasTimeFilter()) {
            queryBuilder.append(" AND time >= :timeFrom AND time <= :timeTo");
            parameters.put("timeFrom", request.timeFrom());
            parameters.put("timeTo", request.timeTo());
        }
        if (request.hasAgentFilter()) {
            queryBuilder.append(" AND agent.agentId = :agentId");
            parameters.put("agentId", request.agentId());
        }
    }

    /**
     * Формирует объект Sort на основе параметров запроса.
     * @param request параметры сортировки
     * @return объект Sort
     */
    private Sort buildSort(CDRQueryRequest request) {
        String sortField = mapSortField(request.sortBy());
        Sort sort = Sort.by(sortField);
        if (request.isSortDescending()) {
            sort = sort.descending();
        } else {
            sort = sort.ascending();
        }
        return sort;
    }

    /**
     * Маппит имена полей сортировки из DTO в имена полей сущности.
     * @param sortBy поле сортировки из запроса
     * @return имя поля сущности
     */
    private String mapSortField(String sortBy) {
        return switch (sortBy) {
            case "callTimestamp" -> "time";           // Маппинг поля DTO на поле сущности
            case "numberA" -> "numberA";
            case "numberB" -> "numberB";
            case "duration" -> "duration";
            case "agentName" -> "agent.name";        // Сортировка по связанному полю
            default -> "time";                       // Сортировка по умолчанию
        };
    }
}
