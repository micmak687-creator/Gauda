const prisma = require('../config/prisma');
const { logAuditTrail } = require('../utils/logger');

/**
 * Commits a single real-time payment collection and updates loan outstanding balances
 */
const recordCollection = async (req, res) => {
  const { loanId, amountPaid, paymentMethod, remarks } = req.body;

  if (!loanId || !amountPaid || !paymentMethod) {
    return res.status(400).json({
      success: false,
      message: 'Validation Error: Loan ID, Payment Amount, and Payment Method are required parameters.'
    });
  }

  const numericPaid = parseFloat(amountPaid);

  try {
    // 1. Fetch related loan
    const loan = await prisma.loan.findUnique({ where: { id: loanId } });
    if (!loan) {
      return res.status(404).json({
        success: false,
        message: 'Loan Account Not Found: Reference key could not be verified.'
      });
    }

    if (loan.outstandingBalance <= 0) {
      return res.status(400).json({
        success: false,
        message: 'Invalid Action: This microfinance loan has already been fully completed.'
      });
    }

    // 2. Compute balance updates
    const collector = req.user.fullName;
    const remainingBalance = Math.max(0.0, loan.outstandingBalance - numericPaid);
    const updatedStatus = remainingBalance <= 0.0 ? "Completed" : loan.status;

    // Get a clean transaction timestamp
    const dateObj = new Date();
    const paymentDate = dateObj.toISOString().slice(0, 10);
    const paymentTime = dateObj.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: true });

    // Generate consecutive receipt ID
    const countPayments = await prisma.payment.count();
    const receiptNumber = `GI-RCP-${String(countPayments + 1).padStart(5, '0')}`;

    // 3. Database operations
    const payment = await prisma.$transaction(async (tx) => {
      // Update Loan record balances
      await tx.loan.update({
        where: { id: loanId },
        data: {
          outstandingBalance: remainingBalance,
          status: updatedStatus,
          lastPaidDate: paymentDate
        }
      });

      // Insert collection Payment receipt
      return await tx.payment.create({
        data: {
          receiptNumber,
          loanId,
          customerId: loan.customerId,
          customerName: loan.customerName,
          paymentDate,
          paymentTime,
          amountPaid: numericPaid,
          remainingBalance,
          collectorName: collector,
          paymentMethod,
          remarks
        }
      });
    });

    await logAuditTrail(
      "Collection Entry",
      `Recorded transaction [${receiptNumber}]: Received LKR ${numericPaid} for Borrower [${loan.customerName}] - Remaining Balance: LKR ${remainingBalance}`,
      collector,
      "Approved"
    );

    return res.status(201).json({
      success: true,
      message: 'Payment collection entry processed and ledger synchronized.',
      receipt: payment
    });

  } catch (error) {
    console.error('Record Payment Execution Error: ', error);
    return res.status(500).json({
      success: false,
      message: 'System Transaction Aborted: Ledger balance mismatch safeguard triggered.'
    });
  }
};

/**
 * Queue Sync Gateway: Batch aggregates and reconciles multiple offline queue transactions
 */
const syncOfflineCollections = async (req, res) => {
  const { queue } = req.body; // Expects an Array of payments JSON objects

  if (!queue || !Array.isArray(queue) || queue.length === 0) {
    return res.status(400).json({
      success: false,
      message: 'Validation Error: No transaction entries found in the sync queue payload.'
    });
  }

  const collector = req.user.fullName;
  let successCount = 0;
  let failureCount = 0;
  const syncResults = [];

  try {
    for (const offlineEntry of queue) {
      const { loanId, amountPaid, paymentMethod, remarks, paymentDate, paymentTime } = offlineEntry;
      const numericPaid = parseFloat(amountPaid);

      try {
        const loan = await prisma.loan.findUnique({ where: { id: loanId } });
        if (!loan) {
          throw new Error(`Referenced Loan ID [${loanId}] could not be resolved in central ledger.`);
        }

        const remainingBalance = Math.max(0.0, loan.outstandingBalance - numericPaid);
        const updatedStatus = remainingBalance <= 0.0 ? "Completed" : loan.status;

        const countPayments = await prisma.payment.count();
        const receiptNumber = `GI-RCP-${String(countPayments + 1).padStart(5, '0')}`;

        await prisma.$transaction(async (tx) => {
          await tx.loan.update({
            where: { id: loanId },
            data: {
              outstandingBalance: remainingBalance,
              status: updatedStatus,
              lastPaidDate: paymentDate || new Date().toISOString().slice(0, 10)
            }
          });

          await tx.payment.create({
            data: {
              receiptNumber,
              loanId,
              customerId: loan.customerId,
              customerName: loan.customerName,
              paymentDate: paymentDate || new Date().toISOString().slice(0, 10),
              paymentTime: paymentTime || '12:00 PM',
              amountPaid: numericPaid,
              remainingBalance,
              collectorName: collector,
              paymentMethod,
              remarks: `[OFFLINE-SYNCHRONIZED] ${remarks || ''}`,
              isOfflineQueued: true
            }
          });
        });

        successCount++;
        syncResults.push({ loanId, status: "Synchronized", receiptNumber });
      } catch (err) {
        failureCount++;
        syncResults.push({ loanId, status: "Failed", reason: err.message });
      }
    }

    await logAuditTrail(
      "Offline Synchronizer",
      `Completed batch sync reconciliation. Success: ${successCount} entries, Failures: ${failureCount} entries`,
      collector,
      "Approved"
    );

    return res.status(200).json({
      success: true,
      message: `Batch sync complete: ${successCount} entries processed successfully, ${failureCount} flagged.`,
      summary: {
        totalReceived: queue.length,
        successCount,
        failureCount
      },
      details: syncResults
    });

  } catch (error) {
    console.error('Batch sync endpoint crashed: ', error);
    return res.status(500).json({
      success: false,
      message: 'Batch synchronization process failed due to schema schema conflicts.'
    });
  }
};

/**
 * Access a payment receipt log list
 */
const listCollectionsOnLedger = async (req, res) => {
  try {
    const historicalPayments = await prisma.payment.findMany({
      orderBy: { createdAt: 'desc' }
    });
    return res.status(200).json({
      success: true,
      payments: historicalPayments
    });
  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};

module.exports = {
  recordCollection,
  syncOfflineCollections,
  listCollectionsOnLedger
};
