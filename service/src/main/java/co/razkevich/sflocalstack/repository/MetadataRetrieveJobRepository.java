package co.razkevich.sflocalstack.repository;

import co.razkevich.sflocalstack.model.MetadataRetrieveJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MetadataRetrieveJobRepository extends JpaRepository<MetadataRetrieveJobEntity, String> {
}
