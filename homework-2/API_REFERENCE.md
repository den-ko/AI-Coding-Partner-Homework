# API Reference

Complete API documentation for the Customer Support Ticket Management System.

## Base URL

```
http://localhost:8080
```

## Content Type

All requests and responses use `application/json` unless otherwise specified.

---

## Endpoints

### Tickets

#### Create Ticket

Creates a new support ticket.

**Endpoint:** `POST /tickets`

**Query Parameters:**
- `autoClassify` (optional, boolean): Enable automatic classification. Default: `false`

**Request Body:**

```json
{
  "customer_id": "CUST001",
  "customer_email": "john.doe@example.com",
  "customer_name": "John Doe",
  "subject": "Cannot login to account",
  "description": "I have been trying to login for the past hour but keep getting invalid credentials error",
  "category": "ACCOUNT_ACCESS",
  "priority": "HIGH",
  "status": "NEW",
  "tags": ["login", "urgent"],
  "metadata": {
    "source": "WEB_FORM",
    "browser": "Chrome 120",
    "device_type": "DESKTOP"
  }
}
```

**Response:** `201 Created`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "customer_id": "CUST001",
  "customer_email": "john.doe@example.com",
  "customer_name": "John Doe",
  "subject": "Cannot login to account",
  "description": "I have been trying to login for the past hour but keep getting invalid credentials error",
  "category": "ACCOUNT_ACCESS",
  "priority": "HIGH",
  "status": "NEW",
  "created_at": "2026-02-10T10:30:00.000Z",
  "updated_at": "2026-02-10T10:30:00.000Z",
  "resolved_at": null,
  "assigned_to": null,
  "tags": ["login", "urgent"],
  "metadata": {
    "source": "WEB_FORM",
    "browser": "Chrome 120",
    "device_type": "DESKTOP"
  },
  "classification_data": null
}
```

**cURL Example:**

```bash
curl -X POST http://localhost:8080/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "customer_id": "CUST001",
    "customer_email": "john.doe@example.com",
    "customer_name": "John Doe",
    "subject": "Cannot login to account",
    "description": "I have been trying to login for the past hour but keep getting invalid credentials error",
    "category": "ACCOUNT_ACCESS",
    "priority": "HIGH",
    "status": "NEW",
    "tags": ["login", "urgent"],
    "metadata": {
      "source": "WEB_FORM",
      "browser": "Chrome 120",
      "device_type": "DESKTOP"
    }
  }'
```

**With Auto-Classification:**

```bash
curl -X POST "http://localhost:8080/tickets?autoClassify=true" \
  -H "Content-Type: application/json" \
  -d '{
    "customer_id": "CUST002",
    "customer_email": "jane.smith@example.com",
    "customer_name": "Jane Smith",
    "subject": "Critical database error",
    "description": "Production database is throwing timeout exceptions and users cannot access the system",
    "tags": ["critical", "database"],
    "metadata": {
      "source": "EMAIL",
      "browser": "N/A",
      "device_type": "DESKTOP"
    }
  }'
```

---

#### List All Tickets

Retrieves all tickets with optional filtering.

**Endpoint:** `GET /tickets`

**Query Parameters:**
- `category` (optional): Filter by category
- `priority` (optional): Filter by priority
- `status` (optional): Filter by status

**Response:** `200 OK`

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "customer_id": "CUST001",
    "customer_email": "john.doe@example.com",
    "customer_name": "John Doe",
    "subject": "Cannot login to account",
    "description": "I have been trying to login for the past hour but keep getting invalid credentials error",
    "category": "ACCOUNT_ACCESS",
    "priority": "HIGH",
    "status": "NEW",
    "created_at": "2026-02-10T10:30:00.000Z",
    "updated_at": "2026-02-10T10:30:00.000Z",
    "resolved_at": null,
    "assigned_to": null,
    "tags": ["login", "urgent"],
    "metadata": {
      "source": "WEB_FORM",
      "browser": "Chrome 120",
      "device_type": "DESKTOP"
    },
    "classification_data": null
  }
]
```

