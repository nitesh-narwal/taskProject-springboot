# 📚 ShopAPI - Complete API Reference

> Quick reference for all backend API endpoints with request/response examples

---

## 🔐 Authentication Endpoints

### Register User
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+1234567890"
}
```
**Response (201):**
```json
{
  "message": "Registration successful. Please check your email to verify your account.",
  "userId": 1
}
```

### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "usernameOrEmail": "johndoe",
  "password": "SecurePass123!"
}
```
**Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900000,
  "user": {
    "id": 1,
    "username": "johndoe",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "ROLE_USER",
    "emailVerified": true,
    "enabled": true
  }
}
```

### Refresh Token
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```
**Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900000
}
```

### Logout
```http
POST /api/auth/logout
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```
**Response (200):**
```json
{
  "message": "Logged out successfully"
}
```

### Verify Email
```http
GET /api/auth/verify-email?token=abc123-uuid-token
```
**Response (200):**
```json
{
  "message": "Email verified successfully. You can now login."
}
```

### Resend Verification Email
```http
POST /api/auth/resend-verification
Content-Type: application/json

{
  "email": "john@example.com"
}
```
**Response (200):**
```json
{
  "message": "Verification email sent"
}
```

### Forgot Password
```http
POST /api/auth/forgot-password
Content-Type: application/json

{
  "email": "john@example.com"
}
```
**Response (200):**
```json
{
  "message": "Password reset email sent"
}
```

### Reset Password
```http
POST /api/auth/reset-password
Content-Type: application/json

