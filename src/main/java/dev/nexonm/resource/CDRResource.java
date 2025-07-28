package dev.nexonm.resource;

import dev.nexonm.dto.request.CDRQueryRequest;
import dev.nexonm.dto.response.ETLReport;
import dev.nexonm.service.CDRService;
import dev.nexonm.service.ETLService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("api/cdr")
public class CDRResource {

    @Inject
    ETLService etlService;
    @Inject
    CDRService cdrService;

    /**
     * Запускает процесс ETL: загружает и обрабатывает CSV-файл по указанному пути.
     * <p>
     * <b>Пример запроса:</b>
     * <pre>
     *   GET /api/cdr/input?path=data/2025-07-26.csv
     * </pre>
     * </p>
     * @param path путь к CSV-файлу (относительный или абсолютный)
     * @return ETLReport с результатами загрузки и валидации
     */
    @GET
    @Path("input")
    public Response input(@QueryParam("path") String path) {
        try{
            ETLReport response = etlService.processCSVFile(path);
            return Response.ok(response).build();
        }catch (Exception e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Ошибка при обработке файла: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Получает список CDR-записей с поддержкой фильтрации, пагинации и сортировки.
     * <p>
     * <b>Пример запроса:</b>
     * <pre>
     *   GET /api/cdr?agentId=1&timeFrom=2025-07-26T00:00:00&timeTo=2025-07-27T23:59:59&page=0&size=10&sortBy=callTimestamp&sortDesc=true
     * </pre>
     * </p>
     * <ul>
     *   <li><b>agentId</b> — фильтр по агенту</li>
     *   <li><b>timeFrom, timeTo</b> — диапазон времени</li>
     *   <li><b>page, size</b> — пагинация</li>
     *   <li><b>sortBy</b> — поле сортировки (callTimestamp, numberA, numberB, duration, agentName)</li>
     *   <li><b>sortDesc</b> — сортировка по убыванию</li>
     * </ul>
     * @param request параметры фильтрации и пагинации (BeanParam)
     * @return JSON-массив CDR-записей
     */
    @GET
    public Response getRecords(@BeanParam @Valid CDRQueryRequest request){
        log.info("Request for retrieve: {}", request.getFiltersDescription());
        try {
            var records = cdrService.search(request);
            return Response.ok(records).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Ошибка при получении записей: " + e.getMessage())
                    .build();
        }
    }
}
