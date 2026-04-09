package co.razkevich.sflocalstack.repository;

import co.razkevich.sflocalstack.model.BulkBatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BulkBatchRepository extends JpaRepository<BulkBatchEntity, Long> {
    List<BulkBatchEntity> findByJobIdOrderBySequenceNumber(String jobId);
    void deleteByJobId(String jobId);
}
