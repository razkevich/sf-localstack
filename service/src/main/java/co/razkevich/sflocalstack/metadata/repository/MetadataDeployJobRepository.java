package co.razkevich.sflocalstack.metadata.repository;

import co.razkevich.sflocalstack.metadata.model.MetadataDeployJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MetadataDeployJobRepository extends JpaRepository<MetadataDeployJobEntity, String> {
    List<MetadataDeployJobEntity> findByOrgId(String orgId);
}
