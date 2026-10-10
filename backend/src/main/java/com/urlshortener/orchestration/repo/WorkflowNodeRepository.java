package com.urlshortener.orchestration.repo;

import com.urlshortener.orchestration.domain.WorkflowNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowNodeRepository extends JpaRepository<WorkflowNode, String> {

    List<WorkflowNode> findByRunId(String runId);

    Optional<WorkflowNode> findByRunIdAndNodeKey(String runId, String nodeKey);
}
