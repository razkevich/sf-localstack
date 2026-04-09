package co.razkevich.sflocalstack.data.controller;

import co.razkevich.sflocalstack.data.model.SObjectRecord;
import co.razkevich.sflocalstack.model.SalesforceError;
import co.razkevich.sflocalstack.data.service.OrgStateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping({"/services/data/{apiVersion}/sobjects/{objectType}", "/data/{apiVersion}/sobjects/{objectType}"})
public class SObjectController {

    private final OrgStateService orgStateService;

    public SObjectController(OrgStateService orgStateService) {
        this.orgStateService = orgStateService;
    }

    @GetMapping("/describe")
    public ResponseEntity<Map<String, Object>> describe(
            @PathVariable String apiVersion,
            @PathVariable String objectType) {
        return ResponseEntity.ok(buildDescribeResponse(apiVersion, objectType));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @PathVariable String apiVersion,
            @PathVariable String objectType) {
        List<SObjectRecord> records = orgStateService.findByType(objectType);
        List<Map<String, Object>> recentItems = records.stream()
                .map(r -> orgStateService.toSalesforceRecord(apiVersion, objectType, orgStateService.fromJson(r.getFieldsJson())))
                .toList();
        return ResponseEntity.ok(Map.of(
                "objectDescribe", buildObjectDescribeSummary(apiVersion, objectType),
                "recentItems", recentItems
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
            @PathVariable String apiVersion,
            @PathVariable String objectType,
            @PathVariable String id) {
        Optional<SObjectRecord> record = orgStateService.findById(id);
        if (record.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(List.of(new SalesforceError("Record not found", "NOT_FOUND")));
        }
        return ResponseEntity.ok(orgStateService.toSalesforceRecord(apiVersion, objectType, orgStateService.fromJson(record.get().getFieldsJson())));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @PathVariable String objectType,
            @RequestBody Map<String, Object> fields) {
        SObjectRecord record = orgStateService.create(objectType, fields);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", record.getId(), "success", true, "errors", List.of()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable String objectType,
            @PathVariable String id,
            @RequestBody Map<String, Object> fields) {
        Optional<SObjectRecord> updated = orgStateService.update(id, fields);
        if (updated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(List.of(new SalesforceError("Record not found", "NOT_FOUND")));
        }
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> replace(
            @PathVariable String objectType,
            @PathVariable String id,
            @RequestBody Map<String, Object> fields) {
        Optional<SObjectRecord> updated = orgStateService.replace(id, fields);
        if (updated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(List.of(new SalesforceError("Record not found", "NOT_FOUND")));
        }
        return ResponseEntity.ok(Map.of("id", updated.get().getId(), "success", true, "errors", List.of()));
    }

    @PatchMapping("/{externalIdField}/{externalIdValue}")
    public ResponseEntity<?> upsert(
            @PathVariable String apiVersion,
            @PathVariable String objectType,
            @PathVariable String externalIdField,
            @PathVariable String externalIdValue,
            @RequestBody Map<String, Object> fields) {
        var result = orgStateService.upsert(objectType, externalIdField, externalIdValue, fields);
        if (result.created()) {
            String location = "/services/data/" + apiVersion + "/sobjects/" + objectType + "/" + result.record().getId();
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header(HttpHeaders.LOCATION, location)
                    .body(Map.of("id", result.record().getId(), "success", true, "errors", List.of(), "created", true));
        }
        return ResponseEntity.ok(Map.of("id", result.record().getId(), "success", true, "errors", List.of(), "created", false));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String objectType, @PathVariable String id) {
        boolean deleted = orgStateService.delete(id);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(List.of(new SalesforceError("Record not found", "NOT_FOUND")));
        }
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> buildDescribeResponse(String apiVersion, String objectType) {
        Map<String, Object> describe = new LinkedHashMap<>();
        describe.put("actionOverrides", List.of());
        describe.put("activateable", false);
        describe.put("associateEntityType", null);
        describe.put("associateParentEntity", null);
        describe.put("childRelationships", buildChildRelationships(objectType));
        describe.put("compactLayoutable", true);
        describe.put("createable", true);
        describe.put("custom", objectType.endsWith("__c"));
        describe.put("customSetting", false);
        describe.put("deepCloneable", false);
        describe.put("defaultImplementation", null);
        describe.put("deletable", true);
        describe.put("deprecatedAndHidden", false);
        describe.put("extendedBy", List.of());
        describe.put("extendsInterfaces", List.of());
        describe.put("feedEnabled", true);
        describe.put("fields", orgStateService.describeFields(objectType));
        describe.put("hasSubtypes", false);
        describe.put("implementedBy", List.of());
        describe.put("implementsInterfaces", List.of());
        describe.put("isInterface", false);
        describe.put("isSubtype", false);
        describe.put("keyPrefix", keyPrefixFor(objectType));
        describe.put("label", objectType);
        describe.put("labelPlural", pluralize(objectType));
        describe.put("layoutable", true);
        describe.put("listviewable", true);
        describe.put("lookupLayoutable", true);
        describe.put("mergeable", !"Contact".equalsIgnoreCase(objectType));
        describe.put("mruEnabled", true);
        describe.put("name", objectType);
        describe.put("queryable", true);
        describe.put("recordTypeInfos", buildRecordTypeInfos());
        describe.put("replicateable", true);
        describe.put("retrieveable", true);
        describe.put("searchLayoutable", true);
        describe.put("searchable", true);
        describe.put("supportedScopes", List.of());
        describe.put("triggerable", true);
        describe.put("undeletable", false);
        describe.put("updateable", true);
        describe.put("urls", buildUrls(apiVersion, objectType));
        return describe;
    }

    private Map<String, Object> buildObjectDescribeSummary(String apiVersion, String objectType) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("activateable", false);
        summary.put("associateEntityType", null);
        summary.put("associateParentEntity", null);
        summary.put("createable", true);
        summary.put("custom", objectType.endsWith("__c"));
        summary.put("customSetting", false);
        summary.put("deepCloneable", false);
        summary.put("deletable", true);
        summary.put("deprecatedAndHidden", false);
        summary.put("feedEnabled", true);
        summary.put("hasSubtypes", false);
        summary.put("isInterface", false);
        summary.put("isSubtype", false);
        summary.put("keyPrefix", keyPrefixFor(objectType));
        summary.put("label", objectType);
        summary.put("labelPlural", pluralize(objectType));
        summary.put("layoutable", true);
        summary.put("mergeable", !"Contact".equalsIgnoreCase(objectType));
        summary.put("mruEnabled", true);
        summary.put("name", objectType);
        summary.put("queryable", true);
        summary.put("replicateable", true);
        summary.put("retrieveable", true);
        summary.put("searchable", true);
        summary.put("triggerable", true);
        summary.put("undeletable", false);
        summary.put("updateable", true);
        summary.put("urls", buildUrls(apiVersion, objectType));
        return summary;
    }

    private Map<String, Object> buildUrls(String apiVersion, String objectType) {
        String base = "/services/data/" + apiVersion + "/sobjects/" + objectType;
        return Map.of(
                "approvalLayouts", base + "/describe/approvalLayouts",
                "compactLayouts", base + "/describe/compactLayouts",
                "describe", base + "/describe",
                "layouts", base + "/describe/layouts",
                "listviews", base + "/listviews",
                "quickActions", base + "/quickActions",
                "rowTemplate", base + "/{ID}",
                "sobject", base
        );
    }

    private List<Map<String, Object>> buildRecordTypeInfos() {
        Map<String, Object> master = new LinkedHashMap<>();
        master.put("active", true);
        master.put("available", true);
        master.put("defaultRecordTypeMapping", true);
        master.put("developerName", "Master");
        master.put("master", true);
        master.put("name", "Master");
        master.put("recordTypeId", "012000000000000AAA");
        return List.of(master);
    }

    private List<Map<String, Object>> buildChildRelationships(String objectType) {
        return switch (objectType) {
            case "Account" -> List.of(
                    childRelationship("Contact", "Contacts", "AccountId"),
                    childRelationship("Opportunity", "Opportunities", "AccountId")
            );
            case "Contact" -> List.of(
                    childRelationship("Case", "Cases", "ContactId")
            );
            default -> List.of();
        };
    }

    private Map<String, Object> childRelationship(String childSObject, String relationshipName, String field) {
        Map<String, Object> rel = new LinkedHashMap<>();
        rel.put("cascadeDelete", false);
        rel.put("childSObject", childSObject);
        rel.put("deprecatedAndHidden", false);
        rel.put("field", field);
        rel.put("junctionIdListNames", List.of());
        rel.put("junctionReferenceTo", List.of());
        rel.put("relationshipName", relationshipName);
        rel.put("restrictedDelete", false);
        return rel;
    }

    private String keyPrefixFor(String objectType) {
        return switch (objectType) {
            case "Account" -> "001";
            case "Contact" -> "003";
            default -> objectType.length() >= 3 ? objectType.substring(0, 3).toUpperCase() : objectType.toUpperCase();
        };
    }

    private String pluralize(String objectType) {
        return objectType.endsWith("s") ? objectType : objectType + "s";
    }
}
