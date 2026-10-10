package com.urlshortener.orchestration.repo;

import com.urlshortener.orchestration.domain.WorkflowRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowRunRepository extends JpaRepository<WorkflowRun, String> {

    List<WorkflowRun> findAllByOrderByCreatedAtDesc();
}
