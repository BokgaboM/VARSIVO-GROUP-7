const institutionStore = require('../services/institutionStore');

exports.getInstitutions = async (req, res, next) => {
  try {
    const institutions = await institutionStore.getAll(req.query.aps);
    res.json(institutions);
  } catch (err) {
    next(err);
  }
};