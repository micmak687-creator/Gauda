const prisma = require('../config/prisma');
const { logAuditTrail } = require('../utils/logger');

/**
 * Disburse a new microfinance debt package (Loan creation)
 */
const createLoan = async (req, res) => {
  const { customerId, loanAmount, interestRate, frequency, paymentDurationDays, startDate } = req.body;

  if (!customerId || !loanAmount || !interestRate || !frequency || !startDate) {
    return res.status(400).json({
      success: false,
      message: 'Validation Error: Customer, Amount, Interest rate, Repayment frequency, and Start date are mandatory parameters.'
    });
  }

  try {
    // 1. Verify consumer account
    const customer = await prisma.customer.findUnique({ where: { id: customerId } });
    if (!customer) {
      return res.status(404).json({
        success: false,
        message: 'Invalid Customer ID: Target borrower portfolio cannot be resolved.'
      });
    }

    const principal = parseFloat(loanAmount);
    const ratePercentage = parseFloat(interestRate);
    const duration = parseInt(paymentDurationDays) || 120; // Default 120 Days duration if not specified

    // 2. Perform EMI calculation matching simple microfinance interest model
    // Simple Interest EMI Calculation Formula: 
    // Total payback = Principal + (Principal * (Rate/100) * (Duration/365))
    const totalInterest = principal * (ratePercentage / 100.0) * (duration / 365.0);
    const totalRepayable = principal + totalInterest;

    // Installment deduction based on repayment frequency class
    let installmentCount = 1;
    if (frequency === "Daily") {
      installmentCount = duration;
    } else if (frequency === "Weekly") {
      installmentCount = Math.ceil(duration / 7);
    } else if (frequency === "Monthly") {
      installmentCount = Math.ceil(duration / 30);
    }

    const installmentAmount = Math.round((totalRepayable / installmentCount) * 100) / 100;

    // 3. Create loan record
    const loan = await prisma.loan.create({
      data: {
        customerId,
        customerName: customer.fullName,
        loanAmount: principal,
        interestRate: ratePercentage,
        installmentAmount,
        outstandingBalance: totalRepayable,
        frequency,
        paymentDurationDays: duration,
        startDate,
        status: "Active"
      }
    });

    await logAuditTrail(
      "Disbursal Engine",
      `Disbursed new loan portfolio: [${loan.id}] to borrower [${customer.fullName}] - Total Repayable LKR: ${totalRepayable}`,
      req.user.fullName,
      "Approved"
    );

    return res.status(201).json({
      success: true,
      message: 'New loan disburse ledger recorded successfully.',
      loan,
      metrics: {
        totalInterest,
        totalRepayable,
        installmentAmount,
        installmentCount
      }
    });

  } catch (error) {
    console.error('Create Loan Endpoint Error: ', error);
    return res.status(500).json({
      success: false,
      message: 'Internal Database Failure: Failed to create loan record.'
    });
  }
};

/**
 * Triggers batch scanning for overdue loans and auto-attaches penalties
 */
const runPenaltyEngine = async (req, res) => {
  try {
    const activeLoans = await prisma.loan.findMany({
      where: {
        status: "Active",
        outstandingBalance: { gt: 0 }
      }
    });

    let penalisedCount = 0;
    const today = new Date();

    for (const loan of activeLoans) {
      const loanCreatedDate = new Date(loan.startDate);
      const daysDiff = Math.floor((today - loanCreatedDate) / (1000 * 60 * 60 * 24));

      // If duration exceeded and balance remains, mark Overdue and charge standard penalty
      if (daysDiff > loan.paymentDurationDays) {
        const standardPenalty = 250.00; // Flat penalty of 250 LKR for overdue status
        
        await prisma.loan.update({
          where: { id: loan.id },
          data: {
            status: "Overdue",
            penaltyAmount: { increment: standardPenalty },
            outstandingBalance: { increment: standardPenalty }
          }
        });

        await logAuditTrail(
          "Penalty Engine",
          `Attached overdue penalty of LKR ${standardPenalty} on Loan [${loan.id}] for Customer: ${loan.customerName}`,
          "System automation",
          "Approved"
        );
        penalisedCount++;
      }
    }

    return res.status(200).json({
      success: true,
      message: `Penalty engine scan completed. ${penalisedCount} active loans updated to Overdue status.`,
      scannedCount: activeLoans.length,
      modifiedCount: penalisedCount
    });

  } catch (error) {
    console.error('Penalty engine scan failed: ', error);
    return res.status(500).json({
      success: false,
      message: error.message
    });
  }
};

/**
 * Fetch list of all active or completed loans
 */
const listLoans = async (req, res) => {
  try {
    const loans = await prisma.loan.findMany({
      orderBy: { createdAt: 'desc' }
    });
    return res.status(200).json({
      success: true,
      loans
    });
  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};

module.exports = {
  createLoan,
  runPenaltyEngine,
  listLoans
};
