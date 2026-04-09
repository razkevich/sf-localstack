# Feature Specification: Salesforce-Familiar UI

**Feature Branch**: `011-salesforce-ui`
**Status**: Draft
**Created**: 2026-04-09

## Compatibility Context *(mandatory)*

- **Salesforce Surface**: Frontend only (consumes REST, Bulk API v2, and Metadata backend APIs)
- **Compatibility Target**: Salesforce Lightning Experience UI patterns (visual familiarity, not pixel-perfect clone)
- **In-Scope Operations**: Complete UI redesign with SLDS-inspired design system; all existing views rebuilt (data manager, metadata manager, bulk explorer, REST explorer, request log)
- **Out-of-Scope Operations**: Full SLDS component library adoption, custom object/field definition UI, mobile responsiveness, accessibility compliance (deferred)
- **Test Isolation Plan**: Frontend component tests with Vitest; integration tested via existing backend test profile. No backend changes required for this feature.
- **Parity Verification Plan**: Visual comparison with real Salesforce Lightning UI for layout and navigation patterns. Key checks: sidebar navigation structure, table column headers and row actions, record detail two-column layout, form control types per field type.

## Feature Iterations *(mandatory)*

### Feature 0 - Design System & Tailwind Config (Priority: P1)

Extend `tailwind.config.js` with SLDS-aligned design tokens covering color, typography, spacing, border radius, and shadows. Create reusable CSS utility classes for SLDS patterns. This is the foundation all subsequent iterations depend on.

**Why this priority**: Nothing else in the redesign can be built consistently without shared design tokens. All components must reference the same color and spacing scale.

**Design tokens to add**:

| Token | Value | Tailwind class prefix |
|---|---|---|
| Brand blue | `#0176d3` | `sf-brand` |
| Success green | `#2e844a` | `sf-success` |
| Warning orange | `#fe9339` | `sf-warning` |
| Error red | `#ea001e` | `sf-error` |
| Destructive red | `#ba0517` | `sf-destructive` |
| Neutral 100 | `#ecebea` | `sf-neutral-100` |
| Neutral 200 | `#dddbda` | `sf-neutral-200` |
| Neutral 300 | `#c9c7c5` | `sf-neutral-300` |
| Neutral 400 | `#b0adab` | `sf-neutral-400` |
| Neutral 500 | `#969492` | `sf-neutral-500` |
| Neutral 600 | `#706e6b` | `sf-neutral-600` |
| Neutral 700 | `#514f4d` | `sf-neutral-700` |
| Neutral 800 | `#3e3e3c` | `sf-neutral-800` |
| Neutral 900 | `#201b1b` | `sf-neutral-900` |

**Typography**:
- Font stack: `-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif`
- Heading sizes: `2rem` (h1), `1.5rem` (h2), `1.25rem` (h3), `1rem` (h4), `0.875rem` (small/label)
- Body: `0.875rem` default

**Spacing**:
- Base grid: 4px (`0.25rem` increments), matching SLDS `$spacing-xxx` scale

**Border radius**:
- Default: `0.25rem`
- Large: `0.5rem`

**Shadows**:
- Small: `0 2px 4px rgba(0,0,0,0.1)`
- Medium: `0 4px 8px rgba(0,0,0,0.1)`

**Reusable CSS utility classes to define** (in a global stylesheet or Tailwind `@layer components`):
- `.sf-badge` — pill badge base styles
- `.sf-card` — white card with border and shadow-sm
- `.sf-section-header` — gray section title bar
- `.sf-field-label` — uppercase gray label text
- `.sf-field-value` — black body text for record values

**Acceptance Scenarios**:

1. **Given** the updated `tailwind.config.js`, **When** a component uses `bg-sf-brand`, **Then** it renders with hex `#0176d3`.
2. **Given** the design token config, **When** building any component in F1-F9, **Then** no hard-coded hex color values appear in component source — all colors reference Tailwind token classes.
3. **Given** the global stylesheet, **When** `.sf-badge` is applied, **Then** the element renders with pill shape, correct padding, and text size matching SLDS badge style.

