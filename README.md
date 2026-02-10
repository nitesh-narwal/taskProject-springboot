# Shop API - Simple E-commerce Backend

A clean, simple, and frontend-ready Java Spring Boot backend application for a small e-commerce style system.

## Features

- **Item Management**: Create, read, update, and delete items
- **Cart Management**: Add items to cart, update quantities, remove items, and calculate totals
- **In-Memory Storage**: Uses ArrayList for data storage (resets on restart)
- **Validation**: Input validation with proper error messages
- **CORS Enabled**: Ready for frontend integration
- **RESTful APIs**: JSON-based REST endpoints

## Tech Stack

- Java 17
- Spring Boot 3.2.2
- Maven
- No database (in-memory ArrayList storage)

## Project Structure

```
com.example.shopapi
├── ShopApiApplication.java      # Main application class
├── config/
│   └── CorsConfig.java          # CORS configuration
├── controller/
│   ├── CartController.java      # Cart REST endpoints
│   ├── HealthController.java    # Health check endpoint
│   └── ItemController.java      # Item REST endpoints
├── exception/
│   ├── CartItemNotFoundException.java
│   ├── ErrorResponse.java       # Error response structure
│   ├── GlobalExceptionHandler.java
│   └── ItemNotFoundException.java
├── model/
│   ├── CartItem.java            # Cart item model
│   └── Item.java                # Item model
├── repository/
│   ├── CartRepository.java      # Cart in-memory storage
│   └── ItemRepository.java      # Item in-memory storage
└── service/
    ├── CartService.java         # Cart business logic
    └── ItemService.java         # Item business logic
```

## How to Run

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Running the Application

