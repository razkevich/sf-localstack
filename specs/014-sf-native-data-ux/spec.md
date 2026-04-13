# Feature Specification: Salesforce-Native Data & Metadata UX

**Feature Branch**: `014-sf-native-data-ux`
**Created**: 2026-04-12
**Status**: Draft
**Input**: User description: "Make the sf-localstack UI intuitive for Salesforce users — Object Manager as central hub, fix field createability, custom object creation from UI, field management, expanded standard object catalog."

## Compatibility Context *(mandatory)*

- **Salesforce Surface**: REST (sObject CRUD, describe), Metadata (CustomObject deploy), UI (Object Manager, record forms)
- **Compatibility Target**: Salesforce Lightning Experience Object Manager and record creation patterns. The UI should feel immediately familiar to anyone who has used Salesforce Setup > Object Manager.
- **In-Scope Operations**:
  - Expand Object Manager to show all standard objects and custom objects from metadata
  - Fix Name field createability on Account, Contact, and other standard objects
  - Add "Create Custom Object" flow in the UI that deploys metadata and makes the object immediately available
  - Add field list view for each object (describe-based)
  - Expand standard object catalog (add Case, User, Task, Event)
- **Out-of-Scope Operations**: Custom field creation/deletion, formula fields, validation rules, page layouts, record types, Lightning app builder, permission sets
- **API Shape Commitments**: No API changes needed — the frontend consumes existing describe and metadata endpoints. The only backend change is fixing the field catalog (Name createable=true) and expanding the standard object list.
- **Frontend Scope**: Enhanced Object Manager view, "New Custom Object" modal, field list view per object, improved record creation form with Name field at top.
- **Test Isolation Plan**: Integration tests verify: standard objects listed correctly, custom object creation via metadata deploys correctly, Name field appears in create forms, field describe returns correct createable flags.
- **Parity Verification Plan**: Compare Object Manager contents and record creation form layout with real Salesforce Lightning Experience Object Manager.

## Feature Iterations *(mandatory)*

### Feature 0 - Fix Field Catalog & Expand Standard Objects (Priority: P1)

Fix the Account Name field so it's marked as createable (currently `false`, should be `true`). Apply the same fix to all standard objects. Expand the standard object catalog to include Case, User, Task, and Event so they appear in Object Manager even without records.

**Why this priority**: The Name field bug blocks record creation from the UI. This is the most impactful single fix — without it, the form is unusable for the most common operation.

**Independent Test**: Open the Account creation form, verify "Account Name" appears as the first field. Create a record with just a Name. Verify it saves and appears in the list.

**Acceptance Scenarios**:

1. **Given** a user opens the "New Account" form, **When** the form loads, **Then** the "Account Name" field appears at the top of the form and is editable.
2. **Given** a user fills in only the Name field and clicks Save, **When** the save completes, **Then** the record appears in the Account list with the name shown.
3. **Given** the Object Manager is loaded, **When** the user views the standard objects section, **Then** Account, Contact, Lead, Opportunity, Case, User, Task, and Event all appear (even with 0 records).
4. **Given** a user opens the Contact creation form, **When** the form loads, **Then** First Name, Last Name, and Email appear prominently at the top.

---

### Feature 1 - Create Custom Object from UI (Priority: P1)

Add a "New Custom Object" button to the Object Manager. Clicking it opens a modal where the user enters the object label and API name. On save, the system creates a CustomObject metadata resource and the object immediately appears in Object Manager and is queryable via SOQL/REST.

**Why this priority**: Custom object creation is a core Salesforce workflow. Users need to create custom objects to model their data, and this should work from the UI without resorting to metadata API calls.

**Independent Test**: Click "New Custom Object" in Object Manager, enter label "Invoice" and API name "Invoice__c", save. Verify it appears in Object Manager. Create a record for Invoice__c. Query it back.

**Acceptance Scenarios**:

1. **Given** the Object Manager view, **When** the user clicks "New Custom Object", **Then** a modal appears with fields for Label, Plural Label, and API Name (auto-suffixed with `__c`).
2. **Given** the user fills in the custom object form and saves, **When** the save completes, **Then** the object immediately appears in the "Custom Objects" section of Object Manager with 0 records.
3. **Given** a custom object exists, **When** the user clicks on it in Object Manager, **Then** they see the record list view (same as standard objects) with a "+ New" button to create records.
4. **Given** a custom object exists, **When** the user creates a record with a Name field, **Then** the record saves and appears in the list and is queryable via `SELECT Id, Name FROM Invoice__c`.

---

### Feature 2 - Field List View (Priority: P2)

Add a "Fields" tab or section to each object in Object Manager that shows all fields from the describe response. Display field name, label, type, and whether it's custom or standard. This gives users visibility into the data model without needing to call the describe API directly.

