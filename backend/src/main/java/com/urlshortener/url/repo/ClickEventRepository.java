package com.urlshortener.url.repo;

import com.urlshortener.url.domain.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    long countByCode(String code);

    @Query("select e from ClickEvent e where e.code = :code and e.occurredAt >= :from order by e.occurredAt asc")
    List<ClickEvent> findSince(@Param("code") String code, @Param("from") Instant from);
}
