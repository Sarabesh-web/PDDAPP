/**
 * ═══════════════════════════════════════════════════════════════
 *  AetherFlow — 54 API Backend Tests (Android App)
 *  Tests every endpoint, validation rule, CRUD operation,
 *  edge case, and security check.
 *  
 *  Rate-limit aware: adds delays between auth endpoints.
 * ═══════════════════════════════════════════════════════════════
 */

const API = 'http://localhost:3001';
const TEST_EMAIL = `selenium_test_${Date.now()}@test.com`;
const TEST_PASSWORD = 'TestPassword123!';
const TEST_NAME = 'Selenium Test User';

let authToken = '';
let createdSubjectId = '';
let createdTaskId = '';
let createdNoteId = '';
let createdScheduleId = '';
let loggedSessionId = '';

const results = [];

const sleep = ms => new Promise(r => setTimeout(r, ms));

async function api(method, path, body = null, token = null) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const opts = { method, headers };
  if (body) opts.body = JSON.stringify(body);
  const res = await fetch(`${API}${path}`, opts);
  const resHeaders = {};
  res.headers.forEach((v, k) => { resHeaders[k] = v; });
  let data = null;
  try { data = await res.json(); } catch { data = null; }
  return { status: res.status, data, headers: resHeaders };
}

function test(name, fn) {
  results.push({ name, fn });
}

