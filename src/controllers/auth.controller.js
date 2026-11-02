const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { validationResult } = require('express-validator');
const userStore = require('../services/userStore');

function generateToken(user) {
  return jwt.sign(
    { uid: user.uid, email: user.email, role: user.role },
    process.env.JWT_SECRET,
    { expiresIn: process.env.JWT_EXPIRES_IN || '7d' }
  );
}

function publicUser(user) {
  return {
    uid: user.uid,
    email: user.email,
    fullName: user.fullName,
    preferredLanguage: user.preferredLanguage,
    role: user.role,
  };
}

/**
 * POST /api/auth/register
 */
exports.register = async (req, res, next) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ error: 'Validation failed', details: errors.array() });
    }

    const {
      email,
      password,
      fullName,
      schoolName = null,
      studentId = null,
      preferredLanguage = 'EN',
    } = req.body;

    // Duplicate check (await because findByEmail is async)
    const existing = await userStore.findByEmail(email);
    if (existing) {
      return res.status(409).json({ error: 'Email already registered' });
    }

    const passwordHash = await bcrypt.hash(password, 12);

    // No `uid` — Azure SQL IDENTITY generates the id automatically
    const user = {
      email: email.toLowerCase(),
      passwordHash,
      fullName,
      schoolName,
      studentId,
      preferredLanguage,
      role: 'high_school_learner',
    };

    const savedUser = await userStore.save(user);
    const token = generateToken(savedUser);

    res.status(201).json({
      message: 'Registration successful',
      token,
      user: publicUser(savedUser),
    });
  } catch (err) {
    next(err);
  }
};

/**
 * POST /api/auth/login
 */
exports.login = async (req, res, next) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ error: 'Validation failed', details: errors.array() });
    }

    const { email, password } = req.body;

    const row = await userStore.findByEmail(email);
    if (!row) {
      return res.status(401).json({ error: 'Invalid email or password' });
    }

    const isMatch = await bcrypt.compare(password, row.password_hash);
    if (!isMatch) {
      return res.status(401).json({ error: 'Invalid email or password' });
    }

    const user = userStore.mapRowToUser(row);
    const token = generateToken(user);

    res.json({
      message: 'Login successful',
      token,
      user: publicUser(user),
    });
  } catch (err) {
    next(err);
  }
};