---

### Feature 1 - App Shell & Navigation (Priority: P1)

Implement the global layout shell that wraps all views: a fixed header bar, a left sidebar for navigation, a main content area with breadcrumbs, and a footer with status information.

**Why this priority**: Without the shell, no other view can be composed into a coherent app. Every subsequent iteration renders inside this shell.

**Global header bar**:
- Left: application logo / name ("sf localstack")
- Center: global search input (placeholder for future use — renders but does not search)
- Right: setup gear icon (links to `/setup`) + user menu button showing username

**User menu dropdown** (positioned below user button):
- Username display
- Role badge (e.g., "Admin", "Developer")
- Logout button (clears auth token, redirects to login)

**Left sidebar**:
- Object navigation section: links for Account, Contact, Lead, Opportunity, and any custom object types returned by `GET /services/data/v60.0/sobjects` — each navigates to `/objects/:objectType`
- Section divider — "Platform"
- Metadata link → `/metadata`
- Bulk Jobs link → `/bulk`
- API Log link → `/log`
- Section divider — "Admin"
- Setup link → `/setup`
- Active item: left border in brand color (`border-l-4 border-sf-brand`), background `bg-sf-neutral-100`

**Main content area**:
- Breadcrumb trail rendered below header, above page content
- Breadcrumb items: Home > ObjectType > Record Name (context-dependent)
- Content area scrollable independently of fixed header and sidebar

**Footer**:
- App version (from build metadata or env variable)
- API version (e.g., "v60.0")
- Connection status indicator: green dot + "Connected" when backend reachable, red dot + "Disconnected" otherwise (health-check ping to `/actuator/health` every 30s)

**Acceptance Scenarios**:

1. **Given** the app loads, **When** any route is visited, **Then** the header, sidebar, main content area, and footer are all visible.
2. **Given** the sidebar is rendered, **When** the user clicks "Account", **Then** the browser navigates to `/objects/Account` and the Account link receives the active highlight style.
3. **Given** the user is on `/objects/Account/001xx000000001`, **When** the breadcrumb renders, **Then** it shows: Home > Account > [record name].
4. **Given** the backend is unreachable, **When** the footer health check runs, **Then** the status indicator shows a red dot and "Disconnected".
5. **Given** the user opens the user menu, **When** they click Logout, **Then** the auth token is cleared and they are redirected to the login page.

---

### Feature 2 - DataTable Component (Priority: P1)

Create a reusable `DataTable` React component that displays tabular record data with sorting, pagination, search, row actions, and empty/loading states. This component is used by every list view in the app.

**Why this priority**: DataTable is the most-used shared component. Object List View (F3), Bulk Job Monitor (F7), and API Log (F8) all depend on it. It must exist before those views can be built.

**Component API** (`DataTable` props):
- `columns: Column[]` — column definitions: `{ key, label, sortable?, width?, render? }`
- `data: Record<string, unknown>[]` — row data
- `loading?: boolean` — show skeleton rows when true
- `onRowClick?: (row) => void` — navigate to detail on row click
- `rowActions?: RowAction[]` — items for the three-dot row action menu: `{ label, icon?, onClick, destructive? }`
- `pagination?: PaginationConfig` — `{ page, pageSize, total, onPageChange, onPageSizeChange }`
- `searchable?: boolean` — show search bar above table when true
- `onSearch?: (query: string) => void` — called when search input changes (debounced 300ms)
- `emptyMessage?: string` — custom empty state message (default: "No records to display")
- `selectable?: boolean` — (P3, optional) show checkbox column for bulk selection

**Table structure**:
- Header row: gray background (`bg-sf-neutral-100`), bold labels, sortable columns show up/down arrow on hover; active sort column shows directional arrow indicator
- Body rows: white background, hover state `bg-sf-neutral-100`; alternating row background is acceptable but not required
- First column frozen with sticky positioning when horizontal scroll is active

