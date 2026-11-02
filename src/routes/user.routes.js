const express = require('express');
const router = express.Router();
const { authenticate } = require('../middleware/auth');
const userController = require('../controllers/user.controller');

// NOTE: the "/me/..." routes must stay above "/:id/settings" so "me" is never read as an id.
router.get('/me', authenticate, userController.getMe);
router.put('/me/profile', authenticate, userController.updateProfile);
router.patch('/me/password', authenticate, userController.changePassword);
router.patch('/:id/settings', authenticate, userController.updateSettings);

module.exports = router;
