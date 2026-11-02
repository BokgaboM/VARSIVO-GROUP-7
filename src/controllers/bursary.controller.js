const bursaryStore = require('../services/bursaryStore');

exports.getBursaries = async (req, res, next) => {
  try {
    const bursaries = await bursaryStore.getAll();
    res.json(bursaries);
  } catch (err) {
    next(err);
  }
};