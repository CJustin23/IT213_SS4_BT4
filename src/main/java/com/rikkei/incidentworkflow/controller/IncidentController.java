package com.rikkei.incidentworkflow.controller;

import com.rikkei.incidentworkflow.entity.IncidentReport;
import com.rikkei.incidentworkflow.service.IncidentETLService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint kich hoat workflow su co khan cap.
 * Cho phep truyen simulateFailure=true de kiem chung kien truc chiu loi.
 */
@RestController
@RequestMapping("/api/v1/incident")
public class IncidentController {

    private final IncidentETLService etlService;

    public IncidentController(IncidentETLService etlService) {
        this.etlService = etlService;
    }

    /**
     * @param rawMessage      tin nhan tho tu tai xe (vd:
     *                        "order=ORD123;plate=29A-12345;urgency=CRITICAL;desc=Xe lat tren cao toc")
     * @param simulateFailure mo phong loi phat canh bao (mac dinh false)
     */
    @PostMapping("/report")
    public IncidentReport report(
            @RequestBody String rawMessage,
            @RequestParam(value = "simulateFailure", defaultValue = "false") boolean simulateFailure) {
        return etlService.processIncident(rawMessage, simulateFailure);
    }
}
