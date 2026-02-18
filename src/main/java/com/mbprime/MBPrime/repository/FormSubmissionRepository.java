package com.mbprime.MBPrime.repository;

import com.mbprime.MBPrime.entity.FormSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FormSubmissionRepository extends JpaRepository<FormSubmission, Long> {
    List<FormSubmission> findByFormTypeOrderByCreatedAtDesc(String formType);
    List<FormSubmission> findAllByOrderByCreatedAtDesc();
}
