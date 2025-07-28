package dev.nexonm.resource;

import dev.nexonm.service.TestDataGeneratorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.util.Map;

@Path("/api/test-data")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class TestGenerationResource {

    @Inject
    TestDataGeneratorService service;


    /**
     * <p>Генерация тестовых данных для звонков и запись в CSV файл.
     * <p>Пример запроса:
     * GET /api/test-data/generate?date=2025-07-21&count=1000
     * <p> Curl запрос: curl "http://localhost:8080/api/test-data/generate?count=500"
     */
    @GET
    @Path("/generate")
    public Response generateTestData(
            @QueryParam("date")
            @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Дата в формате YYYY-MM-DD")
            String date,
            @QueryParam("count") Integer count
    ) {
        try {
            date = date != null ? date : LocalDate.now().toString();
            String filePath = service.generateCSVFile(LocalDate.parse(date), count);
            return Response.ok("Тестовые данные успешно сгенерированы и сохранены в: " + filePath).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Ошибка при генерации тестовых данных: " + e.getMessage())
                    .build();
        }
    }

    /**
     * <p>Очистка всех сгенерированных файлов. Используется GET для простоты работы с программой.
     * <p>Пример запроса:
     * GET /api/test-data/cleanup
     */
    @GET
    @Path("/cleanup")
    public Response cleanupFiles() {
        try {
            service.cleanupGeneratedFiles();
            return Response.ok(Map.of(
                    "status", "success",
                    "message", "Все сгенерированные файлы удалены"
            )).build();
        } catch (Exception e) {
            return Response.status(500)
                    .entity(Map.of("error", "Ошибка очистки: " + e.getMessage()))
                    .build();
        }
    }
}
