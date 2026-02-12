# 🛒 ShopAPI E-Commerce Frontend Development Guide

## Complete AI Prompt for Professional React Frontend Development

---

## 📋 PROJECT OVERVIEW

You are a senior frontend developer tasked with building a **professional, production-grade e-commerce frontend** using **React 18+ with TypeScript**. This frontend must integrate seamlessly with an existing Spring Boot backend API.

**DO NOT** create a basic or beginner-level application.
**DO** create clean, modular, type-safe, and fully functional code.
**DO** implement proper error handling, loading states, and user feedback.
**DO** make it visually stunning with professional UI/UX.

---

## 🎯 TECHNOLOGY STACK (MANDATORY)

```
Framework:        React 18+ with TypeScript
Build Tool:       Vite
Styling:          Tailwind CSS + shadcn/ui components
State Management: Zustand (for global state)
Server State:     TanStack Query (React Query v5)
Routing:          React Router v6
Forms:            React Hook Form + Zod validation
HTTP Client:      Axios with interceptors
Icons:            Lucide React
Animations:       Framer Motion
Notifications:    React Hot Toast or Sonner
Date Handling:    date-fns
Charts:           Recharts (for admin dashboard)
```

---

## 🔐 AUTHENTICATION SYSTEM

### Backend API Endpoints

| Method | Endpoint | Description | Public |
|--------|----------|-------------|--------|
| POST | `/api/auth/register` | Register new user | ✅ |
| POST | `/api/auth/login` | Login and get tokens | ✅ |
| POST | `/api/auth/refresh` | Refresh access token | ✅ |
| POST | `/api/auth/logout` | Logout and invalidate token | ❌ |
| GET | `/api/auth/verify-email?token={token}` | Verify email | ✅ |
| POST | `/api/auth/resend-verification` | Resend verification email | ✅ |
| POST | `/api/auth/forgot-password` | Request password reset | ✅ |
| POST | `/api/auth/reset-password` | Reset password with token | ✅ |

### Authentication Flow Implementation

```typescript
// types/auth.ts
interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
}

interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserInfo;
}

interface UserInfo {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  role: 'ROLE_USER' | 'ROLE_WORKER' | 'ROLE_ADMIN';
  emailVerified: boolean;
  enabled: boolean;
  createdAt: string;
}

interface ForgotPasswordRequest {
  email: string;
}

interface ResetPasswordRequest {
  token: string;
  newPassword: string;
  confirmPassword: string;
}
```

### JWT Token Management

```typescript
// lib/axios.ts - Axios Instance with Interceptors
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request Interceptor - Add Access Token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor - Handle Token Refresh
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) {
          throw new Error('No refresh token');
        }
        
        const response = await axios.post(`${API_BASE_URL}/api/auth/refresh`, {
          refreshToken,
        });
        
        const { accessToken, refreshToken: newRefreshToken } = response.data;
        
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', newRefreshToken);
        
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        // Refresh failed - logout user
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        window.location.href = '/login?session=expired';
        return Promise.reject(refreshError);
      }
    }
    
    return Promise.reject(error);
  }
);

export default api;
```

### Auth Store (Zustand)

```typescript
// stores/authStore.ts
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  user: UserInfo | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  
  login: (credentials: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => Promise<void>;
  refreshAuth: () => Promise<void>;
  setUser: (user: UserInfo) => void;
  
  // Role checks
  isAdmin: () => boolean;
  isWorker: () => boolean;
  isUser: () => boolean;
  hasRole: (roles: string[]) => boolean;
}
```

---

## 🛡️ ROUTE PROTECTION & NAVIGATION

### Route Configuration

```typescript
// routes/index.tsx
import { createBrowserRouter, Navigate, Outlet } from 'react-router-dom';

// Public Routes (accessible without login)
const publicRoutes = [
  { path: '/', element: <HomePage /> },
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  { path: '/forgot-password', element: <ForgotPasswordPage /> },
  { path: '/reset-password', element: <ResetPasswordPage /> },
  { path: '/verify-email', element: <VerifyEmailPage /> },
  { path: '/products', element: <ProductListPage /> },
  { path: '/products/:id', element: <ProductDetailPage /> },
  { path: '/search', element: <SearchResultsPage /> },
];

// Protected Routes (require authentication)
const protectedRoutes = [
  { path: '/profile', element: <ProfilePage /> },
  { path: '/profile/edit', element: <EditProfilePage /> },
  { path: '/cart', element: <CartPage /> },
  { path: '/checkout', element: <CheckoutPage /> },
  { path: '/orders', element: <OrdersPage /> },
  { path: '/orders/:id', element: <OrderDetailPage /> },
  { path: '/support', element: <SupportPage /> },
  { path: '/support/new', element: <CreateTicketPage /> },
  { path: '/support/:id', element: <TicketDetailPage /> },
  { path: '/payment/success', element: <PaymentSuccessPage /> },
  { path: '/payment/cancel', element: <PaymentCancelPage /> },
];

// Worker Routes (ROLE_WORKER or ROLE_ADMIN)
const workerRoutes = [
  { path: '/worker/dashboard', element: <WorkerDashboardPage /> },
  { path: '/worker/products', element: <ManageProductsPage /> },
  { path: '/worker/products/new', element: <CreateProductPage /> },
  { path: '/worker/products/:id/edit', element: <EditProductPage /> },
  { path: '/worker/orders', element: <ManageOrdersPage /> },
  { path: '/worker/tickets', element: <ManageTicketsPage /> },
  { path: '/worker/tickets/:id', element: <TicketResponsePage /> },
];

// Admin Routes (ROLE_ADMIN only)
const adminRoutes = [
  { path: '/admin/dashboard', element: <AdminDashboardPage /> },
  { path: '/admin/users', element: <ManageUsersPage /> },
  { path: '/admin/workers', element: <ManageWorkersPage /> },
  { path: '/admin/workers/new', element: <CreateWorkerPage /> },
  { path: '/admin/audit-logs', element: <AuditLogsPage /> },
  { path: '/admin/statistics', element: <StatisticsPage /> },
  { path: '/admin/payments', element: <PaymentReportsPage /> },
];

// Error Routes
const errorRoutes = [
  { path: '/unauthorized', element: <UnauthorizedPage /> },
  { path: '/forbidden', element: <ForbiddenPage /> },
  { path: '/not-found', element: <NotFoundPage /> },
  { path: '/server-error', element: <ServerErrorPage /> },
  { path: '*', element: <Navigate to="/not-found" replace /> },
];
```

