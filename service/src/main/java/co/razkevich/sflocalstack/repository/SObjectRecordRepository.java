package co.razkevich.sflocalstack.repository;

import co.razkevich.sflocalstack.model.SObjectRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SObjectRecordRepository extends JpaRepository<SObjectRecord, String> {
    List<SObjectRecord> findByObjectType(String objectType);
    void deleteByObjectType(String objectType);
}
