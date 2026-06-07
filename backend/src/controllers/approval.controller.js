const prisma = require('../config/prisma');
const { logAuditTrail } = require('../utils/logger');

/**
 * Initiates a new override/exception approval request
 */
const requestApproval = async (req, res) => {
  const { sectionName, description } = req.body;

  if (!sectionName || !description) {
    return res.status(400).json({
      success: false,
      message: 'Validation Error: Section scope and description context are required.'
    });
  }

  try {
    const approval = await prisma.approval.create({
      data: {
        requesterName: req.user.fullName,
        sectionName,
        description,
        status: "Pending"
      }
    });

    await logAuditTrail(
      "Workflow Approvals",
      `Created approval request [${approval.id}] for: ${sectionName} - ${description}`,
      req.user.fullName,
      "Pending"
    );

    return res.status(201).json({
      success: true,
      message: 'Workflow approval request created successfully.',
      approval
    });

  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};

/**
 * Process/Review an approval request (Owner/Manager role permission ONLY)
 */
const reviewApproval = async (req, res) => {
  const { id } = req.params;
  const { status } = req.body; // Approved or Rejected

  if (!status || !["Approved", "Rejected"].includes(status)) {
    return res.status(400).json({
      success: false,
      message: 'Validation Error: Review decision status MUST be "Approved" or "Rejected".'
    });
  }

  try {
    const request = await prisma.approval.findUnique({ where: { id } });
    if (!request) {
      return res.status(404).json({
        success: false,
        message: 'Approval Request Not Found'
      });
    }

    const updated = await prisma.approval.update({
      where: { id },
      data: {
        status,
        reviewTime: new Date(),
        reviewerName: req.user.fullName,
        reviewerId: req.user.id
      }
    });

    await logAuditTrail(
      "Workflow Approvals",
      `Reviewed request id: [${updated.id}] - Status updated to ${status}. Reviewer: [${req.user.fullName}]`,
      req.user.fullName,
      status
    );

    return res.status(200).json({
      success: true,
      message: `Workflow ticket reviewed: ${status}`,
      approval: updated
    });

  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};

/**
 * List all workflow items
 */
const listApprovals = async (req, res) => {
  try {
    const approvals = await prisma.approval.findMany({
      orderBy: { timestamp: 'desc' }
    });
    return res.status(200).json({
      success: true,
      approvals
    });
  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};

module.exports = {
  requestApproval,
  reviewApproval,
  listApprovals
};