**cURL Examples:**

```bash
# Get all tickets
curl http://localhost:8080/tickets

# Filter by category
curl "http://localhost:8080/tickets?category=BUG_REPORT"

# Filter by priority
curl "http://localhost:8080/tickets?priority=URGENT"

# Filter by status
curl "http://localhost:8080/tickets?status=NEW"

# Multiple filters
curl "http://localhost:8080/tickets?category=BUG_REPORT&priority=HIGH&status=NEW"
```

---

#### Get Ticket by ID

Retrieves a specific ticket by its ID.

**Endpoint:** `GET /tickets/{id}`

**Path Parameters:**
- `id` (UUID): The ticket ID

**Response:** `200 OK`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "customer_id": "CUST001",
  "customer_email": "john.doe@example.com",
  "customer_name": "John Doe",
  "subject": "Cannot login to account",
  "description": "I have been trying to login for the past hour but keep getting invalid credentials error",
  "category": "ACCOUNT_ACCESS",
  "priority": "HIGH",
  "status": "NEW",
  "created_at": "2026-02-10T10:30:00.000Z",
  "updated_at": "2026-02-10T10:30:00.000Z",
  "resolved_at": null,
  "assigned_to": null,
  "tags": ["login", "urgent"],
  "metadata": {
    "source": "WEB_FORM",
    "browser": "Chrome 120",
    "device_type": "DESKTOP"
  },
  "classification_data": null
}
```

**cURL Example:**

```bash
curl http://localhost:8080/tickets/550e8400-e29b-41d4-a716-446655440001
```

**Error Response:** `404 Not Found`

```json
{
  "timestamp": "2026-02-10T10:35:00.000Z",
  "status": 404,
  "error": "Not Found",
  "message": "Ticket not found with id: 550e8400-e29b-41d4-a716-446655440001",
  "path": "/tickets/550e8400-e29b-41d4-a716-446655440001"
}
```

---

#### Update Ticket

Updates an existing ticket.

**Endpoint:** `PUT /tickets/{id}`

**Path Parameters:**
- `id` (UUID): The ticket ID

**Request Body:**

```json
{
  "customer_id": "CUST001",
  "customer_email": "john.doe@example.com",
  "customer_name": "John Doe",
  "subject": "Login issue resolved",
  "description": "Issue was resolved after password reset. User can now login successfully.",
  "category": "ACCOUNT_ACCESS",
  "priority": "HIGH",
  "status": "RESOLVED",
  "resolved_at": "2026-02-10T11:00:00.000Z",
  "assigned_to": "support@example.com",
  "tags": ["login", "resolved"],
  "metadata": {
    "source": "WEB_FORM",
    "browser": "Chrome 120",
    "device_type": "DESKTOP"
  }
}
```

**Response:** `200 OK`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "customer_id": "CUST001",
  "customer_email": "john.doe@example.com",
  "customer_name": "John Doe",
  "subject": "Login issue resolved",
  "description": "Issue was resolved after password reset. User can now login successfully.",
  "category": "ACCOUNT_ACCESS",
  "priority": "HIGH",
  "status": "RESOLVED",
  "created_at": "2026-02-10T10:30:00.000Z",
  "updated_at": "2026-02-10T11:00:00.000Z",
  "resolved_at": "2026-02-10T11:00:00.000Z",
  "assigned_to": "support@example.com",
  "tags": ["login", "resolved"],
  "metadata": {
    "source": "WEB_FORM",
    "browser": "Chrome 120",
    "device_type": "DESKTOP"
  },
  "classification_data": null
}
```

**cURL Example:**

