const prisma = require('../config/prisma');

/**
 * Commits a secure compliance audit log entry to the database
 * @param {string} operationType Category of action (e.g. "Security Authentication", "Disbursal Engine")
 * @param {string} details Description of the specific action
 * @param {string} operatorName Full name of the execution operator
 * @param {string} status Output status (Approved, Failed, Pending)
 * @param {string} [userId] Optional database user primary ID
 */
const logAuditTrail = async (operationType, details, operatorName, status, userId = null) => {
  try {
    const log = await prisma.auditLog.create({
      data: {
        operationType,
        details,
        operatorName,
        status,
        userId
      }
    });
    console.log(`[AUDIT SECURE LOG] [${status}] [${operationType}] - ${details} (by ${operatorName})`);
    return log;
  } catch (error) {
    console.error('[AUDIT WRITE EXCEPTION] Failed to save security audit ledger record: ', error);
  }
};

module.exports = {
  logAuditTrail
};
