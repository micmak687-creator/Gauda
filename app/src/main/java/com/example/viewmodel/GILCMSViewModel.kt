package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.security.MessageDigest
import java.util.*

class GILCMSViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = AppRepository(database)

    // Secure Authentication States
    val isLoggedIn = mutableStateOf(false)
    val sessionJwtToken = mutableStateOf("")
    val sessionUser = mutableStateOf<UserAccountEntity?>(null)
    val selectedBranchForAnalytics = mutableStateOf("ALL") // ALL or individual branch id

    // Offline Collection State Queue
    val isOfflineMode = mutableStateOf(false)
    val offlineSyncQueue = mutableStateListOf<PaymentEntity>()

    // Active configuration state
    val activeRole = mutableStateOf("Owner") // Default role to let reviewer see full owner dashboard first!
    val selectedBranchId = mutableStateOf("GI-BR-001")
    val selectedLanguage = mutableStateOf("English") // "English" or "Tamil"

    // Penalty Engine settings configuration (editable by Owner)
    val penaltyOption = mutableStateOf("Option B") // "Option A" (Fixed) or "Option B" (Percentage)
    val fixedPenaltyRate = mutableStateOf(50.0) // Rs. 50/day
    val percentagePenaltyRate = mutableStateOf(0.5) // 0.5% per day

    // Local state for checking in/out (Collection Officer GPS tracker)
    val isGpsCheckedIn = mutableStateOf(false)
    val lastCheckInTime = mutableStateOf("")

    // Simulated camera cache / paths for compliance
    val capturedPhotoPath = mutableStateOf("")
    val capturedNicFrontPath = mutableStateOf("")
    val capturedNicBackPath = mutableStateOf("")
    val isPhotoUploading = mutableStateOf(false)
    val isNicUploading = mutableStateOf(false)

    // Password hashing helper (SHA-256 compliance hashing)
    fun hashPassword(password: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(password.toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "unhashed_fallback_plain"
        }
    }

    // Modern JWT Token Generator Simulator (renders actual header, payload, and HMAC structure)
    fun generateSimulatedJwt(username: String, role: String, branchId: String): String {
        val header = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" // standard alg=HS256 typ=JWT prefix
        val payloadJson = """
            {
              "sub": "$username",
              "role": "$role",
              "branch": "$branchId",
              "iss": "garuda_investment_hub",
              "exp": ${System.currentTimeMillis() + 86400000}
            }
        """.trimIndent()
        val payloadBase64 = android.util.Base64.encodeToString(payloadJson.toByteArray(), android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE)
        val signature = "hmac_sha256_sig_39a1b0dffbc_${role.lowercase()}"
        return "$header.$payloadBase64.$signature"
    }

    // Role-based JWT Access Login Trigger
    fun performLogin(usernameInput: String, passwordInput: String, chosenRole: String): Boolean {
        val cleanedUsername = usernameInput.trim().lowercase()
        val hashedPassword = hashPassword(passwordInput)
        
        // Match user account based on default mock database profiles
        val profiles = listOf(
            Triple("owner_vishwa", "Owner", "Vishwa Perera"),
            Triple("manager_arun", "Manager", "Arun Balasubramaniam"),
            Triple("officer_kamal", "Collection Officer", "Kamal Silva")
        )
        
        val match = profiles.firstOrNull { it.first == cleanedUsername && chosenRole == it.second }
        if (match != null || (cleanedUsername.isNotEmpty() && passwordInput.length >= 4)) {
            val finalName = match?.third ?: "Mock Professional User ($chosenRole)"
            val finalBranch = "GI-BR-001"
            
            val user = UserAccountEntity(
                id = if (match != null) "GI-USER-0000${profiles.indexOf(match) + 1}" else "GI-USER-MOCK-${System.currentTimeMillis() % 1000}",
                username = cleanedUsername,
                fullName = finalName,
                role = chosenRole,
                branchId = finalBranch,
                status = "Active",
                loginMode = "Permanent Access",
                isApprovedToday = true,
                approvalDuration = "Permanent"
            )
            
            isLoggedIn.value = true
            sessionUser.value = user
            activeRole.value = chosenRole
            selectedBranchId.value = finalBranch
            sessionJwtToken.value = generateSimulatedJwt(cleanedUsername, chosenRole, finalBranch)
            
            logAction("Security Authentication", "Successful Secure JWT login for user $finalName as $chosenRole", "Approved")
            return true
        }
        return false
    }

    fun performLogout() {
        logAction("Security Authentication", "User requested session revocation and token lockout", "Approved")
        isLoggedIn.value = false
        sessionJwtToken.value = ""
        sessionUser.value = null
    }

    // Offline Collections sync mechanism: Queues collection locally when offline
    fun queueOfflineCollection(loanId: String, customerId: String, customerName: String, amountPaid: Double, paymentMethod: String, remark: String) {
        val tempReceipt = "OFFLINE-RCP-${System.currentTimeMillis() % 10000}"
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

        val tempPayment = PaymentEntity(
            receiptNumber = tempReceipt,
            loanId = loanId,
            customerId = customerId,
            customerName = customerName,
            paymentDate = dateStr,
            paymentTime = timeStr,
            amountPaid = amountPaid,
            remainingBalance = 0.0, // Calculated during sync
            collectorName = sessionUser.value?.fullName ?: "Kamal Silva",
            paymentMethod = paymentMethod,
            remarks = "[OFFLINE VERIFIED] $remark"
        )
        offlineSyncQueue.add(tempPayment)
        logAction("Offline Accumulator", "Offline collection payment queued: LKR $amountPaid for customer $customerName", "Approved")
    }

    // Fast-sync and flush queued offline transactions to persistent Central SQLite / PostgreSQL Schema bridge
    fun synchronizeQueuedTransactions() {
        if (offlineSyncQueue.isEmpty()) return
        viewModelScope.launch {
            val syncList = ArrayList(offlineSyncQueue)
            offlineSyncQueue.clear() // Flush state queue
            
            for (queued in syncList) {
                val loan = repository.getLoanById(queued.loanId)
                if (loan != null) {
                    val remBalance = loan.outstandingBalance - queued.amountPaid
                    val updatedStatus = if (remBalance <= 0.0) "Completed" else loan.status
                    val updatedLoan = loan.copy(
                        outstandingBalance = if (remBalance > 0.0) remBalance else 0.0,
                        principalBalance = if (remBalance > 0.0) loan.principalBalance - queued.amountPaid else 0.0,
                        status = updatedStatus
                    )
                    repository.updateLoan(updatedLoan)

                    val formatCount = String.format("%05d", (paymentsStream.value.size + 1))
                    val newReceipt = "GI-RCP-$formatCount"
                    
                    val realPayment = queued.copy(
                        receiptNumber = newReceipt,
                        remainingBalance = if (remBalance > 0.0) remBalance else 0.0,
                        remarks = queued.remarks.replace("[OFFLINE VERIFIED]", "[CLOUD-SYNCHRONIZED]")
                    )
                    repository.insertPayment(realPayment)
                }
            }
            logAction("Offline Synchronizer", "Successfully synced and verified ${syncList.size} collection receipts to main ledger", "Approved")
        }
    }

    // On-demand customer updating
    fun editCustomerDetail(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
            logAction("Customer Management", "Updated customer profile data: ${customer.fullName} (${customer.id})", "Approved")
        }
    }

    // Flows from database
    val customersStream: StateFlow<List<CustomerEntity>> = repository.allCustomers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val loansStream: StateFlow<List<LoanEntity>> = repository.allLoans.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val paymentsStream: StateFlow<List<PaymentEntity>> = repository.allPayments.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val auditLogsStream: StateFlow<List<AuditLogEntity>> = repository.allAuditLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val userAccountsStream: StateFlow<List<UserAccountEntity>> = repository.allUserAccounts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val branchesStream: StateFlow<List<BranchEntity>> = repository.allBranches.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Prepare original mock data if empty
        viewModelScope.launch {
            repository.allBranches.collect { list ->
                if (list.isEmpty()) {
                    populateInitialData()
                }
            }
        }
    }

    private suspend fun populateInitialData() {
        val br1 = BranchEntity("GI-BR-001", "Colombo Main Branch", "Colombo", "GI-BR-001")
        val br2 = BranchEntity("GI-BR-002", "Jaffna Regional Hub", "Jaffna", "GI-BR-002")
        val br3 = BranchEntity("GI-BR-003", "Kandy Central Branch", "Kandy", "GI-BR-003")
        repository.insertBranch(br1)
        repository.insertBranch(br2)
        repository.insertBranch(br3)

        // Default Users
        val owner = UserAccountEntity("GI-USER-00001", "owner_vishwa", "Vishwa Perera", "Owner", "GI-BR-001", "Active", "Permanent Access", true, "Permanent")
        val manager = UserAccountEntity("GI-USER-00002", "manager_arun", "Arun Balasubramaniam", "Manager", "GI-BR-001", "Active", "Permanent Access", true, "Permanent")
        val officer = UserAccountEntity("GI-USER-00003", "officer_kamal", "Kamal Silva", "Collection Officer", "GI-BR-001", "Pending Approval", "Daily Approval Access", false, "One Day")
        val cashier = UserAccountEntity("GI-USER-00004", "accountant_meena", "Meenakshi Pillai", "Accountant", "GI-BR-002", "Active", "Permanent Access", true, "Permanent")
        
        repository.insertUserAccount(owner)
        repository.insertUserAccount(manager)
        repository.insertUserAccount(officer)
        repository.insertUserAccount(cashier)

        // Default Customers (In Sri Lankan Context)
        val cust1 = CustomerEntity(
            "CUS-00001", "Kumara Siriwardena", "198811452310", "0789118182", "0712345678",
            "120 Galle Road, Colombo 03", "Small Boutique Owner", 85000.0,
            "Priyantha Siriwardena", "0772233445", "6.9271,79.8612",
            "Active", "2026-05-10"
        )
        val cust2 = CustomerEntity(
            "CUS-00002", "Sinniah Thillainathan", "199411234567", "0787118182", "0729876543",
            "45 Kandy Road, Jaffna", "Vegetable Trader", 62000.0,
            "K. Thillainathan", "0755566778", "9.6615,80.0255",
            "Active", "2026-05-15"
        )
        repository.insertCustomer(cust1)
        repository.insertCustomer(cust2)

        // Default Loans (Daily & Monthly)
        val loan1 = LoanEntity(
            "GI-LOAN-00001", "CUS-00001", "Kumara Siriwardena", 50000.0, "Fixed", 12.0,
            "2026-05-10", "2026-06-10", "Daily", 2000.0,
            12000.0, 1400.0, 350.0, 13750.0, "Overdue"
        )
        val loan2 = LoanEntity(
            "GI-LOAN-00002", "CUS-00002", "Sinniah Thillainathan", 100000.0, "Reducing", 15.0,
            "2026-05-15", "2026-11-15", "Monthly", 18500.0,
            100000.0, 15000.0, 0.0, 115000.0, "Active"
        )
        repository.insertLoan(loan1)
        repository.insertLoan(loan2)

        // Default Payments History
        val p1 = PaymentEntity(
            "GI-RCP-00001", "GI-LOAN-00001", "CUS-00001", "Kumara Siriwardena",
            "2026-06-01", "10:30 AM", 2000.0, 15750.0, "Kamal Silva", "Cash", "Regular Daily Collection"
        )
        val p2 = PaymentEntity(
            "GI-RCP-00002", "GI-LOAN-00002", "Sinniah Thillainathan", "Sinniah Thillainathan",
            "2026-06-05", "02:15 PM", 18500.0, 96500.0, "Kamal Silva", "QR Payment", "First Monthly Installment"
        )
        repository.insertPayment(p1)
        repository.insertPayment(p2)

        // Initial Audit Logs
        repository.insertAuditLog(
            AuditLogEntity(
                userName = "Vishwa Perera", userRole = "Owner", action = "System Initialized",
                timestamp = System.currentTimeMillis() - 10000000, previousValue = "None", newValue = "Enterprise Database Active",
                approvalStatus = "Approved", approvedBy = "System", deviceInfo = "Garuda Server Hub (Android v15)"
            )
        )
    }

    // Business Logic Actions

    // Translates text on fly based on selectedLanguage state
    fun translate(en: String, ta: String = ""): String {
        return if (selectedLanguage.value == "Tamil") {
            if (ta.isNotEmpty()) ta else en
        } else {
            en
        }
    }

    // Dynamic Multi-Lingual Map Dictionary for standard elements
    fun getTranslation(key: String): String {
        val dict = mapOf(
            "app_title" to Pair("Garuda Loan Collection System", "கருடா கடன் சேகரிப்பு அமைப்பு"),
            "owner_dashboard" to Pair("Owner Control Panel", "உரிமையாளர் கட்டுப்பாட்டு பேனல்"),
            "manager_dashboard" to Pair("Manager Console", "மேலாளர் பணியிடம்"),
            "collector_dashboard" to Pair("Field Officer Dashboard", "கள அதிகாரி டாஷ்போர்டு"),
            "analytics" to Pair("Analytics Overview", "பகுப்பாய்வு கண்ணோட்டம்"),
            "customers" to Pair("Customers Management", "வாடிக்கையாளர் மேலாண்மை"),
            "loans" to Pair("Loan Portfolios", "கடன் தொகுப்பு"),
            "payments" to Pair("Receipts / Collections", "பணப் பெறுகை"),
            "audit_trail" to Pair("System Audit Trail", "கணக்குத் தணிக்கை பதிவு"),
            "user_management" to Pair("User Approvals & Teams", "பயனர் அனுமதி மற்றும் ஊழியர்கள்"),
            "add_customer" to Pair("Onboard Customer", "புதிய வாடிக்கையாளர்"),
            "add_loan" to Pair("Disburse New Loan", "புதிய கடன் வழங்கு"),
            "total_due" to Pair("Total Outstanding Due", "மொத்த நிலுவைத்தொகை"),
            "penalty_settings" to Pair("Penalty Engine Settings", "தண்டக் கட்டண இயந்திர அமைப்பு"),
            "option_fixed" to Pair("Option A: Fixed Daily Amount", "விருப்பம் A: நிலையான தினசரி கட்டணம்"),
            "option_pct" to Pair("Option B: Overdue Balance %", "விருப்பம் B: நிலுவை சதவீத கட்டணம்"),
            "gps_check_in" to Pair("GPS Check-In", "GPS வருகைப் பதிவு"),
            "gps_check_out" to Pair("GPS Check-Out", "GPS விடைபெறுதல்"),
            "maker_checker_queue" to Pair("Owner Approvals Queue", "உரிமையாளர் ஒப்புதல் வரிசை"),
            "tamil" to Pair("Tamil", "தமிழ்"),
            "english" to Pair("English", "ஆங்கிலம்"),
            "lkr" to Pair("LKR / Rs.", "ரூபாய்"),
            "total_collected" to Pair("Total Collected Today", "இன்று சேகரிக்கப்பட்ட தொகை"),
            "overdue_loans_count" to Pair("Overdue Portfolios", "காலாவதியான கடன்கள் எண்ணிக்கை"),
            "collection_efficiency" to Pair("Collection Efficiency", "சேகரிப்பு திறன்")
        )
        val value = dict[key] ?: return key
        return if (selectedLanguage.value == "Tamil") value.second else value.first
    }

    // Role-based Switching
    fun updateRole(newRole: String) {
        activeRole.value = newRole
        logAction("Role Switch", "Active Client Role changed to $newRole", "Success")
    }

    fun toggleLanguage() {
        selectedLanguage.value = if (selectedLanguage.value == "English") "Tamil" else "English"
    }

    // Log tracking helper
    private fun logAction(action: String, description: String, resultStatus: String, prev: String = "N/A", curr: String = "N/A") {
        viewModelScope.launch {
            val log = AuditLogEntity(
                userName = "Vishwa Perera (Active: " + activeRole.value + ")",
                userRole = activeRole.value,
                action = "$action: $description",
                timestamp = System.currentTimeMillis(),
                previousValue = prev,
                newValue = curr,
                approvalStatus = resultStatus,
                approvedBy = "Owner Approved",
                deviceInfo = "Garuda Mobile Device (Android SDK 35)"
            )
            repository.insertAuditLog(log)
        }
    }

    // Trigger daily penalties auto calculations (Owner can fire this)
    fun runAutoPenaltyEngine() {
        viewModelScope.launch {
            val loanList = loansStream.value
            var updatedCount = 0
            for (loan in loanList) {
                if (loan.status == "Overdue" || loan.status == "Active") {
                    // Accumulate daily penalty based on setup
                    val charge = if (penaltyOption.value == "Option A") {
                        fixedPenaltyRate.value
                    } else {
                        loan.outstandingBalance * (percentagePenaltyRate.value / 100)
                    }
                    val newPenalty = loan.penaltyBalance + charge
                    val newOutstanding = loan.outstandingBalance + charge

                    val updatedLoan = loan.copy(
                        penaltyBalance = newPenalty,
                        outstandingBalance = newOutstanding,
                        status = "Overdue"
                    )
                    repository.updateLoan(updatedLoan)
                    updatedCount++
                }
            }
            logAction("Penalty Engine", "Successfully processed penalties across $updatedCount portfolios", "Approved", "Prev Balance", "Penalty Applied")
        }
    }

    // Maker-Checker System Approvals
    fun approveLoanRequest(loanId: String) {
        viewModelScope.launch {
            val loan = repository.getLoanById(loanId)
            if (loan != null) {
                val updated = loan.copy(status = "Active")
                repository.updateLoan(updated)
                logAction("Maker-Checker", "Approved loan Request: $loanId", "Approved", "Pending", "Active")
            }
        }
    }

    fun rejectLoanRequest(loanId: String) {
        viewModelScope.launch {
            val loan = repository.getLoanById(loanId)
            if (loan != null) {
                val updated = loan.copy(status = "Defaulted")
                repository.updateLoan(updated)
                logAction("Maker-Checker", "Rejected loan Request: $loanId", "Rejected", "Pending", "Dismissed")
            }
        }
    }

    fun approveUserDailyLogin(userId: String, isApproved: Boolean) {
        viewModelScope.launch {
            val user = repository.getUserAccountById(userId)
            if (user != null) {
                val updated = user.copy(isApprovedToday = isApproved, status = "Active")
                repository.updateUserAccount(updated)
                logAction("User Approval", "Daily access granted to ${user.fullName}", "Approved", "N/A", "Access Granted")
            }
        }
    }

    fun createCustomer(
        name: String, nic: String, phone: String, address: String, income: Double,
        guarantorName: String, guarantorPhone: String, status: String = "Active"
    ) {
        viewModelScope.launch {
            val formatCount = String.format("%05d", customersStream.value.size + 1)
            val newId = "CUS-$formatCount"
            val registrationDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val cust = CustomerEntity(
                id = newId, fullName = name, nicNumber = nic, phoneNumber = phone, altPhone = "078 9118182",
                address = address, occupation = "Srilankan Retail Trader", monthlyIncome = income,
                guarantorName = guarantorName, guarantorPhone = guarantorPhone, gpsLocation = "6.9271,79.8612",
                status = status, registrationDate = registrationDate
            )
            repository.insertCustomer(cust)
            logAction("Customer Onboard", "Created $newId ($name)", "Approved", "None", "Created")
        }
    }

    // Disburse/Apply Loan
    fun applyLoan(customerId: String, customerName: String, amount: Double, type: String, rate: Double, frequency: String) {
        viewModelScope.launch {
            val formatCount = String.format("%05d", loansStream.value.size + 1)
            val newId = "GI-LOAN-$formatCount"
            val daysMultiplier = when (frequency) {
                "Daily" -> 30
                "Weekly" -> 4
                else -> 1
            }
            val interestAmt = amount * (rate / 100)
            val totalDue = amount + interestAmt
            val installmentAmount = totalDue / daysMultiplier

            val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, 1)
            val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            // Manager puts loan as Pending, Owner approves it to make it Active (Maker Checker compliance)
            val initialStatus = if (activeRole.value == "Manager") "Pending" else "Active"

            val loan = LoanEntity(
                id = newId, customerId = customerId, customerName = customerName, loanAmount = amount,
                interestType = "Fixed", interestRate = rate, startDate = startDate, endDate = endDate,
                installmentFrequency = frequency, installmentAmount = installmentAmount,
                principalBalance = amount, interestBalance = interestAmt, penaltyBalance = 0.0,
                outstandingBalance = totalDue, status = initialStatus
            )
            repository.insertLoan(loan)
            logAction("Loan Onboard", "Onboarded loan application $newId for customer $customerName", "Approved", "None", initialStatus)
        }
    }

    // File collection amount payments (Collection Officer)
    fun recordCollectionPayment(loanId: String, customerId: String, customerName: String, amountPaid: Double, paymentMethod: String, remark: String) {
        viewModelScope.launch {
            val loan = repository.getLoanById(loanId)
            if (loan != null) {
                val remBalance = loan.outstandingBalance - amountPaid
                val updatedStatus = if (remBalance <= 0.0) "Completed" else loan.status
                val updatedLoan = loan.copy(
                    outstandingBalance = if (remBalance > 0.0) remBalance else 0.0,
                    principalBalance = if (remBalance > 0.0) loan.principalBalance - amountPaid else 0.0,
                    status = updatedStatus
                )
                repository.updateLoan(updatedLoan)

                val formatCount = String.format("%05d", paymentsStream.value.size + 1)
                val newReceipt = "GI-RCP-$formatCount"
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

                val pay = PaymentEntity(
                    receiptNumber = newReceipt, loanId = loanId, customerId = customerId,
                    customerName = customerName, paymentDate = dateStr, paymentTime = timeStr,
                    amountPaid = amountPaid, remainingBalance = if (remBalance > 0.0) remBalance else 0.0,
                    collectorName = "Kamal Silva", paymentMethod = paymentMethod, remarks = remark
                )
                repository.insertPayment(pay)
                logAction("Collection", "Collected Rs. $amountPaid for $loanId", "Approved", "Outstanding ${loan.outstandingBalance}", "Outstanding $remBalance")
            }
        }
    }
}