**Pagination controls** (rendered below table):
- Page size selector: 10 / 25 / 50 / 100 per page
- Page navigation: Previous / Next buttons + current page indicator ("Page 2 of 14")
- Record count label: "Showing 26-50 of 340 records"

**Row actions menu** (three-dot `⋯` icon, right-aligned in each row):
- Opens a dropdown on click
- Standard actions passed via `rowActions` prop; destructive actions rendered in red
- Closes on outside click or ESC

**Empty state**:
- Centered layout with an illustration icon (from Lucide)
- Primary message: "No records to display" (or `emptyMessage` prop)
- Optional CTA button if `onEmpty` prop is provided

**Loading state**:
- Skeleton rows (animated gray bars) matching the configured column count
- Shows when `loading={true}`

**Acceptance Scenarios**:

1. **Given** `DataTable` receives column definitions and an array of row data, **When** rendered, **Then** rows and sortable column headers are displayed with correct SLDS table styling.
2. **Given** the user clicks a sortable column header, **When** the sort toggles, **Then** the sort arrow indicator updates and `column.sortable` callback is triggered with the new sort direction.
3. **Given** `loading={true}`, **When** `DataTable` renders, **Then** skeleton rows are displayed instead of data rows.
4. **Given** `data={[]}` and `loading={false}`, **When** rendered, **Then** the empty state message and icon are displayed.
5. **Given** a row's three-dot menu is opened, **When** the user clicks a row action, **Then** `action.onClick` is called with the row's data.
6. **Given** pagination is configured with `pageSize=25` and `total=340`, **When** rendered, **Then** the label "Showing 1-25 of 340 records" and page navigation controls are visible.

---

### Feature 3 - Object List View (Priority: P1)

Implement the list view page for any sObject type at URL `/objects/:objectType`. Fetches records via SOQL and displays them in `DataTable` with describe-driven columns.

**Why this priority**: List view is the primary entry point for all data interaction. Users cannot access records without it.

**Page structure**:
- Page header (`PageHeader` component): object display name (from describe), record count badge, "New" button (right-aligned)
- `DataTable` showing records with columns derived from describe metadata
- Click any row → navigate to `/objects/:objectType/:recordId`
- "New" button → opens record creation modal (uses `RecordForm` from F5)

**Column selection logic**:
- Fetch describe from `GET /services/data/v60.0/sobjects/:objectType/describe`
- Default columns: `Name` field (or first `string`-type field if no `Name`), then any fields marked `nameField` or `externalId`, then `CreatedDate`, `LastModifiedDate`
- Column picker button (gear icon) allows user to add/remove columns from the describe field list — selection stored in `localStorage` per object type

**Data fetch**:
- SOQL: `SELECT [selected fields] FROM :objectType ORDER BY LastModifiedDate DESC LIMIT :pageSize OFFSET :offset`
- Search bar sends: `SELECT [fields] FROM :objectType WHERE Name LIKE '%:query%' ORDER BY LastModifiedDate DESC`
- Pagination handled client-side from SOQL `totalSize`

**Row actions** (passed to `DataTable`):
- "View" → navigate to record detail
- "Edit" → open record edit modal
- "Delete" → open confirmation modal → DELETE record → show success toast, refresh table

**Empty state CTA**: "Create your first :objectType" button (triggers same create modal as "New" button)

**Acceptance Scenarios**:

1. **Given** Account records exist in the emulator, **When** the user navigates to `/objects/Account`, **Then** a paginated table of Account records is displayed with Name and system date columns.
2. **Given** the table is rendered, **When** the user types in the search bar, **Then** results are filtered after a 300ms debounce and the record count updates.
3. **Given** the user clicks a row, **When** navigation occurs, **Then** the browser routes to `/objects/Account/:recordId`.
4. **Given** no records exist for the object type, **When** the list view renders, **Then** the DataTable empty state with "Create your first Account" CTA is displayed.
5. **Given** the user clicks "New", **When** the modal opens, **Then** a `RecordForm` renders with fields appropriate for the object type.
6. **Given** the user clicks "Delete" in a row's action menu, **When** they confirm the modal, **Then** the record is deleted via `DELETE /services/data/v60.0/sobjects/:objectType/:recordId` and a success toast appears.

