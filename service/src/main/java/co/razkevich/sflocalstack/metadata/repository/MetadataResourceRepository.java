package co.razkevich.sflocalstack.metadata.repository;

import co.razkevich.sflocalstack.metadata.model.MetadataResourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetadataResourceRepository extends JpaRepository<MetadataResourceEntity, Long> {
    List<MetadataResourceEntity> findByType(String type);
    Optional<MetadataResourceEntity> findByTypeAndFullName(String type, String fullName);
    void deleteByTypeAndFullName(String type, String fullName);
    List<MetadataResourceEntity> findByOrgIdAndType(String orgId, String type);
    Optional<MetadataResourceEntity> findByOrgIdAndTypeAndFullName(String orgId, String type, String fullName);
    List<MetadataResourceEntity> findByOrgId(String orgId);
}
