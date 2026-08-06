# API Documentation

## Base URL
All API requests are prefixed with `/api/v1`. The default local URL is `http://localhost:8080/api/v1`.

## Authentication
Most endpoints require a JWT token. Send it in the HTTP Header:
`Authorization: Bearer <your_token_here>`

## Standard Response Format
```json
{
  "success": true,
  "data": {},
  "message": "Operation successful"
}
```

## Error Codes
- `400 Bad Request`: Invalid input
- `401 Unauthorized`: Missing or invalid token
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Resource does not exist
- `500 Internal Server Error`: Server failure

---

## Endpoints

### 1. Auth Module
#### Login
- **Method**: POST
- **Path**: `/auth/login`
- **Auth Required**: No
- **Request Body**:
  ```json
  {
    "email": "admin@dequeue.com",
    "password": "password123"
  }
  ```
- **Response**:
  ```json
  {
    "token": "eyJhbGci...",
    "refreshToken": "def456..."
  }
  ```
- **Curl Example**:
  ```bash
  curl -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d '{"email":"admin@dequeue.com", "password":"admin123"}'
  ```

### 2. Orders Module
#### Create Order
- **Method**: POST
- **Path**: `/orders`
- **Auth Required**: Yes (or via public QR context)
- **Request Body**:
  ```json
  {
    "vendorId": "uuid-here",
    "items": [
      {
        "menuItemId": "uuid",
        "quantity": 2,
        "customizations": ["uuid-of-option"]
      }
    ]
  }
  ```

#### Get Vendor Queue
- **Method**: GET
- **Path**: `/orders/queue/{vendorId}`
- **Auth Required**: Yes
- **Query Params**: `status=PENDING,PREPARING`

### 3. Menu Module
#### Get Menu by Vendor
- **Method**: GET
- **Path**: `/menu/{vendorId}`
- **Auth Required**: No
