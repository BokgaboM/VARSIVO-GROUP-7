const applicationStore = require('../services/applicationStore');
const notificationService = require('../services/notificationService');

const MAX_STATUS_LENGTH = 30;

exports.getMyApplications = async (req, res, next) => {
  try {
    const apps = await applicationStore.getAllForUser(req.user.uid);
    res.json(apps);
  } catch (err) {
    next(err);
  }
};

/** POST /api/applications   body: { institutionId, status? }  -> also pushes an "Application saved" alert. */
exports.createApplication = async (req, res, next) => {
  try {
    const institutionId = Number(req.body && req.body.institutionId);
    const status = String((req.body && req.body.status) || 'PENDING').trim();
    if (!Number.isInteger(institutionId) || institutionId <= 0) {
      return res.status(400).json({ error: 'institutionId must be a positive whole number' });
    }
    if (!status || status.length > MAX_STATUS_LENGTH) {
      return res.status(400).json({ error: `status must be 1-${MAX_STATUS_LENGTH} characters` });
    }

    const app = await applicationStore.create(req.user.uid, institutionId, status);
    res.status(201).json(app);

    // Fire-and-forget: a push problem must never fail the request.
    notificationService.notifyApplicationCreated(req.user.uid, app);
  } catch (err) {
    next(err);
  }
};

/** PATCH /api/applications/:id   body: { status }  -> also pushes an "Application update" alert. */
exports.updateApplicationStatus = async (req, res, next) => {
  try {
    const id = Number(req.params.id);
    const status = String((req.body && req.body.status) || '').trim();
    if (!Number.isInteger(id) || id <= 0) return res.status(400).json({ error: 'Invalid application id' });
    if (!status || status.length > MAX_STATUS_LENGTH) {
      return res.status(400).json({ error: `status must be 1-${MAX_STATUS_LENGTH} characters` });
    }

    const updated = await applicationStore.updateStatus(req.user.uid, id, status);
    if (!updated) return res.status(404).json({ error: 'Application not found' });
    res.json(updated);

    notificationService.notifyApplicationStatus(req.user.uid, updated);
  } catch (err) {
    next(err);
  }
};

/** DELETE /api/applications/:id */
exports.deleteApplication = async (req, res, next) => {
  try {
    const id = Number(req.params.id);
    if (!Number.isInteger(id) || id <= 0) return res.status(400).json({ error: 'Invalid application id' });

    const removed = await applicationStore.remove(req.user.uid, id);
    if (!removed) return res.status(404).json({ error: 'Application not found' });
    res.json({ deleted: true });
  } catch (err) {
    next(err);
  }
};
