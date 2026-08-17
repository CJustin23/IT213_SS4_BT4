package com.rikkei.incidentworkflow;

import com.rikkei.incidentworkflow.service.IncidentETLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Tu dong chay 3 kich ban demo khi khoi dong ung dung de tao minh chung log:
 *   (1) CRITICAL + phat canh bao THANH CONG  -> SUCCESS
 *   (2) HIGH + gia lap loi phat canh bao      -> FAILED (du lieu van luu)
 *   (3) LOW  + khong can canh bao             -> NOT_REQUIRED
 */
@Component
public class DemoRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoRunner.class);

    private final IncidentETLService etlService;

    public DemoRunner(IncidentETLService etlService) {
        this.etlService = etlService;
    }

    @Override
    public void run(String... args) {
        log.info("========== DEMO 1: CRITICAL - phat canh bao THANH CONG ==========");
        etlService.processIncident(
                "order=ORD001;plate=29A-11111;urgency=CRITICAL;desc=Xe container mat phanh tren cao toc",
                false);

        log.info("========== DEMO 2: HIGH - gia lap LOI phat canh bao ==========");
        etlService.processIncident(
                "order=ORD002;plate=51B-22222;urgency=HIGH;desc=Chay khoang hang dong lanh",
                true);

        log.info("========== DEMO 3: LOW - khong can canh bao ==========");
        etlService.processIncident(
                "order=ORD003;plate=43C-33333;urgency=LOW;desc=Giao hang cham 15 phut",
                false);
    }
}
