package co.razkevich.sflocalstack.repository;

import co.razkevich.sflocalstack.model.BulkIngestJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BulkIngestJobRepository extends JpaRepository<BulkIngestJob, String> {
}
