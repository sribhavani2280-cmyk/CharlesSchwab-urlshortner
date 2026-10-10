package com.urlshortener.orchestration.repo;

import com.urlshortener.orchestration.domain.Approval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {

    List<Approval> findByRunIdOrderByDecidedAtAsc(String runId);
}