```bash
curl -X PUT http://localhost:8080/tickets/550e8400-e29b-41d4-a716-446655440001 \
  -H "Content-Type: application/json" \
  -d '{
    "customer_id": "CUST001",
    "customer_email": "john.doe@example.com",
    "customer_name": "John Doe",
    "subject": "Login issue resolved",
    "description": "Issue was resolved after password reset. User can now login successfully.",
    "category": "ACCOUNT_ACCESS",
    "priority": "HIGH",
    "status": "RESOLVED",
    "resolved_at": "2026-02-10T11:00:00.000Z",
    "assigned_to": "support@example.com",
    "tags": ["login", "resolved"],
    "metadata": {
      "source": "WEB_FORM",
      "browser": "Chrome 120",
      "device_type": "DESKTOP"
    }
  }'
```

---

#### Delete Ticket

Deletes a ticket by ID.

**Endpoint:** `DELETE /tickets/{id}`

**Path Parameters:**
- `id` (UUID): The ticket ID

**Response:** `204 No Content`

**cURL Example:**

```bash
curl -X DELETE http://localhost:8080/tickets/550e8400-e29b-41d4-a716-446655440001
```

---

### Auto-Classification

#### Auto-Classify Ticket

Automatically classifies an existing ticket's category and priority based on its content.

**Endpoint:** `POST /tickets/{id}/auto-classify`

**Path Parameters:**
- `id` (UUID): The ticket ID

**Response:** `200 OK`

```json
{
  "category": "ACCOUNT_ACCESS",
  "priority": "URGENT",
  "confidence": 0.67,
  "reasoning": "Detected keywords for category ACCOUNT_ACCESS: [login, password, authentication]. Detected keywords for priority URGENT: [critical, urgent]",
  "keywordsFound": ["login", "password", "authentication", "critical", "urgent"]
}
```

**cURL Example:**

```bash
curl -X POST http://localhost:8080/tickets/550e8400-e29b-41d4-a716-446655440001/auto-classify
```

---

### Bulk Import

#### Import Tickets

Bulk imports tickets from CSV, JSON, or XML files.

**Endpoint:** `POST /tickets/import`

**Content-Type:** `multipart/form-data`

**Form Parameters:**
- `file` (file): The file to import
- `format` (string): File format (`csv`, `json`, or `xml`)

**Response:** `200 OK`

```json
{
  "total": 50,
  "successful": 47,
  "failed": 3,
  "errors": [
    {
      "row": 5,
      "reason": "Invalid email format: not-an-email"
    },
    {
      "row": 12,
      "reason": "Description must be between 10 and 2000 characters"
    },
    {
      "row": 23,
      "reason": "Invalid category: INVALID_CATEGORY"
    }
  ]
}
```

**cURL Examples:**

```bash
# CSV Import
curl -X POST http://localhost:8080/tickets/import \
  -F "file=@data/sample_tickets.csv" \
  -F "format=csv"

# JSON Import
curl -X POST http://localhost:8080/tickets/import \
  -F "file=@data/sample_tickets.json" \
  -F "format=json"

# XML Import
curl -X POST http://localhost:8080/tickets/import \
  -F "file=@data/sample_tickets.xml" \
  -F "format=xml"
```

---

## Data Models

### Ticket

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `id` | UUID | Auto | - | Unique ticket identifier |
| `customer_id` | String | Yes | Not blank | Customer identifier |
| `customer_email` | String | Yes | Valid email | Customer email address |
| `customer_name` | String | Yes | Not blank | Customer full name |
| `subject` | String | Yes | 1-200 chars | Ticket subject line |
| `description` | String | Yes | 10-2000 chars | Detailed ticket description |
| `category` | Category | Yes | Enum | Ticket category |
| `priority` | Priority | Yes | Enum | Ticket priority level |
| `status` | Status | Yes | Enum | Current ticket status |
| `created_at` | DateTime | Auto | ISO-8601 | Ticket creation timestamp |
| `updated_at` | DateTime | Auto | ISO-8601 | Last update timestamp |
| `resolved_at` | DateTime | No | ISO-8601 | Resolution timestamp |
| `assigned_to` | String | No | - | Assigned agent email |
| `tags` | Array[String] | Yes | - | Ticket tags (can be empty) |
| `metadata` | Metadata | Yes | Object | Additional metadata |
| `classification_data` | ClassificationData | No | Object | Auto-classification results |

