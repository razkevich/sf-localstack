package co.razkevich.sflocalstack.bulk.repository;

import co.razkevich.sflocalstack.bulk.model.BulkIngestJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BulkIngestJobRepository extends JpaRepository<BulkIngestJob, String> {
}
