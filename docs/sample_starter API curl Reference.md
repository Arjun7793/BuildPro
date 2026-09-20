# sample\_starter API curl Reference

2026-09-20 · @Someone

## Base URL

All requests below assume the app is running locally on the `local` profile:

```bash
BASE=http://localhost:8080
```

### Combined content (what the page itself fetches)

```bash
curl -s $BASE/api/content | jq
```

**Response** (`200 OK`)

```json
{
  "services": [
    {"id": 1, "title": "Residential Construction", "description": "Luxury homes, villas and apartments.", "displayOrder": 1}
  ],
  "stats": [
    {"id": 1, "label": "Projects Completed", "targetValue": 120, "displayOrder": 1}
  ],
  "projects": [
    {"id": 1, "imageUrl": "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab", "title": "Project 1", "displayOrder": 1}
  ],
  "testimonials": [
    {"id": 1, "clientName": "David", "message": "Excellent workmanship and completed our home before deadline.", "rating": 5, "displayOrder": 1}
  ],
  "companyInfo": {"id": 1, "companyName": "BuildPro Construction", "address": "123 Business Street", "phone": "+91 9876543210", "email": "info@buildpro.com"}
}
```

(each array holds all seeded rows for that section — shown here with one item apiece for brevity)

## Services — `/api/services`

```bash
# Get all
curl -s $BASE/api/services | jq

# Get one
curl -s $BASE/api/services/1 | jq

# Create
curl -s -X POST $BASE/api/services \
  -H "Content-Type: application/json" \
  -d '{"title": "Landscaping", "description": "Outdoor design and grounds work.", "displayOrder": 7}' | jq

# Update
curl -s -X PUT $BASE/api/services/1 \
  -H "Content-Type: application/json" \
  -d '{"title": "Residential Construction", "description": "Luxury homes, villas and apartments — updated.", "displayOrder": 1}' | jq

# Delete
curl -s -X DELETE $BASE/api/services/7 -w "%{http_code}\n"
```

**Response** to `GET /api/services` (`200 OK`)

```json
[
  {"id": 1, "title": "Residential Construction", "description": "Luxury homes, villas and apartments.", "displayOrder": 1},
  {"id": 2, "title": "Commercial Buildings", "description": "Office spaces, malls and shopping complexes.", "displayOrder": 2},
  {"id": 3, "title": "Interior Design", "description": "Modern interiors with premium finishing.", "displayOrder": 3},
  {"id": 4, "title": "Renovation", "description": "House renovation and remodeling solutions.", "displayOrder": 4},
  {"id": 5, "title": "Architecture", "description": "Creative architectural planning.", "displayOrder": 5},
  {"id": 6, "title": "Project Management", "description": "End-to-end construction management.", "displayOrder": 6}
]
```

`POST`/`PUT` return one such object (`201`/`200`); a validation failure (e.g. missing `title`) returns `400`:

```json
{
  "timestamp": "2026-09-20T08:15:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for one or more fields.",
  "fieldErrors": {"title": "title is required"}
}
```

## Stats — `/api/stats`

```bash
# Get all
curl -s $BASE/api/stats | jq

# Get one
curl -s $BASE/api/stats/1 | jq

# Create
curl -s -X POST $BASE/api/stats \
  -H "Content-Type: application/json" \
  -d '{"label": "Cities Served", "targetValue": 12, "displayOrder": 5}' | jq

# Update
curl -s -X PUT $BASE/api/stats/1 \
  -H "Content-Type: application/json" \
  -d '{"label": "Projects Completed", "targetValue": 130, "displayOrder": 1}' | jq

# Delete
curl -s -X DELETE $BASE/api/stats/5 -w "%{http_code}\n"
```

**Response** to `GET /api/stats` (`200 OK`)

```json
[
  {"id": 1, "label": "Projects Completed", "targetValue": 120, "displayOrder": 1},
  {"id": 2, "label": "Engineers", "targetValue": 55, "displayOrder": 2},
  {"id": 3, "label": "Years Experience", "targetValue": 25, "displayOrder": 3},
  {"id": 4, "label": "Happy Clients", "targetValue": 100, "displayOrder": 4}
]
```

## Projects — `/api/projects`

```bash
# Get all
curl -s $BASE/api/projects | jq

# Get one
curl -s $BASE/api/projects/1 | jq

# Create
curl -s -X POST $BASE/api/projects \
  -H "Content-Type: application/json" \
  -d '{"imageUrl": "https://images.unsplash.com/photo-example", "title": "New Project", "displayOrder": 4}' | jq

# Update
curl -s -X PUT $BASE/api/projects/1 \
  -H "Content-Type: application/json" \
  -d '{"imageUrl": "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab", "title": "Project 1 — updated", "displayOrder": 1}' | jq

# Delete
curl -s -X DELETE $BASE/api/projects/4 -w "%{http_code}\n"
```