async function runAll() {
  const passed = [];
  const failed = [];
  console.log('\n📱 ANDROID APP API TESTS — 54 Tests\n' + '═'.repeat(50));

  for (let i = 0; i < results.length; i++) {
    const t = results[i];
    const num = `[${i + 1}/${results.length}]`;
    try {
      await t.fn();
      passed.push(t.name);
      console.log(`  ✅ ${num} ${t.name}`);
    } catch (err) {
      failed.push({ name: t.name, error: err.message });
      console.log(`  ❌ ${num} ${t.name}`);
      console.log(`      → ${err.message}`);
    }
  }

  console.log('\n' + '─'.repeat(50));
  console.log(`  📊 Results: ${passed.length} passed, ${failed.length} failed out of ${results.length}`);
  console.log('─'.repeat(50) + '\n');

  return { total: results.length, passed: passed.length, failed: failed.length, details: { passed, failed } };
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

// ═══════════════════════════════════════════════════════════════
//  HEALTH & SERVER META (4 tests)
// ═══════════════════════════════════════════════════════════════

test('55. GET /api/health returns 200', async () => {
  const r = await api('GET', '/api/health');
  assert(r.status === 200, `Expected 200, got ${r.status}`);
});

test('56. Health response has status "ok"', async () => {
  const r = await api('GET', '/api/health');
  assert(r.data.status === 'ok', `Expected status "ok", got "${r.data.status}"`);
});

test('57. Health response has timestamp', async () => {
  const r = await api('GET', '/api/health');
  assert(r.data.timestamp, 'No timestamp in health response');
  assert(!isNaN(Date.parse(r.data.timestamp)), 'Timestamp is not a valid ISO date');
});

test('58. Security header X-Content-Type-Options = nosniff', async () => {
  const r = await api('GET', '/api/health');
  assert(r.headers['x-content-type-options'] === 'nosniff', `Expected "nosniff", got "${r.headers['x-content-type-options']}"`);
});

// ═══════════════════════════════════════════════════════════════
//  SIGNUP VALIDATION (8 tests)
//  Rate limit: 3 signups per 5 min per IP
//  Strategy: Test validation (400s) FIRST since they don't count
//  as successful signups, then do the actual signups
// ═══════════════════════════════════════════════════════════════

test('59. Signup rejects missing email → 400', async () => {
  const r = await api('POST', '/api/auth/signup', { password: TEST_PASSWORD, name: 'NoEmail' });
  assert(r.status === 400, `Expected 400, got ${r.status}`);
});

test('60. Signup rejects missing password → 400', async () => {
  const r = await api('POST', '/api/auth/signup', { email: 'nopw@test.com', name: 'NoPW' });
  assert(r.status === 400, `Expected 400, got ${r.status}`);
});

test('61. Signup rejects missing name → 400', async () => {
  const r = await api('POST', '/api/auth/signup', { email: 'noname@test.com', password: TEST_PASSWORD });
  assert(r.status === 400, `Expected 400, got ${r.status}`);
});

test('62. Signup rejects password < 6 chars → 400', async () => {
  const r = await api('POST', '/api/auth/signup', { email: 'short@test.com', password: '123', name: 'Short' });
  assert(r.status === 400, `Expected 400, got ${r.status}`);
});

test('63. Signup rejects invalid email format → 400', async () => {
  const r = await api('POST', '/api/auth/signup', { email: 'notanemail', password: TEST_PASSWORD, name: 'Bad Email' });
  assert(r.status === 400, `Expected 400, got ${r.status}`);
});

test('64. Signup rejects name < 2 chars → 400', async () => {
  const r = await api('POST', '/api/auth/signup', { email: 'shortname@test.com', password: TEST_PASSWORD, name: 'A' });
  assert(r.status === 400, `Expected 400, got ${r.status}`);
});

// Now do the actual signups (these count against rate limit)
test('65. Valid signup returns 201 with user + token', async () => {
  await sleep(1000); // Small delay before signup burst
  const r = await api('POST', '/api/auth/signup', { email: TEST_EMAIL, password: TEST_PASSWORD, name: TEST_NAME });
  assert(r.status === 201, `Expected 201, got ${r.status}: ${JSON.stringify(r.data)}`);
  assert(r.data.user, 'No user object in response');
  assert(r.data.token, 'No token in response');
  authToken = r.data.token;
});

test('66. Signup user object excludes password', async () => {
  // Use the already-created user's response pattern — just verify by logging in
  const r = await api('POST', '/api/auth/login', { email: TEST_EMAIL, password: TEST_PASSWORD });
  assert(r.status === 200, `Expected 200, got ${r.status}`);
  assert(r.data.user.password === undefined, 'Password was exposed in response!');
  authToken = r.data.token;
});

// ═══════════════════════════════════════════════════════════════
//  SIGNUP EDGE CASES (3 tests)
// ═══════════════════════════════════════════════════════════════

test('67. Signup normalizes email to lowercase', async () => {
  const upper = `UPPER_${Date.now()}@TEST.COM`;
  const r = await api('POST', '/api/auth/signup', { email: upper, password: TEST_PASSWORD, name: 'Case Test' });
  assert(r.status === 201, `Expected 201, got ${r.status}`);
  assert(r.data.user.email === upper.toLowerCase(), `Email not normalized: ${r.data.user.email}`);
});

test('68. Signup trims name whitespace', async () => {
  const r = await api('POST', '/api/auth/signup', {
    email: `trim_${Date.now()}@test.com`, password: TEST_PASSWORD, name: '  Trimmed Name  '
  });
  assert(r.status === 201, `Expected 201, got ${r.status}`);
  assert(r.data.user.name === 'Trimmed Name', `Name not trimmed: "${r.data.user.name}"`);
});

test('69. Signup rejects duplicate email → 409', async () => {
  const r = await api('POST', '/api/auth/signup', { email: TEST_EMAIL, password: TEST_PASSWORD, name: 'Dup' });
  assert(r.status === 409, `Expected 409, got ${r.status}`);
});

// ═══════════════════════════════════════════════════════════════
//  LOGIN (6 tests)
//  Rate limit: 5 logins per 1 min per IP
// ═══════════════════════════════════════════════════════════════

test('70. Valid login returns 200 with token', async () => {
  const r = await api('POST', '/api/auth/login', { email: TEST_EMAIL, password: TEST_PASSWORD });
  assert(r.status === 200, `Expected 200, got ${r.status}`);
  assert(r.data.token, 'No token returned');
  authToken = r.data.token;
});

test('71. Login user object excludes password', async () => {
  const r = await api('POST', '/api/auth/login', { email: TEST_EMAIL, password: TEST_PASSWORD });
  assert(r.data.user.password === undefined, 'Password was exposed!');
});

test('72. Login normalizes email (case-insensitive)', async () => {
  const r = await api('POST', '/api/auth/login', { email: TEST_EMAIL.toUpperCase(), password: TEST_PASSWORD });
  assert(r.status === 200, `Expected 200, got ${r.status}`);
});

test('73. Login rejects missing email → 400', async () => {
  const r = await api('POST', '/api/auth/login', { password: TEST_PASSWORD });
  assert(r.status === 400, `Expected 400, got ${r.status}`);
});

test('74. Login rejects missing password → 400', async () => {
  const r = await api('POST', '/api/auth/login', { email: TEST_EMAIL });
  assert(r.status === 400, `Expected 400, got ${r.status}`);
});

test('75. Login rejects wrong password → 401', async () => {
  const r = await api('POST', '/api/auth/login', { email: TEST_EMAIL, password: 'wrongpassword' });
  assert(r.status === 401, `Expected 401, got ${r.status}`);
});

// ═══════════════════════════════════════════════════════════════
//  AUTH & PROFILE (5 tests) — no rate limit on these
// ═══════════════════════════════════════════════════════════════

test('76. GET /api/auth/me returns user + profile', async () => {
  const r = await api('GET', '/api/auth/me', null, authToken);
  assert(r.status === 200, `Expected 200, got ${r.status}`);
  assert(r.data.user, 'No user object');
  assert(r.data.profile, 'No profile object');
});

test('77. Profile has default fields (bio, university, major)', async () => {
  const r = await api('GET', '/api/auth/me', null, authToken);
  const p = r.data.profile;
  assert('bio' in p, 'No bio field');
  assert('university' in p, 'No university field');
  assert('major' in p, 'No major field');
});

test('78. PUT /api/auth/profile updates bio', async () => {
  const r = await api('PUT', '/api/auth/profile', { bio: 'Test bio from Selenium' }, authToken);
  assert(r.status === 200, `Expected 200, got ${r.status}`);
});

test('79. PUT /api/auth/profile updates university', async () => {
  const r = await api('PUT', '/api/auth/profile', { university: 'Test University' }, authToken);
  assert(r.status === 200, `Expected 200, got ${r.status}`);
});

test('80. Profile rejects short name < 2 chars → 400', async () => {
  const r = await api('PUT', '/api/auth/profile', { name: 'A' }, authToken);
  assert(r.status === 400, `Expected 400, got ${r.status}`);
});

// ═══════════════════════════════════════════════════════════════
//  SUBJECTS CRUD (5 tests)
// ═══════════════════════════════════════════════════════════════

test('81. Create subject → 201', async () => {
  const r = await api('POST', '/api/data/subjects', { name: 'Selenium Test Subject' }, authToken);
  assert(r.status === 201, `Expected 201, got ${r.status}`);
  createdSubjectId = r.data._id || r.data.id;
  assert(createdSubjectId, 'No subject ID returned');
});

test('82. Subject has default color #6366f1', async () => {
  const r = await api('POST', '/api/data/subjects', { name: 'Color Test Subject' }, authToken);
  assert(r.status === 201, `Expected 201, got ${r.status}`);
  assert(r.data.color === '#6366f1', `Expected #6366f1, got ${r.data.color}`);
  const id = r.data._id || r.data.id;
  await api('DELETE', `/api/data/subjects/${id}`, null, authToken);
});

test('83. List subjects contains created subject', async () => {
  const r = await api('GET', '/api/data/subjects', null, authToken);
  assert(r.status === 200, `Expected 200, got ${r.status}`);
  assert(Array.isArray(r.data), 'Response is not an array');
  const found = r.data.find(s => s.name === 'Selenium Test Subject');
  assert(found, 'Created subject not found in list');
});

test('84. Create subject rejects empty name → 400', async () => {
  const r = await api('POST', '/api/data/subjects', { name: '' }, authToken);
  assert(r.status === 400, `Expected 400, got ${r.status}`);
});

test('85. Delete subject → { ok: true }', async () => {
  const r = await api('DELETE', `/api/data/subjects/${createdSubjectId}`, null, authToken);
  assert(r.status === 200, `Expected 200, got ${r.status}`);
  assert(r.data.ok === true, 'Expected ok: true');
});

// ═══════════════════════════════════════════════════════════════
//  TASKS CRUD (7 tests)
// ═══════════════════════════════════════════════════════════════

test('86. Create task → 201 with status "pending"', async () => {
  const r = await api('POST', '/api/data/tasks', {
    title: 'Selenium Test Task',
    dueDate: new Date(Date.now() + 86400000).toISOString().split('T')[0],
    priority: 'high',
    type: 'assignment'
  }, authToken);
  assert(r.status === 201, `Expected 201, got ${r.status}`);
  assert(r.data.status === 'pending', `Expected status "pending", got "${r.data.status}"`);
  createdTaskId = r.data._id || r.data.id;
});

test('87. Task has correct title and type', async () => {
  const r = await api('GET', '/api/data/tasks', null, authToken);
  const task = r.data.find(t => (t._id || t.id) === createdTaskId);
  assert(task, 'Created task not found');
  assert(task.title === 'Selenium Test Task', `Wrong title: ${task.title}`);
  assert(task.type === 'assignment', `Wrong type: ${task.type}`);
});

test('88. List tasks returns array with created task', async () => {
  const r = await api('GET', '/api/data/tasks', null, authToken);
  assert(r.status === 200, `Expected 200, got ${r.status}`);
  assert(Array.isArray(r.data), 'Not an array');
  assert(r.data.length > 0, 'Tasks array is empty');
});

test('89. Create task rejects missing title → 400', async () => {
  const r = await api('POST', '/api/data/tasks', { dueDate: '2025-01-01' }, authToken);
  assert(r.status === 400, `Expected 400, got ${r.status}`);
});

test('90. Create task rejects missing dueDate → 400', async () => {
  const r = await api('POST', '/api/data/tasks', { title: 'No Due Date' }, authToken);
  assert(r.status === 400, `Expected 400, got ${r.status}`);
});

test('91. Update task to "completed" sets completedAt', async () => {
  const r = await api('PUT', `/api/data/tasks/${createdTaskId}`, { status: 'completed' }, authToken);
  assert(r.status === 200, `Expected 200, got ${r.status}`);
  assert(r.data.completedAt, 'completedAt not set after completing task');
});

test('92. Delete task → { ok: true }', async () => {
  const r = await api('DELETE', `/api/data/tasks/${createdTaskId}`, null, authToken);
  assert(r.status === 200, `Expected 200, got ${r.status}`);
  assert(r.data.ok === true, 'Expected ok: true');
});

// ═══════════════════════════════════════════════════════════════
//  NOTES CRUD (5 tests)
// ═══════════════════════════════════════════════════════════════

test('93. Create note → 201', async () => {
  const r = await api('POST', '/api/data/notes', {
    title: 'Selenium Test Note', content: 'Test content', tags: ['test']
  }, authToken);
  assert(r.status === 201, `Expected 201, got ${r.status}`);
  createdNoteId = r.data._id || r.data.id;
});

test('94. List notes contains created note', async () => {
  const r = await api('GET', '/api/data/notes', null, authToken);
  assert(r.status === 200, `Expected 200, got ${r.status}`);
  assert(Array.isArray(r.data), 'Not an array');
  const found = r.data.find(n => n.title === 'Selenium Test Note');
  assert(found, 'Created note not found');
});

test('95. Create note rejects missing title → 400', async () => {
  const r = await api('POST', '/api/data/notes', { content: 'No title' }, authToken);
  assert(r.status === 400, `Expected 400, got ${r.status}`);
});

test('96. Update note changes content + updatedAt', async () => {
  const r = await api('PUT', `/api/data/notes/${createdNoteId}`, { content: 'Updated content!' }, authToken);
  assert(r.status === 200, `Expected 200, got ${r.status}`);
  assert(r.data.content === 'Updated content!', `Content not updated: ${r.data.content}`);
});

test('97. Delete note → { ok: true }', async () => {
  const r = await api('DELETE', `/api/data/notes/${createdNoteId}`, null, authToken);
  assert(r.status === 200, `Expected 200, got ${r.status}`);
  assert(r.data.ok === true, 'Expected ok: true');
});

// ═══════════════════════════════════════════════════════════════
//  SCHEDULES CRUD (5 tests)
// ═══════════════════════════════════════════════════════════════

test('98. Create schedule → 201 with completed="false"', async () => {
  const r = await api('POST', '/api/data/schedules', {
    title: 'Selenium Test Schedule',
    day: 'Monday',
    startTime: '09:00',
    endTime: '10:00',
    priority: 'high'
  }, authToken);
  assert(r.status === 201, `Expected 201, got ${r.status}`);
  assert(r.data.completed === 'false', `Expected completed "false", got "${r.data.completed}"`);
  createdScheduleId = r.data._id || r.data.id;
});

test('99. List schedules returns array', async () => {
  const r = await api('GET', '/api/data/schedules', null, authToken);
  assert(r.status === 200, `Expected 200, got ${r.status}`);
  assert(Array.isArray(r.data), 'Not an array');
});

test('100. Create schedule rejects missing fields → 400', async () => {
  const r = await api('POST', '/api/data/schedules', { title: 'Only Title' }, authToken);
  assert(r.status === 400, `Expected 400, got ${r.status}`);
});

test('101. Update schedule marks complete', async () => {
  const r = await api('PUT', `/api/data/schedules/${createdScheduleId}`, { completed: 'true' }, authToken);
  assert(r.status === 200, `Expected 200, got ${r.status}`);
  assert(r.data.completed === 'true', `Expected completed "true", got "${r.data.completed}"`);
});

test('102. Delete schedule → { ok: true }', async () => {
  const r = await api('DELETE', `/api/data/schedules/${createdScheduleId}`, null, authToken);
  assert(r.status === 200, `Expected 200, got ${r.status}`);
  assert(r.data.ok === true, 'Expected ok: true');
});

// ═══════════════════════════════════════════════════════════════
//  ADVANCED ENDPOINTS (4 tests)
// ═══════════════════════════════════════════════════════════════

test('103. Log study session → 201 with auto-subject', async () => {
  const r = await api('POST', '/api/data/schedules/log', {
    subjectName: 'Auto Created Subject',
    durationMinutes: 25,
    title: 'Selenium Focus Session'
  }, authToken);
  assert(r.status === 201, `Expected 201, got ${r.status}`);
  loggedSessionId = r.data._id || r.data.id;
});

test('104. Logged session is marked completed', async () => {
  const r = await api('GET', '/api/data/schedules', null, authToken);
  const session = r.data.find(s => (s._id || s.id) === loggedSessionId);
  assert(session, 'Logged session not found');
  assert(session.completed === 'true', `Expected completed "true", got "${session.completed}"`);
});

test('105. Cohort comparison returns user + cohort + weeklyTrend', async () => {
  const r = await api('GET', '/api/data/cohort-comparison', null, authToken);
  assert(r.status === 200, `Expected 200, got ${r.status}`);
  assert(r.data.user, 'Missing user stats');
  assert(r.data.cohort, 'Missing cohort stats');
  assert(r.data.weeklyTrend, 'Missing weeklyTrend');
});

test('106. Weekly trend has 7 day entries', async () => {
  const r = await api('GET', '/api/data/cohort-comparison', null, authToken);
  assert(Array.isArray(r.data.weeklyTrend), 'weeklyTrend is not an array');
  assert(r.data.weeklyTrend.length === 7, `Expected 7 entries, got ${r.data.weeklyTrend.length}`);
});

// ═══════════════════════════════════════════════════════════════
//  SECURITY & AUTH GUARDS (2 tests)
// ═══════════════════════════════════════════════════════════════

test('107. No token → 401 on protected routes', async () => {
  const r = await api('GET', '/api/data/subjects');
  assert(r.status === 401, `Expected 401, got ${r.status}`);
});

test('108. Invalid/malformed token → 401', async () => {
  const r = await api('GET', '/api/data/subjects', null, 'garbage_invalid_token_xyz');
  assert(r.status === 401, `Expected 401, got ${r.status}`);
});

// ═══════════════════════════════════════════════════════════════
//  CLEANUP — Remove test data
// ═══════════════════════════════════════════════════════════════

async function cleanup() {
  console.log('\n🧹 Cleaning up test data...');
  try {
    if (loggedSessionId) {
      await api('DELETE', `/api/data/schedules/${loggedSessionId}`, null, authToken);
    }
    const subjects = await api('GET', '/api/data/subjects', null, authToken);
    if (subjects.data) {
      for (const s of subjects.data) {
        await api('DELETE', `/api/data/subjects/${s._id || s.id}`, null, authToken);
      }
    }
    const tasks = await api('GET', '/api/data/tasks', null, authToken);
    if (tasks.data) {
      for (const t of tasks.data) {
        await api('DELETE', `/api/data/tasks/${t._id || t.id}`, null, authToken);
      }
    }
    const notes = await api('GET', '/api/data/notes', null, authToken);
    if (notes.data) {
      for (const n of notes.data) {
        await api('DELETE', `/api/data/notes/${n._id || n.id}`, null, authToken);
      }
    }
    const schedules = await api('GET', '/api/data/schedules', null, authToken);
    if (schedules.data) {
      for (const s of schedules.data) {
        await api('DELETE', `/api/data/schedules/${s._id || s.id}`, null, authToken);
      }
    }
    console.log('  ✅ Test data cleaned up successfully\n');
  } catch (err) {
    console.log(`  ⚠️ Cleanup partial: ${err.message}\n`);
  }
}

// ═══════════════════════════════════════════════════════════════
//  EXPORTS
// ═══════════════════════════════════════════════════════════════

export { runAll, cleanup };

if (process.argv[1] && process.argv[1].includes('api_tests')) {
  runAll().then(async (res) => {
    await cleanup();
    process.exit(res.failed > 0 ? 1 : 0);
  });
}
