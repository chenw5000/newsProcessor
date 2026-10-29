package com.newsprocessor.repo;

import com.newsprocessor.domain.NewsBatch;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsBatchRepository extends JpaRepository<NewsBatch, Long> {

    @Query("""
            select b.id from NewsBatch b
            where b.tickerId = :tickerId
            order by b.id desc
            """)
    List<Long> findIdsByTickerIdOrderByIdDesc(
            @Param("tickerId") Integer tickerId,
            Pageable pageable);
}
