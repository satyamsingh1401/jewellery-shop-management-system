package com.myhourly.jewellery.reporting.service;

import com.myhourly.jewellery.reporting.dto.DailySalesReportResponse;
import com.myhourly.jewellery.reporting.dto.SalesRangeReportResponse;
import com.myhourly.jewellery.reporting.mapper.ReportMapper;
import com.myhourly.jewellery.reporting.model.DailySalesFigures;
import com.myhourly.jewellery.reporting.model.ReportPeriod;
import com.myhourly.jewellery.reporting.model.SalesFigures;
import com.myhourly.jewellery.reporting.repository.SalesReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SalesReportService {

    private final SalesReportRepository repository;
    private final ReportRequestValidator validator;
    private final ReportMapper mapper;

    public SalesReportService(SalesReportRepository repository, ReportRequestValidator validator, ReportMapper mapper) {
        this.repository = repository;
        this.validator = validator;
        this.mapper = mapper;
    }

    public DailySalesReportResponse dailySales(LocalDate date) {
        ReportPeriod period = validator.day(date);
        return mapper.toDaily(fillDays(period).getFirst());
    }

    public SalesRangeReportResponse salesRange(LocalDate from, LocalDate to) {
        ReportPeriod period = validator.range(from, to);
        List<DailySalesFigures> days = fillDays(period);
        SalesFigures totals = days.stream()
                .map(DailySalesFigures::figures)
                .reduce(SalesFigures.ZERO, SalesFigures::plus);
        return new SalesRangeReportResponse(
                period.from(),
                period.to(),
                mapper.toSummary(totals),
                days.stream().map(mapper::toDaily).toList()
        );
    }

    private List<DailySalesFigures> fillDays(ReportPeriod period) {
        Map<LocalDate, SalesFigures> found = repository.findDailySalesFigures(period).stream()
                .collect(Collectors.toMap(DailySalesFigures::date, DailySalesFigures::figures, SalesFigures::plus));
        return period.days().stream()
                .map(day -> new DailySalesFigures(day, found.getOrDefault(day, SalesFigures.ZERO)))
                .toList();
    }
}
