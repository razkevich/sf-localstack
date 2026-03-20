package co.prodly.sflocalstack.service;

import co.prodly.sflocalstack.model.MetadataCatalogEntry;
import co.prodly.sflocalstack.model.MetadataDeployJob;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MetadataSoapRenderer {

    public String renderDescribeMetadata(List<MetadataCatalogEntry> entries) {
        String metadataObjects = entries.stream()
                .map(entry -> """
                        <metadataObjects>
                          <xmlName>%s</xmlName>
                          <directoryName>%s</directoryName>
                          <inFolder>%s</inFolder>
                          <metaFile>%s</metaFile>
                        </metadataObjects>
                        """.formatted(entry.type(), entry.directoryName(), entry.inFolder(), entry.metaFile()))
                .distinct()
                .reduce("", String::concat);
        return envelope("describeMetadataResponse", """
                <result>
                  %s
                  <organizationNamespace></organizationNamespace>
                  <partialSaveAllowed>false</partialSaveAllowed>
                  <testRequired>false</testRequired>
                </result>
                """.formatted(metadataObjects));
    }

    public String renderListMetadata(List<MetadataCatalogEntry> entries) {
        String results = entries.stream()
                .map(entry -> """
                        <result>
                          <createdByName>sf-localstack</createdByName>
                          <createdDate>%s</createdDate>
                          <fileName>%s</fileName>
                          <fullName>%s</fullName>
                          <lastModifiedByName>sf-localstack</lastModifiedByName>
                          <lastModifiedDate>%s</lastModifiedDate>
                          <manageableState>unmanaged</manageableState>
                          <type>%s</type>
                        </result>
                        """.formatted(entry.lastModifiedDate(), entry.fileName(), entry.fullName(), entry.lastModifiedDate(), entry.type()))
                .reduce("", String::concat);
        return envelope("listMetadataResponse", results);
    }

    public String renderReadMetadata(String type, List<MetadataService.ReadMetadataRecord> records) {
        String result = records.stream()
                .map(record -> renderReadRecord(type, record))
                .reduce("", String::concat);
        return envelope("readMetadataResponse", "<result>" + result + "</result>");
    }

    public String renderDeploy(MetadataDeployJob job) {
        return envelope("deployResponse", "<result><id>%s</id><done>false</done><stateDetail>Queued</stateDetail></result>".formatted(job.id()));
    }

    public String renderCheckDeployStatus(MetadataDeployJob job) {
        return envelope("checkDeployStatusResponse", """
                <result>
                  <id>%s</id>
                  <done>%s</done>
                  <success>%s</success>
                  <status>%s</status>
                  <numberComponentsTotal>%d</numberComponentsTotal>
                  <numberComponentsDeployed>%d</numberComponentsDeployed>
                  <numberComponentErrors>%d</numberComponentErrors>
                  <details/>
                </result>
                """.formatted(job.id(), job.done(), job.success(), job.status(), job.numberComponentsTotal(), job.numberComponentsDeployed(), job.numberComponentErrors()));
    }

    public String renderCancelDeploy(MetadataDeployJob job) {
        return envelope("cancelDeployResponse", "<result><id>%s</id><done>true</done><success>true</success></result>".formatted(job.id()));
    }

    public String renderFault(String code, String message) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                  <soapenv:Body>
                    <soapenv:Fault>
                      <faultcode>%s</faultcode>
                      <faultstring>%s</faultstring>
                    </soapenv:Fault>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(code, escape(message));
    }

    private String renderReadRecord(String type, MetadataService.ReadMetadataRecord record) {
        String fieldType = String.valueOf(record.attributes().getOrDefault("fieldType", "Text"));
        String body = switch (type) {
            case "CustomField" -> "<fullName>%s</fullName><label>%s</label><type>Text</type><valueSet><restricted>false</restricted></valueSet>"
                    .formatted(record.fullName(), record.label()).replace("Text", fieldType);
            case "StandardValueSet" -> "<fullName>%s</fullName>%s"
                    .formatted(record.fullName(), valueEntries(record, "standardValue"));
            case "GlobalValueSet" -> "<fullName>%s</fullName>%s"
                    .formatted(record.fullName(), valueEntries(record, "customValue"));
            default -> "<fullName>%s</fullName><label>%s</label>".formatted(record.fullName(), record.label());
        };
        return "<records xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"%s\">%s</records>".formatted(type, body);
    }

    @SuppressWarnings("unchecked")
    private String valueEntries(MetadataService.ReadMetadataRecord record, String tagName) {
        Object values = record.attributes().get("values");
        if (!(values instanceof List<?> list) || list.isEmpty()) {
            return "<%s><fullName>%s</fullName><default>false</default><label>%s</label></%s>"
                    .formatted(tagName, record.label(), record.label(), tagName);
        }
        return list.stream()
                .map(String::valueOf)
                .map(value -> "<%s><fullName>%s</fullName><default>false</default><label>%s</label></%s>"
                        .formatted(tagName, value, value, tagName))
                .reduce("", String::concat);
    }

    private String envelope(String operation, String body) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns="http://soap.sforce.com/2006/04/metadata">
                  <soapenv:Body>
                    <%s>
                      %s
                    </%s>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(operation, body, operation);
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
