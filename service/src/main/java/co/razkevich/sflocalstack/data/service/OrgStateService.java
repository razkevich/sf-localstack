package co.razkevich.sflocalstack.data.service;

import co.razkevich.sflocalstack.data.model.SObjectRecord;
import co.razkevich.sflocalstack.data.repository.SObjectRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import co.razkevich.sflocalstack.model.SalesforceIdGenerator;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrgStateService {

    private static final Map<String, List<FieldSpec>> STANDARD_FIELD_CATALOG = Map.ofEntries(
            Map.entry("Account", List.of(
                    new FieldSpec("Id", "Id", "id", false),
                    new FieldSpec("IsDeleted", "Deleted", "boolean", false),
                    new FieldSpec("MasterRecordId", "Master Record ID", "reference", false),
                    new FieldSpec("Name", "Account Name", "string", true),
                    new FieldSpec("LastName", "Last Name", "string", true),
                    new FieldSpec("FirstName", "First Name", "string", true),
                    new FieldSpec("Salutation", "Salutation", "picklist", true),
                    new FieldSpec("Type", "Type", "string", true),
                    new FieldSpec("RecordTypeId", "Record Type ID", "reference", true),
                    new FieldSpec("ParentId", "Parent Account ID", "reference", true),
                    new FieldSpec("BillingStreet", "Billing Street", "textarea", true),
                    new FieldSpec("BillingCity", "Billing City", "string", true),
                    new FieldSpec("BillingState", "Billing State/Province", "string", true),
                    new FieldSpec("BillingPostalCode", "Billing Zip/Postal Code", "string", true),
                    new FieldSpec("BillingCountry", "Billing Country", "string", true),
                    new FieldSpec("BillingLatitude", "Billing Latitude", "double", true),
                    new FieldSpec("BillingLongitude", "Billing Longitude", "double", true),
                    new FieldSpec("BillingGeocodeAccuracy", "Billing Geocode Accuracy", "string", true),
                    new FieldSpec("BillingAddress", "Billing Address", "address", true),
                    new FieldSpec("ShippingStreet", "Shipping Street", "textarea", true),
                    new FieldSpec("ShippingCity", "Shipping City", "string", true),
                    new FieldSpec("ShippingState", "Shipping State/Province", "string", true),
                    new FieldSpec("ShippingPostalCode", "Shipping Zip/Postal Code", "string", true),
                    new FieldSpec("ShippingCountry", "Shipping Country", "string", true),
                    new FieldSpec("ShippingLatitude", "Shipping Latitude", "double", true),
                    new FieldSpec("ShippingLongitude", "Shipping Longitude", "double", true),
                    new FieldSpec("ShippingGeocodeAccuracy", "Shipping Geocode Accuracy", "string", true),
                    new FieldSpec("ShippingAddress", "Shipping Address", "address", true),
                    new FieldSpec("Phone", "Account Phone", "phone", true),
                    new FieldSpec("Fax", "Account Fax", "phone", true),
                    new FieldSpec("AccountNumber", "Account Number", "string", true),
                    new FieldSpec("Website", "Website", "url", true),
                    new FieldSpec("Industry", "Industry", "string", true),
                    new FieldSpec("AnnualRevenue", "Annual Revenue", "currency", true),
                    new FieldSpec("NumberOfEmployees", "Employees", "int", true),
                    new FieldSpec("Ownership", "Ownership", "picklist", true),
                    new FieldSpec("Description", "Description", "textarea", true),
                    new FieldSpec("Rating", "Account Rating", "picklist", true),
                    new FieldSpec("Site", "Account Site", "string", true),
                    new FieldSpec("OwnerId", "Owner ID", "reference", false),
                    new FieldSpec("CreatedDate", "Created Date", "datetime", false),
                    new FieldSpec("CreatedById", "Created By ID", "reference", false),
                    new FieldSpec("LastModifiedDate", "Last Modified Date", "datetime", false),
                    new FieldSpec("LastModifiedById", "Last Modified By ID", "reference", false),
                    new FieldSpec("SystemModstamp", "System Modstamp", "datetime", false),
                    new FieldSpec("LastActivityDate", "Last Activity", "date", true),
                    new FieldSpec("LastViewedDate", "Last Viewed Date", "datetime", true),
                    new FieldSpec("LastReferencedDate", "Last Referenced Date", "datetime", true)
            )),
            Map.entry("Contact", List.of(
                    new FieldSpec("Id", "Contact ID", "id", false),
                    new FieldSpec("IsDeleted", "Deleted", "boolean", false),
                    new FieldSpec("MasterRecordId", "Master Record ID", "reference", false),
                    new FieldSpec("AccountId", "Account ID", "reference", true),
                    new FieldSpec("IsPersonAccount", "Is Person Account", "boolean", false),
                    new FieldSpec("LastName", "Last Name", "string", false),
                    new FieldSpec("FirstName", "First Name", "string", true),
                    new FieldSpec("Salutation", "Salutation", "picklist", true),
                    new FieldSpec("Name", "Full Name", "string", false),
                    new FieldSpec("OtherStreet", "Other Street", "textarea", true),
                    new FieldSpec("OtherCity", "Other City", "string", true),
                    new FieldSpec("OtherState", "Other State/Province", "string", true),
                    new FieldSpec("OtherPostalCode", "Other Zip/Postal Code", "string", true),
                    new FieldSpec("OtherCountry", "Other Country", "string", true),
                    new FieldSpec("OtherLatitude", "Other Latitude", "double", true),
                    new FieldSpec("OtherLongitude", "Other Longitude", "double", true),
                    new FieldSpec("OtherGeocodeAccuracy", "Other Geocode Accuracy", "string", true),
                    new FieldSpec("OtherAddress", "Other Address", "address", true),
                    new FieldSpec("MailingStreet", "Mailing Street", "textarea", true),
                    new FieldSpec("MailingCity", "Mailing City", "string", true),
                    new FieldSpec("MailingState", "Mailing State/Province", "string", true),
                    new FieldSpec("MailingPostalCode", "Mailing Zip/Postal Code", "string", true),
                    new FieldSpec("MailingCountry", "Mailing Country", "string", true),
                    new FieldSpec("MailingLatitude", "Mailing Latitude", "double", true),
                    new FieldSpec("MailingLongitude", "Mailing Longitude", "double", true),
                    new FieldSpec("MailingGeocodeAccuracy", "Mailing Geocode Accuracy", "string", true),
                    new FieldSpec("MailingAddress", "Mailing Address", "address", true),
                    new FieldSpec("Phone", "Business Phone", "phone", true),
                    new FieldSpec("MobilePhone", "Mobile", "phone", true),
                    new FieldSpec("HomePhone", "Home Phone", "phone", true),
                    new FieldSpec("OtherPhone", "Other Phone", "phone", true),
                    new FieldSpec("Email", "Email", "email", true),
                    new FieldSpec("Title", "Title", "string", true),
                    new FieldSpec("Department", "Department", "string", true),
                    new FieldSpec("Birthdate", "Birthdate", "date", true),
                    new FieldSpec("Description", "Description", "textarea", true),
                    new FieldSpec("OwnerId", "Owner ID", "reference", false),
                    new FieldSpec("CreatedDate", "Created Date", "datetime", false),
                    new FieldSpec("CreatedById", "Created By ID", "reference", false),
                    new FieldSpec("LastModifiedDate", "Last Modified Date", "datetime", false),
                    new FieldSpec("LastModifiedById", "Last Modified By ID", "reference", false),
                    new FieldSpec("SystemModstamp", "System Modstamp", "datetime", false),
                    new FieldSpec("LastActivityDate", "Last Activity", "date", true),
                    new FieldSpec("LastViewedDate", "Last Viewed Date", "datetime", true),
                    new FieldSpec("LastReferencedDate", "Last Referenced Date", "datetime", true)
            )),
            Map.entry("User", List.of(
                    new FieldSpec("Id", "User ID", "id", false),
                    new FieldSpec("Username", "Username", "string", false),
                    new FieldSpec("LastName", "Last Name", "string", false),
                    new FieldSpec("FirstName", "First Name", "string", true),
                    new FieldSpec("Name", "Full Name", "string", false),
                    new FieldSpec("CompanyName", "Company Name", "string", true),
                    new FieldSpec("Division", "Division", "string", true),
                    new FieldSpec("Department", "Department", "string", true),
                    new FieldSpec("Title", "Title", "string", true),
                    new FieldSpec("Street", "Street", "textarea", true),
                    new FieldSpec("City", "City", "string", true),
                    new FieldSpec("State", "State/Province", "string", true),
                    new FieldSpec("PostalCode", "Zip/Postal Code", "string", true),
                    new FieldSpec("Country", "Country", "string", true),
                    new FieldSpec("Email", "Email", "email", true),
                    new FieldSpec("Phone", "Phone", "phone", true),
                    new FieldSpec("MobilePhone", "Mobile Phone", "phone", true),
                    new FieldSpec("Alias", "Alias", "string", true),
                    new FieldSpec("TimeZoneSidKey", "Time Zone", "picklist", true),
                    new FieldSpec("LocaleSidKey", "Locale", "picklist", true),
                    new FieldSpec("EmailEncodingKey", "Email Encoding", "picklist", true),
                    new FieldSpec("ProfileId", "Profile ID", "reference", false),
                    new FieldSpec("LanguageLocaleKey", "Language", "picklist", true),
                    new FieldSpec("EmployeeNumber", "Employee Number", "string", true),
                    new FieldSpec("IsActive", "Active", "boolean", false),
                    new FieldSpec("UserRoleId", "User Role ID", "reference", true),
                    new FieldSpec("CreatedDate", "Created Date", "datetime", false),
                    new FieldSpec("CreatedById", "Created By ID", "reference", false),
                    new FieldSpec("LastModifiedDate", "Last Modified Date", "datetime", false),
                    new FieldSpec("LastModifiedById", "Last Modified By ID", "reference", false),
                    new FieldSpec("SystemModstamp", "System Modstamp", "datetime", false),
                    new FieldSpec("LastViewedDate", "Last Viewed Date", "datetime", true),
                    new FieldSpec("LastReferencedDate", "Last Referenced Date", "datetime", true)
            )),
            Map.entry("Lead", List.of(
                    new FieldSpec("Id", "Lead ID", "id", false),
                    new FieldSpec("IsDeleted", "Deleted", "boolean", false),
                    new FieldSpec("LastName", "Last Name", "string", true),
                    new FieldSpec("FirstName", "First Name", "string", true),
                    new FieldSpec("Salutation", "Salutation", "picklist", true),
                    new FieldSpec("Name", "Full Name", "string", true),
                    new FieldSpec("Title", "Title", "string", true),
                    new FieldSpec("Company", "Company", "string", true),
                    new FieldSpec("Street", "Street", "textarea", true),
                    new FieldSpec("City", "City", "string", true),
                    new FieldSpec("State", "State/Province", "string", true),
                    new FieldSpec("PostalCode", "Zip/Postal Code", "string", true),
                    new FieldSpec("Country", "Country", "string", true),
                    new FieldSpec("Phone", "Phone", "phone", true),
                    new FieldSpec("MobilePhone", "Mobile Phone", "phone", true),
                    new FieldSpec("Email", "Email", "email", true),
                    new FieldSpec("Website", "Website", "url", true),
                    new FieldSpec("Description", "Description", "textarea", true),
                    new FieldSpec("LeadSource", "Lead Source", "picklist", true),
                    new FieldSpec("Status", "Status", "picklist", true),
                    new FieldSpec("Industry", "Industry", "string", true),
                    new FieldSpec("Rating", "Rating", "picklist", true),
                    new FieldSpec("AnnualRevenue", "Annual Revenue", "currency", true),
                    new FieldSpec("NumberOfEmployees", "Employees", "int", true),
                    new FieldSpec("OwnerId", "Owner ID", "reference", false),
                    new FieldSpec("IsConverted", "Converted", "boolean", false),
                    new FieldSpec("ConvertedDate", "Converted Date", "date", false),
                    new FieldSpec("ConvertedAccountId", "Converted Account ID", "reference", false),
                    new FieldSpec("ConvertedContactId", "Converted Contact ID", "reference", false),
                    new FieldSpec("CreatedDate", "Created Date", "datetime", false),
                    new FieldSpec("CreatedById", "Created By ID", "reference", false),
                    new FieldSpec("LastModifiedDate", "Last Modified Date", "datetime", false),
                    new FieldSpec("LastModifiedById", "Last Modified By ID", "reference", false),
                    new FieldSpec("SystemModstamp", "System Modstamp", "datetime", false)
            )),
            Map.entry("Opportunity", List.of(
                    new FieldSpec("Id", "Opportunity ID", "id", false),
                    new FieldSpec("IsDeleted", "Deleted", "boolean", false),
                    new FieldSpec("AccountId", "Account ID", "reference", true),
                    new FieldSpec("Name", "Opportunity Name", "string", true),
                    new FieldSpec("Description", "Description", "textarea", true),
                    new FieldSpec("StageName", "Stage", "picklist", true),
                    new FieldSpec("Amount", "Amount", "currency", true),
                    new FieldSpec("Probability", "Probability (%)", "double", true),
                    new FieldSpec("CloseDate", "Close Date", "date", true),
                    new FieldSpec("Type", "Opportunity Type", "picklist", true),
                    new FieldSpec("NextStep", "Next Step", "string", true),
                    new FieldSpec("LeadSource", "Lead Source", "picklist", true),
                    new FieldSpec("IsClosed", "Closed", "boolean", false),
                    new FieldSpec("IsWon", "Won", "boolean", false),
                    new FieldSpec("ForecastCategory", "Forecast Category", "picklist", false),
                    new FieldSpec("ForecastCategoryName", "Forecast Category Name", "string", false),
                    new FieldSpec("OwnerId", "Owner ID", "reference", false),
                    new FieldSpec("CreatedDate", "Created Date", "datetime", false),
                    new FieldSpec("CreatedById", "Created By ID", "reference", false),
                    new FieldSpec("LastModifiedDate", "Last Modified Date", "datetime", false),
                    new FieldSpec("LastModifiedById", "Last Modified By ID", "reference", false),
                    new FieldSpec("SystemModstamp", "System Modstamp", "datetime", false)
            )),
            Map.entry("Case", List.of(
                    new FieldSpec("Id", "Case ID", "id", false),
                    new FieldSpec("IsDeleted", "Deleted", "boolean", false),
                    new FieldSpec("CaseNumber", "Case Number", "string", false),
                    new FieldSpec("ContactId", "Contact ID", "reference", true),
                    new FieldSpec("AccountId", "Account ID", "reference", true),
                    new FieldSpec("Subject", "Subject", "string", true),
                    new FieldSpec("Description", "Description", "textarea", true),
                    new FieldSpec("Status", "Status", "picklist", true),
                    new FieldSpec("Priority", "Priority", "picklist", true),
                    new FieldSpec("Origin", "Case Origin", "picklist", true),
                    new FieldSpec("Type", "Case Type", "picklist", true),
                    new FieldSpec("Reason", "Case Reason", "picklist", true),
                    new FieldSpec("IsClosed", "Closed", "boolean", false),
                    new FieldSpec("IsEscalated", "Escalated", "boolean", false),
                    new FieldSpec("OwnerId", "Owner ID", "reference", false),
                    new FieldSpec("CreatedDate", "Created Date", "datetime", false),
                    new FieldSpec("CreatedById", "Created By ID", "reference", false),
                    new FieldSpec("LastModifiedDate", "Last Modified Date", "datetime", false),
                    new FieldSpec("LastModifiedById", "Last Modified By ID", "reference", false),
                    new FieldSpec("SystemModstamp", "System Modstamp", "datetime", false),
                    new FieldSpec("ClosedDate", "Closed Date", "datetime", false)
            )),
            Map.entry("Task", List.of(
                    new FieldSpec("Id", "Task ID", "id", false),
                    new FieldSpec("Subject", "Subject", "string", true),
                    new FieldSpec("Status", "Status", "picklist", true),
                    new FieldSpec("Priority", "Priority", "picklist", true),
                    new FieldSpec("WhoId", "Name", "reference", true),
                    new FieldSpec("WhatId", "Related To", "reference", true),
                    new FieldSpec("ActivityDate", "Due Date", "date", true),
                    new FieldSpec("Description", "Comments", "textarea", true),
                    new FieldSpec("OwnerId", "Assigned To ID", "reference", false),
                    new FieldSpec("IsClosed", "Closed", "boolean", false),
                    new FieldSpec("IsHighPriority", "High Priority", "boolean", false),
                    new FieldSpec("CreatedDate", "Created Date", "datetime", false),
                    new FieldSpec("CreatedById", "Created By ID", "reference", false),
                    new FieldSpec("LastModifiedDate", "Last Modified Date", "datetime", false),
                    new FieldSpec("LastModifiedById", "Last Modified By ID", "reference", false),
                    new FieldSpec("SystemModstamp", "System Modstamp", "datetime", false)
            )),
            Map.entry("Event", List.of(
                    new FieldSpec("Id", "Event ID", "id", false),
                    new FieldSpec("Subject", "Subject", "string", true),
                    new FieldSpec("Location", "Location", "string", true),
                    new FieldSpec("IsAllDayEvent", "All Day Event", "boolean", true),
                    new FieldSpec("ActivityDate", "Date", "date", true),
                    new FieldSpec("StartDateTime", "Start Date/Time", "datetime", true),
                    new FieldSpec("EndDateTime", "End Date/Time", "datetime", true),
                    new FieldSpec("Description", "Description", "textarea", true),
                    new FieldSpec("WhoId", "Name", "reference", true),
                    new FieldSpec("WhatId", "Related To", "reference", true),
                    new FieldSpec("OwnerId", "Assigned To ID", "reference", false),
                    new FieldSpec("IsPrivate", "Private", "boolean", true),
                    new FieldSpec("CreatedDate", "Created Date", "datetime", false),
                    new FieldSpec("CreatedById", "Created By ID", "reference", false),
                    new FieldSpec("LastModifiedDate", "Last Modified Date", "datetime", false),
                    new FieldSpec("LastModifiedById", "Last Modified By ID", "reference", false),
                    new FieldSpec("SystemModstamp", "System Modstamp", "datetime", false)
            ))
    );

    private static final Logger log = LoggerFactory.getLogger(OrgStateService.class);

    private final SObjectRecordRepository repository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public OrgStateService(SObjectRecordRepository repository, ObjectMapper objectMapper, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional
    public SObjectRecord create(String objectType, Map<String, Object> fields) {
        return create(null, objectType, fields);
    }

    @Transactional
    public SObjectRecord create(String orgId, String objectType, Map<String, Object> fields) {
        String id = fields.get("Id") instanceof String existingId && !existingId.isBlank()
                ? existingId
                : generateId(objectType);
        Instant now = Instant.now();
        fields.putIfAbsent("Id", id);
        fields.putIfAbsent("CreatedDate", now.toString());
        fields.putIfAbsent("LastModifiedDate", now.toString());
        String json = toJson(fields);
        SObjectRecord record = new SObjectRecord(id, objectType, json, now, now);
        if (orgId != null) record.setOrgId(orgId);
        return repository.save(record);
    }

    @Transactional(readOnly = true)
    public List<SObjectRecord> findByType(String objectType) {
        return repository.findByObjectType(objectType);
    }

    @Transactional(readOnly = true)
    public List<SObjectRecord> findByType(String orgId, String objectType) {
        return orgId != null ? repository.findByOrgIdAndObjectType(orgId, objectType) : repository.findByObjectType(objectType);
    }

    @Transactional(readOnly = true)
    public Optional<SObjectRecord> findById(String id) {
        Optional<SObjectRecord> result = repository.findById(id);
        if (result.isEmpty() && id != null && id.length() == 18) {
            result = repository.findById(SalesforceIdGenerator.to15(id));
        }
        if (result.isEmpty() && id != null && id.length() == 15) {
            result = repository.findById(SalesforceIdGenerator.to18(id));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Optional<SObjectRecord> findByTypeAndId(String objectType, String id) {
        return repository.findById(id)
                .filter(record -> objectType.equalsIgnoreCase(record.getObjectType()));
    }

    @Transactional(readOnly = true)
    public List<SObjectRecord> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<SObjectRecord> findAll(String orgId) {
        return orgId != null ? repository.findByOrgId(orgId) : repository.findAll();
    }

    @Transactional(readOnly = true)
    public Map<String, Integer> countByObjectType() {
        return repository.findAll().stream()
                .collect(Collectors.groupingBy(
                        SObjectRecord::getObjectType,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
    }

    @Transactional(readOnly = true)
    public Map<String, Integer> countByObjectType(String orgId) {
        return findAll(orgId).stream()
                .collect(Collectors.groupingBy(
                        SObjectRecord::getObjectType,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
    }

    @Transactional(readOnly = true)
    public Object resolveFieldValue(String objectType, Map<String, Object> fields, String fieldPath) {
        if (fields.containsKey(fieldPath)) {
            return fields.get(fieldPath);
        }

        String canonicalFieldPath = canonicalFieldPath(fields, fieldPath);
        if (!canonicalFieldPath.equals(fieldPath) && fields.containsKey(canonicalFieldPath)) {
            return fields.get(canonicalFieldPath);
        }

        if (!fieldPath.contains(".")) {
            return fields.get(canonicalFieldPath);
        }

        String[] parts = canonicalFieldPath.split("\\.", 2);
        String relationshipName = parts[0];
        String relatedField = parts[1];

        Object literalValue = fields.get(canonicalFieldPath);
        if (literalValue != null) {
            return literalValue;
        }

        Object relationshipId = fields.get(relationshipName + "Id");
        if (!(relationshipId instanceof String relationshipRecordId)) {
            return null;
        }

        String relatedObjectType = inferObjectTypeFromRelationship(relationshipName);
        return findByTypeAndId(relatedObjectType, relationshipRecordId)
                .map(SObjectRecord::getFieldsJson)
                .map(this::fromJson)
                .map(relatedFields -> resolveFieldValue(relatedObjectType, relatedFields, relatedField))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> toSalesforceRecord(String apiVersion, String objectType, Map<String, Object> fields) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("attributes", Map.of(
                "type", objectType,
                "url", "/services/data/" + apiVersion + "/sobjects/" + objectType + "/" + fields.get("Id")
        ));

        fields.forEach((key, value) -> {
            if ("attributes".equals(key)) {
                return;
            }

            if (key.contains(".")) {
                addNestedValue(result, key, value);
            } else {
                result.put(key, value);
            }
        });

        return result;
    }

    @Transactional(readOnly = true)
    public String canonicalFieldPath(Map<String, Object> fields, String requestedFieldPath) {
        if (fields.containsKey(requestedFieldPath)) {
            return requestedFieldPath;
        }

        if (!requestedFieldPath.contains(".")) {
            return fields.keySet().stream()
                    .filter(key -> key.equalsIgnoreCase(requestedFieldPath))
                    .findFirst()
                    .orElse(requestedFieldPath);
        }

        String[] parts = requestedFieldPath.split("\\.");
        StringBuilder canonical = new StringBuilder();
        StringBuilder probe = new StringBuilder();
        for (int index = 0; index < parts.length; index++) {
            if (index > 0) {
                canonical.append('.');
                probe.append('.');
            }
            probe.append(parts[index]);
            String matched = fields.keySet().stream()
                    .filter(key -> key.equalsIgnoreCase(probe.toString()))
                    .findFirst()
                    .orElse(parts[index]);
            if (matched.contains(".")) {
                canonical.setLength(0);
                canonical.append(matched);
                probe.setLength(0);
                probe.append(matched);
            } else {
                canonical.append(matched);
            }
        }
        return canonical.toString();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> describeFields(String objectType) {
        Map<String, FieldSpec> fieldSpecs = new LinkedHashMap<>();

        STANDARD_FIELD_CATALOG.getOrDefault(objectType, List.of())
                .forEach(spec -> fieldSpecs.put(spec.name(), spec));

        findByType(objectType).stream()
                .map(SObjectRecord::getFieldsJson)
                .map(this::fromJson)
                .flatMap(fields -> fields.entrySet().stream())
                .filter(entry -> !"attributes".equals(entry.getKey()))
                .forEach(entry -> fieldSpecs.putIfAbsent(
                        entry.getKey(),
                        new FieldSpec(entry.getKey(), entry.getKey(), guessFieldType(entry.getValue()), true)
                ));

        return fieldSpecs.values().stream()
                .sorted(Comparator.comparing(FieldSpec::name, Comparator.naturalOrder()))
                .map(spec -> {
                    Map<String, Object> field = new LinkedHashMap<>();
                    field.put("name", spec.name());
                    field.put("label", spec.label());
                    field.put("type", spec.type());
                    field.put("custom", spec.name().endsWith("__c"));
                    field.put("createable", spec.createable());
                    field.put("updateable", spec.createable());
                    field.put("deprecatedAndHidden", false);
                    field.put("filterable", true);
                    field.put("sortable", true);
                    field.put("soapType", soapTypeFor(spec.type()));
                    field.put("nillable", true);
                    return field;
                })
                .toList();
    }

    @Transactional
    public Optional<SObjectRecord> update(String id, Map<String, Object> fields) {
        return repository.findById(id).map(record -> {
            Map<String, Object> existing = fromJson(record.getFieldsJson());
            existing.putAll(fields);
            Instant now = Instant.now();
            existing.put("LastModifiedDate", now.toString());
            record.setFieldsJson(toJson(existing));
            record.setLastModifiedDate(now);
            return repository.save(record);
        });
    }

    @Transactional
    public Optional<SObjectRecord> replace(String id, Map<String, Object> fields) {
        return repository.findById(id).map(record -> {
            Map<String, Object> existing = fromJson(record.getFieldsJson());
            Instant now = Instant.now();
            fields.put("Id", existing.get("Id"));
            fields.put("CreatedDate", existing.get("CreatedDate"));
            fields.put("LastModifiedDate", now.toString());
            record.setFieldsJson(toJson(fields));
            record.setLastModifiedDate(now);
            return repository.save(record);
        });
    }

    public synchronized UpsertResult upsert(
            String objectType,
            String externalIdField,
            String externalIdValue,
            Map<String, Object> fields) {
        return transactionTemplate.execute(status -> {
            Optional<SObjectRecord> existing = findByType(objectType).stream()
                    .filter(record -> externalIdValue.equals(String.valueOf(fromJson(record.getFieldsJson()).get(externalIdField))))
                    .findFirst();

            if (existing.isPresent()) {
                SObjectRecord updated = update(existing.get().getId(), mergeExternalId(fields, externalIdField, externalIdValue))
                        .orElseThrow();
                return new UpsertResult(updated, false);
            }

            SObjectRecord created = create(objectType, mergeExternalId(fields, externalIdField, externalIdValue));
            return new UpsertResult(created, true);
        });
    }

    @Transactional
    public boolean delete(String id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional
    public void reset() {
        repository.deleteAll();
        log.info("Org state reset — all sObject records deleted");
    }

    @Transactional
    public void reset(String orgId) {
        if (orgId != null) {
            repository.deleteByOrgId(orgId);
            log.info("Org state reset for org {} — sObject records deleted", orgId);
        } else {
            reset();
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> fromJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse fields JSON", e);
            return Map.of();
        }
    }

    private String toJson(Map<String, Object> fields) {
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (Exception e) {
            log.error("Failed to serialize fields", e);
            return "{}";
        }
    }

    private String generateId(String objectType) {
        String prefix = switch (objectType) {
            case "Account" -> "001";
            case "Contact" -> "003";
            case "User" -> "005";
            default -> objectType.length() >= 3
                    ? objectType.substring(0, 3).toUpperCase()
                    : objectType.toUpperCase();
        };
        return SalesforceIdGenerator.generate(prefix);
    }

    @SuppressWarnings("unchecked")
    private void addNestedValue(Map<String, Object> target, String fieldPath, Object value) {
        String[] parts = fieldPath.split("\\.");
        Map<String, Object> current = target;
        for (int index = 0; index < parts.length - 1; index++) {
            Object existing = current.get(parts[index]);
            if (!(existing instanceof Map<?, ?> existingMap)) {
                Map<String, Object> nested = new LinkedHashMap<>();
                current.put(parts[index], nested);
                current = nested;
            } else {
                current = (Map<String, Object>) existingMap;
            }
        }
        current.put(parts[parts.length - 1], value);
    }

    private String guessFieldType(Object value) {
        if (value == null) {
            return "string";
        }
        if (value instanceof Boolean) {
            return "boolean";
        }
        if (value instanceof Integer || value instanceof Long) {
            return "int";
        }
        if (value instanceof Float || value instanceof Double) {
            return "double";
        }
        if (value instanceof String stringValue && stringValue.matches("\\d{4}-\\d{2}-\\d{2}T.*Z")) {
            return "datetime";
        }
        return "string";
    }

    private String inferObjectTypeFromRelationship(String relationshipName) {
        if (relationshipName.endsWith("__r")) {
            return relationshipName.substring(0, relationshipName.length() - 3) + "__c";
        }
        return relationshipName;
    }

    private String soapTypeFor(String fieldType) {
        return switch (fieldType) {
            case "boolean" -> "xsd:boolean";
            case "int" -> "xsd:int";
            case "double" -> "xsd:double";
            case "datetime" -> "xsd:dateTime";
            default -> "xsd:string";
        };
    }

    private Map<String, Object> mergeExternalId(Map<String, Object> fields, String externalIdField, String externalIdValue) {
        Map<String, Object> merged = new LinkedHashMap<>(fields);
        merged.put(externalIdField, externalIdValue);
        return merged;
    }

    private record FieldSpec(String name, String label, String type, boolean createable) {
    }
}
