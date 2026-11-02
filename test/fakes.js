/**
 * In-memory stand-ins used by the automated tests, so they run anywhere (also on GitHub Actions)
 * without Azure SQL, Firestore or Firebase Cloud Messaging.
 */

// ---------- Firestore ----------
class FakeFirestore {
  constructor() { this.docs = new Map(); } // 'users/1/devices/abc' -> data

  collection(name) { return new FakeCollection(this, name, null); }

  collectionGroup(name) {
    return {
      get: async () => {
        const docs = [];
        for (const path of this.docs.keys()) {
          const parts = path.split('/');
          if (parts.length >= 2 && parts[parts.length - 2] === name) docs.push(this._snapshot(path));
        }
        return { docs, empty: docs.length === 0, size: docs.length };
      },
    };
  }

  _ref(path) {
    const parts = path.split('/');
    const id = parts[parts.length - 1];
    const parentPath = parts.slice(0, -1).join('/');
    const ownerPath = parts.length > 2 ? parts.slice(0, -2).join('/') : null;
    const parent = new FakeCollection(this, parentPath, ownerPath ? this._ref(ownerPath) : null);
    return new FakeDocRef(this, path, id, parent);
  }

  _snapshot(path) {
    const ref = this._ref(path);
    const exists = this.docs.has(path);
    return {
      id: ref.id,
      ref,
      exists,
      data: () => (exists ? JSON.parse(JSON.stringify(this.docs.get(path))) : undefined),
    };
  }

  pathsUnder(collectionPath) {
    const prefix = collectionPath + '/';
    return [...this.docs.keys()].filter((p) => p.startsWith(prefix) && !p.slice(prefix.length).includes('/'));
  }
}

class FakeDocRef {
  constructor(db, path, id, parent) { this.db = db; this.path = path; this.id = id; this.parent = parent; }
  collection(name) { return new FakeCollection(this.db, `${this.path}/${name}`, this); }
  async get() { return this.db._snapshot(this.path); }
  async set(data, options = {}) {
    const clean = JSON.parse(JSON.stringify(data));
    this.db.docs.set(this.path, options.merge ? { ...(this.db.docs.get(this.path) || {}), ...clean } : clean);
  }
  async create(data) {
    if (this.db.docs.has(this.path)) {
      throw Object.assign(new Error('6 ALREADY_EXISTS: Document already exists'), { code: 6 });
    }
    this.db.docs.set(this.path, JSON.parse(JSON.stringify(data)));
  }
  async delete() { this.db.docs.delete(this.path); }
}

class FakeCollection {
  constructor(db, path, parentDoc) { this.db = db; this.path = path; this.parent = parentDoc; this._order = null; this._limit = null; this._counter = 0; }
  doc(id) { return this.db._ref(`${this.path}/${id || `auto${Date.now()}${Math.random().toString(16).slice(2, 8)}`}`); }
  orderBy(field, direction = 'asc') { const c = new FakeCollection(this.db, this.path, this.parent); c._order = { field, direction }; c._limit = this._limit; return c; }
  limit(n) { const c = new FakeCollection(this.db, this.path, this.parent); c._order = this._order; c._limit = n; return c; }
  async get() {
    let docs = this.db.pathsUnder(this.path).map((p) => this.db._snapshot(p));
    if (this._order) {
      const { field, direction } = this._order;
      docs.sort((a, b) => (a.data()[field] < b.data()[field] ? -1 : a.data()[field] > b.data()[field] ? 1 : 0) * (direction === 'desc' ? -1 : 1));
    }
    if (this._limit) docs = docs.slice(0, this._limit);
    return { docs, empty: docs.length === 0, size: docs.length };
  }
}

// ---------- Firebase Cloud Messaging ----------
class FakeMessaging {
  constructor() { this.sent = []; this.failures = new Map(); } // token -> error code
  async sendEachForMulticast(message) {
    this.sent.push(message);
    return {
      responses: message.tokens.map((token) =>
        this.failures.has(token)
          ? { success: false, error: { code: this.failures.get(token) } }
          : { success: true, messageId: `m-${token.slice(0, 6)}` }
      ),
    };
  }
}

module.exports = { FakeFirestore, FakeMessaging };
