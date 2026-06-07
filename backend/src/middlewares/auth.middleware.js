const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'garuda_finance_super_secret_jwt_signature_key_2026';

// General JWT Validation Middleware
const authenticateToken = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  // Decodes Bearer token
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    return res.status(401).json({
      success: false,
      message: 'Access Denied: Secure JWT authorization token is missing'
    });
  }

  try {
    const verified = jwt.verify(token, JWT_SECRET);
    req.user = verified; // Append decoded claims (id, username, role, branchId)
    next();
  } catch (err) {
    return res.status(403).json({
      success: false,
      message: 'Access Denied: Invalid or expired cryptographic ticket'
    });
  }
};

// Role-Based Authorization Filter Guard
const authorizeRoles = (...allowedRoles) => {
  return (req, res, next) => {
    if (!req.user || !req.user.role) {
      return res.status(403).json({
        success: false,
        message: 'Access Denied: Indeterminate authorization credentials'
      });
    }

    if (!allowedRoles.includes(req.user.role)) {
      return res.status(403).json({
        success: false,
        message: `Forbidden: This action requires one of the following privilege hierarchies: [${allowedRoles.join(', ')}]. Active operator role: [${req.user.role}].`
      });
    }

    next();
  };
};

module.exports = {
  authenticateToken,
  authorizeRoles
};
