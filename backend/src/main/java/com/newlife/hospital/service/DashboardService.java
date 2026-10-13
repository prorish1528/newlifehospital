package com.newlife.hospital.service;

import com.newlife.hospital.config.AppProperties;
import com.newlife.hospital.repository.InvoiceRepository;
import com.newlife.hospital.web.dto.AdminDtos.Dashboard;
import com.newlife.hospital.web.dto.AdminDtos.RevenuePoint;
import com.newlife.hospital.web.dto.AdminDtos.Totals;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revenue metrics. "Billed" is the net invoice value; "collected" is the amount paid on invoices raised in the
 * period. Daily and monthly series are zero-filled so charts have no gaps.
 */
@Service
public class DashboardService {

    private final InvoiceRepository invoices;
    private final AppProperties props;

    public DashboardService(InvoiceRepository invoices, AppProperties props) {
        this.invoices = invoices;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public Dashboard dashboard() {
        LocalDate today = LocalDate.now(props.zone());
        Totals day = totals(today, today);
        Totals month = totals(today.withDayOfMonth(1), today);
        Totals year = totals(today.withDayOfYear(1), today);

        LocalDate dailyFrom = today.minusDays(29);
        Map<LocalDate, RevenuePoint> byDay = new HashMap<>();
        for (Object[] r : invoices.dailyBetween(dailyFrom, today)) {
            LocalDate d = (LocalDate) r[0];
            byDay.put(d, new RevenuePoint(d.toString(), num(r[1]).longValue(), dec(r[2]), dec(r[3])));
        }
        List<RevenuePoint> daily = new ArrayList<>();
        for (LocalDate d = dailyFrom; !d.isAfter(today); d = d.plusDays(1)) {
            daily.add(byDay.getOrDefault(d, empty(d.toString())));
        }

        YearMonth current = YearMonth.from(today);
        YearMonth monthlyFrom = current.minusMonths(11);
        Map<YearMonth, RevenuePoint> byMonth = new HashMap<>();
        for (Object[] r : invoices.monthlyBetween(monthlyFrom.atDay(1), today)) {
            YearMonth ym = YearMonth.of(num(r[0]).intValue(), num(r[1]).intValue());
            byMonth.put(ym, new RevenuePoint(ym.toString(), num(r[2]).longValue(), dec(r[3]), dec(r[4])));
        }
        List<RevenuePoint> monthly = new ArrayList<>();
        for (YearMonth ym = monthlyFrom; !ym.isAfter(current); ym = ym.plusMonths(1)) {
            monthly.add(byMonth.getOrDefault(ym, empty(ym.toString())));
        }

        List<RevenuePoint> yearly = invoices.yearly().stream()
                .map(r -> new RevenuePoint(String.valueOf(num(r[0]).intValue()), num(r[1]).longValue(), dec(r[2]), dec(r[3])))
                .toList();

        return new Dashboard(day, month, year, daily, monthly, yearly);
    }

    private Totals totals(LocalDate from, LocalDate to) {
        Object[] r = invoices.totalsBetween(from, to).get(0);
        return new Totals(num(r[0]).longValue(), dec(r[1]), dec(r[2]), dec(r[3]));
    }

    private static RevenuePoint empty(String label) {
        return new RevenuePoint(label, 0, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private static Number num(Object o) {
        return o == null ? 0 : (Number) o;
    }

    private static BigDecimal dec(Object o) {
        if (o == null) {
            return BigDecimal.ZERO;
        }
        return o instanceof BigDecimal b ? b : new BigDecimal(o.toString());
    }
}
