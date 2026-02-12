# Shop API - Production E-Commerce Backend
A professional, production-ready e-commerce backend API built with Spring Boot.
## 🚀 Features
- JWT authentication (Access + Refresh tokens)
- Role-based access control (USER, WORKER, ADMIN)
- Email verification and password reset
- Product management with Cloudinary images
- Redis caching for performance
- Order & payment system with Razorpay
- Support ticket system
- Audit logging
- Swagger documentation
## 🛠 Tech Stack
- Spring Boot 3.2.2, Java 17, Maven
- PostgreSQL (Auth, Users, Payments, Audit)
- MongoDB (Products, Orders, Carts, Tickets)
- Redis (Caching)
- Razorpay (Payments), Mailjet (Email), Cloudinary (Images)
## 🏃‍♂️ Quick Start
### With Docker Compose
```bash
docker-compose up -d
```
### Local Development
```bash
docker-compose up -d postgres mongodb redis
mvn spring-boot:run
```
Access Swagger UI: http://localhost:8080/swagger-ui.html
## 🔐 Default Admin
- Username: admin
- Password: Admin@123456
## 📁 Structure
```
src/main/java/com/example/shopapi/
├── config/       # Configuration
├── controller/   # REST endpoints
├── document/     # MongoDB documents
├── dto/          # Data transfer objects
├── entity/       # PostgreSQL entities
├── exception/    # Custom exceptions
├── repository/   # Data repositories
├── security/     # JWT & Security
├── service/      # Business logic
└── util/         # Utilities
```
## 🔑 Main Endpoints
| Endpoint | Access | Description |
|----------|--------|-------------|
| POST /api/auth/register | Public | Register user |
| POST /api/auth/login | Public | Login |
| GET /api/products | Public | Browse products |
| POST /api/cart/items | User+ | Add to cart |
| POST /api/orders | User+ | Create order |
| POST /api/products | Worker+ | Create product |
| GET /api/admin/stats | Admin | System stats |
## 📝 Environment Variables
Create `.env` file with:
- CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET
- MAILJET_API_KEY, MAILJET_API_SECRET
- STRIPE_API_KEY, STRIPE_WEBHOOK_SECRET
- JWT_SECRET
## 📄 License
MIT License