### Protected Route Component

```typescript
// components/auth/ProtectedRoute.tsx
interface ProtectedRouteProps {
  children: React.ReactNode;
  allowedRoles?: ('ROLE_USER' | 'ROLE_WORKER' | 'ROLE_ADMIN')[];
  requireEmailVerified?: boolean;
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  allowedRoles,
  requireEmailVerified = true,
}) => {
  const { user, isAuthenticated, isLoading } = useAuthStore();
  const location = useLocation();

  if (isLoading) {
    return <FullPageLoader />;
  }

  if (!isAuthenticated) {
    // Save attempted URL for redirect after login
    return <Navigate to={`/login?redirect=${encodeURIComponent(location.pathname)}`} replace />;
  }

  if (requireEmailVerified && !user?.emailVerified) {
    return <Navigate to="/verify-email-required" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user!.role)) {
    return <Navigate to="/forbidden" replace />;
  }

  return <>{children}</>;
};
```

---

## 🎨 PAGE COMPONENTS & FEATURES

### 1. HOME PAGE (`/`)

**Features:**
- Hero section with call-to-action
- Featured products carousel
- Category grid
- New arrivals section
- Promotional banners
- Newsletter subscription
- Testimonials

**API Calls:**
```typescript
GET /api/products?featured=true&size=8  // Featured products
GET /api/products?sort=createdAt,desc&size=12  // New arrivals
```

### 2. PRODUCT PAGES

#### Product List (`/products`)

**Features:**
- Grid/List view toggle
- Advanced filtering sidebar:
  - Category filter
  - Price range slider
  - Brand filter
  - In-stock toggle
  - Rating filter
- Sorting options (price, name, newest, popularity)
- Pagination with page size selector
- Quick view modal
- Add to cart directly from list

**API Endpoint:**
```typescript
GET /api/products/search
Query Parameters:
  - keyword: string
  - category: string
  - minPrice: number
  - maxPrice: number
  - inStock: boolean
  - featured: boolean
  - brand: string
  - sortBy: 'price' | 'name' | 'createdAt' | 'rating'
  - sortDir: 'asc' | 'desc'
  - page: number (0-indexed)
  - size: number
```

**Response Type:**
```typescript
interface PagedResponse<T> {
  content: T[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

interface ProductResponse {
  id: string;
  name: string;
  description: string;
  price: number;
  category: string;
  stockQuantity: number;
  imageUrl: string;
  additionalImages: string[];
  sku: string;
  brand: string;
  rating: number;
  reviewCount: number;
  tags: string[];
  active: boolean;
  featured: boolean;
  discountPrice: number | null;
  discountPercentage: number | null;
  effectivePrice: number;
  inStock: boolean;
  createdAt: string;
  updatedAt: string;
}
```

#### Product Detail (`/products/:id`)

**Features:**
- Image gallery with zoom
- Product info (name, price, description)
- Discount badge if applicable
- Stock status indicator
- Quantity selector
- Add to cart button
- Add to wishlist
- Share buttons
- Product specifications/attributes
- Related products
- Customer reviews section

**API Calls:**
```typescript
GET /api/products/{id}
GET /api/products?category={category}&size=4  // Related products
```

### 3. CART PAGE (`/cart`)

**Features:**
- Cart items list with images
- Quantity adjustment (+/-)
- Remove item
- Update cart
- Price summary (subtotal, tax, shipping, total)
- Promo code input
- Continue shopping link
- Proceed to checkout button
- Empty cart state
- Save for later functionality

**API Endpoints:**
```typescript
GET /api/cart                    // Get current user's cart
POST /api/cart/items             // Add item to cart
  Body: { productId: string, quantity: number }
PUT /api/cart/items/{productId}  // Update quantity
  Body: { quantity: number }
DELETE /api/cart/items/{productId}  // Remove item
DELETE /api/cart                 // Clear cart
```

**Cart Types:**
```typescript
interface CartResponse {
  id: string;
  userId: number;
  items: CartItemResponse[];
  totalItems: number;
  totalPrice: number;
  createdAt: string;
  updatedAt: string;
}

interface CartItemResponse {
  productId: string;
  productName: string;
  productImage: string;
  price: number;
  quantity: number;
  subtotal: number;
}
```

### 4. CHECKOUT PAGE (`/checkout`)

**Features:**
- Order summary (read-only cart)
- Shipping address form
- Billing address (same as shipping checkbox)
- Payment method selection
- Order notes
- Terms acceptance checkbox
- Place order button
- Order total with breakdown

**API Endpoint:**
```typescript
POST /api/orders
Body: {
  shippingAddress: {
    fullName: string;
    phone: string;
    addressLine1: string;
    addressLine2?: string;
    city: string;
    state: string;
    postalCode: string;
    country: string;
  };
  billingAddress?: {...};
  notes?: string;
}
```

### 5. PAYMENT FLOW (RAZORPAY)

#### Step 1: Create Razorpay Order
```typescript
POST /api/payment/create-order/{orderId}
Response: {
  orderId: string;           // Your order ID
  razorpayOrderId: string;   // Razorpay order ID (order_xxxxx)
  razorpayKeyId: string;     // Razorpay public key
  amount: number;            // Amount in INR
  currency: string;          // "INR"
  companyName: string;       // Shown in checkout modal
  status: string;            // "PENDING"
}
```

