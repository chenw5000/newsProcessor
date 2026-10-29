package com.newsprocessor.repo;

import com.newsprocessor.domain.AppSetting;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {

    List<AppSetting> findByKeyIn(Collection<String> keys);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AppSetting s
            set s.value = 'true', s.updatedAt = CURRENT_TIMESTAMP
            where s.key = 'schedule.run_in_progress'
              and (s.value is null or lower(s.value) = 'false')
            """)
    int tryAcquireRunLock();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AppSetting s
            set s.value = :value, s.updatedAt = CURRENT_TIMESTAMP
            where s.key = :key
            """)
    int updateValue(@Param("key") String key, @Param("value") String value);
}
