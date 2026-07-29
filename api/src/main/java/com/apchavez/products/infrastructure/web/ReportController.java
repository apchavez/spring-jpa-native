package com.apchavez.products.infrastructure.web;

import com.apchavez.products.application.ReportApplicationService;
import com.apchavez.products.infrastructure.mapper.ReportMapper;
import com.apchavez.products.infrastructure.web.dto.InventorySummaryDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Reportes agregados de inventario")
public class ReportController {

    private final ReportApplicationService reportService;
    private final ReportMapper mapper;

    public ReportController(ReportApplicationService reportService, ReportMapper mapper) {
        this.reportService = reportService;
        this.mapper = mapper;
    }

    @GetMapping("/inventory-summary")
    @Operation(summary = "Resumen de inventario", description = "Totales de productos activos/inactivos, "
            + "stock y valor de inventario (solo productos activos), con desglose por categoría. "
            + "Pensado para verificación end-to-end de la app móvil de inventario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen de inventario",
                    content = @Content(schema = @Schema(implementation = InventorySummaryDTO.class)))
    })
    public InventorySummaryDTO inventorySummary() {
        return mapper.toDTO(reportService.inventorySummary());
    }
}
