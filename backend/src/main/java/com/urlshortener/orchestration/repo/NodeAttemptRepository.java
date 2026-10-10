package com.urlshortener.orchestration.repo;

import com.urlshortener.orchestration.domain.NodeAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NodeAttemptRepository extends JpaRepository<NodeAttempt, Long> {

    List<NodeAttempt> findByNodeIdOrderByAttemptNumberAsc(String nodeId);
}