---

### Feature 4 - Record Detail Page (Priority: P1)

Implement the record detail page at `/objects/:objectType/:recordId` with view mode, edit mode, and delete capability. Layout follows SLDS two-column record layout.

**Why this priority**: Record detail is the second core data interaction view. Together with list view, it completes the read/write data interaction loop.

**Page structure**:
- Page header (`PageHeader` component): record name, breadcrumb (`Home > :objectType > :recordName`), Edit and Delete buttons (right-aligned)
- Two-column grid of collapsible sections:
  - "Details" section: key describe fields in label-value pairs (4-column grid on wide screens: two label+value pairs per row)
  - "System Information" section: `Id`, `CreatedDate`, `LastModifiedDate`, `CreatedById`, `LastModifiedById`

**View mode — field rendering by type**:
- `string`, `textarea`: plain text (textarea values preserve newlines)
- `number`, `currency`, `percent`: formatted with locale (e.g., "1,234.56", "$1,234.56", "12.5%")
- `date`: formatted as "Apr 9, 2026"
- `datetime`: formatted as "Apr 9, 2026, 10:34 AM"
- `boolean`: "Yes" / "No" text (not a checkbox in view mode)
- `email`, `phone`, `url`: rendered as `<a>` links with appropriate `href` (mailto:, tel:, url)
- Long text (> 255 chars) truncated to 3 lines with "Show more" expansion

**Edit mode** (activated by "Edit" button):
- Header Edit button changes to "Save" and "Cancel"
- Field label-value pairs convert to form controls (see `RecordForm` in F5 for control-type mapping)
- "Save" → `PATCH /services/data/v60.0/sobjects/:objectType/:recordId` with changed fields only
- On success: exit edit mode, show success toast "Record saved"
- On backend validation error: display error message under the relevant field
- "Cancel" → revert all changes, exit edit mode

**Delete flow**:
- "Delete" button → open `Modal` (F10) confirmation: "Delete this [objectType]? This cannot be undone."
- Confirm → `DELETE /services/data/v60.0/sobjects/:objectType/:recordId`
- On success: show success toast, navigate back to list view `/objects/:objectType`

**Acceptance Scenarios**:

1. **Given** a valid record ID, **When** the page loads, **Then** all field values are displayed in two-column layout with correct type formatting.
2. **Given** the user clicks "Edit", **When** edit mode activates, **Then** all editable fields become form controls appropriate to their field type.
3. **Given** edit mode is active and the user changes a field, **When** they click "Save", **Then** only the changed fields are sent in the PATCH body.
4. **Given** the backend returns a validation error on save, **When** the error is received, **Then** the error message is displayed inline under the relevant field, not in a generic alert.
5. **Given** the user clicks "Delete" and confirms the modal, **When** the DELETE request succeeds, **Then** a success toast appears and the browser navigates to the list view.
6. **Given** a URL field value, **When** rendered in view mode, **Then** it is displayed as a clickable link opening in a new tab.

---

### Feature 5 - RecordForm Component (Priority: P2)

Create a reusable `RecordForm` React component that renders dynamic form controls based on Salesforce describe metadata. Used by record creation modals, inline record edit (F4), and any other form-driven workflow.

**Why this priority**: Multiple views (F3 create, F4 edit) need dynamic forms. Centralizing the logic prevents duplicated field-type-to-control mapping.

**Component API** (`RecordForm` props):
- `fields: FieldDescribe[]` — field metadata from the describe endpoint
- `values: Record<string, unknown>` — current field values (controlled)
- `onChange: (fieldName: string, value: unknown) => void` — called on any field change
- `errors?: Record<string, string>` — field-level error messages (displayed inline)
- `mode: 'create' | 'edit'` — affects which fields are required and visible
- `visibleFields?: string[]` — if provided, only render these fields (for section layouts)