#### Step 2: Open Razorpay Checkout Modal
```typescript
// Install: npm install razorpay

// After getting order details, open Razorpay checkout
const handlePayment = async (orderId: string) => {
  try {
    // Step 1: Create Razorpay order
    const response = await api.post(`/api/payment/create-order/${orderId}`);
    const { razorpayOrderId, razorpayKeyId, amount, currency, companyName } = response.data.data;
    
    // Step 2: Configure Razorpay options
    const options = {
      key: razorpayKeyId,
      amount: amount * 100,  // Amount in paise
      currency: currency,
      name: companyName,
      description: `Order #${orderId}`,
      order_id: razorpayOrderId,
      handler: async function (response: any) {
        // Step 3: Verify payment on backend
        await verifyPayment({
          razorpayOrderId: response.razorpay_order_id,
          razorpayPaymentId: response.razorpay_payment_id,
          razorpaySignature: response.razorpay_signature,
        });
      },
      prefill: {
        name: user.fullName,
        email: user.email,
        contact: user.phone,
      },
      theme: {
        color: '#3B82F6',
      },
    };
    
    // Step 4: Open Razorpay checkout
    const razorpay = new (window as any).Razorpay(options);
    razorpay.open();
    
  } catch (error) {
    toast.error('Failed to initiate payment');
  }
};
```

#### Step 3: Verify Payment
```typescript
POST /api/payment/verify
Body: {
  razorpayOrderId: string;    // razorpay_order_id from handler
  razorpayPaymentId: string;  // razorpay_payment_id from handler
  razorpaySignature: string;  // razorpay_signature from handler
}
Response: {
  orderId: string;
  razorpayOrderId: string;
  razorpayPaymentId: string;
  status: "COMPLETED";
  message: "Payment successful";
}
```

#### Add Razorpay Script to index.html
```html
<script src="https://checkout.razorpay.com/v1/checkout.js"></script>
```

#### Payment Success Page (`/payment/success`)


**Features:**
- Success animation/icon
- Order confirmation number
- Order summary
- Email confirmation message
- Continue shopping button
- View order details link

#### Payment Cancel Page (`/payment/cancel`)

**Features:**
- Cancel message
- Return to cart button
- Contact support link
- Retry payment option

### 6. ORDER PAGES

#### Order List (`/orders`)

**Features:**
- Orders table/cards with:
  - Order number
  - Date
  - Status badge (color-coded)
  - Total amount
  - Items count
  - View details button
- Filter by status
- Sort by date
- Pagination
- Empty state for no orders

**API Endpoint:**
```typescript
GET /api/orders/my-orders?page=0&size=10&sort=createdAt,desc
```

**Order Types:**
```typescript
type OrderStatus = 'CREATED' | 'PAID' | 'FAILED' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

interface OrderResponse {
  id: string;
  userId: number;
  items: OrderItemResponse[];
  totalAmount: number;
  status: OrderStatus;
  paymentId: string | null;
  shippingAddress: AddressResponse;
  billingAddress: AddressResponse;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
  paidAt: string | null;
  shippedAt: string | null;
  deliveredAt: string | null;
}
```

#### Order Detail (`/orders/:id`)

**Features:**
- Order header with status timeline
- Order items with images
- Shipping information
- Billing information
- Payment information
- Order total breakdown
- Track shipment button (if shipped)
- Cancel order button (if cancellable)
- Reorder button
- Download invoice

**API Endpoint:**
```typescript
GET /api/orders/{id}
```

### 7. USER PROFILE PAGES

#### Profile View (`/profile`)

**Features:**
- User avatar/initials
- Personal information display
- Account statistics
- Recent orders preview
- Edit profile button
- Change password link
- Delete account button (with confirmation)

**API Endpoints:**
```typescript
GET /api/user/profile
DELETE /api/user/account  // Delete own account
```

#### Edit Profile (`/profile/edit`)

**Features:**
- Form with current values pre-filled
- Avatar upload
- Validation feedback
- Save/Cancel buttons

**API Endpoint:**
```typescript
PUT /api/user/profile
Body: {
  firstName: string;
  lastName: string;
  phoneNumber?: string;
}
```

#### Change Password

**API Endpoint:**
```typescript
POST /api/user/change-password
Body: {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}
```

### 8. SUPPORT TICKET SYSTEM

#### Support List (`/support`)

**Features:**
- Tickets table with:
  - Ticket ID
  - Subject
  - Status badge
  - Priority badge
  - Category
  - Last updated
  - View button
- Create new ticket button
- Filter by status
- Search tickets

**API Endpoint:**
```typescript
GET /api/support/my-tickets?page=0&size=10
```

**Ticket Types:**
```typescript
type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'WAITING_CUSTOMER' | 'RESOLVED' | 'CLOSED';
type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
type TicketCategory = 'GENERAL' | 'ORDER' | 'PAYMENT' | 'PRODUCT' | 'ACCOUNT' | 'TECHNICAL' | 'OTHER';

interface TicketResponse {
  id: string;
  userId: number;
  userEmail: string;
  userName: string;
  subject: string;
  description: string;
  status: TicketStatus;
  priority: TicketPriority;
  category: TicketCategory;
  orderId: string | null;
  assignedTo: number | null;
  assignedToName: string | null;
  messages: TicketMessageResponse[];
  createdAt: string;
  updatedAt: string;
  resolvedAt: string | null;
  closedAt: string | null;
}
```

#### Create Ticket (`/support/new`)

**Features:**
- Subject input
- Category dropdown
- Priority dropdown
- Related order dropdown (optional)
- Description textarea
- File attachments
- Submit button

**API Endpoint:**
```typescript
POST /api/support/tickets
Body: {
  subject: string;
  description: string;
  category: TicketCategory;
  priority?: TicketPriority;
  orderId?: string;
}
```

#### Ticket Detail (`/support/:id`)

**Features:**
- Ticket header with status
- Ticket information
- Message thread (chat-like UI)
- Reply input with attachments
- Status updates in thread
- Close ticket button (if resolved)

**API Endpoint:**
```typescript
GET /api/support/tickets/{id}
POST /api/support/tickets/{id}/messages
Body: {
  message: string;
  attachments?: string[];
}
```

---

## 👷 WORKER DASHBOARD

### Access: `ROLE_WORKER` or `ROLE_ADMIN`

#### Worker Dashboard (`/worker/dashboard`)

**Features:**
- Quick stats cards:
  - Total products managed
  - Low stock alerts
  - Pending orders
  - Open tickets assigned
- Recent activity feed
- Quick actions

#### Manage Products (`/worker/products`)

**Features:**
- Products data table with:
  - Image thumbnail
  - Name
  - SKU
  - Price
  - Stock
  - Status (Active/Inactive)
  - Actions (Edit, Delete, View)
- Add new product button
- Bulk actions
- Filter by category, status
- Search products
- Export to CSV

**API Endpoints:**
```typescript
GET /api/products?page=0&size=20
POST /api/products
PUT /api/products/{id}
DELETE /api/products/{id}
POST /api/products/{id}/image  // Multipart form data
PUT /api/products/{id}/stock
  Body: { quantity: number }
