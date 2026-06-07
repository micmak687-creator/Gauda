const express = require('express');
const router = express.Router();

// Middlewares
const { authenticateToken, authorizeRoles } = require('../middlewares/auth.middleware');

// Controllers
const authCtrl = require('../controllers/auth.controller');
const customerCtrl = require('../controllers/customer.controller');
const loanCtrl = require('../controllers/loan.controller');
const collectionCtrl = require('../controllers/collection.controller');
const approvalCtrl = require('../controllers/approval.controller');
const auditCtrl = require('../controllers/audit.controller');

// ==========================================
// 1. AUTHENTICATION SERVICES (Identity Management)
// ==========================================
router.post('/auth/login', authCtrl.login);
router.post('/auth/register', authenticateToken, authorizeRoles('Owner', 'Manager'), authCtrl.registerOperator);

// ==========================================
// 2. CUSTOMER MANAGEMENT (CRM Ledger)
// ==========================================
router.get('/customers', authenticateToken, customerCtrl.listCustomers);
router.get('/customers/:id', authenticateToken, customerCtrl.getCustomerProfile);
router.post('/customers', authenticateToken, customerCtrl.addCustomer);
router.put('/customers/:id', authenticateToken, customerCtrl.editCustomer);

// ==========================================
// 3. SECURE DISBURSAL ENGINE (Microfinance Loans)
// ==========================================
router.get('/loans', authenticateToken, loanCtrl.listLoans);
router.post('/loans', authenticateToken, authorizeRoles('Owner', 'Manager'), loanCtrl.createLoan);
router.post('/loans/penalties', authenticateToken, authorizeRoles('Owner', 'Manager'), loanCtrl.runPenaltyEngine);

// ==========================================
// 4. LEDGER COLLECTIONS SERVICES (Payments Gateway)
// ==========================================
router.get('/collections', authenticateToken, collectionCtrl.listCollectionsOnLedger);
router.post('/collections', authenticateToken, collectionCtrl.recordCollection);
router.post('/collections/sync', authenticateToken, collectionCtrl.syncOfflineCollections);

// ==========================================
// 5. WORKFLOW OVERRIDES & APPROVALS (Control)
// ==========================================
router.get('/approvals', authenticateToken, authorizeRoles('Owner', 'Manager'), approvalCtrl.listApprovals);
router.post('/approvals', authenticateToken, approvalCtrl.requestApproval);
router.put('/approvals/:id', authenticateToken, authorizeRoles('Owner', 'Manager'), approvalCtrl.reviewApproval);

// ==========================================
// 6. COMPLIANCE AUDIT TRAIL (Supervision Records)
// ==========================================
router.get('/audit', authenticateToken, authorizeRoles('Owner', 'Manager'), auditCtrl.getAuditTrail);

module.exports = router;
