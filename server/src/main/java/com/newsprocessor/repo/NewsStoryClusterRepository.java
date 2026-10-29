package com.newsprocessor.repo;

import com.newsprocessor.domain.NewsStoryCluster;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsStoryClusterRepository extends JpaRepository<NewsStoryCluster, Long> {
}
