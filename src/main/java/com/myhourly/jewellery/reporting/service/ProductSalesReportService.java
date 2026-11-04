package com.myhourly.jewellery.reporting.service;

import com.myhourly.jewellery.reporting.dto.ProductSalesReportResponse;
import com.myhourly.jewellery.reporting.dto.ProductSalesRowResponse;
import com.myhourly.jewellery.reporting.mapper.ReportMapper;
import com.myhourly.jewellery.reporting.model.ProductSalesFigures;
import com.myhourly.jewellery.reporting.model.ReportPeriod;
import com.myhourly.jewellery.reporting.repository.SalesReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

@Service
@Transactional(readOnly = true)
public class ProductSalesReportService {

    private final SalesReportRepository repository;
    private final ReportRequestValidator validator;
    private final ReportMapper mapper;

    public ProductSalesReportService(SalesReportRepository repository, ReportRequestValidator validator,
                                     ReportMapper mapper) {
        this.repository = repository;
        this.validator = validator;
        this.mapper = mapper;
    }

    public ProductSalesReportResponse topProducts(LocalDate from, LocalDate to, Integer limit) {
        ReportPeriod period = validator.range(from, to);
        int resolvedLimit = validator.limit(limit);
        List<ProductSalesFigures> figures = repository.findTopProductSales(period, resolvedLimit);
        List<ProductSalesRowResponse> rows = IntStream.range(0, figures.size())
                .mapToObj(index -> mapper.toProductRow(index + 1, figures.get(index)))
                .toList();
        return new ProductSalesReportResponse(period.from(), period.to(), resolvedLimit, rows);
    }
}