### Category (Enum)

| Value | Description |
|-------|-------------|
| `ACCOUNT_ACCESS` | Login, password, 2FA, authentication issues |
| `TECHNICAL_ISSUE` | Bugs, errors, crashes, technical problems |
| `BILLING_QUESTION` | Payments, invoices, refunds, subscriptions |
| `FEATURE_REQUEST` | Enhancement suggestions, new features |
| `BUG_REPORT` | Defects with reproduction steps |
| `OTHER` | Uncategorizable tickets |

### Priority (Enum)

| Value | Description |
|-------|-------------|
| `URGENT` | Critical issues requiring immediate attention |
| `HIGH` | Important issues that block work |
| `MEDIUM` | Standard priority (default) |
| `LOW` | Minor issues, cosmetic changes |

### Status (Enum)

| Value | Description |
|-------|-------------|
| `NEW` | Newly created ticket |
| `IN_PROGRESS` | Currently being worked on |
| `WAITING_CUSTOMER` | Waiting for customer response |
| `RESOLVED` | Issue has been resolved |
| `CLOSED` | Ticket is closed |

### Metadata

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `source` | Source | Yes | Enum | Ticket source channel |
| `browser` | String | No | - | Browser information |
| `device_type` | DeviceType | Yes | Enum | Device type |

### Source (Enum)

| Value | Description |
|-------|-------------|
| `WEB_FORM` | Web form submission |
| `EMAIL` | Email communication |
| `API` | API submission |
| `CHAT` | Live chat |
| `PHONE` | Phone call |

### DeviceType (Enum)

| Value | Description |
|-------|-------------|
| `DESKTOP` | Desktop computer |
| `MOBILE` | Mobile phone |
| `TABLET` | Tablet device |

### ClassificationData

| Field | Type | Description |
|-------|------|-------------|
| `category` | Category | Auto-detected category |
| `priority` | Priority | Auto-detected priority |
| `confidence` | Double | Confidence score (0.0 - 1.0) |
| `reasoning` | String | Explanation of classification decision |
| `auto_classified` | Boolean | Whether classification was automatic |

### ImportResult

| Field | Type | Description |
|-------|------|-------------|
| `total` | Integer | Total records in file |
| `successful` | Integer | Successfully imported records |
| `failed` | Integer | Failed records |
| `errors` | Array[ImportError] | List of errors |

### ImportError

| Field | Type | Description |
|-------|------|-------------|
| `row` | Integer | Row number (1-based) |
| `reason` | String | Error description |

---

## File Formats

### CSV Format

CSV files should use commas (`,`) as field separators. Tags should be separated by semicolons (`;`).

**Example:**

```csv
id,customer_id,customer_email,customer_name,subject,description,category,priority,status,created_at,updated_at,resolved_at,assigned_to,tags,source,browser,device_type
550e8400-e29b-41d4-a716-446655440001,CUST001,john@example.com,John Doe,Login Issue,Cannot access my account,ACCOUNT_ACCESS,HIGH,NEW,2026-02-01T09:00:00,2026-02-01T09:00:00,,,login;password;urgent,WEB_FORM,Chrome,DESKTOP
```

### JSON Format

**Example:**

```json
{
  "tickets": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "customer_id": "CUST001",
      "customer_email": "john@example.com",
      "customer_name": "John Doe",
      "subject": "Login Issue",
      "description": "Cannot access my account",
      "category": "ACCOUNT_ACCESS",
      "priority": "HIGH",
      "status": "NEW",
      "created_at": "2026-02-01T09:00:00",
      "updated_at": "2026-02-01T09:00:00",
      "resolved_at": null,
      "assigned_to": null,
      "tags": ["login", "password", "urgent"],
      "metadata": {
        "source": "WEB_FORM",
        "browser": "Chrome",
        "device_type": "DESKTOP"
      }
    }
  ]
}
```

