package co.prodly.sflocalstack.service;

import co.prodly.sflocalstack.model.MetadataResource;
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

            // Deduplicate by fileName to avoid duplicate entries
            Map<String, MetadataResource> byFileName = new LinkedHashMap<>();
            for (MetadataResource resource : resources) {
                byFileName.putIfAbsent(resource.fileName(), resource);
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

    private String buildResourceXml(MetadataResource resource) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <%s xmlns="http://soap.sforce.com/2006/04/metadata">
                  <fullName>%s</fullName>
                  <label>%s</label>
                </%s>
                """.formatted(resource.type(), resource.fullName(), resource.label(), resource.type());
    }
}
