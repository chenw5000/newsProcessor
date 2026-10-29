package com.newsprocessor.repo;

import com.newsprocessor.domain.NewsItem;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsItemRepository extends JpaRepository<NewsItem, Long> {

    boolean existsByHeadlineFingerprint(String headlineFingerprint);

    Optional<NewsItem> findFirstByHeadlineFingerprint(String headlineFingerprint);

    @Query("""
            select ni from NewsItem ni
            where ni.tickerId = :tickerId
              and ni.createdAt >= :since
            order by ni.createdAt desc
            """)
    List<NewsItem> findRecentByTickerId(
            @Param("tickerId") Integer tickerId,
            @Param("since") Instant since);

    List<NewsItem> findByBatchIdIn(Collection<Long> batchIds);

    @Query("""
            select ni from NewsItem ni
            where ni.tickerId = :tickerId
              and (
                    ni.createdAt >= :since
                 or ni.batchId in :batchIds
              )
            order by ni.createdAt desc
            """)
    List<NewsItem> findExclusionCandidates(
            @Param("tickerId") Integer tickerId,
            @Param("since") Instant since,
            @Param("batchIds") Collection<Long> batchIds);

    List<NewsItem> findByStoryClusterId(Long storyClusterId);
}