**Field type to control mapping**:

| Salesforce field type | Control rendered |
|---|---|
| `string` | `<input type="text">` |
| `textarea` | `<textarea>` |
| `int`, `double`, `currency` | `<input type="number">` |
| `date` | `<input type="date">` |
| `datetime` | `<input type="datetime-local">` |
| `boolean` | Toggle switch (custom component) |
| `picklist` | `<select>` with options from `picklistValues` |
| `multipicklist` | Multi-select checkboxes or listbox |
| `email` | `<input type="email">` |
| `phone` | `<input type="tel">` |
| `url` | `<input type="url">` |
| `reference` (lookup) | Text input with "(lookup)" placeholder — full lookup UI is out of scope |
| `id` | Read-only display in edit mode, hidden in create mode |

**Validation**:
- Required fields: marked with red asterisk (`*`) on label; validated on form submit
- Field length: `length` property from describe enforced on text inputs via `maxLength`
- Inline error display: red text below the field with error icon

**Layout**:
- Two-column grid by default (matching record detail layout in F4)
- Full-width for `textarea` and `multipicklist`
- Field label above control (not inline)

**Acceptance Scenarios**:

1. **Given** a describe response with a `picklist` field, **When** `RecordForm` renders, **Then** the field renders as a `<select>` with options from `picklistValues`.
2. **Given** a required field is left empty, **When** the form is submitted, **Then** an asterisk label and "This field is required" error appear under the field without making a network request.
3. **Given** `errors={{ Name: "Duplicate value found" }}`, **When** the component renders, **Then** "Duplicate value found" is displayed in red beneath the Name field.
4. **Given** a `boolean` field, **When** rendered in `RecordForm`, **Then** it appears as a toggle switch, not a text input.
5. **Given** `mode='create'`, **When** rendering an `id`-type field, **Then** it is not shown in the form.

---

### Feature 6 - Metadata Manager Redesign (Priority: P2)

Replace the existing raw JSON editor in the metadata manager with structured table views and attribute forms. Browsing and editing metadata resources must not require reading or writing JSON.

**Why this priority**: FR-001 and FR-002 prohibit raw JSON in the UI. The current metadata manager violates both.

**Page structure** (URL: `/metadata`):
- Left panel: metadata type list (ApexClass, CustomObject, Layout, FlexiPage, PermissionSet, etc.) — fetched from `describeMetadata`; click to select
- Right panel: table of resources for the selected type with columns: Name, Label, LastModified, Actions
- Click resource row → open detail panel (right slide-out or inline) with attribute form

**Resource detail panel**:
- Header: resource name, type badge, "Edit" button, "Delete" button
- Attribute form: key fields rendered with `RecordForm`-style controls (field type determined by metadata type schema)
- Not all metadata types will have editable attributes — for unsupported types, show a read-only list of known attributes

**Deploy/Retrieve status panel**:
- A collapsible status section at the bottom showing recent deploy/retrieve job status
- Columns: Job ID, Type (Deploy/Retrieve), State, Started, Completed
- State badges: Pending (gray), InProgress (yellow), Succeeded (green), Failed (red)
- Click job → show log output in a `<pre>` block (not a JSON editor)

**Acceptance Scenarios**:

1. **Given** metadata resources exist, **When** the user selects "ApexClass" from the type list, **Then** a table of ApexClass resources is displayed with Name, Label, and LastModified columns.
2. **Given** the user clicks a resource row, **When** the detail panel opens, **Then** resource attributes are displayed as labeled fields, not raw JSON.
3. **Given** the user edits a metadata attribute and saves, **When** the save request succeeds, **Then** a success toast is shown and the resource table refreshes.
4. **Given** a deploy job is in progress, **When** the status panel is visible, **Then** the job row shows state "InProgress" with a yellow badge.

