package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String, // e.g. CUS-00001
    val fullName: String,
    val nicNumber: String,
    val phoneNumber: String,
    val altPhone: String,
    val address: String,
    val occupation: String,
    val monthlyIncome: Double,
    val guarantorName: String,
    val guarantorPhone: String,
    val gpsLocation: String,
    val status: String, // Active, Pending, Completed, Blacklisted
    val registrationDate: String
)

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey val id: String, // e.g. GI-LOAN-00001
    val customerId: String,
    val customerName: String,
    val loanAmount: Double,
    val interestType: String, // Fixed, Reducing
    val interestRate: Double, // e.g. 12%
    val startDate: String,
    val endDate: String,
    val installmentFrequency: String, // Daily, Weekly, Monthly
    val installmentAmount: Double,
    val principalBalance: Double,
    val interestBalance: Double,
    val penaltyBalance: Double,
    val outstandingBalance: Double,
    val status: String // Pending, Approved, Active, Completed, Overdue, Defaulted
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val receiptNumber: String, // e.g. GI-RCP-00001
    val loanId: String,
    val customerId: String,
    val customerName: String,
    val paymentDate: String,
    val paymentTime: String,
    val amountPaid: Double,
    val remainingBalance: Double,
    val collectorName: String,
    val paymentMethod: String, // Cash, Bank Transfer, QR, Mobile Wallet
    val remarks: String
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userName: String,
    val userRole: String,
    val action: String,
    val timestamp: Long,
    val previousValue: String,
    val newValue: String,
    val approvalStatus: String, // Approved, Rejected, Pending Approval
    val approvedBy: String,
    val deviceInfo: String
)

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey val id: String, // e.g. GI-USER-00001
    val username: String,
    val fullName: String,
    val role: String, // Owner, Manager, Collection Officer, Accountant, Cashier, Branch Manager
    val branchId: String,
    val status: String, // Pending Approval, Active, Suspended, Disabled
    val loginMode: String, // Permanent Access, Daily Approval Access
    val isApprovedToday: Boolean,
    val approvalDuration: String // One Day, One Week, Permanent
)

@Entity(tableName = "branches")
data class BranchEntity(
    @PrimaryKey val id: String, // e.g. GI-BR-001
    val name: String,
    val location: String,
    val branchCode: String
)
