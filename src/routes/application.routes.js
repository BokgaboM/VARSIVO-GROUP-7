const express = require('express');
const router = express.Router();
const { authenticate } = require('../middleware/auth');
const applicationController = require('../controllers/application.controller');

router.get('/', authenticate, applicationController.getMyApplications);
router.post('/', authenticate, applicationController.createApplication);
router.patch('/:id', authenticate, applicationController.updateApplicationStatus);
router.delete('/:id', authenticate, applicationController.deleteApplication);

module.exports = router;