{
  "token": "reset-token-uuid",
  "newPassword": "NewSecurePass123!",
  "confirmPassword": "NewSecurePass123!"
}
```
**Response (200):**
```json
{
  "message": "Password reset successfully"
}
```

---

## 📦 Product Endpoints

### Get All Products (Paginated)
```http
GET /api/products?page=0&size=20&sort=createdAt,desc
```
**Response (200):**
```json
{
  "content": [
    {
      "id": "65abc123def456",
      "name": "Premium Wireless Headphones",
      "description": "High-quality wireless headphones...",
      "price": 199.99,
      "category": "Electronics",
      "stockQuantity": 50,
      "imageUrl": "https://res.cloudinary.com/.../headphones.jpg",
      "additionalImages": [],
      "sku": "WH-001",
      "brand": "AudioTech",
      "rating": 4.5,
      "reviewCount": 128,
      "tags": ["wireless", "bluetooth", "premium"],
      "active": true,
      "featured": true,
      "discountPrice": 149.99,
      "discountPercentage": 25,
      "effectivePrice": 149.99,
      "inStock": true,
      "createdAt": "2024-01-15T10:30:00Z",
      "updatedAt": "2024-01-15T10:30:00Z"
    }
  ],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

### Search Products
```http
GET /api/products/search?keyword=headphones&category=Electronics&minPrice=50&maxPrice=300&inStock=true&sortBy=price&sortDir=asc&page=0&size=20
```

### Get Product by ID
```http
GET /api/products/{id}
```

### Create Product (Worker/Admin)
```http
POST /api/products
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "name": "Premium Wireless Headphones",
  "description": "High-quality wireless headphones with noise cancellation",
  "price": 199.99,
  "category": "Electronics",
  "stockQuantity": 50,
  "sku": "WH-001",
  "brand": "AudioTech",
  "tags": ["wireless", "bluetooth", "premium"],
  "featured": true,
  "discountPrice": 149.99,
  "attributes": [
    { "name": "Color", "value": "Black" },
    { "name": "Battery Life", "value": "30 hours" }
  ]
}
```

### Update Product (Worker/Admin)
```http
PUT /api/products/{id}
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "name": "Premium Wireless Headphones v2",
  "price": 179.99,
  ...
}
```

### Upload Product Image (Worker/Admin)
```http
POST /api/products/{id}/image
Authorization: Bearer {accessToken}
Content-Type: multipart/form-data

file: [binary image data]
```
**Response (200):**
```json
{
  "imageUrl": "https://res.cloudinary.com/.../new-image.jpg"
}
```

### Update Stock (Worker/Admin)
```http
PUT /api/products/{id}/stock
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "quantity": 100
}
```

### Delete Product (Worker/Admin)
```http
DELETE /api/products/{id}
Authorization: Bearer {accessToken}
```

### Get Low Stock Products (Worker/Admin)
```http
GET /api/products/low-stock?threshold=10
Authorization: Bearer {accessToken}
```

---

## 🛒 Cart Endpoints

### Get Cart
```http
GET /api/cart
Authorization: Bearer {accessToken}
```
**Response (200):**
```json
{
  "id": "cart123",
  "userId": 1,
  "items": [
    {
      "productId": "65abc123def456",
      "productName": "Premium Wireless Headphones",
      "productImage": "https://...",
      "price": 149.99,
      "quantity": 2,
      "subtotal": 299.98
    }
  ],
  "totalItems": 2,
  "totalPrice": 299.98,
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

### Add Item to Cart
```http
POST /api/cart/items
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "productId": "65abc123def456",
  "quantity": 1
}
```

### Update Cart Item Quantity
```http
PUT /api/cart/items/{productId}
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "quantity": 3
}
```

### Remove Item from Cart
```http
DELETE /api/cart/items/{productId}
Authorization: Bearer {accessToken}
```

### Clear Cart
```http
DELETE /api/cart
Authorization: Bearer {accessToken}
```

---

## 📋 Order Endpoints

### Create Order
```http
POST /api/orders
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "shippingAddress": {
    "fullName": "John Doe",
    "phone": "+1234567890",
    "addressLine1": "123 Main Street",
    "addressLine2": "Apt 4B",
    "city": "New York",
    "state": "NY",
    "postalCode": "10001",
    "country": "USA"
  },
  "billingAddress": {
    "fullName": "John Doe",
    "phone": "+1234567890",
    "addressLine1": "123 Main Street",
    "city": "New York",
    "state": "NY",
    "postalCode": "10001",
    "country": "USA"
  },
  "notes": "Please leave at door"
}
```
**Response (201):**
```json
{
  "id": "order123abc",
  "userId": 1,
  "items": [...],
  "totalAmount": 299.98,
  "status": "CREATED",
  "paymentId": null,
  "shippingAddress": {...},
  "billingAddress": {...},
  "notes": "Please leave at door",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

### Get My Orders
```http
GET /api/orders/my-orders?page=0&size=10&sort=createdAt,desc
Authorization: Bearer {accessToken}
```

### Get Order by ID
```http
GET /api/orders/{id}
Authorization: Bearer {accessToken}
```

### Get All Orders (Worker/Admin)
```http
GET /api/orders?page=0&size=20&status=PAID
Authorization: Bearer {accessToken}
```

### Update Order Status (Worker/Admin)
```http
PATCH /api/orders/{id}/status
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "status": "SHIPPED"
}
```
**Valid statuses:** `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`

### Cancel Order
```http
POST /api/orders/{id}/cancel
Authorization: Bearer {accessToken}
```

---

## 💳 Payment Endpoints (Razorpay)

### Create Razorpay Order
```http
POST /api/payment/create-order/{orderId}
Authorization: Bearer {accessToken}
```
**Response (200):**
```json
{
  "message": "Payment order created successfully",
  "data": {
    "orderId": "order123abc",
    "razorpayOrderId": "order_NxPz7ZK1Bf3h5Y",
    "razorpayKeyId": "rzp_test_xxxxxxxx",
    "amount": 299.99,
    "currency": "INR",
    "companyName": "Shop API",
    "status": "PENDING"
  }
}
```

### Verify Payment
```http
POST /api/payment/verify
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "razorpayOrderId": "order_NxPz7ZK1Bf3h5Y",
  "razorpayPaymentId": "pay_NxPzABC123xyz",
  "razorpaySignature": "generated_signature_hash"
}
```
**Response (200):**
```json
{
  "message": "Payment verified successfully",
  "data": {
    "orderId": "order123abc",
    "razorpayOrderId": "order_NxPz7ZK1Bf3h5Y",
    "razorpayPaymentId": "pay_NxPzABC123xyz",
    "status": "COMPLETED",
    "message": "Payment successful"
  }
}
```

### Payment Webhook (Razorpay calls this)
```http
POST /api/webhook/razorpay
X-Razorpay-Signature: {razorpay_webhook_signature}
Content-Type: application/json

{
  "event": "payment.captured",
  "payload": {
    "payment": {
      "entity": {
        "id": "pay_NxPzABC123xyz",
        "order_id": "order_NxPz7ZK1Bf3h5Y",
        "amount": 29999,
        "currency": "INR",
        "status": "captured"
      }
    }
  }
}
```

---

## 🎫 Support Ticket Endpoints

### Create Ticket
```http
POST /api/support/tickets
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "subject": "Issue with my order",
  "description": "I received a damaged item...",
  "category": "ORDER",
  "priority": "HIGH",
  "orderId": "order123abc"
}
```
**Categories:** `GENERAL`, `ORDER`, `PAYMENT`, `PRODUCT`, `ACCOUNT`, `TECHNICAL`, `OTHER`
**Priorities:** `LOW`, `MEDIUM`, `HIGH`, `URGENT`

**Response (201):**
```json
{
  "id": "ticket123",
  "userId": 1,
  "userEmail": "john@example.com",
  "userName": "John Doe",
  "subject": "Issue with my order",
  "description": "I received a damaged item...",
  "status": "OPEN",
  "priority": "HIGH",
  "category": "ORDER",
  "orderId": "order123abc",
  "assignedTo": null,
  "assignedToName": null,
  "messages": [],
  "createdAt": "2024-01-15T10:30:00Z"
}
```

### Get My Tickets
```http
GET /api/support/my-tickets?page=0&size=10
Authorization: Bearer {accessToken}
```

### Get Ticket by ID
```http
GET /api/support/tickets/{id}
Authorization: Bearer {accessToken}
```

### Add Message to Ticket
```http
POST /api/support/tickets/{id}/messages
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "message": "Here is additional information...",
  "attachments": ["https://..."]
}
```

### Get Open Tickets (Worker/Admin)
```http
GET /api/support/tickets/open?page=0&size=20
Authorization: Bearer {accessToken}
```

### Get Unassigned Tickets (Worker/Admin)
```http
GET /api/support/tickets/unassigned?page=0&size=20
Authorization: Bearer {accessToken}
```

### Get My Assigned Tickets (Worker)
```http
GET /api/support/tickets/my-assigned?page=0&size=20
Authorization: Bearer {accessToken}
```

### Assign Ticket to Self (Worker/Admin)
```http
POST /api/support/tickets/{id}/assign
Authorization: Bearer {accessToken}
```

### Update Ticket Status (Worker/Admin)
```http
PATCH /api/support/tickets/{id}/status
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "status": "RESOLVED"
}
```
**Statuses:** `OPEN`, `IN_PROGRESS`, `WAITING_CUSTOMER`, `RESOLVED`, `CLOSED`

### Add Internal Note (Worker/Admin)
```http
POST /api/support/tickets/{id}/messages
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "message": "Internal note: Customer verified",
  "isInternal": true
}
```

---

## 👤 User Endpoints

### Get Profile
```http
GET /api/user/profile
Authorization: Bearer {accessToken}
```
**Response (200):**
```json
{
  "id": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+1234567890",
  "role": "ROLE_USER",
  "emailVerified": true,
  "enabled": true,
  "createdAt": "2024-01-01T00:00:00Z"
}
```

### Update Profile
```http
PUT /api/user/profile
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Smith",
  "phoneNumber": "+1987654321"
}
```

### Change Password
```http
POST /api/user/change-password
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewPassword456!",
  "confirmPassword": "NewPassword456!"
}
```

### Delete Account
```http
DELETE /api/user/account
Authorization: Bearer {accessToken}
```

---

## 👨‍💼 Admin Endpoints

### Get System Statistics
```http
GET /api/admin/stats
Authorization: Bearer {accessToken}
```
**Response (200):**
```json
{
  "totalUsers": 1250,
  "totalWorkers": 15,
  "totalProducts": 500,
  "totalOrders": 3200,
  "successfulPayments": 2800,
  "failedPayments": 150,
  "openTickets": 45,
  "totalRevenue": 125000.00
}
```

### Get All Users
```http
GET /api/admin/users?page=0&size=20&role=ROLE_USER
Authorization: Bearer {accessToken}
```

### Enable User
```http
PATCH /api/admin/users/{id}/enable
Authorization: Bearer {accessToken}
```

### Disable User
```http
PATCH /api/admin/users/{id}/disable
Authorization: Bearer {accessToken}
```

### Get Workers
```http
GET /api/admin/workers?page=0&size=20
Authorization: Bearer {accessToken}
```

### Create Worker
```http
POST /api/admin/workers
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "username": "worker1",
  "email": "worker1@company.com",
  "password": "WorkerPass123!",
  "firstName": "Jane",
  "lastName": "Worker"
}
```

### Get Audit Logs
```http
GET /api/admin/audit-logs?page=0&size=50&action=LOGIN&startDate=2024-01-01&endDate=2024-01-31
Authorization: Bearer {accessToken}
```
**Response (200):**
```json
{
  "content": [
    {
      "id": 1,
      "action": "LOGIN",
      "entityType": "USER",
      "entityId": "1",
      "details": "User logged in successfully",
      "performedBy": 1,
      "performedByUsername": "johndoe",
      "role": "ROLE_USER",
      "ipAddress": "192.168.1.1",
      "timestamp": "2024-01-15T10:30:00Z"
    }
  ],
  "page": {...}
}
```

---

## ❌ Error Response Format

All errors follow this format:
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "errorCode": "VALIDATION_ERROR",
  "message": "Validation failed",
  "path": "/api/auth/register",
  "errors": {
    "email": "Email is already taken",
    "password": "Password must be at least 8 characters"
  }
}
```

### Common Error Codes
| Code | Status | Description |
|------|--------|-------------|
| `VALIDATION_ERROR` | 400 | Request validation failed |
| `INVALID_CREDENTIALS` | 401 | Wrong username/password |
| `TOKEN_EXPIRED` | 401 | JWT token has expired |
| `EMAIL_NOT_VERIFIED` | 401 | Email verification required |
| `ACCESS_DENIED` | 403 | Insufficient permissions |
| `RESOURCE_NOT_FOUND` | 404 | Entity not found |
| `USER_ALREADY_EXISTS` | 409 | Username/email taken |
| `INSUFFICIENT_STOCK` | 409 | Not enough stock |
| `PAYMENT_FAILED` | 422 | Payment processing failed |
| `INTERNAL_ERROR` | 500 | Server error |

---

## 🔑 Role-Based Access

| Endpoint Pattern | ROLE_USER | ROLE_WORKER | ROLE_ADMIN |
|------------------|-----------|-------------|------------|
| `/api/auth/**` | ✅ Public | ✅ Public | ✅ Public |
| `/api/products` (GET) | ✅ | ✅ | ✅ |
| `/api/products` (POST/PUT/DELETE) | ❌ | ✅ | ✅ |
| `/api/cart/**` | ✅ | ✅ | ✅ |
| `/api/orders/my-orders` | ✅ | ✅ | ✅ |
| `/api/orders` (all) | ❌ | ✅ | ✅ |
| `/api/orders/{id}/status` | ❌ | ✅ | ✅ |
| `/api/support/my-tickets` | ✅ | ✅ | ✅ |
| `/api/support/tickets/**` (manage) | ❌ | ✅ | ✅ |
| `/api/user/**` | ✅ | ✅ | ✅ |
| `/api/admin/**` | ❌ | ❌ | ✅ |

---

## 📡 CORS Configuration

The backend allows requests from:
- `http://localhost:3000`
- `http://localhost:5173`
- Configured frontend URL in environment

Allowed methods: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`
Allowed headers: `*`
Credentials: `true`

---

## 🏥 Health Check

```http
GET /actuator/health
```
**Response (200):**
```json
{
  "status": "UP"
}
```

---

## 📖 Swagger Documentation

Access interactive API documentation at:
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/api-docs`


