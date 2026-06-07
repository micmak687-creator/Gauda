package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val db: AppDatabase) {
    val allCustomers: Flow<List<CustomerEntity>> = db.customerDao().getAllCustomers()
    val allLoans: Flow<List<LoanEntity>> = db.loanDao().getAllLoans()
    val allPayments: Flow<List<PaymentEntity>> = db.paymentDao().getAllPayments()
    val allAuditLogs: Flow<List<AuditLogEntity>> = db.auditLogDao().getAllAuditLogs()
    val allUserAccounts: Flow<List<UserAccountEntity>> = db.userAccountDao().getAllUserAccounts()
    val allBranches: Flow<List<BranchEntity>> = db.branchDao().getAllBranches()

    suspend fun getCustomerById(id: String) = db.customerDao().getCustomerById(id)
    suspend fun insertCustomer(customer: CustomerEntity) = db.customerDao().insertCustomer(customer)
    suspend fun updateCustomer(customer: CustomerEntity) = db.customerDao().updateCustomer(customer)
    suspend fun deleteCustomer(id: String) = db.customerDao().deleteCustomerById(id)

    suspend fun getLoanById(id: String) = db.loanDao().getLoanById(id)
    suspend fun insertLoan(loan: LoanEntity) = db.loanDao().insertLoan(loan)
    suspend fun updateLoan(loan: LoanEntity) = db.loanDao().updateLoan(loan)
    suspend fun deleteLoan(id: String) = db.loanDao().deleteLoanById(id)
    fun getLoansByCustomerId(customerId: String) = db.loanDao().getLoansByCustomerId(customerId)

    suspend fun insertPayment(payment: PaymentEntity) = db.paymentDao().insertPayment(payment)
    fun getPaymentsByLoanId(loanId: String) = db.paymentDao().getPaymentsByLoanId(loanId)

    suspend fun insertAuditLog(log: AuditLogEntity) = db.auditLogDao().insertAuditLog(log)

    suspend fun getUserAccountById(id: String) = db.userAccountDao().getUserAccountById(id)
    suspend fun insertUserAccount(user: UserAccountEntity) = db.userAccountDao().insertUserAccount(user)
    suspend fun updateUserAccount(user: UserAccountEntity) = db.userAccountDao().updateUserAccount(user)
    suspend fun deleteUserAccount(id: String) = db.userAccountDao().deleteUserAccountById(id)

    suspend fun getBranchById(id: String) = db.branchDao().getBranchById(id)
    suspend fun insertBranch(branch: BranchEntity) = db.branchDao().insertBranch(branch)
    suspend fun updateBranch(branch: BranchEntity) = db.branchDao().updateBranch(branch)
    suspend fun deleteBranch(id: String) = db.branchDao().deleteBranchById(id)
}
