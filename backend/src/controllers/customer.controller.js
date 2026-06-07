const prisma = require('../config/prisma');
const { logAuditTrail } = require('../utils/logger');

/**
 * Register a new microfinance customer with full identification info
 */
const addCustomer = async (req, res) => {
  const {
    fullName,
    nicNumber,
    phoneNumber,
    address,
    monthlyIncome,
    guarantorName,
    guarantorPhone,
    gpsLocation,
    nicFrontPath,
    biometricSelfiePath
  } = req.body;

  if (!fullName || !nicNumber || !phoneNumber || !address || !monthlyIncome) {
    return res.status(400).json({
      success: false,
      message: 'Validation Error: Full name, NIC, phone number, address, and monthly income are mandatory.'
    });
  }

  try {
    // Check for unique index constraints
    const existingNic = await prisma.customer.findUnique({ where: { nicNumber } });
    if (existingNic) {
      return res.status(409).json({
        success: false,
        message: 'Conflict Error: Registration with this NIC number already exists.'
      });
    }

    const customer = await prisma.customer.create({
      data: {
        fullName,
        nicNumber,
        phoneNumber,
        address,
        monthlyIncome: parseFloat(monthlyIncome),
        guarantorName: guarantorName || 'N/A',
        guarantorPhone: guarantorPhone || 'N/A',
        gpsLocation: gpsLocation || 'Not Stamped',
        nicFrontPath,
        biometricSelfiePath
      }
    });

    await logAuditTrail(
      "Customer Management",
      `Created customer profile: [${fullName}] (NIC: ${nicNumber})`,
      req.user.fullName,
      "Approved"
    );

    return res.status(201).json({
      success: true,
      message: 'Customer profile registered successfully on remote host.',
      customer
    });

  } catch (error) {
    console.error('Add Customer DB Error: ', error);
    return res.status(500).json({
      success: false,
      message: 'Internal DB Error: Failed to commit customer portfolio.'
    });
  }
};

/**
 * Update an existing customer profile record
 */
const editCustomer = async (req, res) => {
  const { id } = req.params;
  const {
    fullName,
    phoneNumber,
    address,
    monthlyIncome,
    guarantorName,
    guarantorPhone,
    gpsLocation,
    nicFrontPath,
    biometricSelfiePath
  } = req.body;

  try {
    const customer = await prisma.customer.findUnique({ where: { id } });
    if (!customer) {
      return res.status(404).json({
        success: false,
        message: 'Customer Not Found: Specified record could not be located'
      });
    }

    const updated = await prisma.customer.update({
      where: { id },
      data: {
        fullName: fullName || customer.fullName,
        phoneNumber: phoneNumber || customer.phoneNumber,
        address: address || customer.address,
        monthlyIncome: monthlyIncome ? parseFloat(monthlyIncome) : customer.monthlyIncome,
        guarantorName: guarantorName || customer.guarantorName,
        guarantorPhone: guarantorPhone || customer.guarantorPhone,
        gpsLocation: gpsLocation || customer.gpsLocation,
        nicFrontPath: nicFrontPath || customer.nicFrontPath,
        biometricSelfiePath: biometricSelfiePath || customer.biometricSelfiePath
      }
    });

    await logAuditTrail(
      "Customer Management",
      `Modified customer profile id: [${id}] - Name: ${updated.fullName}`,
      req.user.fullName,
      "Approved"
    );

    return res.status(200).json({
      success: true,
      message: 'Customer profile updated successfully.',
      customer: updated
    });

  } catch (error) {
    console.error('Update Customer DB Error: ', error);
    return res.status(500).json({
      success: false,
      message: 'Internal Error: Customer update transaction aborted.'
    });
  }
};

/**
 * Fetch a customer record with active microfinance loan details
 */
const getCustomerProfile = async (req, res) => {
  const { id } = req.params;

  try {
    const customer = await prisma.customer.findUnique({
      where: { id },
      include: {
        loans: true
      }
    });

    if (!customer) {
      return res.status(404).json({
        success: false,
        message: 'Customer profile not found'
      });
    }

    return res.status(200).json({
      success: true,
      customer
    });

  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};

/**
 * List all registered customer records
 */
const listCustomers = async (req, res) => {
  try {
    const customers = await prisma.customer.findMany({
      orderBy: { createdAt: 'desc' }
    });
    return res.status(200).json({
      success: true,
      customers
    });
  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};

module.exports = {
  addCustomer,
  editCustomer,
  getCustomerProfile,
  listCustomers
};
