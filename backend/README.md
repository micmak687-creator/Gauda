# Garuda Finance (GILCMS) - Core REST API Design Documentation

Welcome to the enterprise-grade backend services for **Garuda Investment Ledger & Collection Management System (GILCMS)**. This Node.js controller service uses **Express**, **Prisma**, and **PostgreSQL** to maintain strict microfinance records, record live fields collection tracking, synchronize offline queues, and enforce role-based access control (RBAC).

---

## 🔒 Security & RBAC Guard Protocol
The backend uses standard crypto-signed **JSON Web Tokens (JWT)** for access leases. All request calls made to protected routes require the following authorization header with a 24-Hour leased ticket:

```http
Authorization: Bearer <JWT_SECURITY_TOKEN>
```

### Role Matrices
*   **Owner (`Owner`)**: Unrestricted dashboard views, full database analytics, disburse loans, review approvals/bypasses, read system compliance logs.
*   **Manager (`Manager`)**: Access regional indicators, edit customer accounts, disburse loans, review approvals/bypasses, read system logs.
*   **Collection Officer (`Collection Officer`)**: Create customer profiles, submit field collection payments, upload biometric metadata, queue offline payment packets.

---

## 🧭 Endpoint Reference Manual

### 1. Identity Services

#### 🔑 Secure Operator Login
*   **Endpoint / Verb**: `/api/auth/login` | `POST`
*   **Security Level**: Public
*   **Description**: Validates operator credentials and issues an audited JWT claiming branch roles.
*   **Request Payload**:
    ```json
    {
      "username": "owner_vishwa",
      "password": "owner123",
      "role": "Owner"
    }
    ```
*   **Successful Response (200 OK)**:
    ```json
    {
      "success": true,
      "message": "Access Granted: JWT authenticated successfully.",
      "token": "eyJhbGciOiJIUzI1...hmac_key",
      "user": {
        "id": "c8ea230c-ea19-487c-a496-eacda603b2",
        "username": "owner_vishwa",
        "fullName": "Vishwa Perera",
        "role": "Owner",
        "branchId": "GI-BR-001"
      }
    }
    ```

---

### 2. CRM Portfolio Ledger

#### 📂 List All Customers
*   **Endpoint / Verb**: `/api/customers` | `GET`
*   **Security Level**: JWT Protected (`Owner`, `Manager`, `Collection Officer`)
*   **Successful Response (200 OK)**:
    ```json
    {
      "success": true,
      "customers": [
        {
          "id": "e67bfa39-478a-49bf-ba1b-b4ca9bb1a1a7",
          "fullName": "Tharindu Perera",
          "nicNumber": "199418293712",
          "phoneNumber": "+94771234567",
          "address": "12/A Galle Road, Colombo-03",
          "monthlyIncome": 85000.00,
          "guarantorName": "Kumara Silva",
          "guarantorPhone": "+94779876543",
          "gpsLocation": "6.9271, 79.8612"
        }
      ]
    }
    ```

#### ➕ Register a Customer
*   **Endpoint / Verb**: `/api/customers` | `POST`
*   **Security Level**: JWT Protected (`Owner`, `Manager`, `Collection Officer`)
*   **Request Payload**:
    ```json
    {
      "fullName": "Anura Senanayake",
      "nicNumber": "199042918302",
      "phoneNumber": "+94711122334",
      "address": "45/3 Negombo Road, Kurunegala",
      "monthlyIncome": 72000.00,
      "guarantorName": "Wimal Siri",
      "guarantorPhone": "+94717778899",
      "gpsLocation": "7.4818, 80.3609"
    }
    ```

#### ✏️ Edit Customer Profile Detail
*   **Endpoint / Verb**: `/api/customers/:id` | `PUT`
*   **Security Level**: JWT Protected (`Owner`, `Manager`, `Collection Officer`)
*   **Request Payload**: Only provide updated items:
    ```json
    {
      "phoneNumber": "+94719998877",
      "address": "45/4 Negombo Road, Kurunegala"
    }
    ```

