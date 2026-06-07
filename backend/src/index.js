require('dotenv').config();
const express = require('express');
const cors = require('cors');
const morgan = require('morgan');
const apiRoutes = require('./routes/api');

const app = express();
const PORT = process.env.PORT || 5000;

// ==========================================
// CENTRAL NETWORK MIDDLEWARES
// ==========================================
app.use(cors());
app.use(express.json());

// Enable request logging during debugging
if (process.env.NODE_ENV !== 'production') {
  app.use(morgan('dev'));
}

// ==========================================
// DEFAULT HEARTBEAT CHECK (Root Entry)
// ==========================================
app.get('/', (req, res) => {
  res.status(200).json({
    status: "Healthy",
    serviceName: "Garuda Finance GILCMS API Gateway",
    timestamp: new Date().toISOString(),
    apiDocUrl: "/api/docs",
    environment: process.env.NODE_ENV || "development"
  });
});

// ==========================================
// REGISTER CENTRAL ENDPOINTS
// ==========================================
app.use('/api', apiRoutes);

// ==========================================
// SECURE ERROR HANDLER BOUNDARY
// ==========================================
app.use((err, req, res, next) => {
  console.error('[SERVER GLOBAL RECOVERY GUARD] Uncaught Exception: ', err);
  res.status(500).json({
    success: false,
    message: 'Global Recovery: An internal database or gateway exception occurred.'
  });
});

// ==========================================
// SERVER INITIALIZATION
// ==========================================
app.listen(PORT, () => {
  console.log(`=======================================================`);
  console.log(` GARUDA FINANCE - GILCMS API GATEWAY IS ONLINE`);
  console.log(` Access local URL: http://localhost:${PORT}`);
  console.log(` Environment Mode: ${process.env.NODE_ENV || 'development'}`);
  console.log(`=======================================================`);
});

module.exports = app;
