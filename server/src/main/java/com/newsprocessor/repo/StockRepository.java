package com.newsprocessor.repo;

import com.newsprocessor.domain.Stock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findFirstByTickerIgnoreCase(String ticker);

    @Query("""
            select s.ticker from Stock s
            where s.enabled = true
              and s.ticker is not null
              and trim(s.ticker) <> ''
            order by s.ticker asc
            """)
    List<String> findEnabledTickers();
}
