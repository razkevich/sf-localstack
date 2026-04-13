package co.razkevich.sflocalstack.data.repository;

import co.razkevich.sflocalstack.data.model.SObjectRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SObjectRecordRepository extends JpaRepository<SObjectRecord, String> {
    List<SObjectRecord> findByObjectType(String objectType);
    void deleteByObjectType(String objectType);
    List<SObjectRecord> findByOrgIdAndObjectType(String orgId, String objectType);
    List<SObjectRecord> findByOrgId(String orgId);
    void deleteByOrgId(String orgId);
    void deleteByOrgIdAndObjectType(String orgId, String objectType);
}
