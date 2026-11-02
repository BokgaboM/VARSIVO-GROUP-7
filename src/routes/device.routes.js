const express = require('express');
const router = express.Router();
const { authenticate } = require('../middleware/auth');
const deviceController = require('../controllers/device.controller');

router.post('/', authenticate, deviceController.registerDevice);
router.delete('/', authenticate, deviceController.unregisterDevice);

module.exports = router;
