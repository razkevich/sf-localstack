package co.razkevich.sflocalstack.service;

import co.razkevich.sflocalstack.model.MetadataResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds a deterministic Salesforce-style metadata retrieve ZIP from matched resources.
 */
@Service
public class MetadataZipService {

    /**
     * Matches requested types/members against available resources, builds a ZIP,
     * and returns the base64-encoded result.
     */
    public String buildZip(List<MetadataManifestParser.TypeRequest> typeRequests,
                           List<MetadataResource> availableResources) {
        List<MetadataResource> matched = matchResources(typeRequests, availableResources);
        return encodeZip(matched, typeRequests);
    }

    private List<MetadataResource> matchResources(List<MetadataManifestParser.TypeRequest> typeRequests,
                                                  List<MetadataResource> available) {
        List<MetadataResource> matched = new ArrayList<>();
        for (MetadataManifestParser.TypeRequest request : typeRequests) {
            boolean wildcard = request.members().contains("*");
            for (MetadataResource resource : available) {
                if (!resource.type().equals(request.type())) {
                    continue;
                }
                if (wildcard || request.members().contains(resource.fullName())) {
                    matched.add(resource);
                }
            }
        }
        return matched;
    }

    private String encodeZip(List<MetadataResource> resources,
                             List<MetadataManifestParser.TypeRequest> typeRequests) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zip = new ZipOutputStream(baos);

            zip.putNextEntry(new ZipEntry("unpackaged/package.xml"));
            zip.write(buildPackageXml(typeRequests, resources).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            // Deduplicate by fileName; CustomObject takes precedence over CustomField for same file
            Map<String, MetadataResource> byFileName = new LinkedHashMap<>();
            for (MetadataResource resource : resources) {
                byFileName.merge(resource.fileName(), resource, (existing, incoming) ->
                        "CustomObject".equals(incoming.type()) ? incoming : existing);
            }

            for (Map.Entry<String, MetadataResource> entry : byFileName.entrySet()) {
                zip.putNextEntry(new ZipEntry("unpackaged/" + entry.getKey()));
                zip.write(buildResourceXml(entry.getValue()).getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }

            zip.finish();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build retrieve ZIP", ex);
        }
    }

    private String buildPackageXml(List<MetadataManifestParser.TypeRequest> typeRequests,
                                   List<MetadataResource> matched) {
        StringBuilder types = new StringBuilder();
        for (MetadataManifestParser.TypeRequest request : typeRequests) {
            List<String> matchedNames = matched.stream()
                    .filter(r -> r.type().equals(request.type()))
                    .map(MetadataResource::fullName)
                    .toList();
            if (matchedNames.isEmpty()) {
                continue;
            }
            types.append("  <types>\n");
            for (String name : matchedNames) {
                types.append("    <members>").append(name).append("</members>\n");
            }
            types.append("    <name>").append(request.type()).append("</name>\n");
            types.append("  </types>\n");
        }
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Package xmlns="http://soap.sforce.com/2006/04/metadata">
                %s  <version>60.0</version>
                </Package>
                """.formatted(types);
    }

    @SuppressWarnings("unchecked")
    private String buildResourceXml(MetadataResource resource) {
        return switch (resource.type()) {
            case "CustomObject" -> {
                List<Map<String, Object>> fields = (List<Map<String, Object>>) resource.attributes().getOrDefault("fields", List.of());
                StringBuilder sb = new StringBuilder();
                sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                sb.append("<CustomObject xmlns=\"http://soap.sforce.com/2006/04/metadata\">\n");
                for (Map<String, Object> field : fields) {
                    sb.append("  <fields>\n");
                    sb.append("    <fullName>").append(field.get("fullName")).append("</fullName>\n");
                    sb.append("    <label>").append(field.get("label")).append("</label>\n");
                    sb.append("    <type>").append(field.get("type")).append("</type>\n");
                    sb.append("  </fields>\n");
                }
                sb.append("</CustomObject>\n");
                yield sb.toString();
            }
            case "CustomField" -> {
                // SF CLI expects <CustomObject> wrapping <fields> children
                String fieldName = resource.fullName().contains(".")
                        ? resource.fullName().substring(resource.fullName().indexOf('.') + 1)
                        : resource.fullName();
                String fieldType = (String) resource.attributes().getOrDefault("fieldType", "Text");
                yield """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <CustomObject xmlns="http://soap.sforce.com/2006/04/metadata">
                          <fields>
                            <fullName>%s</fullName>
                            <label>%s</label>
                            <type>%s</type>
                          </fields>
                        </CustomObject>
                        """.formatted(fieldName, resource.label(), fieldType);
            }
            case "StandardValueSet" -> {
                List<String> values = (List<String>) resource.attributes().getOrDefault("values", List.of());
                StringBuilder sb = new StringBuilder();
                sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                sb.append("<StandardValueSet xmlns=\"http://soap.sforce.com/2006/04/metadata\">\n");
                for (String value : values) {
                    sb.append("  <standardValue>\n");
                    sb.append("    <fullName>").append(value).append("</fullName>\n");
                    sb.append("    <default>false</default>\n");
                    sb.append("    <label>").append(value).append("</label>\n");
                    sb.append("  </standardValue>\n");
                }
                sb.append("</StandardValueSet>\n");
                yield sb.toString();
            }
            case "GlobalValueSet" -> {
                List<String> values = (List<String>) resource.attributes().getOrDefault("values", List.of());
                StringBuilder sb = new StringBuilder();
                sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                sb.append("<GlobalValueSet xmlns=\"http://soap.sforce.com/2006/04/metadata\">\n");
                for (String value : values) {
                    sb.append("  <customValue>\n");
                    sb.append("    <fullName>").append(value).append("</fullName>\n");
                    sb.append("    <default>false</default>\n");
                    sb.append("    <label>").append(value).append("</label>\n");
                    sb.append("  </customValue>\n");
                }
                sb.append("</GlobalValueSet>\n");
                yield sb.toString();
            }
            default -> """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <%s xmlns="http://soap.sforce.com/2006/04/metadata">
                      <fullName>%s</fullName>
                      <label>%s</label>
                    </%s>
                    """.formatted(resource.type(), resource.fullName(), resource.label(), resource.type());
        };
    }
}
