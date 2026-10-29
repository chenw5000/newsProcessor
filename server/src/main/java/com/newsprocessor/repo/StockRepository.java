package com.newsprocessor.repo;

import com.newsprocessor.domain.Stock;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findFirstByTickerIgnoreCase(String ticker);
}
