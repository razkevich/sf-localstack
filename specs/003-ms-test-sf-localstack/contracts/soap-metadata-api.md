# Contract: SOAP Metadata API (v66.0)

Endpoint: `POST /services/Soap/m/66.0`
Content-Type: `text/xml`
Namespace: `http://soap.sforce.com/2006/04/metadata`

All operations use the same endpoint and are disambiguated by request body content.

---

## describeMetadata

### Request (trigger)
Body contains `<describeMetadata/>` element.

### Response shape (must match `describemetadata-response.xml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns="http://soap.sforce.com/2006/04/metadata">
  <soapenv:Body>
    <describeMetadataResponse>
      <result>
        <metadataObjects>
          <directoryName>classes</directoryName>
          <inFolder>false</inFolder>
          <metaFile>true</metaFile>
          <suffix>cls</suffix>
          <xmlName>ApexClass</xmlName>
        </metadataObjects>
        <metadataObjects>
          <directoryName>classes</directoryName>
          <inFolder>false</inFolder>
          <metaFile>true</metaFile>
          <suffix>cls</suffix>
          <xmlName>EmailTemplate</xmlName>
        </metadataObjects>
        <organizationNamespace></organizationNamespace>
        <partialSaveAllowed>true</partialSaveAllowed>
        <testRequired>false</testRequired>
      </result>
    </describeMetadataResponse>
  </soapenv:Body>
</soapenv:Envelope>
```

**Shape gaps to fix in sf-localstack**:
- Add `<suffix>` element to `MetadataSoapRenderer.renderDescribeMetadata()` — currently missing
- `partialSaveAllowed` is `true` in fixture but `false` in current renderer — must match

---

## listMetadata

### Request (trigger)
Body contains `<listMetadata>` with one or more `<queries><type>ApexClass</type></queries>` elements.

### Response shape (must match `listmetadata-response.xml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns="http://soap.sforce.com/2006/04/metadata">
  <soapenv:Body>
    <listMetadataResponse>
      <result>
        <createdById>0053t000007KJshAAG</createdById>
        <createdByName>Vishnu Sharma</createdByName>
        <createdDate>2021-02-01T07:33:05.000Z</createdDate>
        <fileName>classes/SBQQ__CalculateCallbackAdapterTests.cls</fileName>
        <fullName>SBQQ__CalculateCallbackAdapterTests</fullName>
        <id>01p3t00000B6feyAAB</id>
        <lastModifiedById>0053t000008t01eAAA</lastModifiedById>
        <lastModifiedByName>Andreea Lazar</lastModifiedByName>
        <lastModifiedDate>2050-01-01T01:01:01.000Z</lastModifiedDate>
        <manageableState>unmanaged</manageableState>
        <namespacePrefix>SBQQ</namespacePrefix>
        <type>ApexClass</type>
      </result>
      <!-- additional <result> elements for each seeded metadata resource -->
    </listMetadataResponse>
  </soapenv:Body>
</soapenv:Envelope>
```

**Notes**: The exact `createdById`, `id`, `lastModifiedById` values are test-fixture-specific. Tests assert on `fileName`, `fullName`, `type`, `lastModifiedDate`, and `manageableState`. The seeded `MetadataCatalogEntry` records in sf-localstack drive this output.

---

## readMetadata (StandardValueSet)

### Request (trigger)
Body contains `<readMetadata>` with `<type>StandardValueSet</type>`.

### Response shape (must match `readmetadata-standardvalueset-response.xml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns="http://soap.sforce.com/2006/04/metadata"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <soapenv:Body>
    <readMetadataResponse>
      <result>
        <records xsi:type="StandardValueSet">
          <fullName>valueSet</fullName>
          <sorted>false</sorted>
          <standardValue>
            <default>false</default>
            <label>valueSet</label>
          </standardValue>
        </records>
      </result>
    </readMetadataResponse>
  </soapenv:Body>
</soapenv:Envelope>
```

**Shape gaps to fix**: sf-localstack's `renderReadRecord` for `StandardValueSet` omits `<sorted>false</sorted>`. Must add it between `<fullName>` and the `<standardValue>` entries.

---

## readMetadata (CustomField / Picklist)

### Request (trigger)
Body contains `<readMetadata>` with `<type>CustomField</type>`.

### Response shape (must match `readmetadata-picklist-response.xml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns="http://soap.sforce.com/2006/04/metadata"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <soapenv:Body>
    <readMetadataResponse>
      <result>
        <records xsi:type="CustomField">
          <fullName>picklistField</fullName>
          <deprecated>false</deprecated>
          <description>Field on Lookup Object corresponding to Product Action Type picklist...</description>
          <externalId>false</externalId>
          <inlineHelpText>Field on Lookup Object corresponding to Product Action Type picklist...</inlineHelpText>
          <label>Lookup Type Field</label>
          <required>false</required>
          <trackTrending>false</trackTrending>
          <type>Picklist</type>
          <valueSet>
            <valueSetDefinition>
              <sorted>false</sorted>
              <value>
                <fullName>SBQQ__Type__c</fullName>
                <default>false</default>
                <label>SBQQ__Type__c</label>
              </value>
            </valueSetDefinition>
          </valueSet>
        </records>
      </result>
    </readMetadataResponse>
  </soapenv:Body>
</soapenv:Envelope>
```

**Notes**: Test assertions should be verified — the current sf-localstack CustomField renderer is simplified. The seeded `MetadataResource` for this type must carry all required fields.

---

## cancelDeploy

### Request (trigger)
Body contains `<cancelDeploy>` with `<asyncProcessId>...</asyncProcessId>`.

### Response shape (must match `canceldeployresult-response.xml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns="http://soap.sforce.com/2006/04/metadata">
  <soapenv:Body>
    <cancelDeployResponse>
      <result>
        <done>false</done>
        <id>id</id>
      </result>
    </cancelDeployResponse>
  </soapenv:Body>
</soapenv:Envelope>
```

**Shape gaps to fix**: sf-localstack's `renderCancelDeploy` returns `<done>true</done><success>true</success>` but the fixture has `<done>false</done>` with no `<success>`. Verify test assertions — if tests only check HTTP 200 and envelope structure, the current renderer may be acceptable; otherwise adjust to match fixture exactly.

---

## Error / Unsupported Operation

```xml
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
  <soapenv:Body>
    <soapenv:Fault>
      <faultcode>soapenv:Client</faultcode>
      <faultstring>Unsupported metadata operation: &lt;operation-name&gt;</faultstring>
    </soapenv:Fault>
  </soapenv:Body>
</soapenv:Envelope>
```

Already implemented in sf-localstack as the `default` switch case.