GET /api/products/low-stock?threshold=10
```

#### Create/Edit Product (`/worker/products/new`, `/worker/products/:id/edit`)

**Features:**
- Form fields:
  - Name (required)
  - Description (rich text editor)
  - Price (required)
  - Discount price (optional)
  - Category (dropdown)
  - SKU
  - Brand
  - Stock quantity
  - Featured toggle
  - Active toggle
  - Tags (multi-select)
  - Attributes (dynamic key-value pairs)
- Image upload with preview
- Multiple images support
- Form validation
- Save as draft option

**Product Request Type:**
```typescript
interface ProductRequest {
  name: string;
  description: string;
  price: number;
  category: string;
  stockQuantity?: number;
  sku?: string;
  brand?: string;
  tags?: string[];
  featured?: boolean;
  discountPrice?: number;
  discountPercentage?: number;
  attributes?: { name: string; value: string }[];
}
```

#### Manage Orders (`/worker/orders`)

**Features:**
- Orders table with status filters
- Update order status dropdown:
  - PROCESSING
  - SHIPPED
  - DELIVERED
- Add tracking information
- View order details
- Print packing slip

**API Endpoints:**
```typescript
GET /api/orders?page=0&size=20&status={status}
PATCH /api/orders/{id}/status
  Body: { status: string }
```

#### Manage Tickets (`/worker/tickets`)

**Features:**
- Unassigned tickets section
- My assigned tickets section
- Assign to self button
- Respond to tickets
- Update ticket status
- Mark as resolved

**API Endpoints:**
```typescript
GET /api/support/tickets/open
GET /api/support/tickets/unassigned
GET /api/support/tickets/my-assigned
POST /api/support/tickets/{id}/assign
PATCH /api/support/tickets/{id}/status
  Body: { status: string }
POST /api/support/tickets/{id}/messages
  Body: { message: string, isInternal?: boolean }  // Internal notes for workers
```

---

## 👨‍💼 ADMIN DASHBOARD

### Access: `ROLE_ADMIN` only

#### Admin Dashboard (`/admin/dashboard`)

**Features:**
- Statistics overview cards:
  - Total users
  - Total workers
  - Total products
  - Total orders
  - Revenue (successful payments)
  - Failed payments
  - Open tickets
- Revenue chart (line/bar chart)
- Orders by status (pie chart)
- Recent activity timeline
- System health indicators

**API Endpoint:**
```typescript
GET /api/admin/stats
Response: {
  totalUsers: number;
  totalWorkers: number;
  totalProducts: number;
  totalOrders: number;
  successfulPayments: number;
  failedPayments: number;
  openTickets: number;
  totalRevenue: number;
}
```

#### Manage Users (`/admin/users`)

**Features:**
- Users table with:
  - Avatar
  - Name
  - Email
  - Role badge
  - Status (Enabled/Disabled)
  - Email verified status
  - Registration date
  - Actions
- Enable/Disable user toggle
- View user details modal
- Filter by role, status
- Search users

**API Endpoints:**
```typescript
GET /api/admin/users?page=0&size=20&role={role}
PATCH /api/admin/users/{id}/enable
PATCH /api/admin/users/{id}/disable
```

#### Manage Workers (`/admin/workers`)

**Features:**
- Workers table
- Create new worker button
- Disable worker
- View worker statistics

**API Endpoints:**
```typescript
GET /api/admin/workers
POST /api/admin/workers
Body: {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}
```

#### Audit Logs (`/admin/audit-logs`)

**Features:**
- Logs table with:
  - Timestamp
  - Action
  - Performed by (user)
  - Role
  - Entity type
  - Entity ID
  - Details
  - IP Address
- Filter by action type
- Filter by user
- Date range filter
- Export logs

**API Endpoint:**
```typescript
GET /api/admin/audit-logs?page=0&size=50&action={action}&userId={userId}&startDate={date}&endDate={date}

interface AuditLogResponse {
  id: number;
  action: string;
  entityType: string;
  entityId: string;
  details: string;
  performedBy: number;
  performedByUsername: string;
  role: string;
  ipAddress: string;
  timestamp: string;
}
```

#### Payment Reports (`/admin/payments`)

**Features:**
- Payment transactions table
- Filter by status (SUCCESS, FAILED, PENDING)
- Date range filter
- Total revenue calculation
- Export to CSV
- Refund capability (if implemented)

---

## 🚨 ERROR HANDLING

### Global Error Handler

```typescript
// lib/errorHandler.ts
import { AxiosError } from 'axios';
import { toast } from 'sonner';

interface ApiError {
  timestamp: string;
  status: number;
  errorCode: string;
  message: string;
  path: string;
  errors?: Record<string, string>;  // Validation errors
}