---

### 3. Disbursal & Repayment Engine

#### 💸 Create/Disburse Loan Portfolio
*   **Endpoint / Verb**: `/api/loans` | `POST`
*   **Security Level**: JWT Restricted (`Owner`, `Manager`)
*   **Description**: Disburses custom capital, calculates simple interest payback models, and schedules equal installments based on chosen frequency.
*   **Request Payload**:
    ```json
    {
      "customerId": "e67bfa39-478a-49bf-ba1b-b4ca9bb1a1a7",
      "loanAmount": 100000.00,
      "interestRate": 12.0,
      "frequency": "Daily",
      "paymentDurationDays": 100,
      "startDate": "2026-06-07"
    }
    ```
*   **Response Dashboard (201 Created)**:
    ```json
    {
      "success": true,
      "loan": {
        "id": "b1b01c1c-c765-4d2c-9a2c-e366fd2fbd20",
        "customerId": "e67bfa39-478a-49bf-ba1b-b4ca9bb1a1a7",
        "customerName": "Tharindu Perera",
        "loanAmount": 100000.00,
        "interestRate": 12.0,
        "installmentAmount": 1032.88,
        "outstandingBalance": 103288.00,
        "frequency": "Daily",
        "status": "Active"
      },
      "metrics": {
        "totalInterest": 3288.00,
        "totalRepayable": 103288.00,
        "installmentAmount": 1032.88,
        "installmentCount": 100
      }
    }
    ```

#### ⚡ Force Penalty Checks Scan
*   **Endpoint / Verb**: `/api/loans/penalties` | `POST`
*   **Security Level**: JWT Restricted (`Owner`, `Manager`)
*   **Description**: Scans the outstanding ledger manually, auto-updates expired loans to "Overdue" status, and applies standard late charge penalties.

---

### 4. Collection Ledger Interfaces

#### 💵 Submit Collection Payment
*   **Endpoint / Verb**: `/api/collections` | `POST`
*   **Security Level**: JWT Protected (`Owner`, `Manager`, `Collection Officer`)
*   **Description**: Records standard cash/bank installment collection. Reduces outstanding balance, closes status to "Completed" if pay is fully resolved.
*   **Request Payload**:
    ```json
    {
      "loanId": "b1b01c1c-c765-4d2c-9a2c-e366fd2fbd20",
      "amountPaid": 5000.00,
      "paymentMethod": "Cash",
      "remarks": "Regular cycle field payment"
    }
    ```

#### 🔄 Sync Offline Queues (Synchronizer Gateway)
*   **Endpoint / Verb**: `/api/collections/sync` | `POST`
*   **Security Level**: JWT Protected (`Owner`, `Manager`, `Collection Officer`)
*   **Description**: Accepts an ordered array list of collection payments queued locally on mobile storage when offline during field inspections. This performs individual transaction reconciliations.
*   **Request Payload**:
    ```json
    {
      "queue": [
        {
          "loanId": "b1b01c1c-c765-4d2c-9a2c-e366fd2fbd20",
          "amountPaid": 1032.88,
          "paymentMethod": "Cash",
          "remarks": "Day 4 offline trace",
          "paymentDate": "2026-06-05",
          "paymentTime": "02:30 PM"
        }
      ]
    }
    ```

---

### 5. Corporate Administration Tools

#### 📝 List All Audit Trails
*   **Endpoint / Verb**: `/api/audit` | `GET`
*   **Security Level**: JWT Restricted (`Owner`, `Manager`)

#### 📋 View Operation Approval Queries
*   **Endpoint / Verb**: `/api/approvals` | `GET`
*   **Security Level**: JWT Restricted (`Owner`, `Manager`)
*   **Description**: Provides lists of all system bypasses requested by lower collection roles.