---

### Feature 7 - Bulk Job Monitor Redesign (Priority: P2)

Replace the existing bulk explorer with a Lightning-style job monitor featuring a status table and a detail panel for each job. State must be communicated via color-coded badges.

**Why this priority**: FR-001 prohibits raw JSON display. The current bulk explorer shows raw job JSON.

**Page structure** (URL: `/bulk`):
- Page header: "Bulk Jobs", record count badge, "New Job" button
- `DataTable` of all bulk jobs with columns: Job ID (truncated with tooltip), Object, Operation, State (badge), Records Processed, Created
- Click job row → open detail panel

**State badges**:

| State string | Badge color |
|---|---|
| `Open` | Blue (`bg-sf-brand`) |
| `UploadComplete` | Yellow (`bg-sf-warning`) |
| `InProgress` | Yellow (`bg-sf-warning`) |
| `JobComplete` | Green (`bg-sf-success`) |
| `Failed` | Red (`bg-sf-error`) |
| `Aborted` | Gray (`bg-sf-neutral-500`) |

**Job detail panel** (slide-out or inline section below table):
- Job metadata: ID, Object, Operation, State, API version, Created, Completed
- CSV upload area (enabled only when job state is `Open` or `UploadComplete`): drag-and-drop file input or click-to-browse
- Status timeline: ordered list of state transitions with timestamps
- Results download buttons: "Successful Results (CSV)", "Failed Results (CSV)", "Unprocessed Records (CSV)" — enabled when job is `JobComplete`

**"New Job" creation form** (modal):
- Fields: Object (text input), Operation (select: insert / update / upsert / delete), External ID field (shown only when operation is upsert)
- Submit → `POST /services/data/v60.0/jobs/ingest` → on success, close modal and open the new job's detail panel

**Acceptance Scenarios**:

1. **Given** bulk jobs exist, **When** the user navigates to `/bulk`, **Then** all jobs are displayed in a table with State column rendered as colored badges.
2. **Given** a job with state `JobComplete`, **When** the user clicks its row, **Then** the detail panel shows "Successful Results (CSV)" and "Failed Results (CSV)" download buttons (enabled).
3. **Given** a job with state `Open`, **When** the user views the detail panel, **Then** the CSV upload area is active and accepts file input.
4. **Given** the user clicks "New Job" and fills in Object and Operation, **When** they submit the form, **Then** a `POST /services/data/v60.0/jobs/ingest` request is made and the new job appears in the table.

---

### Feature 8 - API Request Log Redesign (Priority: P2)

Replace the existing request log view with an SLDS-styled live table using the existing SSE stream. Add filtering controls and a slide-out detail panel.

**Why this priority**: FR-001 prohibits raw JSON. The current log displays raw request/response JSON inline.

**Page structure** (URL: `/log`):
- Page header: "API Request Log", pause/resume toggle, "Clear" button
- Filter bar: Method dropdown (ALL, GET, POST, PATCH, PUT, DELETE), Status range (ALL, 2xx, 4xx, 5xx), Path search input
- `DataTable` of log entries (live-updating via SSE) with columns: Timestamp, Method (badge), Path, Status (badge), Duration
- Click row → slide-out detail panel

**Method badges**:

| Method | Badge color |
|---|---|
| GET | Gray (`bg-sf-neutral-400`) |
| POST | Blue (`bg-sf-brand`) |
| PATCH | Yellow (`bg-sf-warning`) |
| PUT | Purple (custom token) |
| DELETE | Red (`bg-sf-error`) |

**Status badges**:
- 2xx → green (`bg-sf-success`)
- 4xx → yellow (`bg-sf-warning`)
- 5xx → red (`bg-sf-error`)

**Detail panel** (slide-out from right, 40% width):
- Request section: Method, URL, Headers table, Body (formatted JSON in `<pre>` with syntax highlighting — this is the only place JSON is acceptable in the UI)
- Response section: Status, Headers table, Body (formatted JSON in `<pre>`)
- Panel closes on ESC or clicking outside