export const handleApiError = (error: AxiosError<ApiError>) => {
  const { response } = error;
  
  if (!response) {
    toast.error('Network error. Please check your connection.');
    return;
  }
  
  const { status, data } = response;
  
  switch (status) {
    case 400:
      // Validation errors
      if (data.errors) {
        Object.values(data.errors).forEach(msg => toast.error(msg));
      } else {
        toast.error(data.message || 'Invalid request');
      }
      break;
      
    case 401:
      // Handled by interceptor (token refresh)
      toast.error('Session expired. Please login again.');
      break;
      
    case 403:
      toast.error('You do not have permission to perform this action.');
      window.location.href = '/forbidden';
      break;
      
    case 404:
      toast.error(data.message || 'Resource not found');
      break;
      
    case 409:
      toast.error(data.message || 'Conflict occurred');
      break;
      
    case 422:
      toast.error(data.message || 'Validation failed');
      break;
      
    case 429:
      toast.error('Too many requests. Please try again later.');
      break;
      
    case 500:
      toast.error('Server error. Please try again later.');
      break;
      
    default:
      toast.error(data.message || 'An unexpected error occurred');
  }
};
```

### Error Pages

#### Unauthorized Page (`/unauthorized`)
- "Please login to continue" message
- Login button
- Register link

#### Forbidden Page (`/forbidden`)
- "Access Denied" message
- "You don't have permission to view this page"
- Go back button
- Contact support link

#### Not Found Page (`/not-found`)
- 404 illustration
- "Page not found" message
- Search bar
- Popular links
- Go home button

#### Server Error Page (`/server-error`)
- 500 illustration
- "Something went wrong" message
- Retry button
- Report issue link

#### Email Verification Required Page (`/verify-email-required`)
- Message explaining verification is needed
- Resend verification email button
- Check spam folder reminder
- Logout option

---

## 🎨 UI/UX REQUIREMENTS

### Design System

1. **Color Palette:**
   - Primary: Blue (#3B82F6)
   - Secondary: Slate (#64748B)
   - Success: Green (#22C55E)
   - Warning: Amber (#F59E0B)
   - Error: Red (#EF4444)
   - Background: White/Slate-50
   - Text: Slate-900/Slate-600

2. **Typography:**
   - Headings: Inter (Bold)
   - Body: Inter (Regular)
   - Monospace: JetBrains Mono (for codes)

3. **Spacing:** Use Tailwind's spacing scale consistently

4. **Border Radius:** Rounded-lg (8px) for cards, rounded-md (6px) for buttons

5. **Shadows:** Use subtle shadows for elevation

### Component Requirements

1. **Buttons:**
   - Primary (filled)
   - Secondary (outlined)
   - Ghost (text only)
   - Destructive (red)
   - Loading state with spinner
   - Disabled state

2. **Form Inputs:**
   - Labels
   - Helper text
   - Error messages
   - Required indicator
   - Focus states
   - Disabled states

3. **Cards:**
   - Consistent padding
   - Hover effects where clickable
   - Image aspect ratios

4. **Tables:**
   - Sortable columns
   - Row hover effects
   - Action buttons
   - Empty state
   - Loading skeleton

5. **Modals:**
   - Backdrop blur
   - Close button
   - Keyboard escape support
   - Focus trap

6. **Loading States:**
   - Full page loader
   - Skeleton loaders
   - Button spinners
   - Inline loaders

7. **Empty States:**
   - Illustration
   - Descriptive message
   - Action button

### Responsive Breakpoints

```typescript
// Tailwind default breakpoints
sm: 640px   // Mobile landscape
md: 768px   // Tablet
lg: 1024px  // Desktop
xl: 1280px  // Large desktop
2xl: 1536px // Extra large
```

### Accessibility Requirements

- Proper heading hierarchy
- Alt text for images
- ARIA labels for interactive elements
- Keyboard navigation support
- Focus visible states
- Color contrast compliance
- Screen reader support

---

## 📁 RECOMMENDED PROJECT STRUCTURE

```
src/
├── components/
│   ├── ui/                    # shadcn/ui components
│   ├── common/                # Shared components
│   │   ├── Header.tsx
│   │   ├── Footer.tsx
│   │   ├── Sidebar.tsx
│   │   ├── Breadcrumbs.tsx
│   │   ├── Pagination.tsx
│   │   ├── SearchBar.tsx
│   │   ├── LoadingSpinner.tsx
│   │   ├── EmptyState.tsx
│   │   └── ConfirmDialog.tsx
│   ├── auth/
│   │   ├── LoginForm.tsx
│   │   ├── RegisterForm.tsx
│   │   ├── ForgotPasswordForm.tsx
│   │   ├── ResetPasswordForm.tsx
│   │   └── ProtectedRoute.tsx
│   ├── products/
│   │   ├── ProductCard.tsx
│   │   ├── ProductGrid.tsx
│   │   ├── ProductFilters.tsx
│   │   ├── ProductGallery.tsx
│   │   ├── ProductInfo.tsx
│   │   └── QuickViewModal.tsx
│   ├── cart/
│   │   ├── CartItem.tsx
│   │   ├── CartSummary.tsx
│   │   └── MiniCart.tsx
│   ├── orders/
│   │   ├── OrderCard.tsx
│   │   ├── OrderTimeline.tsx
│   │   └── OrderItems.tsx
│   ├── support/
│   │   ├── TicketCard.tsx
│   │   ├── MessageThread.tsx
│   │   └── TicketForm.tsx
│   ├── admin/
│   │   ├── StatsCard.tsx
│   │   ├── DataTable.tsx
│   │   ├── Charts.tsx
│   │   └── AuditLogItem.tsx
│   └── layouts/
│       ├── MainLayout.tsx
│       ├── AuthLayout.tsx
│       ├── DashboardLayout.tsx
│       └── AdminLayout.tsx
├── pages/
│   ├── public/
│   │   ├── HomePage.tsx
│   │   ├── ProductListPage.tsx
│   │   ├── ProductDetailPage.tsx
│   │   └── SearchResultsPage.tsx
│   ├── auth/
│   │   ├── LoginPage.tsx
│   │   ├── RegisterPage.tsx
│   │   ├── ForgotPasswordPage.tsx
│   │   ├── ResetPasswordPage.tsx
│   │   └── VerifyEmailPage.tsx
│   ├── user/
│   │   ├── ProfilePage.tsx
│   │   ├── CartPage.tsx
│   │   ├── CheckoutPage.tsx
│   │   ├── OrdersPage.tsx
│   │   ├── OrderDetailPage.tsx
│   │   └── SupportPage.tsx
│   ├── worker/
│   │   ├── WorkerDashboardPage.tsx
│   │   ├── ManageProductsPage.tsx
│   │   ├── ManageOrdersPage.tsx
│   │   └── ManageTicketsPage.tsx
│   ├── admin/
│   │   ├── AdminDashboardPage.tsx
│   │   ├── ManageUsersPage.tsx
│   │   ├── ManageWorkersPage.tsx
│   │   ├── AuditLogsPage.tsx
│   │   └── PaymentReportsPage.tsx
│   └── errors/
│       ├── UnauthorizedPage.tsx
│       ├── ForbiddenPage.tsx
│       ├── NotFoundPage.tsx
│       └── ServerErrorPage.tsx
├── hooks/
│   ├── useAuth.ts
│   ├── useProducts.ts
│   ├── useCart.ts
│   ├── useOrders.ts
│   ├── useTickets.ts
│   ├── useAdmin.ts
│   └── useDebounce.ts
├── services/
│   ├── authService.ts
│   ├── productService.ts
│   ├── cartService.ts
│   ├── orderService.ts
│   ├── paymentService.ts
│   ├── supportService.ts
│   ├── userService.ts
│   └── adminService.ts
├── stores/
│   ├── authStore.ts
│   ├── cartStore.ts
│   └── uiStore.ts
├── types/
│   ├── auth.ts
│   ├── product.ts
│   ├── cart.ts
│   ├── order.ts
│   ├── support.ts
│   ├── user.ts
│   └── admin.ts
├── lib/
│   ├── axios.ts
│   ├── errorHandler.ts
│   ├── utils.ts
│   └── validators.ts
├── routes/
│   └── index.tsx
├── styles/
│   └── globals.css
├── App.tsx
└── main.tsx
```

---

## 🔧 ENVIRONMENT VARIABLES

```env
# .env.local
VITE_API_BASE_URL=http://localhost:8080
VITE_STRIPE_PUBLISHABLE_KEY=pk_test_your_stripe_publishable_key
VITE_APP_NAME=ShopAPI
VITE_APP_URL=http://localhost:3000
```

---

## 📦 PACKAGE.JSON DEPENDENCIES

```json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.22.0",
    "@tanstack/react-query": "^5.17.0",
    "zustand": "^4.5.0",
    "axios": "^1.6.5",
    "react-hook-form": "^7.49.3",
    "@hookform/resolvers": "^3.3.4",
    "zod": "^3.22.4",
    "tailwindcss": "^3.4.1",
    "class-variance-authority": "^0.7.0",
    "clsx": "^2.1.0",
    "tailwind-merge": "^2.2.0",
    "lucide-react": "^0.312.0",
    "framer-motion": "^11.0.3",
    "sonner": "^1.4.0",
    "date-fns": "^3.3.1",
    "recharts": "^2.10.4",
    "@radix-ui/react-dialog": "^1.0.5",
    "@radix-ui/react-dropdown-menu": "^2.0.6",
    "@radix-ui/react-select": "^2.0.0",
    "@radix-ui/react-tabs": "^1.0.4",
    "@radix-ui/react-tooltip": "^1.0.7"
  },
  "devDependencies": {
    "@types/react": "^18.2.48",
    "@types/react-dom": "^18.2.18",
    "@vitejs/plugin-react": "^4.2.1",
    "typescript": "^5.3.3",
    "vite": "^5.0.12",
    "autoprefixer": "^10.4.17",
    "postcss": "^8.4.33"
  }
}
```

---

## ✅ IMPLEMENTATION CHECKLIST

### Phase 1: Foundation
- [ ] Project setup with Vite + TypeScript
- [ ] Tailwind CSS + shadcn/ui configuration
- [ ] Axios instance with interceptors
- [ ] Auth store (Zustand)
- [ ] React Query setup
- [ ] Route configuration
- [ ] Layout components

### Phase 2: Authentication
- [ ] Login page
- [ ] Register page
- [ ] Forgot password page
- [ ] Reset password page
- [ ] Email verification page
- [ ] Protected route component

### Phase 3: Public Pages
- [ ] Home page
- [ ] Product list with filters
- [ ] Product detail page
- [ ] Search results page

### Phase 4: User Features
- [ ] Cart functionality
- [ ] Checkout flow
- [ ] Payment integration
- [ ] Order history
- [ ] Order details
- [ ] Profile management
- [ ] Support tickets

### Phase 5: Worker Dashboard
- [ ] Worker dashboard
- [ ] Product management
- [ ] Order management
- [ ] Ticket management

### Phase 6: Admin Dashboard
- [ ] Admin dashboard
- [ ] User management
- [ ] Worker management
- [ ] Audit logs
- [ ] Statistics

### Phase 7: Polish
- [ ] Error pages
- [ ] Loading states
- [ ] Empty states
- [ ] Animations
- [ ] Responsive design
- [ ] Accessibility audit

---

## 🚀 FINAL NOTES

1. **All API responses follow this error format:**
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

2. **Pagination follows Spring Data format:**
```json
{
  "content": [...],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

3. **All authenticated requests must include:**
```
Authorization: Bearer {accessToken}
```

4. **The backend runs on port 8080 by default**

5. **Razorpay payment flow uses modal-based checkout (not redirect)**

6. **Roles hierarchy:** ADMIN > WORKER > USER

---

This prompt provides complete specifications for building a professional frontend that integrates seamlessly with the ShopAPI backend. Follow the types, endpoints, and structure exactly as specified to avoid integration errors.

---

## 🎭 DETAILED UI COMPONENT SPECIFICATIONS

### Navigation Header Component

```typescript
// components/common/Header.tsx
interface HeaderProps {
  // Sticky on scroll
  // Logo (left)
  // Search bar (center) - expandable on mobile
  // Navigation links (center-right)
  // User menu (right) - dropdown
  // Cart icon with badge (right)
  // Mobile hamburger menu
}

Features:
- Transparent on homepage hero, solid on scroll
- Search with autocomplete (debounced API calls)
- Dropdown menu showing: Profile, Orders, Settings, Logout
- Cart badge showing item count from cart store
- Role-based navigation links:
  - User: Home, Products, Orders, Support
  - Worker: + Worker Dashboard
  - Admin: + Admin Dashboard
```

### Product Card Component

```typescript
// components/products/ProductCard.tsx
interface ProductCardProps {
  product: ProductResponse;
  viewMode: 'grid' | 'list';
  onAddToCart: (productId: string) => void;
  onQuickView: (product: ProductResponse) => void;
}

Visual Elements:
- Image with hover zoom effect
- "Sale" badge if discountPrice exists (top-left, red)
- "Featured" badge if featured (top-right, gold)
- "Out of Stock" overlay if !inStock
- Product name (truncate 2 lines)
- Brand name (subtle)
- Star rating with review count
- Price display:
  - If discount: strikethrough original + red discounted price + percentage off
  - If no discount: normal price
- Quick view button (eye icon) on hover
- Add to cart button (appears on hover for grid, always visible for list)
- Wishlist heart icon (top-right of image)

Animations:
- Card lift on hover (translateY -4px + shadow)
- Image scale on hover (1.05)
- Buttons fade in on hover
- Add to cart success: brief green checkmark
```

### Shopping Cart Drawer/Page

```typescript
// components/cart/CartDrawer.tsx (Slide-in drawer)
// pages/user/CartPage.tsx (Full page)

Features:
- Slide-in from right (drawer) or full page
- Item list with:
  - Product thumbnail (clickable to product page)
  - Product name
  - Unit price
  - Quantity selector (-, number input, +)
  - Item subtotal
  - Remove button (trash icon)
- Update cart button (if changes made)
- Subtotal, Shipping estimate, Tax, Total
- Promo code input with Apply button
- Continue Shopping link
- Proceed to Checkout button (disabled if cart empty)
- Empty cart state with illustration

Animations:
- Drawer slide in/out (300ms ease-out)
- Item removal: slide out + height collapse
- Quantity change: brief price highlight
- Loading skeleton while fetching
```

### Checkout Multi-Step Form

```typescript
// pages/user/CheckoutPage.tsx

Steps:
1. Shipping Information
   - Full name
   - Phone number
   - Address line 1
   - Address line 2 (optional)
   - City
   - State/Province
   - Postal code
   - Country (dropdown)
   
2. Billing Information
   - "Same as shipping" checkbox (default checked)
   - If unchecked: show same fields as shipping
   
3. Review Order
   - Order summary (items, quantities, prices)
   - Shipping address display
   - Billing address display
   - Order notes textarea
   - Terms & conditions checkbox
   - Total breakdown
   - Place Order button

UI Features:
- Step indicator (1 - 2 - 3) with current highlighted
- Back/Next navigation
- Form validation on each step
- Persist form data in local state
- Disable navigation if current step invalid
- Loading state on Place Order
- Success redirect to payment
```

### Order Timeline Component

```typescript
// components/orders/OrderTimeline.tsx

interface OrderTimelineProps {
  status: OrderStatus;
  createdAt: string;
  paidAt?: string;
  shippedAt?: string;
  deliveredAt?: string;
}

Visual:
- Horizontal or vertical timeline
- Steps: Created → Paid → Processing → Shipped → Delivered
- Current step highlighted (blue)
- Completed steps with checkmark (green)
- Future steps grayed out
- Timestamps below each completed step
- For FAILED/CANCELLED: show red X at failed step
```

### Admin Dashboard Stats Cards

```typescript
// components/admin/StatsCard.tsx

interface StatsCardProps {
  title: string;
  value: number | string;
  icon: LucideIcon;
  trend?: {
    value: number;
    isPositive: boolean;
  };
  color: 'blue' | 'green' | 'amber' | 'red' | 'purple';
}

Visual:
- Icon in colored circle (left)
- Title (small, muted)
- Value (large, bold)
- Trend indicator (arrow up/down + percentage)
- Subtle colored left border
- Hover: slight lift effect
```

### Data Table Component (Admin/Worker)

```typescript
// components/admin/DataTable.tsx

interface DataTableProps<T> {
  data: T[];
  columns: ColumnDef<T>[];
  pagination: PaginationState;
  onPaginationChange: (pagination: PaginationState) => void;
  sorting?: SortingState;
  onSortingChange?: (sorting: SortingState) => void;
  filters?: FilterState;
  onFilterChange?: (filters: FilterState) => void;
  isLoading?: boolean;
  emptyMessage?: string;
  onRowClick?: (row: T) => void;
  selectedRows?: string[];
  onSelectionChange?: (ids: string[]) => void;
}

Features:
- Sortable column headers (click to sort, icon indicator)
- Filter dropdowns per column
- Search input (global or per-column)
- Row selection with checkboxes
- Bulk actions dropdown (when rows selected)
- Pagination controls (prev, next, page numbers, page size)
- Loading skeleton rows
- Empty state with custom message
- Responsive: horizontal scroll on mobile
- Row hover highlight
- Action buttons column (view, edit, delete)
```

---

## 🔄 STATE MANAGEMENT DETAILS

### Cart Store (Zustand)

```typescript
// stores/cartStore.ts
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface CartStore {
  // State
  cart: CartResponse | null;
  isLoading: boolean;
  error: string | null;
  
  // Computed
  itemCount: number;
  totalPrice: number;
  
  // Actions
  fetchCart: () => Promise<void>;
  addItem: (productId: string, quantity: number) => Promise<void>;
  updateQuantity: (productId: string, quantity: number) => Promise<void>;
  removeItem: (productId: string) => Promise<void>;
  clearCart: () => Promise<void>;
  
  // Optimistic updates
  optimisticAddItem: (item: CartItemResponse) => void;
  revertOptimisticAdd: (productId: string) => void;
}

// Persist cart state for quick access
// Sync with server on mount and after mutations
```

### UI Store

```typescript
// stores/uiStore.ts
interface UIStore {
  // Sidebar
  sidebarOpen: boolean;
  toggleSidebar: () => void;
  
  // Cart drawer
  cartDrawerOpen: boolean;
  openCartDrawer: () => void;
  closeCartDrawer: () => void;
  
  // Quick view modal
  quickViewProduct: ProductResponse | null;
  openQuickView: (product: ProductResponse) => void;
  closeQuickView: () => void;
  
  // Confirm dialog
  confirmDialog: {
    open: boolean;
    title: string;
    message: string;
    onConfirm: () => void;
  } | null;
  showConfirmDialog: (config: ConfirmDialogConfig) => void;
  hideConfirmDialog: () => void;
  
  // Global loading
  globalLoading: boolean;
  setGlobalLoading: (loading: boolean) => void;
}
```

---

## 🎬 ANIMATION SPECIFICATIONS

### Page Transitions

```typescript
// Use framer-motion AnimatePresence

const pageVariants = {
  initial: { opacity: 0, y: 20 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: -20 }
};

const pageTransition = {
  type: 'tween',
  ease: 'easeInOut',
  duration: 0.3
};

// Wrap page content in motion.div with these variants
```

### List Animations

```typescript
// Stagger children animation for product grids, order lists, etc.

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.1
    }
  }
};

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  visible: { opacity: 1, y: 0 }
};
```

### Micro-interactions

```typescript
// Button press effect
whileTap={{ scale: 0.98 }}

// Card hover
whileHover={{ y: -4, boxShadow: '0 10px 40px rgba(0,0,0,0.1)' }}

// Icon button hover
whileHover={{ scale: 1.1 }}
whileTap={{ scale: 0.9 }}

// Success checkmark
animate={{ scale: [0, 1.2, 1], rotate: [0, 10, 0] }}

// Error shake
animate={{ x: [0, -10, 10, -10, 10, 0] }}
```

### Loading Skeletons

```typescript
// Skeleton pulse animation (Tailwind)
className="animate-pulse bg-slate-200 rounded"

// Custom skeleton with shimmer
const shimmer = `
  relative overflow-hidden
  before:absolute before:inset-0
  before:-translate-x-full
  before:animate-[shimmer_1.5s_infinite]
  before:bg-gradient-to-r
  before:from-transparent
  before:via-white/20
  before:to-transparent
`;
```

---

## 🚨 COMPREHENSIVE ERROR HANDLING

### Error Boundary

```typescript
// components/common/ErrorBoundary.tsx
class ErrorBoundary extends React.Component<Props, State> {
  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    // Log to error reporting service
    console.error('Error boundary caught:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center">
          <div className="text-center">
            <h1 className="text-2xl font-bold text-red-600">Something went wrong</h1>
            <p className="text-slate-600 mt-2">{this.state.error?.message}</p>
            <button 
              onClick={() => window.location.reload()}
              className="mt-4 btn-primary"
            >
              Reload Page
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
```

### Specific Error Scenarios & Handling

```typescript
// 1. Session Expired During Action
// Scenario: User is filling checkout form, token expires
// Handling: 
//   - Interceptor attempts refresh
//   - If refresh fails: save form data to sessionStorage
//   - Redirect to login with ?redirect=/checkout
//   - After login: restore form data and continue

// 2. Product Out of Stock During Checkout
// Scenario: User tries to checkout but item became unavailable
// API returns: 409 INSUFFICIENT_STOCK
// Handling:
//   - Show modal: "Some items in your cart are no longer available"
//   - List affected items
//   - Options: "Update Cart" (redirect to cart) or "Remove & Continue"
//   - Update cart state to reflect actual availability

// 3. Payment Failed
// Scenario: Razorpay payment fails
// Handling:
//   - Redirect to /payment/cancel
//   - Show friendly error message
//   - Offer: "Try Again" (creates new payment session) or "Contact Support"
//   - Order status remains CREATED

// 4. Email Already Exists (Registration)
// API returns: 409 USER_ALREADY_EXISTS
// Handling:
//   - Show inline error under email field
//   - Offer: "Already have an account? Login here"

// 5. Invalid Reset Token
// Scenario: Token expired or already used
// API returns: 400 TOKEN_EXPIRED
// Handling:
//   - Show error page
//   - Offer: "Request new reset link"

// 6. Validation Errors
// API returns: 400 with errors object
// Handling:
//   - Map errors to form fields
//   - Show inline error messages
//   - Focus first invalid field

// 7. Network Error
// Scenario: No internet connection
// Handling:
//   - Show toast: "Network error. Please check your connection."
//   - Add offline indicator in header
//   - Queue mutations for retry when online

// 8. 403 Forbidden
// Scenario: User tries to access admin page
// Handling:
//   - Redirect to /forbidden
//   - Log attempted access (for security)

// 9. 404 Not Found
// Scenario: Product/Order no longer exists
// Handling:
//   - Show 404 page with search option
//   - "The item you're looking for doesn't exist"
//   - Suggest similar products (if applicable)

// 10. Server Error (500)
// Handling:
//   - Show friendly error page
//   - Offer retry button
//   - Contact support link
//   - Auto-retry with exponential backoff for GET requests
```

### Toast Notification Patterns

```typescript
// Success toasts
toast.success('Item added to cart');
toast.success('Order placed successfully!');
toast.success('Password changed');
toast.success('Profile updated');

// Error toasts
toast.error('Failed to add item to cart');
toast.error('Payment failed. Please try again.');
toast.error('Session expired. Please login again.');

// Info toasts
toast.info('Verification email sent');
toast.info('Your order is being processed');

// Warning toasts  
toast.warning('Low stock - only 3 items left');
toast.warning('Some items in your cart have changed');

// Loading toasts (for long operations)
const loadingToast = toast.loading('Processing payment...');
// Later:
toast.dismiss(loadingToast);
toast.success('Payment successful!');
```

---

## 📱 RESPONSIVE DESIGN DETAILS

### Breakpoint-Specific Layouts

```typescript
// Mobile (< 640px)
- Single column layouts
- Bottom navigation bar
- Full-width cards
- Collapsed filters (expandable)
- Hamburger menu
- Simplified tables (cards instead)

// Tablet (640px - 1024px)
- 2-column product grid
- Sidebar as overlay/drawer
- Compact navigation

// Desktop (> 1024px)
- 3-4 column product grid
- Persistent sidebar
- Full navigation
- Hover interactions
```

### Mobile-Specific Components

```typescript
// Mobile Navigation
- Bottom tab bar (Home, Search, Cart, Profile)
- Cart badge on cart tab
- Active state indicator

// Mobile Filters
- Filter button opens full-screen modal
- Apply/Clear buttons at bottom
- Sticky header with close button

// Mobile Product Grid
- 2 columns
- Smaller images
- Essential info only
- Infinite scroll instead of pagination
```

---

## 🔒 SECURITY CONSIDERATIONS

```typescript
// 1. Token Storage
- Store tokens in localStorage (acceptable for SPAs)
- NEVER log tokens
- Clear on logout

// 2. XSS Prevention
- React automatically escapes
- Don't use dangerouslySetInnerHTML
- Sanitize user-generated content

// 3. Form Validation
- Client-side validation for UX
- Server-side validation is authoritative
- Never trust client-side validation alone

// 4. Sensitive Data
- Don't log sensitive data
- Mask card numbers, passwords
- Clear forms after submission

// 5. HTTPS
- Ensure API calls use HTTPS in production
- Set secure cookie flags
```

---

## 🧪 TESTING RECOMMENDATIONS

```typescript
// Unit Tests (Vitest)
- Component rendering
- Store actions
- Utility functions
- Form validation

// Integration Tests (Testing Library)
- User flows (login, checkout)
- API integration
- Error handling

// E2E Tests (Playwright/Cypress)
- Critical user journeys
- Payment flow
- Admin operations
```

---

## 📊 PERFORMANCE OPTIMIZATIONS

```typescript
// 1. Code Splitting
- Lazy load routes
- Dynamic imports for heavy components

// 2. Image Optimization
- Use next/image or similar
- Lazy loading
- Proper sizes attribute
- WebP format

// 3. Caching
- React Query cache
- Service worker for assets

// 4. Bundle Size
- Tree shaking
- Analyze with bundlesize
- Import only needed icons

// 5. Rendering
- useMemo for expensive computations
- useCallback for event handlers
- Virtualize long lists
```

---

## 🚀 DEPLOYMENT CHECKLIST

```bash
# Environment Variables
VITE_API_BASE_URL=https://api.yourshop.com
VITE_STRIPE_PUBLISHABLE_KEY=pk_live_xxx

# Build
npm run build

# Preview locally
npm run preview

# Deploy to:
- Vercel (recommended)
- Netlify
- AWS Amplify
- Any static host with SPA support
```

---

**END OF FRONTEND DEVELOPMENT PROMPT**

This comprehensive guide ensures that any AI or developer can build a fully functional, professional e-commerce frontend that integrates perfectly with the ShopAPI backend. Follow every specification exactly to avoid integration issues and create a seamless user experience.
