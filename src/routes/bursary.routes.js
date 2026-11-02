const express = require('express');
const router = express.Router();
const bursaryController = require('../controllers/bursary.controller');

router.get('/', bursaryController.getBursaries);

module.exports = router;