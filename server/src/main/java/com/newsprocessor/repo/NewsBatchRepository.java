package com.newsprocessor.repo;

import com.newsprocessor.domain.NewsBatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsBatchRepository extends JpaRepository<NewsBatch, Long> {
}
