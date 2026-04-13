package co.razkevich.sflocalstack.metadata.repository;

import co.razkevich.sflocalstack.metadata.model.MetadataRetrieveJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MetadataRetrieveJobRepository extends JpaRepository<MetadataRetrieveJobEntity, String> {
    List<MetadataRetrieveJobEntity> findByOrgId(String orgId);
}