**Live update behavior**:
- "Pause" button stops appending new rows (existing rows remain)
- "Resume" button re-connects to SSE and appends new events
- Rows inserted at top (newest first)
- Maximum 500 rows in the table; older rows are removed when limit is exceeded

**Acceptance Scenarios**:

1. **Given** the SSE stream is active, **When** a new API request is made to the emulator, **Then** a new row appears at the top of the log table within 1 second.
2. **Given** the user selects "POST" in the Method filter, **When** filtering is applied, **Then** only POST rows are visible in the table.
3. **Given** the user clicks "Pause", **When** new API requests occur, **Then** no new rows appear in the table until "Resume" is clicked.
4. **Given** the user clicks a log row, **When** the detail panel opens, **Then** request and response bodies are displayed as formatted JSON (not raw strings).

---

### Feature 9 - Setup & Admin Page (Priority: P3)

Implement the setup page at `/setup` with user management, org settings display, and a system reset action. Follows SLDS settings page layout.

**Why this priority**: P3 — admin functionality useful but not critical for core data workflows.

**Page structure** (URL: `/setup`):
- Left nav: section links within the page (User Management, Org Settings, System Reset)
- Content area: scrollable sections

**User Management section** (admin role only):
- `DataTable` of users: Username, Role, Created, Actions (Remove)
- "Add User" button → modal with username and role fields
- Remove user → confirmation modal → DELETE user

**Org Settings section**:
- Read-only display: API version, App version, Org ID (if available), connection string
- Displayed as labeled fields in `sf-card` layout (not editable — emulator settings are environment-driven)

**System Reset section**:
- Warning text: "This will delete all records and bulk jobs from the emulator. This cannot be undone."
- "Reset System" button (destructive red styling)
- Click → confirmation modal with typed confirmation: user must type "RESET" to enable the confirm button
- On confirm → call reset endpoint → show success toast → reload app

**Acceptance Scenarios**:

1. **Given** an admin user visits `/setup`, **When** the page loads, **Then** User Management, Org Settings, and System Reset sections are all visible.
2. **Given** the admin clicks "Add User", **When** they fill in the form and submit, **Then** a new user row appears in the User Management table.
3. **Given** the user clicks "Reset System", **When** the confirmation modal appears, **Then** the confirm button is disabled until the user types "RESET" in the text field.

---

### Feature 10 - Toast Notifications & Modals (Priority: P3)

Implement shared `Toast` and `Modal` components used across all views for mutation feedback and destructive action confirmation.

**Why this priority**: Many other iterations (F3, F4, F6, F7, F9) depend on toast and modal. Defining them here keeps the dependency explicit.

**Toast component**:
- Variants: `success` (green), `error` (red), `warning` (yellow), `info` (blue)
- Position: fixed, top-right corner, stacks vertically if multiple toasts active
- Slide-in animation from right
- Auto-dismiss after 5 seconds; dismiss immediately on close button click
- Global toast queue managed via React context (`ToastProvider`) — components call `useToast().show({ message, variant })`

**Modal component**:
- Overlay: semi-transparent black backdrop
- Structure: header (title + close icon), body (slot for content), footer (slot for action buttons)
- Close on: close icon click, ESC key, backdrop click (configurable via `closeOnBackdrop` prop)
- `Modal` props: `open`, `onClose`, `title`, `children`, `footer`

**Confirmation dialog variant**:
- Built on `Modal` — `ConfirmModal` component
- Props: `open`, `onClose`, `onConfirm`, `title`, `message`, `confirmLabel`, `destructive?`
- When `destructive={true}`: confirm button styled with `bg-sf-destructive`

**Typed confirmation variant** (`TypedConfirmModal`):
- Props: all `ConfirmModal` props + `confirmPhrase: string`
- Renders a text input below the message; confirm button disabled until input value matches `confirmPhrase`

**Acceptance Scenarios**:

