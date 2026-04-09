package co.razkevich.sflocalstack.metadata.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "metadata_resources", uniqueConstraints = @UniqueConstraint(columnNames = {"type", "fullName"}))
public class MetadataResourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String fullName;

    private String fileName;

    private String directoryName;

    private boolean inFolder;

    private boolean metaFile;

    private String label;

    private String suffix;

    @Lob
    private String attributesJson;

    private Instant lastModifiedDate;

    public MetadataResourceEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getDirectoryName() { return directoryName; }
    public void setDirectoryName(String directoryName) { this.directoryName = directoryName; }

    public boolean isInFolder() { return inFolder; }
    public void setInFolder(boolean inFolder) { this.inFolder = inFolder; }

    public boolean isMetaFile() { return metaFile; }
    public void setMetaFile(boolean metaFile) { this.metaFile = metaFile; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getSuffix() { return suffix; }
    public void setSuffix(String suffix) { this.suffix = suffix; }

    public String getAttributesJson() { return attributesJson; }
    public void setAttributesJson(String attributesJson) { this.attributesJson = attributesJson; }

    public Instant getLastModifiedDate() { return lastModifiedDate; }
    public void setLastModifiedDate(Instant lastModifiedDate) { this.lastModifiedDate = lastModifiedDate; }
}