**Why this priority**: Field visibility is essential for understanding and working with data. Salesforce users expect to see fields listed when they navigate to an object in Object Manager.

**Independent Test**: Navigate to Account in Object Manager, click "Fields" or a fields tab, verify all 48 describe fields are listed with their types.

**Acceptance Scenarios**:

1. **Given** the user is viewing an object's record list, **When** they access the fields view, **Then** all fields from the describe response are listed in a sortable table.
2. **Given** the fields list is displayed, **When** the user looks at a field row, **Then** they see the field API name, label, data type, and a badge indicating "Standard" or "Custom".
3. **Given** a custom object with both standard fields (Id, Name, CreatedDate) and custom fields, **When** the fields list loads, **Then** both standard and custom fields are shown with correct badges.

---

### Feature 3 - Improved Record Form Layout (Priority: P2)

Reorganize the record creation and edit forms to place the most important fields at the top: Name (or FirstName/LastName for Contacts), followed by commonly-used fields, then remaining fields alphabetically. This matches the Salesforce Lightning Experience pattern where key fields are prominent.

**Why this priority**: The current alphabetical layout buries the Name field. Salesforce users expect Name at the top of every form.

**Independent Test**: Open "New Account" form — Name is first. Open "New Contact" form — First Name, Last Name, Email are first. Open a custom object form — Name is first.

**Acceptance Scenarios**:

1. **Given** the Account creation form, **When** it loads, **Then** "Account Name" is the first field, followed by Industry, Phone, Website, then remaining fields.
2. **Given** the Contact creation form, **When** it loads, **Then** First Name, Last Name, and Email are the top three fields.
3. **Given** a custom object creation form, **When** it loads, **Then** "Name" is the first field.

---

### Edge Cases

- What happens when a custom object API name conflicts with an existing object? The system returns an error message and the modal stays open.
- What happens when the user enters an API name without the `__c` suffix? The system auto-appends `__c`.
- What happens when the standard object catalog doesn't match the describe fields? The describe endpoint always returns the fields defined in the catalog; the UI renders whatever describe returns.
- What happens when a custom object has no records? It still appears in Object Manager with "0 records" and the user can create records.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The Name field on Account MUST be marked as createable so it appears in the record creation form
- **FR-002**: The Name field MUST be the first field displayed in record creation and edit forms for all objects
- **FR-003**: The standard object catalog MUST include at minimum: Account, Contact, Lead, Opportunity, Case, User, Task, Event
- **FR-004**: All standard objects MUST appear in Object Manager even when they have 0 records
- **FR-005**: Custom objects from metadata MUST appear in Object Manager with accurate record counts
- **FR-006**: Users MUST be able to create a new custom object from the Object Manager UI by entering a label and API name
- **FR-007**: A newly created custom object MUST be immediately visible in Object Manager and queryable via REST/SOQL without application restart
- **FR-008**: Each object in Object Manager MUST provide a way to view its field list (name, label, type, standard/custom badge)
- **FR-009**: The Contact creation form MUST show First Name, Last Name, and Email as the top fields
- **FR-010**: Custom object creation forms MUST show the Name field as the first field

### Key Entities

- **Standard Object Catalog**: The built-in set of Salesforce object definitions with their field specifications. Expanded to include Case, User, Task, Event alongside existing Account, Contact, Lead, Opportunity.
- **Custom Object**: A user-defined sObject created through the metadata system. Has a `__c` suffix, a label, and appears alongside standard objects in the Object Manager.

## Assumptions

- Custom objects are created as metadata resources of type "CustomObject". The metadata deploy flow already exists — the UI simply calls it.
- Custom objects created via the UI get a default set of fields: Id, Name, CreatedDate, LastModifiedDate, OwnerId (same as standard objects).
- The field list view is read-only. Custom field creation/deletion is out of scope for this feature.
- The "improved form layout" uses a priority ordering system: key fields first (Name, common fields), then remaining fields alphabetically. No drag-and-drop layout customization.
- The Case, User, Task, and Event objects use a similar field catalog structure to Account/Contact, with appropriate fields for each (e.g., Case has Subject, Status, Priority; Task has Subject, DueDate; Event has Subject, StartDateTime).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can create an Account record from the UI in under 30 seconds (Name field visible and first in form)
- **SC-002**: All 8 standard objects appear in Object Manager on first load, regardless of whether they have records
- **SC-003**: Users can create a custom object from the UI and have a record in it within 60 seconds (no API calls or restarts needed)
- **SC-004**: Every object in Object Manager shows an accurate record count that matches SOQL query results
- **SC-005**: The field list for Account shows at least 40 fields with correct types and standard/custom badges
- **SC-006**: All integration tests pass in CI covering: standard object catalog, custom object creation, Name field createability, field list display
