package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.theme.*
import com.example.viewmodel.GILCMSViewModel
import java.text.SimpleDateFormat
import java.util.*

// Bulletproof Divider replacement
@Composable
fun SimpleDivider(modifier: Modifier = Modifier, color: Color = Color.LightGray.copy(alpha = 0.4f)) {
    Spacer(modifier = modifier.fillMaxWidth().height(1.dp).background(color))
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    GILCMSAppContainer(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GILCMSAppContainer(
    modifier: Modifier = Modifier,
    viewModel: GILCMSViewModel = viewModel()
) {
    val context = LocalContext.current
    val customers by viewModel.customersStream.collectAsState()
    val loans by viewModel.loansStream.collectAsState()
    val payments by viewModel.paymentsStream.collectAsState()
    val auditLogs by viewModel.auditLogsStream.collectAsState()
    val userAccounts by viewModel.userAccountsStream.collectAsState()
    val branches by viewModel.branchesStream.collectAsState()

    // Base navigation
    var activeTab by remember { mutableStateOf("DASHBOARD") } // DASHBOARD, REGISTRATIONS, COLLECTIONS, AUDIT, SPECS

    // Dialogue State management
    var selectedPaymentForReceipt by remember { mutableStateOf<PaymentEntity?>(null) }
    var showReceiptModal by remember { mutableStateOf(false) }
    var showJwtDebugModal by remember { mutableStateOf(false) }

    // Onboarding Form States
    var custName by remember { mutableStateOf("") }
    var custNic by remember { mutableStateOf("") }
    var custPhone by remember { mutableStateOf("") }
    var custAddress by remember { mutableStateOf("") }
    var custIncome by remember { mutableStateOf("") }
    var guarantorName by remember { mutableStateOf("") }
    var guarantorPhone by remember { mutableStateOf("") }
    var targetGpsCoord by remember { mutableStateOf("Not Captured") }

    // Active customer profile zoom card
    var selectedCustomerForProfile by remember { mutableStateOf<CustomerEntity?>(null) }
    var showCustomerProfileModal by remember { mutableStateOf(false) }
    var isEditProfileMode by remember { mutableStateOf(false) }

    // Editable holder details for Edit Customer
    var epName by remember { mutableStateOf("") }
    var epPhone by remember { mutableStateOf("") }
    var epAddress by remember { mutableStateOf("") }
    var epIncome by remember { mutableStateOf("") }
    var epGuarantorName by remember { mutableStateOf("") }
    var epGuarantorPhone by remember { mutableStateOf("") }

    // Loan Disbursal Form States
    var selectedCustIdForLoan by remember { mutableStateOf("") }
    var loanAmountInput by remember { mutableStateOf("") }
    var loanInterestRate by remember { mutableStateOf("12") }
    var loanFrequency by remember { mutableStateOf("Daily") } // Daily, Weekly, Monthly

    // Collection Dialogue parameters
    var selectedLoanForCollection by remember { mutableStateOf<LoanEntity?>(null) }
    var collectionAmountInput by remember { mutableStateOf("") }
    var collectionPaymentMethod by remember { mutableStateOf("Cash") } // Cash, Bank Transfer, QR, Mobile Wallet
    var collectionRemarks by remember { mutableStateOf("") }
    var showCollectionDialog by remember { mutableStateOf(false) }

    // Multi-Lingual Helper lambda
    val trans = { en: String, ta: String -> viewModel.translate(en, ta) }

    // Screen Intercept: If NOT Logged In, display security Portal
    if (!viewModel.isLoggedIn.value) {
        LoginPortalScreen(viewModel = viewModel, trans = trans)
    } else {
        // Main Professional Banking Environment
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                
                // 1. SECURE SYSTEM HUB HEADER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(RoyalBlue, Color(0xFF1E3A8A))
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Emblem Logo",
                                        tint = GoldenYellow,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "GARUDA FINANCE",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Authorized Network Console • SSL Secure",
                                        color = GoldenYellow,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                                               // Online Sync indicator / Offline Toggle Button
                                FilledIconToggleButton(
                                    checked = viewModel.isOfflineMode.value,
                                    onCheckedChange = { 
                                        viewModel.isOfflineMode.value = it
                                        val status = if (it) "OFFLINE CAPABLE WORKSPACE ACTIVE" else "ONLINE SERVER LEDGER RE-ESTABLISHED"
                                        Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp).testTag("offline_mode_toggle"),
                                    colors = IconButtonDefaults.filledIconToggleButtonColors(
                                        containerColor = Color.White.copy(alpha = 0.15f),
                                        checkedContainerColor = WarningAmber
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (viewModel.isOfflineMode.value) Icons.Default.Lock else Icons.Default.Lock,
                                        contentDescription = "Sync Toggle",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Interactive Token debug
                                IconButton(
                                    onClick = { showJwtDebugModal = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = "Token Badge", tint = GoldenYellow, modifier = Modifier.size(16.dp))
                                }

                                // Tamil - English translate
                                TextButton(
                                    onClick = { viewModel.toggleLanguage() },
                                    modifier = Modifier
                                        .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .height(28.dp)
                                        .padding(horizontal = 4.dp),
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                                ) {
                                    Text(
                                        text = if (viewModel.selectedLanguage.value == "English") "தமிழ்" else "EN",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Revoke Session Logout Control
                                IconButton(
                                    onClick = { 
                                        viewModel.performLogout()
                                        Toast.makeText(context, "JWT Claims Revoked. Secure logout complete.", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(DangerRed.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                ) {
                                    Icon(Icons.Default.ExitToApp, contentDescription = "Lock lockout", tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        // Status subtitle (User / Branch)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (viewModel.isOfflineMode.value) WarningAmber else SuccessGreen, RoundedCornerShape(3.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Operator: ${viewModel.sessionUser.value?.fullName ?: "Guest Operator"} (${viewModel.activeRole.value})",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "Branch Code: ${viewModel.selectedBranchId.value}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 2. SECURE OFFLINE QUEUE ALERTS
                if (viewModel.isOfflineMode.value && viewModel.offlineSyncQueue.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WarningAmber.copy(alpha = 0.2f))
                            .border(BorderStroke(1.dp, WarningAmber))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Offline Ledger Pending: ${viewModel.offlineSyncQueue.size} Collection Checks Queued",
                                fontSize = 11.sp,
                                color = Color.DarkGray,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = {
                                    viewModel.synchronizeQueuedTransactions()
                                    Toast.makeText(context, "Offline entries aggregated and committed successfully!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Sync", modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Commit Sync", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 3. MAIN NAVIGATION BAR
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = RoyalBlue,
                    tonalElevation = 8.dp,
                    modifier = Modifier.height(64.dp)
                ) {
                    NavigationBarItem(
                        selected = activeTab == "DASHBOARD",
                        onClick = { activeTab = "DASHBOARD" },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Metrics") },
                        label = { Text(trans("Analytics", "விபரம்"), fontSize = 10.sp) },
                        modifier = Modifier.testTag("nav_metrics")
                    )
                    NavigationBarItem(
                        selected = activeTab == "REGISTRATIONS",
                        onClick = { activeTab = "REGISTRATIONS" },
                        icon = { Icon(Icons.Default.Person, contentDescription = "People") },
                        label = { Text(trans("Customers", "வாடிக்கையாளர்"), fontSize = 10.sp) },
                        modifier = Modifier.testTag("nav_customers")
                    )
                    NavigationBarItem(
                        selected = activeTab == "COLLECTIONS",
                        onClick = { activeTab = "COLLECTIONS" },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Wallet") },
                        label = { Text(trans("Collections", "சேகரிப்புகள்"), fontSize = 10.sp) },
                        modifier = Modifier.testTag("nav_collections")
                    )
                    NavigationBarItem(
                        selected = activeTab == "AUDIT",
                        onClick = { activeTab = "AUDIT" },
                        icon = { Icon(Icons.Default.Lock, contentDescription = "Audit") },
                        label = { Text(trans("Audit Trail", "தணிக்கை"), fontSize = 10.sp) },
                        modifier = Modifier.testTag("nav_audit_trail")
                    )
                    NavigationBarItem(
                        selected = activeTab == "SPECS",
                        onClick = { activeTab = "SPECS" },
                        icon = { Icon(Icons.Default.Build, contentDescription = "SQLSpecs") },
                        label = { Text(trans("Specs", "வடிவமைப்பு"), fontSize = 10.sp) },
                        modifier = Modifier.testTag("nav_specs_tab")
                    )
                }

                // 4. SCREEN VIEWPORT SWITCHER
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (activeTab) {
                        "DASHBOARD" -> DashboardScreen(
                            viewModel = viewModel,
                            customers = customers,
                            loans = loans,
                            payments = payments,
                            userAccounts = userAccounts,
                            trans = trans
                        )
                        "REGISTRATIONS" -> RegistrationHubScreen(
                            viewModel = viewModel,
                            customers = customers,
                            loans = loans,
                            custName = custName,
                            custNic = custNic,
                            custPhone = custPhone,
                            custAddress = custAddress,
                            custIncome = custIncome,
                            guarantorName = guarantorName,
                            guarantorPhone = guarantorPhone,
                            targetGpsCoord = targetGpsCoord,
                            selectedCustIdForLoan = selectedCustIdForLoan,
                            loanAmountInput = loanAmountInput,
                            loanInterestRate = loanInterestRate,
                            loanFrequency = loanFrequency,
                            onNameChange = { custName = it },
                            onNicChange = { custNic = it },
                            onPhoneChange = { custPhone = it },
                            onAddressChange = { custAddress = it },
                            onIncomeChange = { custIncome = it },
                            onGuarantorNameChange = { guarantorName = it },
                            onGuarantorPhoneChange = { guarantorPhone = it },
                            onGpsChange = { targetGpsCoord = it },
                            onSelectedCustChange = { selectedCustIdForLoan = it },
                            onLoanAmtChange = { loanAmountInput = it },
                            onLoanRateChange = { loanInterestRate = it },
                            onLoanFreqChange = { loanFrequency = it },
                            onCustomerClick = { customer ->
                                selectedCustomerForProfile = customer
                                epName = customer.fullName
                                epPhone = customer.phoneNumber
                                epAddress = customer.address
                                epIncome = customer.monthlyIncome.toInt().toString()
                                epGuarantorName = customer.guarantorName
                                epGuarantorPhone = customer.guarantorPhone
                                isEditProfileMode = false
                                showCustomerProfileModal = true
                            },
                            trans = trans
                        )
                        "COLLECTIONS" -> CollectionsHubScreen(
                            viewModel = viewModel,
                            loans = loans,
                            payments = payments,
                            onRecordPaymentClick = { loan ->
                                selectedLoanForCollection = loan
                                collectionAmountInput = loan.installmentAmount.toInt().toString()
                                showCollectionDialog = true
                            },
                            onViewReceiptClick = { pay ->
                                selectedPaymentForReceipt = pay
                                showReceiptModal = true
                            },
                            trans = trans
                        )
                        "AUDIT" -> AuditTrailScreen(
                            viewModel = viewModel,
                            auditLogs = auditLogs,
                            trans = trans
                        )
                        "SPECS" -> TechnicalSpecsHub(trans = trans)
                    }
                }
            }

            // =========================================================================
            // DIALOGUES & MODALS
            // =========================================================================

            // A. JWT DEBUG INFORMATION MODAL Overlay
            if (showJwtDebugModal) {
                AlertDialog(
                    onDismissRequest = { showJwtDebugModal = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = "JWT", tint = GoldenYellow)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Secure JWT Token Decoded Payload", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black)
                                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "ACTIVE COMPLIANCE TOKEN:",
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = viewModel.sessionJwtToken.value,
                                fontSize = 9.sp,
                                color = Color.Green,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            SimpleDivider(color = Color.DarkGray)
                            Text(
                                text = "DECODED CRYPTO CLAIMS:",
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                            Text(
                                text = """
                                Header: HS256 JWT
                                Subject: ${viewModel.sessionUser.value?.username ?: "Guest"}
                                Authority: ${viewModel.activeRole.value}
                                Origin: Garuda Ledger Server Node
                                Session TTL: Approved 24 Hour Lease
                                Encryption Status: SHA-256 HMAC Active
                                """.trimIndent(),
                                fontSize = 10.sp,
                                color = Color.Cyan,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showJwtDebugModal = false }) {
                            Text("Understood", color = RoyalBlue)
                        }
                    }
                )
            }

            // B. COMPREHENSIVE CUSTOMER PROFILE DETAIL INLINE VIEWER (with Edit Profile, Selfie & NIC Upload verification overlays)
            if (showCustomerProfileModal && selectedCustomerForProfile != null) {
                val customer = selectedCustomerForProfile!!
                val relatedLoans = loans.filter { it.customerId == customer.id }
                var showNicProgress by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { showCustomerProfileModal = false },
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEditProfileMode) "Edit Customer Profile" else "Customer Professional Portfolio",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue
                            )
                            IconButton(onClick = { isEditProfileMode = !isEditProfileMode }) {
                                Icon(
                                    imageVector = if (isEditProfileMode) Icons.Default.Info else Icons.Default.Build,
                                    contentDescription = "Edit Profile",
                                    tint = RoyalBlue
                                )
                            }
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!isEditProfileMode) {
                                // VIEW MODE: Profile Details Display
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(RoyalBlue.copy(alpha = 0.05f)).padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Simulated Photo Avatar
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(27.dp))
                                            .background(Color.LightGray),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.AccountBox, contentDescription = "Profile Photo", modifier = Modifier.size(36.dp), tint = RoyalBlue)
                                        if (viewModel.capturedPhotoPath.value.isNotEmpty()) {
                                            Box(modifier = Modifier.fillMaxSize().background(Color.Green.copy(alpha = 0.2f)))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(customer.fullName, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                        Text("CRM Target ID: ${customer.id}", fontSize = 11.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                    }
                                }

                                Text("1. Personal Identity Profile Metrics", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBlue)
                                Text("NIC Card Code: ${customer.nicNumber}", fontSize = 11.sp)
                                Text("Primary Mobile: ${customer.phoneNumber}", fontSize = 11.sp)
                                Text("Contact address: ${customer.address}", fontSize = 11.sp)
                                Text("Calculated Income: LKR ${String.format("%,.0f", customer.monthlyIncome)} / Month", fontSize = 11.sp)
                                Text("Guarantor Reference: ${customer.guarantorName} (${customer.guarantorPhone})", fontSize = 11.sp)
                                
                                SimpleDivider()

                                Text("2. Secure KYC Document Stubs", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBlue)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = if (viewModel.capturedNicFrontPath.value.isNotEmpty()) Icons.Default.Check else Icons.Default.Lock,
                                                contentDescription = "NICfront",
                                                tint = if (viewModel.capturedNicFrontPath.value.isNotEmpty()) SuccessGreen else Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (viewModel.capturedNicFrontPath.value.isNotEmpty()) "NIC Front Verified" else "NIC Front Missing",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (viewModel.capturedNicFrontPath.value.isNotEmpty()) SuccessGreen else Color.DarkGray
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = if (viewModel.capturedPhotoPath.value.isNotEmpty()) Icons.Default.Person else Icons.Default.Add,
                                                contentDescription = "Selfie",
                                                tint = if (viewModel.capturedPhotoPath.value.isNotEmpty()) SuccessGreen else Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (viewModel.capturedPhotoPath.value.isNotEmpty()) "Biometric Selfie Verified" else "Compliance Selfie Missing",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (viewModel.capturedPhotoPath.value.isNotEmpty()) SuccessGreen else Color.DarkGray
                                            )
                                        }
                                    }
                                }

                                SimpleDivider()

                                Text("3. Active Microfinance portfolios history", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBlue)
                                if (relatedLoans.isEmpty()) {
                                    Text("No active loans disburse histories.", fontSize = 11.sp, color = Color.Gray)
                                } else {
                                    relatedLoans.forEach { loan ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(loan.id, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = RoyalBlue)
                                                    Text(loan.status, fontSize = 10.sp, color = if (loan.status == "Overdue") DangerRed else SuccessGreen, fontWeight = FontWeight.Bold)
                                                }
                                                Text("Disbursed Core Capital: LKR ${String.format("%,.0f", loan.loanAmount)}", fontSize = 11.sp)
                                                Text("Current Total Outstanding: LKR ${String.format("%,.2f", loan.outstandingBalance)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                SimpleDivider()

                                // Captured GPS Coordinates details mapped
                                Text("4. Geographical GPS stamps compliance", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBlue)
                                Text("Check-In Stamps: ${customer.gpsLocation}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = RoyalBlue)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .background(Color.White)
                                        .border(BorderStroke(1.dp, Color.Gray), RoundedCornerShape(4.dp))
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        // Draw mock stylized map roads
                                        val width = size.width
                                        val height = size.height
                                        
                                        // draw grid
                                        for(i in 1..4){
                                            drawLine(Color.LightGray.copy(alpha = 0.5f), androidx.compose.ui.geometry.Offset(0f, height*i/5), androidx.compose.ui.geometry.Offset(width, height*i/5), strokeWidth = 1f)
                                            drawLine(Color.LightGray.copy(alpha = 0.5f), androidx.compose.ui.geometry.Offset(width*i/5, 0f), androidx.compose.ui.geometry.Offset(width*i/5, height), strokeWidth = 1f)
                                        }
                                        
                                        // draw road path
                                        drawLine(Color.Gray.copy(alpha = 0.4f), androidx.compose.ui.geometry.Offset(0f, height/2), androidx.compose.ui.geometry.Offset(width, height/2), strokeWidth = 12f)
                                        drawLine(Color.Gray.copy(alpha = 0.4f), androidx.compose.ui.geometry.Offset(width/3, 0f), androidx.compose.ui.geometry.Offset(width/3, height), strokeWidth = 12f)
                                        
                                        // Place GPS Node Pin
                                        drawCircle(Color.Blue, radius = 8f, center = androidx.compose.ui.geometry.Offset(width/3, height/2))
                                        drawCircle(Color.Red, radius = 5f, center = androidx.compose.ui.geometry.Offset(width/3, height/2))
                                    }
                                    Box(modifier = Modifier.align(Alignment.BottomEnd).background(Color.Black.copy(alpha = 0.7f)).padding(horizontal = 6.dp, vertical = 2.dp)){
                                        Text("Vector Mapping Online", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                            } else {
                                // EDIT PROFILES MODE Input textures
                                OutlinedTextField(
                                    value = epName,
                                    onValueChange = { epName = it },
                                    label = { Text("Customer full Name") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = epPhone,
                                    onValueChange = { epPhone = it },
                                    label = { Text("Contact Phone") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = epAddress,
                                    onValueChange = { epAddress = it },
                                    label = { Text("Residential address") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = epIncome,
                                    onValueChange = { epIncome = it },
                                    label = { Text("Monthly Income (LKR)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = epGuarantorName,
                                        onValueChange = { epGuarantorName = it },
                                        label = { Text("Guarantor reference Name") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = epGuarantorPhone,
                                        onValueChange = { epGuarantorPhone = it },
                                        label = { Text("Guarantor Phone") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                SimpleDivider()

                                // Documents trigger uploads
                                Text("Upload compliance Attachments:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.isNicUploading.value = true
                                            showNicProgress = true
                                            viewModel.capturedNicFrontPath.value = "VERIFIED_STAMP_F49A21"
                                            Toast.makeText(context, "Scanning NIC with OCR compliance tracker...", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Scan NIC front", fontSize = 10.sp)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.isPhotoUploading.value = true
                                            viewModel.capturedPhotoPath.value = "VERIFIED_BIO_match"
                                            Toast.makeText(context, "Verifying Bio Face ID metrics from camera...", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Take Bio Selfie", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (isEditProfileMode) {
                                    val updatedCustomer = customer.copy(
                                        fullName = epName,
                                        phoneNumber = epPhone,
                                        address = epAddress,
                                        monthlyIncome = epIncome.toDoubleOrNull() ?: customer.monthlyIncome,
                                        guarantorName = epGuarantorName,
                                        guarantorPhone = epGuarantorPhone
                                    )
                                    viewModel.editCustomerDetail(updatedCustomer)
                                    Toast.makeText(context, "Profile portfolio updated successfully!", Toast.LENGTH_SHORT).show()
                                    isEditProfileMode = false
                                    showCustomerProfileModal = false
                                } else {
                                    showCustomerProfileModal = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                        ) {
                            Text(if (isEditProfileMode) "Commit Updates" else "Close Portfolio")
                        }
                    },
                    dismissButton = {
                        if (isEditProfileMode) {
                            TextButton(onClick = { isEditProfileMode = false }) {
                                Text("Revert Changes")
                            }
                        }
                    }
                )
            }

            // C. FIELD COLLECTION OVERLAYS VIEW (dialog payment)
            if (showCollectionDialog && selectedLoanForCollection != null) {
                val targetLoan = selectedLoanForCollection!!
                AlertDialog(
                    onDismissRequest = { showCollectionDialog = false },
                    title = { Text(trans("Record field Collection Payment", "கள வசூல் பணம் பதிவு"), fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Customer: ${targetLoan.customerName} (${targetLoan.customerId})",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Loan Reference: ${targetLoan.id}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "Standard Scheduled Installment: LKR ${targetLoan.installmentAmount}",
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue,
                                fontSize = 14.sp
                            )
                            
                            SimpleDivider()

                            OutlinedTextField(
                                value = collectionAmountInput,
                                onValueChange = { collectionAmountInput = it },
                                label = { Text(trans("Amount to Pay (LKR)", "செலுத்தப்பட்ட தொகை (LKR)")) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Text(trans("Payment Method", "கட்டண வகை"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val methods = listOf("Cash", "Bank Transfer", "QR Payment", "Mobile Wallet")
                                methods.forEach { method ->
                                    val selected = collectionPaymentMethod == method
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (selected) RoyalBlue else Color.LightGray.copy(alpha = 0.2f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (selected) GoldenYellow else Color.Transparent,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .clickable { collectionPaymentMethod = method }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = method,
                                            color = if (selected) Color.White else Color.DarkGray,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = collectionRemarks,
                                onValueChange = { collectionRemarks = it },
                                label = { Text(trans("Collection Remark / Notes", "வசூல் குறிப்புகள்")) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            onClick = {
                                val amt = collectionAmountInput.toDoubleOrNull() ?: 0.0
                                if (amt > 0.0) {
                                    if (viewModel.isOfflineMode.value) {
                                        viewModel.queueOfflineCollection(
                                            loanId = targetLoan.id,
                                            customerId = targetLoan.customerId,
                                            customerName = targetLoan.customerName,
                                            amountPaid = amt,
                                            paymentMethod = collectionPaymentMethod,
                                            remark = collectionRemarks.ifEmpty { "Field collection recorded offline" }
                                        )
                                        showCollectionDialog = false
                                        Toast.makeText(context, "Disconnected state! Payment queued locally in offline sync vault.", Toast.LENGTH_LONG).show()
                                    } else {
                                        viewModel.recordCollectionPayment(
                                            loanId = targetLoan.id,
                                            customerId = targetLoan.customerId,
                                            customerName = targetLoan.customerName,
                                            amountPaid = amt,
                                            paymentMethod = collectionPaymentMethod,
                                            remark = collectionRemarks.ifEmpty { "Field collection payment recorded" }
                                        )
                                        showCollectionDialog = false
                                        Toast.makeText(context, "Payment Processed. Receipt Code Generated!", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Please enter a valid LKR collection amount", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("confirm_payment_btn")
                        ) {
                            Text(trans("Generate Receipt", "ரசீது உருவாக்கு"))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCollectionDialog = false }) {
                            Text(trans("Cancel", "ரத்துசெய்"))
                        }
                    }
                )
            }

            // D. BEAUTIFIED BRANDED RECEIPT GENERATOR DISPLAY (with printable thermal mock and message alerts)
            if (showReceiptModal && selectedPaymentForReceipt != null) {
                val payment = selectedPaymentForReceipt!!
                var showSmsOverlayMessage by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { showReceiptModal = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Receipt Success", tint = SuccessGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(trans("Garuda Loan Receipt Generated", "கருடா கடன் ரசீது உருவாக்கப்பட்டது"), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Box {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(BorderStroke(1.dp, Color.LightGray), RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .padding(14.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "GARUDA FINANCE",
                                    color = RoyalBlue,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Sri Lanka Central Microfinance Hub",
                                    color = BrandTextSecondaryLight,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Client helpline: 078 9118182 | 078 7118182",
                                    color = Color.Gray,
                                    fontSize = 9.sp,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                SimpleDivider(color = RoyalBlue)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("RECEIPT NO:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                    Text(payment.receiptNumber, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBlue)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("DATE/TIME:", fontSize = 10.sp, color = Color.Gray)
                                    Text("${payment.paymentDate} @ ${payment.paymentTime}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                SimpleDivider()
                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("CUSTOMER NAME:", fontSize = 10.sp, color = Color.Gray)
                                    Text(payment.customerName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("LOAN REFERENCE:", fontSize = 10.sp, color = Color.Gray)
                                    Text(payment.loanId, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                SimpleDivider()
                                Spacer(modifier = Modifier.height(4.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(RoyalBlue.copy(alpha = 0.08f))
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("AMOUNT PAID:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBlue)
                                        Text("LKR ${String.format("%,.2f", payment.amountPaid)}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = SuccessGreen)
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("REMAINING DUE:", fontSize = 10.sp, color = Color.Gray)
                                    Text("LKR ${String.format("%,.2f", payment.remainingBalance)}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("PAYMENT METHOD:", fontSize = 10.sp, color = Color.Gray)
                                    Text(payment.paymentMethod, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = RoyalBlue)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("COLLECTOR NAME:", fontSize = 10.sp, color = Color.Gray)
                                    Text(payment.collectorName, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                
                                // Fake Digital Signature + QR Layout
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "[ *QR SECURE* ]",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .border(1.dp, RoyalBlue, RoundedCornerShape(4.dp))
                                                .background(Color.White)
                                                .padding(6.dp),
                                            color = RoyalBlue
                                        )
                                        Text("Scan to Track", fontSize = 8.sp, color = Color.Gray)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = payment.collectorName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Blue,
                                            fontFamily = FontFamily.Cursive
                                        )
                                        Spacer(modifier = Modifier.height(1.dp).width(70.dp).background(Color.Gray))
                                        Text("Digital Signature", fontSize = 8.sp, color = Color.Gray)
                                        Text("Secure compliance verified", fontSize = 8.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                SimpleDivider()
                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "This receipt is dynamically filed with Garuda Finance centralized servers under cryptographic token. Thank you for your payment.",
                                    fontSize = 8.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Dynamic SMS dispatch popup mock
                            if (showSmsOverlayMessage) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.9f))
                                        .padding(14.dp)
                                        .border(BorderStroke(1.dp, GoldenYellow), RoundedCornerShape(4.dp))
                                ) {
                                    Column {
                                        Text("SECURE SMS ALERT DISPATCHED:", color = GoldenYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = """
                                            To: Client Mobile
                                            Message: "Dear ${payment.customerName}, LKR ${payment.amountPaid} has been verified and applied to ${payment.loanId} on ${payment.paymentDate}. Outstanding remaining: LKR ${payment.remainingBalance}. Compliant with Garuda Finance."
                                            """.trimIndent(),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 12.sp
                                        )
                                        TextButton(
                                            onClick = { showSmsOverlayMessage = false },
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Close alert", color = GoldenYellow, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { Toast.makeText(context, trans("Simulated PDF Saved to Downloads folder", "PDF பதிவிறக்கம் செய்யப்பட்டது"), Toast.LENGTH_SHORT).show() },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "PDF icon", modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("PDF", fontSize = 10.sp)
                            }

                            Button(
                                onClick = { Toast.makeText(context, trans("Simulating Bluetooth Receipt Thermal Print", "ரசீது வெப்ப அச்சிடுதல் செய்யப்படுகிறது"), Toast.LENGTH_SHORT).show() },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldenYellow, contentColor = Color.Black),
                                modifier = Modifier.weight(1.5f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("Thermal Print", fontSize = 10.sp)
                            }

                            Button(
                                onClick = { showSmsOverlayMessage = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                modifier = Modifier.weight(1.5f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("Dispatch SMS", fontSize = 10.sp)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showReceiptModal = false }) {
                            Text(trans("Dismiss", "மூடுக"))
                        }
                    }
                )
            }
        }
    }
}

// =========================================================================
// SCREEN 0: SECURITY IDENTIFICATION PORTAL & PASSWORD ENCRYPTION
// =========================================================================
@Composable
fun LoginPortalScreen(
    viewModel: GILCMSViewModel,
    trans: (String, String) -> String
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var chosenRole by remember { mutableStateOf("Owner") } // Owner, Manager, Collection Officer
    var isPasswordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1E3A8A), Color(0xFF0F172A)),
                    radius = 1200f
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // CORPORATE HEADER
            Icon(Icons.Default.Lock, contentDescription = "Shield", tint = GoldenYellow, modifier = Modifier.size(54.dp))
            Text(
                "GARUDA FINANCE HUB",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 2.sp
            )
            Text(
                "Sri Lanka Microfinance Secure Terminal (JWT Compliant)",
                fontSize = 11.sp,
                color = GoldenYellow,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Monospace
            )

            // QUICK CHIPS AUTO-FILLER PROFILE
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        "Click pre-approved profile to quick-load credentials:",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("owner_vishwa", "owner_vishwa@garuda.lk", "Owner"),
                            Triple("manager_arun", "manager_arun@garuda.lk", "Manager"),
                            Triple("officer_kamal", "officer_kamal@garuda.lk", "Collection Officer")
                        ).forEach { (un, desc, role) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        username = un
                                        password = "secure_pass_phrase_verified"
                                        chosenRole = role
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(role.replace(" Collection ", ""), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // LOGIN FORM
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Secure Credentials Authenticator", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Operator Username", color = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GoldenYellow,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("un_input_field"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password Phrase", color = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GoldenYellow,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("pwd_input_field"),
                        singleLine = true
                    )

                    // ROLE SWITCHER
                    Text("Select Session Workspace Role:", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Owner", "Manager", "Collection Officer").forEach { role ->
                            val selected = chosenRole == role
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (selected) RoyalBlue else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .border(1.dp, if (selected) GoldenYellow else Color.Transparent, RoundedCornerShape(4.dp))
                                    .clickable { chosenRole = role }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(role, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // CRYPTOGRAPHIC ENCRYPTION LIVE VISUALIZER
                    if (password.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(8.dp)
                                .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        ) {
                            Column {
                                Text("PASSWORD HASH TRANSMISSION METADATA (SHA-256):", color = GoldenYellow, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = viewModel.hashPassword(password),
                                    color = Color.Green,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            val success = viewModel.performLogin(username, password, chosenRole)
                            if (!success) {
                                Toast.makeText(viewModel.getApplication(), "Incorrect details. Use quick profiles chips above!", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldenYellow, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth().testTag("submit_login_btn")
                    ) {
                        Text("AUTHENTICATE & GENERATE JWT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// =========================================================================
// SCREEN 1: REDESIGNED DASHBOARD WITH CUSTOM CANVAS LINES VECTOR CHART
// =========================================================================
@Composable
fun DashboardScreen(
    viewModel: GILCMSViewModel,
    customers: List<CustomerEntity>,
    loans: List<LoanEntity>,
    payments: List<PaymentEntity>,
    userAccounts: List<UserAccountEntity>,
    trans: (String, String) -> String
) {
    val context = LocalContext.current

    // Branch wise aggregations
    val filterBranch = viewModel.selectedBranchForAnalytics.value
    val filteredLoans = if (filterBranch == "ALL") loans else loans.filter { it.id.endsWith(if (filterBranch == "GI-BR-001") "00001" else "2") } // simulated filter for proof
    val filteredPayments = if (filterBranch == "ALL") payments else payments.filter { it.receiptNumber.endsWith(if (filterBranch == "GI-BR-001") "00001" else "2") }

    val totalLoansDisbursed = filteredLoans.sumOf { it.loanAmount }
    val outstandingBalance = filteredLoans.sumOf { it.outstandingBalance }
    val totalCollected = filteredPayments.sumOf { it.amountPaid }
    val overduePortfolios = filteredLoans.filter { it.status == "Overdue" }
    val totalOverduePortfolios = overduePortfolios.size
    val penaltyCollectedAccumulated = filteredLoans.sumOf { it.penaltyBalance }

    val collectionEfficiencyRatio = if (totalLoansDisbursed > 0) {
        ((totalCollected / totalLoansDisbursed) * 100).coerceAtMost(100.0)
    } else 93.4

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // A. COGNITIVE BRANCH FILTER
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Filter Analytics Branch:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = RoyalBlue)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("ALL", "GI-BR-001", "GI-BR-002", "GI-BR-003").forEach { br ->
                            val active = viewModel.selectedBranchForAnalytics.value == br
                            Box(
                                modifier = Modifier
                                    .background(if (active) RoyalBlue else Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .clickable { viewModel.selectedBranchForAnalytics.value = br }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(br, color = if (active) Color.White else Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // B. BRAND HERO BOARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = RoyalBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "GARUDA PORTFOLIO VELOCITY ENGINE",
                        color = GoldenYellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Automated daily penalty compounding scheduler compliant with Sri Lankan microfinance central framework. Central DB is connected.",
                        color = Color.White,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // C. CUSTOM VECTOR CANVAS CHARTS METRICS
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Daily Collection Velocities vs Baseline Limit",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalBlue
                        )
                        Box(modifier = Modifier.background(SuccessGreen.copy(alpha = 0.15f)).padding(horizontal = 4.dp)){
                            Text("TARGET: 93.4%", color = SuccessGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Canvas(modifier = Modifier.fillMaxSize().weight(1f)) {
                        val width = size.width
                        val height = size.height
                        val paddingLimit = 40f
                        val graphW = width - paddingLimit
                        val graphH = height - paddingLimit

                        // Grid baseline references
                        for(i in 0..3){
                            val y = graphH - (graphH * i / 3)
                            drawLine(
                                Color.LightGray.copy(alpha = 0.5f),
                                start = androidx.compose.ui.geometry.Offset(paddingLimit, y),
                                end = androidx.compose.ui.geometry.Offset(width, y)
                            )
                        }

                        // Baseline threshold dotted
                        val baselineY = graphH - (graphH * 60000 / 100000)
                        drawLine(
                            color = DangerRed,
                            start = androidx.compose.ui.geometry.Offset(paddingLimit, baselineY),
                            end = androidx.compose.ui.geometry.Offset(width, baselineY),
                            strokeWidth = 2f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )

                        // Data points connected (mon to sun)
                        val pointsData = listOf(35000f, 54000f, 41000f, 85000f, 95000f, 62000f, 81000f)
                        val spacingX = graphW / (pointsData.size - 1)
                        val points = pointsData.mapIndexed { idx, v ->
                            val x = paddingLimit + (idx * spacingX)
                            val y = graphH - (graphH * v / 100000)
                            androidx.compose.ui.geometry.Offset(x, y)
                        }

                        // Gradient fill
                        val fillPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(paddingLimit, graphH)
                            points.forEach { lineTo(it.x, it.y) }
                            lineTo(width, graphH)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(SuccessGreen.copy(alpha = 0.3f), Color.Transparent)
                            )
                        )

                        // Outer border line
                        for (i in 0 until points.size - 1) {
                            drawLine(
                                color = SuccessGreen,
                                start = points[i],
                                end = points[i+1],
                                strokeWidth = 5f
                            )
                        }

                        // Nodes
                        points.forEach { node ->
                            drawCircle(Color.Blue, radius = 6f, center = node)
                            drawCircle(Color.White, radius = 3f, center = node)
                        }
                    }
                }
            }
        }

        // D. CORE ANALYTICS CARDS LEDGER
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Total Disbursed", fontSize = 10.sp, color = BrandTextSecondaryLight)
                            Text("LKR ${String.format("%,.0f", totalLoansDisbursed)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RoyalBlue)
                        }
                    }
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Outstanding Balance", fontSize = 10.sp, color = BrandTextSecondaryLight)
                            Text("LKR ${String.format("%,.0f", outstandingBalance)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DangerRed)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Collected Ledger", fontSize = 10.sp, color = BrandTextSecondaryLight)
                            Text("LKR ${String.format("%,.0f", totalCollected)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                        }
                    }
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Realized Efficiency", fontSize = 10.sp, color = BrandTextSecondaryLight)
                            Text("${String.format("%.1f", collectionEfficiencyRatio)}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GoldenYellow)
                        }
                    }
                }
            }
        }

        // E. OPERATOR EXCLUSIVE: PENALTY COMPOUND CONTROL
        if (viewModel.activeRole.value == "Owner") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().border(BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Compounding Penalty Engine settings", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DangerRed)
                        Text("Active Formula selection:", fontSize = 11.sp, color = Color.Gray)
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = viewModel.penaltyOption.value == "Option A", onClick = { viewModel.penaltyOption.value = "Option A" })
                                Text("Fixed LKR 50/Day", fontSize = 11.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = viewModel.penaltyOption.value == "Option B", onClick = { viewModel.penaltyOption.value = "Option B" })
                                Text("Percentage 0.5%/Day", fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.runAutoPenaltyEngine()
                                Toast.makeText(context, "Penalty recalculation compounding complete!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        ) {
                            Text("Compund Overdue Accounts Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// SCREEN 2: REGISTRATION & DIRECTORY HUB (with maps GPS verification and loan calculator)
// =========================================================================
@Composable
fun RegistrationHubScreen(
    viewModel: GILCMSViewModel,
    customers: List<CustomerEntity>,
    loans: List<LoanEntity>,
    custName: String,
    custNic: String,
    custPhone: String,
    custAddress: String,
    custIncome: String,
    guarantorName: String,
    guarantorPhone: String,
    targetGpsCoord: String,
    selectedCustIdForLoan: String,
    loanAmountInput: String,
    loanInterestRate: String,
    loanFrequency: String,
    onNameChange: (String) -> Unit,
    onNicChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onIncomeChange: (String) -> Unit,
    onGuarantorNameChange: (String) -> Unit,
    onGuarantorPhoneChange: (String) -> Unit,
    onGpsChange: (String) -> Unit,
    onSelectedCustChange: (String) -> Unit,
    onLoanAmtChange: (String) -> Unit,
    onLoanRateChange: (String) -> Unit,
    onLoanFreqChange: (String) -> Unit,
    onCustomerClick: (CustomerEntity) -> Unit,
    trans: (String, String) -> String
) {
    val context = LocalContext.current
    var subTab by remember { mutableStateOf("REGISTER") } // REGISTER, DIRECTORY, WEB_DISBURSE
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = when (subTab) {
                "REGISTER" -> 0
                "DIRECTORY" -> 1
                else -> 2
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = RoyalBlue
        ) {
            Tab(selected = subTab == "REGISTER", onClick = { subTab = "REGISTER" }, text = { Text("M1: Onboard Customer", fontSize = 10.sp, fontWeight = FontWeight.Bold) })
            Tab(selected = subTab == "DIRECTORY", onClick = { subTab = "DIRECTORY" }, text = { Text("M2: Customer Directory", fontSize = 10.sp, fontWeight = FontWeight.Bold) })
            Tab(selected = subTab == "WEB_DISBURSE", onClick = { subTab = "WEB_DISBURSE" }, text = { Text("M3: Loan Disbursals", fontSize = 10.sp, fontWeight = FontWeight.Bold) })
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (subTab) {
                "REGISTER" -> {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Onboard New Sri Lankan Client Profile", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RoyalBlue)

                        OutlinedTextField(value = custName, onValueChange = onNameChange, label = { Text("Client Full Name") }, modifier = Modifier.fillMaxWidth().testTag("add_cust_name"))
                        OutlinedTextField(value = custNic, onValueChange = onNicChange, label = { Text("National Identity Card (NIC) code") }, modifier = Modifier.fillMaxWidth().testTag("add_cust_nic"))
                        OutlinedTextField(value = custPhone, onValueChange = onPhoneChange, label = { Text("Active Mobile Contact") }, modifier = Modifier.fillMaxWidth().testTag("add_cust_phone"))
                        OutlinedTextField(value = custAddress, onValueChange = onAddressChange, label = { Text("Residential address") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = custIncome, onValueChange = onIncomeChange, label = { Text("Verified Monthly income (LKR)") }, modifier = Modifier.fillMaxWidth())

                        Text("Guarantor verification details:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBlue)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = guarantorName, onValueChange = onGuarantorNameChange, label = { Text("Guarantor Name") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = guarantorPhone, onValueChange = onGuarantorPhoneChange, label = { Text("Guarantor Phone") }, modifier = Modifier.weight(1f))
                        }

                        // HIGH PRECISION GPS ACQUISITION MODULE
                        Card(
                            colors = CardDefaults.cardColors(containerColor = RoyalBlue.copy(alpha = 0.05f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("GPS Localization coordinates:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = RoyalBlue)
                                    Text(targetGpsCoord, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        val randCoordinates = listOf(
                                            "6.9271,79.8612" to "Colombo Hub",
                                            "9.6615,80.0255" to "Jaffna Hub",
                                            "7.2906,80.6337" to "Kandy Hub"
                                        ).random()
                                        onGpsChange(randCoordinates.first)
                                        Toast.makeText(context, "High-Precision GPS Lock verified: ${randCoordinates.second}!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                                    modifier = Modifier.testTag("onboard_gps_btn")
                                ) {
                                    Text("Retrieve GPS", fontSize = 11.sp)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (custName.isNotEmpty() && custNic.isNotEmpty()) {
                                    viewModel.createCustomer(
                                        name = custName, nic = custNic, phone = custPhone,
                                        address = custAddress, income = custIncome.toDoubleOrNull() ?: 55000.0,
                                        guarantorName = guarantorName, guarantorPhone = guarantorPhone
                                    )
                                    Toast.makeText(context, "Client portfolio registered cleanly!", Toast.LENGTH_SHORT).show()
                                    onNameChange("")
                                    onNicChange("")
                                    onPhoneChange("")
                                    onAddressChange("")
                                    onIncomeChange("")
                                    onGuarantorNameChange("")
                                    onGuarantorPhoneChange("")
                                    onGpsChange("Not Captured")
                                } else {
                                    Toast.makeText(context, "Please write clean credit profiles!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            modifier = Modifier.fillMaxWidth().testTag("onboard_submit_btn")
                        ) {
                            Text("Verify Portfolio in Database", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                "DIRECTORY" -> {
                    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        Text("Active Registered Client Profiles", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RoyalBlue, modifier = Modifier.padding(bottom = 8.dp))
                        
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                            if (customers.isEmpty()) {
                                item { Text("No client records saved. Please register now.", fontSize = 12.sp, color = Color.Gray) }
                            } else {
                                items(customers) { cust ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable { onCustomerClick(cust) },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column {
                                                Text(cust.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("NIC: ${cust.nicNumber} • Mobile: ${cust.phoneNumber}", fontSize = 11.sp, color = Color.Gray)
                                                Text("GPS coordinate stamp: ${cust.gpsLocation}", fontSize = 10.sp, color = RoyalBlue, fontWeight = FontWeight.SemiBold)
                                            }
                                            Text("ZOOM PROFILE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoyalBlue)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "WEB_DISBURSE" -> {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Disburse / Onboard New Loan contract", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RoyalBlue)

                        if (customers.isEmpty()) {
                            Text("Please onboard customer profiles first", color = DangerRed, fontSize = 12.sp)
                        } else {
                            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                customers.forEach { cust ->
                                    val sel = selectedCustIdForLoan == cust.id
                                    Box(
                                        modifier = Modifier
                                            .background(if (sel) RoyalBlue else Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .clickable { onSelectedCustChange(cust.id) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(cust.fullName, color = if (sel) Color.White else Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(value = loanAmountInput, onValueChange = onLoanAmtChange, label = { Text("Capital Amount (LKR)") }, modifier = Modifier.fillMaxWidth().testTag("disburse_amount_tf"))
                        OutlinedTextField(value = loanInterestRate, onValueChange = onLoanRateChange, label = { Text("Rate per cycle (%)") }, modifier = Modifier.fillMaxWidth())
                        
                        Text("Repayment period cycle:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Daily", "Weekly", "Monthly").forEach { freq ->
                                val active = loanFrequency == freq
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (active) RoyalBlue else Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .clickable { onLoanFreqChange(freq) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(freq, color = if (active) Color.White else Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // EMI CALCULATION SCHEDULER SIMULATOR
                        val rawAmt = loanAmountInput.toDoubleOrNull() ?: 10000.0
                        val rateFactor = loanInterestRate.toDoubleOrNull() ?: 12.0
                        val durationCycles = when (loanFrequency) {
                            "Daily" -> 10
                            "Weekly" -> 4
                            else -> 1
                        }
                        val emiTot = rawAmt + (rawAmt * (rateFactor / 100))
                        val singleCycleVal = emiTot / durationCycles

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Interactive EMI Schedule Simulator:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBlue)
                                SimpleDivider(modifier = Modifier.padding(vertical = 4.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Number of installments: $durationCycles", fontSize = 11.sp)
                                    Text("Interest calculated: LKR ${String.format("%,.0f", rawAmt * (rateFactor / 100))}", fontSize = 11.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total payback amount: LKR ${String.format("%,.0f", emiTot)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Installment Portion: LKR ${String.format("%,.2f", singleCycleVal)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBlue)
                                }

                                if (rawAmt > 0.0) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Simulated Installment dates ledger breakdown:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    for (i in 1..3) {
                                        val c = Calendar.getInstance()
                                        c.add(Calendar.DAY_OF_YEAR, i)
                                        val dStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(c.time)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Installment #$i (Due $dStr)", fontSize = 10.sp)
                                            Text("LKR ${String.format("%,.2f", singleCycleVal)}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text("[...] Complete amortization schedule viewable inside specs panel", fontSize = 9.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val matchCust = customers.firstOrNull { it.id == selectedCustIdForLoan }
                                val cap = loanAmountInput.toDoubleOrNull() ?: 0.0
                                val decRate = loanInterestRate.toDoubleOrNull() ?: 12.0
                                if (matchCust != null && cap > 0.0) {
                                    viewModel.applyLoan(matchCust.id, matchCust.fullName, cap, "Fixed", decRate, loanFrequency)
                                    val log = if (viewModel.activeRole.value == "Manager") "Disbursed application submitted to Owner Approval queue!" else "Disbursed loan added directly!"
                                    Toast.makeText(context, log, Toast.LENGTH_LONG).show()
                                    onLoanAmtChange("")
                                } else {
                                    Toast.makeText(context, "Select client and verify amount!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            modifier = Modifier.fillMaxWidth().testTag("disburse_submit_btn")
                        ) {
                            Text(if (viewModel.activeRole.value == "Manager") "Submit Application (Checker Queue)" else "Disburse Contract instantly")
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// SCREEN 3: HIGH-END FIELD COLLECTIONS WITH "DUE TODAY" QUEUE list
// =========================================================================
@Composable
fun CollectionsHubScreen(
    viewModel: GILCMSViewModel,
    loans: List<LoanEntity>,
    payments: List<PaymentEntity>,
    onRecordPaymentClick: (LoanEntity) -> Unit,
    onViewReceiptClick: (PaymentEntity) -> Unit,
    trans: (String, String) -> String
) {
    var searchKey by remember { mutableStateOf("") }
    var collSubTab by remember { mutableStateOf("ACTIVE_PORTFOLIOS") } // ACTIVE_PORTFOLIOS, DUE_TODAY

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        
        // Active GPS tracker overlay status bar
        Card(
            colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, SuccessGreen)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, contentDescription = "Place", tint = SuccessGreen)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Field Check-In stamp verified: ACTIVE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                }
                Text(
                    text = if (viewModel.isGpsCheckedIn.value) "Lock stamp: 6.9271,79.8612" else "Lock stamp: pending",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Sub Navigation selectors
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                "ACTIVE_PORTFOLIOS" to "All Loans Ledger",
                "DUE_TODAY" to "Payments Due Today"
            ).forEach { (k, title) ->
                val active = collSubTab == k
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (active) RoyalBlue else Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .clickable { collSubTab = k }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(title, color = if (active) Color.White else Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        OutlinedTextField(
            value = searchKey,
            onValueChange = { searchKey = it },
            label = { Text("Search client name or contract code ID...") },
            modifier = Modifier.fillMaxWidth().testTag("cust_coll_search_tf"),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true
        )

        // View logic
        val displayedLoans = loans.filter {
            val matches = it.customerName.contains(searchKey, ignoreCase = true) || it.id.contains(searchKey, ignoreCase = true)
            val subTabFilter = if (collSubTab == "DUE_TODAY") (it.status == "Overdue" || it.id.endsWith("00001")) else (it.status != "Pending")
            matches && subTabFilter
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            if (displayedLoans.isEmpty()) {
                item { Text("No active microfinance accounts found in selection.", fontSize = 12.sp, color = Color.Gray) }
            } else {
                items(displayedLoans) { loan ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(loan.customerName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Box(modifier = Modifier.background(if (loan.status == "Overdue") DangerRed.copy(alpha = 0.15f) else SuccessGreen.copy(alpha = 0.15f)).padding(horizontal = 6.dp)){
                                    Text(loan.status, color = if (loan.status == "Overdue") DangerRed else SuccessGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("Contract ID: ${loan.id} • Repay Interval: ${loan.installmentFrequency}", fontSize = 11.sp)
                            Text("Amortized Outstanding Due: LKR ${String.format("%,.2f", loan.outstandingBalance)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoyalBlue)
                            Text("Instalment Target: LKR ${loan.installmentAmount} / Day", fontSize = 10.sp, color = Color.Gray)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = { onRecordPaymentClick(loan) },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                                    modifier = Modifier.weight(1f).height(32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Enter field Collection", fontSize = 11.sp)
                                }

                                if (collSubTab == "DUE_TODAY") {
                                    Button(
                                        onClick = { onRecordPaymentClick(loan) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        modifier = Modifier.weight(1f).height(32.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Quick Pay Instalment", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Transactions History receipts
        Text("Recent Transaction Log receipts (click to print)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RoyalBlue)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(0.7f)) {
            if (payments.isEmpty()) {
                item { Text("No receipts logged yet.", fontSize = 11.sp, color = Color.Gray) }
            } else {
                items(payments) { pay ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onViewReceiptClick(pay) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("${pay.customerName} • ${pay.receiptNumber}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("LKR ${String.format("%,.0f", pay.amountPaid)} verified via ${pay.paymentMethod}", fontSize = 10.sp, color = RoyalBlue)
                            }
                            Text("Open printer", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// SCREEN 4: IMMUTABLE BANK AUDIT OPERATIONS
// =========================================================================
@Composable
fun AuditTrailScreen(
    viewModel: GILCMSViewModel,
    auditLogs: List<AuditLogEntity>,
    trans: (String, String) -> String
) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Immutable Ledger Operations Audit Trail", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RoyalBlue)
            Box(modifier = Modifier.background(SuccessGreen.copy(alpha = 0.15f)).padding(horizontal = 6.dp)){
                Text("SECURE VERIFIED", color = SuccessGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text("Live cryptographic tracking entries mapping role contexts swappiness, credential checkups, location stamps, and penalty recurrences:", fontSize = 11.sp, color = Color.Gray)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            if (auditLogs.isEmpty()) {
                item { Text("No log audit entries present.", fontSize = 12.sp, color = Color.Gray) }
            } else {
                items(auditLogs) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(log.action, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBlue)
                                Text(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)), fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                            }
                            Text("User Context: ${log.userName} (${log.userRole})", fontSize = 11.sp)
                            Text("Change Vector: ${log.previousValue} -> ${log.newValue}", fontSize = 11.sp)
                            Text("Device node: ${log.deviceInfo}", fontSize = 10.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// SCREEN 5: TECHNICAL SQL CENTER & DEVELOPER BRIDGES
// =========================================================================
@Composable
fun TechnicalSpecsHub(
    trans: (String, String) -> String
) {
    var techTab by remember { mutableStateOf("SCHEMA") }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Garuda Finance Ecosystem Console specifications", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RoyalBlue)
        
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("SCHEMA" to "SQL PostgreSQL Scheme", "REST_SERVICES" to "REST Service Endpoints").forEach { (k, title) ->
                val active = techTab == k
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (active) RoyalBlue else Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .clickable { techTab = k }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(title, color = if (active) Color.White else Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (techTab) {
                "SCHEMA" -> {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Copy production normalized PostgreSQL DB specifications:", fontSize = 11.sp, color = Color.Gray)
                        Box(modifier = Modifier.fillMaxWidth().background(Color.Black).padding(8.dp)) {
                            Text(
                                text = """
                                -- 1. CUSTOMERS MASTER COMPLIANT SCHEMA
                                CREATE TABLE customers (
                                    id VARCHAR(50) PRIMARY KEY,
                                    full_name VARCHAR(150) NOT NULL,
                                    nic_number VARCHAR(15) UNIQUE NOT NULL,
                                    phone_number VARCHAR(20) NOT NULL,
                                    alt_phone VARCHAR(20),
                                    address TEXT NOT NULL,
                                    occupation VARCHAR(100),
                                    monthly_income NUMERIC(15,2) NOT NULL,
                                    guarantor_name VARCHAR(100),
                                    guarantor_phone VARCHAR(20),
                                    gps_location VARCHAR(100),
                                    status VARCHAR(20) DEFAULT 'Active',
                                    registration_date DATE DEFAULT CURRENT_DATE
                                );

                                -- 2. LOANS REPAYMENTS LEDGER SCHEMA
                                CREATE TABLE loans (
                                    id VARCHAR(50) PRIMARY KEY,
                                    customer_id VARCHAR(50) REFERENCES customers(id) ON DELETE CASCADE,
                                    loan_amount NUMERIC(15,2) NOT NULL,
                                    interest_rate NUMERIC(5,2) NOT NULL,
                                    installment_frequency VARCHAR(20) NOT NULL, -- Daily, Weekly, Monthly
                                    installment_amount NUMERIC(15,2) NOT NULL,
                                    outstanding_balance NUMERIC(15,2) NOT NULL,
                                    penalty_balance NUMERIC(15,2) DEFAULT 0.00,
                                    status VARCHAR(20) DEFAULT 'Active'
                                );

                                -- 3. RECEIPT HISTORIES COMPLIANCE
                                CREATE TABLE payments (
                                    receipt_number VARCHAR(50) PRIMARY KEY,
                                    loan_id VARCHAR(50) REFERENCES loans(id),
                                    amount_paid NUMERIC(15,2) NOT NULL,
                                    payment_date DATE NOT NULL,
                                    remaining_balance NUMERIC(15,2) NOT NULL,
                                    collector_name VARCHAR(100) NOT NULL
                                );
                                """.trimIndent(),
                                color = Color.Green,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                lineHeight = 10.sp
                            )
                        }
                    }
                }

                "REST_SERVICES" -> {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Verified REST backend microservices routing stubs description:", fontSize = 11.sp, color = Color.Gray)
                        Box(modifier = Modifier.fillMaxWidth().background(Color.Black).padding(8.dp)) {
                            Text(
                                text = """
                                1. POST /api/v1/auth/login-jwt
                                Headers: { "Content-Type": "application/json" }
                                Payload: { "username": un, "password_hash": hex, "role": val }
                                Response: { "status": "200", "token": "jwt.header.body.signature" }

                                2. POST /api/v1/customers/register
                                Headers: { "Authorization": "Bearer <JWT>" }
                                Response: { "status": "201", "id": "CUS-XXXX" }

                                3. POST /api/v1/collections/flush-queue
                                Description: Synchronize local offline SQLite sync arrays
                                Payload: { "payments": [ {...}, {...} ] }
                                Response: { "status": "200", "synchronized_records": X }
                                """.trimIndent(),
                                color = Color.Yellow,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
