package co.razkevich.sflocalstack.repository;

import co.razkevich.sflocalstack.model.BulkRowResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BulkRowResultRepository extends JpaRepository<BulkRowResultEntity, Long> {
    List<BulkRowResultEntity> findByJobIdAndResultType(String jobId, String resultType);
    void deleteByJobId(String jobId);
}
