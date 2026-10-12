package com.stocker.repository;

import com.stocker.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findBySymbolIgnoreCase(String symbol);

    @Query("select s from Stock s " +
           "where lower(s.symbol) like lower(concat('%', :kw, '%')) " +
           "or lower(s.companyName) like lower(concat('%', :kw, '%')) " +
           "order by s.symbol")
    List<Stock> search(@Param("kw") String keyword);
}
