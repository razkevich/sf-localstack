package co.razkevich.sflocalstack.repository;

import co.razkevich.sflocalstack.model.MetadataDeployJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MetadataDeployJobRepository extends JpaRepository<MetadataDeployJobEntity, String> {
}
