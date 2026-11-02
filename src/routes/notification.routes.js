const express = require('express');
const router = express.Router();
const { authenticate } = require('../middleware/auth');
const { requireAdminKey } = require('../middleware/adminKey');
const notificationController = require('../controllers/notification.controller');

router.get('/', authenticate, notificationController.listMine);
router.post('/test', authenticate, notificationController.sendTest);
router.post('/broadcast', requireAdminKey, notificationController.broadcast);
router.post('/deadlines/run', requireAdminKey, notificationController.runDeadlines);

module.exports = router;
