const prisma = require('../config/prisma');

/**
 * Fetch all secure transaction logs from the audit trail database
 */
const getAuditTrail = async (req, res) => {
  const { operationType } = req.query;

  try {
    const whereClause = {};
    if (operationType) {
      whereClause.operationType = operationType;
    }

    const auditTrail = await prisma.auditLog.findMany({
      where: whereClause,
      orderBy: { timestamp: 'desc' }
    });

    return res.status(200).json({
      success: true,
      count: auditTrail.length,
      auditTrail
    });

  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Failed to query database audit records: ' + error.message
    });
  }
};

module.exports = {
  getAuditTrail
};
