const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const prisma = require('../config/prisma');
const { logAuditTrail } = require('../utils/logger');

const JWT_SECRET = process.env.JWT_SECRET || 'garuda_finance_super_secret_jwt_signature_key_2026';

/**
 * Handle secure credential authentication and JWT lease generation
 */
const login = async (req, res) => {
  const { username, password, role } = req.body;

  if (!username || !password || !role) {
    return res.status(400).json({
      success: false,
      message: 'Input Validation Error: Username, password and role classification fields are mandatory.'
    });
  }

  const cleanedUsername = username.trim().toLowerCase();

  try {
    // 1. Query operator record from Postgres
    let user = await prisma.user.findUnique({
      where: { username: cleanedUsername }
    });

    // 2. Mock fallback accounts if first boot seed is absent, helping easy deployment / manual review
    if (!user) {
      const matchPre = [
        { u: "owner_vishwa", r: "Owner", n: "Vishwa Perera", p: "owner123" },
        { u: "manager_arun", r: "Manager", n: "Arun Balasubramaniam", p: "manager123" },
        { u: "officer_kamal", r: "Collection Officer", n: "Kamal Silva", p: "officer123" }
      ].find(mock => mock.u === cleanedUsername && mock.r === role);

      if (matchPre && password === matchPre.p) {
        // Create permanent profile on-the-fly for PostgreSQL sandbox consistency
        const hash = await bcrypt.hash(password, 10);
        user = await prisma.user.create({
          data: {
            username: cleanedUsername,
            passwordHash: hash,
            fullName: matchPre.n,
            role: matchPre.r,
            branchId: "GI-BR-001",
            status: "Active"
          }
        });
      }
    }

    if (!user) {
      await logAuditTrail("Security Authentication", `Failed login attempt for indeterminate user: [${cleanedUsername}]`, cleanedUsername, "Failed");
      return res.status(401).json({
        success: false,
        message: 'Authentication Failure: Invalid username or role correlation.'
      });
    }

    if (user.role !== role) {
      await logAuditTrail("Security Authentication", `Role mismatch breach: user tried to login as ${role}, expected role ${user.role}`, user.fullName, "Failed", user.id);
      return res.status(403).json({
        success: false,
        message: `Authentication Failure: Provided credentials map to a different access profile [${user.role}].`
      });
    }

    if (user.status !== "Active") {
      return res.status(403).json({
        success: false,
        message: 'Account Suspended: Contact systems administrator immediately.'
      });
    }

    // 3. Verify salted password encryption hash
    const isMatched = await bcrypt.compare(password, user.passwordHash);
    if (!isMatched) {
      await logAuditTrail("Security Authentication", `Incorrect password challenge entered for [${cleanedUsername}]`, user.fullName, "Failed", user.id);
      return res.status(401).json({
        success: false,
        message: 'Authentication Failure: Decrypt challenge failed. Password incorrect.'
      });
    }

    // 4. Generate 24-hour JWT ticket lease
    const tokenPayload = {
      id: user.id,
      username: user.username,
      fullName: user.fullName,
      role: user.role,
      branchId: user.branchId
    };

    const token = jwt.sign(tokenPayload, JWT_SECRET, { expiresIn: '24h' });

    // 5. Publish Audit Transaction Log
    await logAuditTrail(
      "Security Authentication",
      `Successful JWT session lease granted. Operator: [${user.fullName}] (Role: ${user.role})`,
      user.fullName,
      "Approved",
      user.id
    );

    return res.status(200).json({
      success: true,
      message: 'Access Granted: JWT authenticated successfully.',
      token,
      user: {
        id: user.id,
        username: user.username,
        fullName: user.fullName,
        role: user.role,
        branchId: user.branchId,
        status: user.status
      }
    });

  } catch (error) {
    console.error('Login Endpoint Crash: ', error);
    return res.status(500).json({
      success: false,
      message: 'Internal Application Fault: Transaction could not be executed by server.'
    });
  }
};

/**
 * Endpoint for registration of additional network operators (Internal Admin tool)
 */
const registerOperator = async (req, res) => {
  const { username, password, fullName, role, branchId } = req.body;

  if (!username || !password || !fullName || !role || !branchId) {
    return res.status(400).json({
      success: false,
      message: 'Validation Error: Full metadata footprint required.'
    });
  }

  try {
    const existing = await prisma.user.findUnique({ where: { username: username.toLowerCase() } });
    if (existing) {
      return res.status(409).json({
        success: false,
        message: 'Registration Conflict: Account username already exists.'
      });
    }

    const saltRounds = 10;
    const passwordHash = await bcrypt.hash(password, saltRounds);

    const user = await prisma.user.create({
      data: {
        username: username.toLowerCase(),
        passwordHash,
        fullName,
        role,
        branchId,
        status: "Active"
      }
    });

    await logAuditTrail(
      "Security Authentication",
      `Provisioned new security credentials for user [${fullName}] as [${role}] on branch [${branchId}]`,
      req.user?.fullName || "System Admin Service",
      "Approved",
      user.id
    );

    return res.status(201).json({
      success: true,
      user: {
        id: user.id,
        username: user.username,
        fullName: user.fullName,
        role: user.role,
        branch: user.branchId
      }
    });

  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};

module.exports = {
  login,
  registerOperator
};