**Response** to `GET /api/projects` (`200 OK`)

```json
[
  {"id": 1, "imageUrl": "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&w=800&q=80", "title": "Project 1", "displayOrder": 1},
  {"id": 2, "imageUrl": "https://images.unsplash.com/photo-1460317442991-0ec209397118?auto=format&fit=crop&w=800&q=80", "title": "Project 2", "displayOrder": 2},
  {"id": 3, "imageUrl": "https://images.unsplash.com/photo-1511818966892-d7d671e672a2?auto=format&fit=crop&w=800&q=80", "title": "Project 3", "displayOrder": 3}
]
```

## Testimonials — `/api/testimonials`

```bash
# Get all
curl -s $BASE/api/testimonials | jq

# Get one
curl -s $BASE/api/testimonials/1 | jq

# Create
curl -s -X POST $BASE/api/testimonials \
  -H "Content-Type: application/json" \
  -d '{"clientName": "Priya", "message": "Great communication throughout the build.", "rating": 5, "displayOrder": 4}' | jq

# Update
curl -s -X PUT $BASE/api/testimonials/1 \
  -H "Content-Type: application/json" \
  -d '{"clientName": "David", "message": "Excellent workmanship, finished early.", "rating": 5, "displayOrder": 1}' | jq

# Delete
curl -s -X DELETE $BASE/api/testimonials/4 -w "%{http_code}\n"
```

**Response** to `GET /api/testimonials` (`200 OK`)

```json
[
  {"id": 1, "clientName": "David", "message": "Excellent workmanship and completed our home before deadline.", "rating": 5, "displayOrder": 1},
  {"id": 2, "clientName": "Jennifer", "message": "Professional engineers and outstanding quality.", "rating": 5, "displayOrder": 2},
  {"id": 3, "clientName": "Michael", "message": "Highly recommended construction company.", "rating": 5, "displayOrder": 3}
]
```

## Company info — `/api/company-info`

```bash
# Get all (usually just one record)
curl -s $BASE/api/company-info | jq

# Get one
curl -s $BASE/api/company-info/1 | jq

# Create
curl -s -X POST $BASE/api/company-info \
  -H "Content-Type: application/json" \
  -d '{"companyName": "BuildPro North", "address": "45 Industrial Ave", "phone": "+91 9988776655", "email": "north@buildpro.com"}' | jq

# Update
curl -s -X PUT $BASE/api/company-info/1 \
  -H "Content-Type: application/json" \
  -d '{"companyName": "BuildPro Construction", "address": "123 Business Street", "phone": "+91 9876543210", "email": "info@buildpro.com"}' | jq

# Delete
curl -s -X DELETE $BASE/api/company-info/2 -w "%{http_code}\n"
```

**Response** to `GET /api/company-info` (`200 OK`)

```json
[
  {"id": 1, "companyName": "BuildPro Construction", "address": "123 Business Street", "phone": "+91 9876543210", "email": "info@buildpro.com"}
]
```

## Leads (contact form) — `/api/leads`

No `PUT` here — a submitted lead shouldn't be silently rewritten, only created, listed, or removed.

```bash
# Create (this is what the contact form on the page actually calls)
curl -s -X POST $BASE/api/leads \
  -H "Content-Type: application/json" \
  -d '{"name": "Arjun", "email": "arjun@example.com", "phone": "+91 9000000000", "message": "Interested in a quote for a 3BHK villa."}' | jq

# List all (newest first)
curl -s $BASE/api/leads | jq

# Get one
curl -s $BASE/api/leads/1 | jq

# Delete
curl -s -X DELETE $BASE/api/leads/1 -w "%{http_code}\n"
```

**Response** to the `POST` above (`201 Created`)

```json
{
  "id": 1,
  "name": "Arjun",
  "email": "arjun@example.com",
  "phone": "+91 9000000000",
  "message": "Interested in a quote for a 3BHK villa.",
  "createdAt": "2026-09-20T08:15:00"
}
```

**Response** to `GET /api/leads` (`200 OK`, newest first)

```json
[
  {
    "id": 1,
    "name": "Arjun",
    "email": "arjun@example.com",
    "phone": "+91 9000000000",
    "message": "Interested in a quote for a 3BHK villa.",
    "createdAt": "2026-09-20T08:15:00"
  }
]
```