1. **Given** `useToast().show({ message: "Record saved", variant: "success" })` is called, **When** the toast renders, **Then** a green toast with "Record saved" appears in the top-right corner.
2. **Given** a toast is displayed, **When** 5 seconds elapse, **Then** the toast is automatically dismissed.
3. **Given** `<ConfirmModal destructive onConfirm={handleDelete}>` is rendered, **When** the confirm button is visible, **Then** it has red (`bg-sf-destructive`) styling.
4. **Given** `<TypedConfirmModal confirmPhrase="RESET">` is rendered, **When** the user has not typed "RESET", **Then** the confirm button is disabled.
5. **Given** a modal is open, **When** the user presses ESC, **Then** `onClose` is called and the modal closes.

---

### Edge Cases

- **What about very long field values in tables?** Truncate at approximately 80 characters with an ellipsis; show full value in a browser tooltip (`title` attribute) on hover. Full value is always shown on the record detail page.
- **What about objects with many columns?** Default to key columns (Name, primary identifier fields, CreatedDate, LastModifiedDate). A column picker button (gear icon in table header) allows adding/removing columns; selection is persisted in `localStorage` per object type.
- **What about objects with no records?** Show the `DataTable` empty state with a Lucide illustration icon and a "Create your first [ObjectType]" CTA button that opens the create modal.
- **What about very wide tables?** Enable horizontal scroll on the table container; freeze the first column (Name) using `position: sticky; left: 0` to keep it visible during scroll.
- **What about backend validation errors on form save?** The error response from `PATCH` or `POST` returns a field-level error array. Each error is mapped to its `field` key and displayed inline beneath the corresponding form control using red text and an error icon. If no `field` is present, the error appears as a general form-level error above the submit button.
- **What about the login page when no auth token is present?** A login page is shown at `/login` when no auth token exists in storage. FR-007 requires this. The login page renders a simple form (username, password) that posts to the OAuth token endpoint. On success, token is stored and the user is redirected to `/`.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: All data MUST be displayed in tables, never as raw JSON (exception: API request log detail panel showing raw request/response body for debugging purposes).
- **FR-002**: All editing MUST use form controls (text inputs, selects, toggles, date pickers) — never text areas containing raw JSON.
- **FR-003**: Navigation MUST follow Lightning-style patterns: fixed header bar, left sidebar, list view, record detail with breadcrumb trail.
- **FR-004**: Design tokens MUST match SLDS color palette, spacing, and typography as defined in Feature 0. No hard-coded hex colors in component source.
- **FR-005**: All existing functionality MUST be preserved in the redesign — request logging, data CRUD, metadata browsing, bulk job management, REST explorer.
- **FR-006**: Components DataTable, RecordForm, PageHeader, Badge, Modal, Toast, ConfirmModal MUST be reusable and accept typed props (TypeScript interfaces required for all component props).
- **FR-007**: A login page MUST be shown when no valid auth token is present in storage. All protected routes redirect to `/login` when unauthenticated.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A Salesforce developer or admin can navigate the app without instructions — layout, navigation, and interaction patterns are familiar from Lightning Experience.
- **SC-002**: All CRUD operations (create, read, update, delete) work through table views and form controls — zero workflows require typing or reading JSON.
- **SC-003**: Zero instances of raw JSON rendered to users in normal workflows. The only acceptable JSON display is the request/response body in the API log detail panel (developer debugging tool).
- **SC-004**: At least 6 reusable shared components are created: `DataTable`, `RecordForm`, `PageHeader`, `Badge`, `Modal`, `Toast`. Each must have a TypeScript interface for all props.
- **SC-005**: All five existing views are rebuilt: data manager (→ object list view + record detail), metadata manager (→ F6 redesign), bulk explorer (→ F7 redesign), request log (→ F8 redesign), REST explorer (preserved with SLDS styling).
- **SC-006**: TypeScript types are fully defined for all component props, API response shapes consumed by the frontend, and route params. Zero `any` types in new component code.
