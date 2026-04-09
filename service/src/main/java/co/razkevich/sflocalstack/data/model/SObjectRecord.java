package co.razkevich.sflocalstack.data.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "sobject_records")
public class SObjectRecord {

    @Id
    private String id;

    private String objectType;

    @Column(columnDefinition = "TEXT")
    private String fieldsJson;

    private Instant createdDate;

    private Instant lastModifiedDate;

    public SObjectRecord() {}

    public SObjectRecord(String id, String objectType, String fieldsJson, Instant createdDate, Instant lastModifiedDate) {
        this.id = id;
        this.objectType = objectType;
        this.fieldsJson = fieldsJson;
        this.createdDate = createdDate;
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getObjectType() { return objectType; }
    public void setObjectType(String objectType) { this.objectType = objectType; }

    public String getFieldsJson() { return fieldsJson; }
    public void setFieldsJson(String fieldsJson) { this.fieldsJson = fieldsJson; }

    public Instant getCreatedDate() { return createdDate; }
    public void setCreatedDate(Instant createdDate) { this.createdDate = createdDate; }

    public Instant getLastModifiedDate() { return lastModifiedDate; }
    public void setLastModifiedDate(Instant lastModifiedDate) { this.lastModifiedDate = lastModifiedDate; }
}