### XML Format

**Example:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<tickets>
  <ticket>
    <id>550e8400-e29b-41d4-a716-446655440001</id>
    <customer_id>CUST001</customer_id>
    <customer_email>john@example.com</customer_email>
    <customer_name>John Doe</customer_name>
    <subject>Login Issue</subject>
    <description>Cannot access my account</description>
    <category>ACCOUNT_ACCESS</category>
    <priority>HIGH</priority>
    <status>NEW</status>
    <created_at>2026-02-01T09:00:00</created_at>
    <updated_at>2026-02-01T09:00:00</updated_at>
    <resolved_at/>
    <assigned_to/>
    <tags>
      <tag>login</tag>
      <tag>password</tag>
      <tag>urgent</tag>
    </tags>
    <metadata>
      <source>WEB_FORM</source>
      <browser>Chrome</browser>
      <device_type>DESKTOP</device_type>
    </metadata>
  </ticket>
</tickets>
```

---

## Error Responses

### Validation Error (400)

```json
{
  "timestamp": "2026-02-10T10:30:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": {
    "customerEmail": "must be a well-formed email address",
    "description": "size must be between 10 and 2000",
    "subject": "size must be between 1 and 200"
  },
  "path": "/tickets"
}
```

### Not Found Error (404)

```json
{
  "timestamp": "2026-02-10T10:30:00.000Z",
  "status": 404,
  "error": "Not Found",
  "message": "Ticket not found with id: 550e8400-e29b-41d4-a716-446655440001",
  "path": "/tickets/550e8400-e29b-41d4-a716-446655440001"
}
```

### Invalid File Format Error (400)

```json
{
  "timestamp": "2026-02-10T10:30:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid file format",
  "path": "/tickets/import"
}
```

### Internal Server Error (500)

```json
{
  "timestamp": "2026-02-10T10:30:00.000Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "path": "/tickets"
}
```

---

## HTTP Status Codes

| Code | Description |
|------|-------------|
| `200 OK` | Successful GET, PUT requests |
| `201 Created` | Successful POST requests (create) |
| `204 No Content` | Successful DELETE requests |
| `400 Bad Request` | Validation errors, invalid input |
| `404 Not Found` | Resource not found |
| `500 Internal Server Error` | Server errors |

---

## Auto-Classification Details

### Category Detection Keywords

| Category | Keywords |
|----------|----------|
| `ACCOUNT_ACCESS` | login, password, authentication, access, credentials, locked, reset, signin, 2fa, mfa, sso, oauth |
| `TECHNICAL_ISSUE` | error, crash, bug, broken, not working, issue, problem, failure, exception, timeout |
| `BILLING_QUESTION` | payment, invoice, charge, billing, refund, subscription, upgrade, downgrade, pricing, cost |
| `FEATURE_REQUEST` | feature, enhancement, suggest, improve, request, add, wishlist, idea, proposal |
| `BUG_REPORT` | bug, reproduce, steps to reproduce, expected, actual, defect |

### Priority Detection Keywords

| Priority | Keywords |
|----------|----------|
| `URGENT` | critical, urgent, emergency, asap, immediate, security, production down, outage |
| `HIGH` | important, blocking, cannot work, major, serious, asap |
| `LOW` | minor, cosmetic, suggestion, nice to have, enhancement, future |

### Confidence Scoring

- **High Confidence (0.6-1.0)**: Multiple keyword matches found
- **Medium Confidence (0.3-0.6)**: Some keyword matches found
- **Low Confidence (0.0-0.3)**: Few or no keyword matches

---

## Rate Limits

Currently, there are no rate limits implemented. This may change in future versions.

---

## Versioning

API Version: 1.0.0

The API does not currently use versioning in the URL. This may be added in future releases.
