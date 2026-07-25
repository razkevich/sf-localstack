package co.razkevich.sflocalstack.data.controller;

import co.razkevich.sflocalstack.data.service.OrgStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Org-level REST resources that are not scoped to a single sObject:
 * {@code /sobjects} (global describe) and {@code /limits}.
 *
 * <p>Tools discover an org's shape through the global describe before they can
 * offer object pickers or schema browsing, and read {@code /limits} to show
 * consumption. Both are read-only and derived from current org state, so they
 * stay consistent with whatever the emulator is holding.
 */
@RestController
@RequestMapping({"/services/data/{apiVersion}", "/data/{apiVersion}"})
public class OrgApiController {

    /** Objects always advertised, so a freshly reset org still describes usefully. */
    private static final List<String> BASELINE_OBJECTS =
            List.of("Account", "Contact", "Lead", "Opportunity", "User", "Organization");

    private static final Map<String, String> KEY_PREFIXES = Map.of(
            "Account", "001",
            "Contact", "003",
            "Lead", "00Q",
            "Opportunity", "006",
            "User", "005",
            "Organization", "00D");

    private final OrgStateService orgStateService;

    public OrgApiController(OrgStateService orgStateService) {
        this.orgStateService = orgStateService;
    }

    @GetMapping({"/sobjects", "/sobjects/"})
    public ResponseEntity<Map<String, Object>> describeGlobal(@PathVariable String apiVersion) {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        names.addAll(BASELINE_OBJECTS);
        names.addAll(orgStateService.countByObjectType().keySet());

        List<Map<String, Object>> sobjects = new ArrayList<>();
        for (String name : names) {
            sobjects.add(summarize(apiVersion, name));
        }
        sobjects.sort(Comparator.comparing(entry -> String.valueOf(entry.get("name"))));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("encoding", "UTF-8");
        body.put("maxBatchSize", 200);
        body.put("sobjects", sobjects);
        return ResponseEntity.ok(body);
    }

    @GetMapping({"/limits", "/limits/"})
    public ResponseEntity<Map<String, Object>> limits() {
        int records = orgStateService.findAll().size();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("DailyApiRequests", limit(100000, 100000 - Math.min(records, 100000)));
        body.put("DailyBulkApiBatches", limit(15000, 15000));
        body.put("DailyBulkV2QueryJobs", limit(10000, 10000));
        body.put("DataStorageMB", limit(1024, 1024 - storageMb(records)));
        body.put("FileStorageMB", limit(1024, 1024));
        body.put("DailyAsyncApexExecutions", limit(250000, 250000));
        body.put("DailyDurableGenericStreamingApiEvents", limit(10000, 10000));
        body.put("HourlyODataCallout", limit(20000, 20000));
        body.put("MassEmail", limit(5000, 5000));
        body.put("SingleEmail", limit(5000, 5000));
        body.put("PermissionSets", limit(1500, 1500));
        return ResponseEntity.ok(body);
    }

    private Map<String, Object> summarize(String apiVersion, String name) {
        String version = apiVersion.startsWith("v") ? apiVersion.substring(1) : apiVersion;
        boolean custom = name.endsWith("__c");
        String basePath = "/services/data/v" + version + "/sobjects/" + name;

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("activateable", false);
        entry.put("createable", true);
        entry.put("custom", custom);
        entry.put("customSetting", false);
        entry.put("deletable", true);
        entry.put("deprecatedAndHidden", false);
        entry.put("feedEnabled", true);
        entry.put("keyPrefix", keyPrefixFor(name));
        entry.put("label", name);
        entry.put("labelPlural", name.endsWith("s") ? name : name + "s");
        entry.put("layoutable", true);
        entry.put("mergeable", false);
        entry.put("mruEnabled", true);
        entry.put("name", name);
        entry.put("queryable", true);
        entry.put("replicateable", true);
        entry.put("retrieveable", true);
        entry.put("searchable", true);
        entry.put("triggerable", true);
        entry.put("undeletable", true);
        entry.put("updateable", true);
        entry.put("urls", Map.of(
                "sobject", basePath,
                "describe", basePath + "/describe",
                "rowTemplate", basePath + "/{ID}"));
        return entry;
    }

    private String keyPrefixFor(String objectType) {
        String prefix = KEY_PREFIXES.get(objectType);
        if (prefix != null) {
            return prefix;
        }
        return objectType.length() >= 3
                ? objectType.substring(0, 3).toUpperCase()
                : objectType.toUpperCase();
    }

    private Map<String, Object> limit(int max, int remaining) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("Max", max);
        value.put("Remaining", Math.max(remaining, 0));
        return value;
    }

    /** Rough stand-in for storage consumption: Salesforce bills 2 KB per record. */
    private int storageMb(int records) {
        return (int) Math.ceil(records * 2.0 / 1024.0);
    }
}
