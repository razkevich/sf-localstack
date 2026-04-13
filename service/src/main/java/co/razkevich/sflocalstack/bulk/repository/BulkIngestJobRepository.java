package co.razkevich.sflocalstack.bulk.repository;

import co.razkevich.sflocalstack.bulk.model.BulkIngestJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BulkIngestJobRepository extends JpaRepository<BulkIngestJob, String> {
    List<BulkIngestJob> findByOrgId(String orgId);
}
