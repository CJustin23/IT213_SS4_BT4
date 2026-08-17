package com.rikkei.incidentworkflow.service;

import com.rikkei.incidentworkflow.entity.IncidentReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * ConsoleAlertService - phat "Canh bao do khan cap" ra Console Log bang SLF4J
 * de nhan vien dieu hanh tong kho theo doi.
 *
 * Ho tro mo phong loi (simulateFailure) de kiem chung kien truc chiu loi:
 * khi phat that bai, service nem AlertDispatchException; tang goi (ETL Service)
 * se bat va danh dau notificationStatus = FAILED nhung van giu du lieu su co trong DB.
 */
@Service
public class ConsoleAlertService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleAlertService.class);

    /**
     * Phat canh bao do ra console.
     *
     * @param report          su co can canh bao
     * @param simulateFailure neu true, mo phong "thiet bi phat tin hieu ban" -> nem loi
     */
    public void dispatchRedAlert(IncidentReport report, boolean simulateFailure) {
        if (simulateFailure) {
            // Mo phong loi thiet bi phat tin hieu ban truoc khi in canh bao
            throw new AlertDispatchException(
                    "Thiet bi phat tin hieu dang ban - khong the phat canh bao cho don "
                            + report.getOrderCode());
        }

        // Khung canh bao noi bat tren console
        String banner = System.lineSeparator()
                + "==================== 🔴 CANH BAO DO KHAN CAP 🔴 ====================" + System.lineSeparator()
                + "  Muc do khan cap : " + report.getUrgencyLevel() + System.lineSeparator()
                + "  Ma don hang     : " + report.getOrderCode() + System.lineSeparator()
                + "  Bien so xe      : " + report.getVehiclePlate() + System.lineSeparator()
                + "  Thoi gian       : " + report.getIncidentTime() + System.lineSeparator()
                + "  Mo ta su co     : " + report.getDescription() + System.lineSeparator()
                + "  >> YEU CAU NHAN VIEN DIEU HANH XU LY NGAY <<" + System.lineSeparator()
                + "====================================================================";

        log.error(banner);
    }
}