```bash
# Clone and navigate to project directory
cd taskProject

# Run with Maven
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Verify the Application

```bash
curl http://localhost:8080/api/health
```

## API Endpoints

### Health Check

| Method | Endpoint      | Description        |
|--------|---------------|--------------------|
| GET    | /api/health   | Check API status   |

### Item APIs

| Method | Endpoint         | Description                    |
|--------|------------------|--------------------------------|
| POST   | /api/items       | Create a new item              |
| GET    | /api/items       | Get all items                  |
| GET    | /api/items?search={name} | Search items by name    |
| GET    | /api/items?category={category} | Filter by category |
| GET    | /api/items/{id}  | Get item by ID                 |
| PUT    | /api/items/{id}  | Update an item                 |
| DELETE | /api/items/{id}  | Delete an item                 |

### Cart APIs

| Method | Endpoint                  | Description                |
|--------|---------------------------|----------------------------|
| POST   | /api/cart/add             | Add item to cart           |
| GET    | /api/cart                 | Get all cart items         |
| PUT    | /api/cart/update/{itemId} | Update cart item quantity  |
| DELETE | /api/cart/remove/{itemId} | Remove item from cart      |
| DELETE | /api/cart/clear           | Clear entire cart          |
| GET    | /api/cart/total           | Get cart total price       |

## Sample JSON Requests

### Create an Item

**POST** `/api/items`

```json
{
    "name": "Laptop",
    "description": "Gaming laptop with RTX 4060",
    "price": 1299.99,
    "category": "Electronics",
    "inStock": true
}
```

**Response (201 Created)**:
```json
{
    "id": 1,
    "name": "Laptop",
    "description": "Gaming laptop with RTX 4060",
    "price": 1299.99,
    "category": "Electronics",
    "inStock": true
}
```

### Get All Items

**GET** `/api/items`

**Response (200 OK)**:
```json
[
    {
        "id": 1,
        "name": "Laptop",
        "description": "Gaming laptop with RTX 4060",
        "price": 1299.99,
        "category": "Electronics",
        "inStock": true
    }
]
```

### Search Items by Name

**GET** `/api/items?search=laptop`

### Filter Items by Category

**GET** `/api/items?category=Electronics`

### Update an Item

**PUT** `/api/items/1`

```json
{
    "name": "Laptop Pro",
    "description": "Gaming laptop with RTX 4070",
    "price": 1499.99,
    "category": "Electronics",
    "inStock": true
}
```

### Delete an Item

**DELETE** `/api/items/1`

**Response (204 No Content)**

### Add Item to Cart

**POST** `/api/cart/add`

```json
{
    "itemId": 1,
    "quantity": 2
}
```

**Response (201 Created)**:
```json
{
    "itemId": 1,
    "quantity": 2
}
```

### Get All Cart Items

**GET** `/api/cart`

**Response (200 OK)**:
```json
[
    {
        "itemId": 1,
        "quantity": 2
    }
]
```

### Update Cart Item Quantity

**PUT** `/api/cart/update/1`

```json
{
    "quantity": 5
}
```

### Remove Item from Cart

**DELETE** `/api/cart/remove/1`

**Response (204 No Content)**

### Clear Cart

**DELETE** `/api/cart/clear`

**Response (204 No Content)**

### Get Cart Total

**GET** `/api/cart/total`

**Response (200 OK)**:
```json
{
    "total": 2599.98
}
```

## Error Responses

### Validation Error (400 Bad Request)

```json
{
    "timestamp": "2026-02-10T12:00:00",
    "status": 400,
    "error": "Validation Failed",
    "message": "Invalid request data",
    "details": [
        "name: Name is required",
        "price: Price must be greater than 0"
    ]
}
```

### Item Not Found (404 Not Found)

```json
{
    "timestamp": "2026-02-10T12:00:00",
    "status": 404,
    "error": "Not Found",
    "message": "Item not found with id: 999"
}
```

## Frontend Integration Notes

### CORS Configuration

CORS is globally enabled for all origins on `/api/**` endpoints. For production, update `CorsConfig.java` to specify allowed origins:

```java
config.setAllowedOrigins(Arrays.asList("https://your-frontend-domain.com"));
config.setAllowCredentials(true);
```

### Making API Calls (JavaScript Example)

```javascript
// Create an item
const response = await fetch('http://localhost:8080/api/items', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        name: 'Product Name',
        description: 'Product description',
        price: 29.99,
        category: 'Category',
        inStock: true
    })
});
const item = await response.json();

// Get all items
const itemsResponse = await fetch('http://localhost:8080/api/items');
const items = await itemsResponse.json();

// Add to cart
await fetch('http://localhost:8080/api/cart/add', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        itemId: 1,
        quantity: 2
    })
});

// Get cart total
const totalResponse = await fetch('http://localhost:8080/api/cart/total');
const { total } = await totalResponse.json();
```

### React/Axios Example

```javascript
import axios from 'axios';

const API_BASE = 'http://localhost:8080/api';

// Get all items
const { data: items } = await axios.get(`${API_BASE}/items`);

// Create item
const { data: newItem } = await axios.post(`${API_BASE}/items`, {
    name: 'New Item',
    price: 19.99
});

// Add to cart
await axios.post(`${API_BASE}/cart/add`, { itemId: 1, quantity: 1 });

// Get cart total
const { data: { total } } = await axios.get(`${API_BASE}/cart/total`);
```

## In-Memory Behavior

⚠️ **Important Notes**:

- All data is stored in ArrayList objects
- Data is **NOT persisted** to disk
- Data **resets on application restart**
- Item IDs are auto-generated using AtomicLong (starts from 1)
- Suitable for development, testing, and demo purposes
- For production, integrate a proper database

## HTTP Status Codes

| Code | Meaning                          |
|------|----------------------------------|
| 200  | Success                          |
| 201  | Created (new resource)           |
| 204  | No Content (successful deletion) |
| 400  | Bad Request (validation errors)  |
| 404  | Not Found                        |
| 500  | Internal Server Error            |

## License

This project is for educational and demo purposes.

