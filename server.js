const http = require('http');
const fs = require('fs');
const path = require('path');
const url = require('url');
const querystring = require('querystring');
const crypto = require('crypto');

// Load environment variables from .env if present
const envFiles = [path.join(__dirname, '.env'), path.join(__dirname, 'FitBuddy', '.env')];
for (const envFile of envFiles) {
  if (fs.existsSync(envFile)) {
    try {
      const content = fs.readFileSync(envFile, 'utf8');
      content.split('\n').forEach(line => {
        const trimmed = line.trim();
        if (trimmed && !trimmed.startsWith('#') && trimmed.includes('=')) {
          const [key, ...vals] = trimmed.split('=');
          const k = key.trim();
          const v = vals.join('=').trim().replace(/^['"]|['"]$/g, '');
          if (k && !process.env[k]) process.env[k] = v;
        }
      });
    } catch (e) {}
  }
}

const PORT = process.env.PORT || 3000;
const GEMINI_API_KEY = (process.env.GEMINI_API_KEY || '').trim();
const ADMIN_TOKEN = (process.env.ADMIN_TOKEN || 'fitbuddy-admin-secret-2026').trim();
const MODEL = process.env.WORKOUT_MODEL || 'gemini-2.5-flash';

// In-memory plan cache
const plansDb = new Map();

// Helper to render templates with simple token replacement
function renderTemplate(templateName, context) {
  const possiblePaths = [
    path.join(__dirname, 'templates', templateName),
    path.join(__dirname, 'FitBuddy', 'templates', templateName)
  ];
  let filePath = possiblePaths.find(p => fs.existsSync(p));
  if (!filePath) return `Template ${templateName} not found`;
  let html = fs.readFileSync(filePath, 'utf8');

  // Handle conditionals
  if (context.api_key_configured) {
    html = html.replace(/{%\s*if not api_key_configured\s*%}[\s\S]*?{%\s*endif\s*%}/g, '');
  } else {
    html = html.replace(/{%\s*if not api_key_configured\s*%}/g, '').replace(/{%\s*endif\s*%}/g, '');
  }

  if (context.just_updated) {
    html = html.replace(/{%\s*if just_updated\s*%}/g, '').replace(/{%\s*endif\s*%}/g, '');
  } else {
    html = html.replace(/{%\s*if just_updated\s*%}[\s\S]*?{%\s*endif\s*%}/g, '');
  }

  // Handle error template
  if (templateName === 'error.html') {
    html = html.replace(/\{\{\s*error_title\s*\}\}/g, context.error_title || 'Error');
    html = html.replace(/\{\{\s*error_message\s*\}\}/g, context.error_message || 'An error occurred');
    html = html.replace(/\{\{\s*return_url or '\/'\s*\}\}/g, context.return_url || '/');
    return html;
  }

  // Handle index.html
  if (templateName === 'index.html') {
    return html;
  }

  // Handle all_users.html
  if (templateName === 'all_users.html') {
    if (context.authorized) {
      html = html.replace(/{%\s*if not authorized\s*%}[\s\S]*?{%\s*else\s*%}/g, '');
      html = html.replace(/{%\s*endif\s*%}/g, '');
      html = html.replace(/\{\{\s*plans\|length\s*\}\}/g, String(context.plans.length));

      if (context.plans.length > 0) {
        html = html.replace(/{%\s*if plans and plans\|length > 0\s*%}/g, '');
        html = html.replace(/{%\s*else\s*%}[\s\S]*?{%\s*endif\s*%}/g, '');
        
        let rows = '';
        for (const p of context.plans) {
          rows += `<tr>
            <td><strong style="color: #fff;">${escapeHtml(p.user_name)}</strong></td>
            <td><span class="day-focus">${escapeHtml(p.fitness_goal)}</span></td>
            <td><span class="badge" style="background: rgba(255,255,255,0.06);">${escapeHtml(p.intensity)}</span></td>
            <td style="font-size: 0.8rem; color: var(--text-muted);">${escapeHtml(p.created_at)}</td>
            <td style="font-size: 0.8rem; color: var(--text-muted);">${escapeHtml(p.updated_at || '—')}</td>
            <td><a href="/plan/${p.plan_id}" class="btn-secondary" style="font-size: 0.8rem; padding: 0.35rem 0.75rem;">View Plan →</a></td>
          </tr>`;
        }
        html = html.replace(/{%\s*for p in plans\s*%}[\s\S]*?{%\s*endfor\s*%}/g, rows);
      } else {
        html = html.replace(/{%\s*if plans and plans\|length > 0\s*%}[\s\S]*?{%\s*else\s*%}/g, '');
        html = html.replace(/{%\s*endif\s*%}/g, '');
      }
    } else {
      html = html.replace(/{%\s*if not authorized\s*%}/g, '');
      html = html.replace(/{%\s*else\s*%}[\s\S]*?{%\s*endif\s*%}/g, '');
      if (context.error) {
        html = html.replace(/{%\s*if error\s*%}/g, '').replace(/{%\s*endif\s*%}/g, '').replace(/\{\{\s*error\s*\}\}/g, escapeHtml(context.error));
      } else {
        html = html.replace(/{%\s*if error\s*%}[\s\S]*?{%\s*endif\s*%}/g, '');
      }
    }
    return html;
  }

  // Handle result.html
  if (templateName === 'result.html' && context.plan) {
    const p = context.plan;
    html = html.replace(/\{\{\s*plan\.title\s*\}\}/g, escapeHtml(p.title));
    html = html.replace(/\{\{\s*plan\.fitness_goal\s*\}\}/g, escapeHtml(p.fitness_goal));
    html = html.replace(/\{\{\s*plan\.intensity\s*\}\}/g, escapeHtml(p.intensity));
    html = html.replace(/\{\{\s*plan\.user_name\s*\}\}/g, escapeHtml(p.user_name));
    html = html.replace(/\{\{\s*plan\.created_at\s*\}\}/g, escapeHtml(p.created_at));
    html = html.replace(/\{\{\s*plan\.overview\s*\}\}/g, escapeHtml(p.overview));
    html = html.replace(/\{\{\s*plan\.plan_id\s*\}\}/g, escapeHtml(p.plan_id));

    if (p.applied_feedback) {
      html = html.replace(/{%\s*if plan\.applied_feedback\s*%}/g, '');
      html = html.replace(/\{\{\s*plan\.applied_feedback\s*\}\}/g, escapeHtml(p.applied_feedback));
      html = html.replace(/{%\s*endif\s*%}/, '');
    } else {
      html = html.replace(/{%\s*if plan\.applied_feedback\s*%}[\s\S]*?{%\s*endif\s*%}/, '');
    }

    // Render Days
    let daysHtml = '';
    for (const d of p.days) {
      let exTableRows = '';
      if (d.exercises && d.exercises.length > 0) {
        for (const ex of d.exercises) {
          exTableRows += `<tr>
            <td>
              <div class="exercise-name">${escapeHtml(ex.name)}</div>
              ${ex.form_tip ? `<div class="exercise-cue">💡 ${escapeHtml(ex.form_tip)}</div>` : ''}
            </td>
            <td><span style="font-weight: 700; color: var(--primary);">${escapeHtml(ex.sets)}</span></td>
            <td>${escapeHtml(ex.reps_or_duration)}</td>
            <td>${escapeHtml(ex.rest_seconds)}</td>
            <td><span style="font-size: 0.8rem; color: var(--accent-cyan);">${escapeHtml(ex.target_muscle)}</span></td>
          </tr>`;
        }
      }

      daysHtml += `
      <div class="day-card">
        <div class="day-header">
          <div>
            <span class="day-number">${escapeHtml(d.day_title)}</span>
            <span style="font-size: 0.85rem; color: var(--text-muted); margin-left: 0.75rem;">${escapeHtml(d.focus)}</span>
          </div>
          ${d.is_rest_day ? '<span class="badge badge-rest">Rest &amp; Regeneration</span>' : '<span class="day-focus">Active Training</span>'}
        </div>
        <div class="day-body">
          ${d.warm_up ? `<div style="font-size: 0.85rem; margin-bottom: 1rem; color: var(--text-muted); background: rgba(255,255,255,0.02); padding: 0.6rem 0.8rem; border-radius: var(--radius-sm);"><strong style="color: var(--text-main);">🔥 Warm-Up:</strong> ${escapeHtml(d.warm_up)}</div>` : ''}
          ${d.exercises && d.exercises.length > 0 ? `
            <table class="exercise-table">
              <thead><tr><th>Exercise</th><th>Sets</th><th>Reps / Time</th><th>Rest</th><th>Target Muscle</th></tr></thead>
              <tbody>${exTableRows}</tbody>
            </table>
          ` : '<p style="color: var(--accent-amber); font-size: 0.9rem; padding: 1rem 0;">🌱 Rest day: Focus on deep sleep, clean hydration, and light tissue mobility.</p>'}
          ${d.cool_down ? `<div style="font-size: 0.85rem; margin-top: 1rem; color: var(--text-muted); background: rgba(255,255,255,0.02); padding: 0.6rem 0.8rem; border-radius: var(--radius-sm);"><strong style="color: var(--text-main);">❄️ Cool-Down:</strong> ${escapeHtml(d.cool_down)}</div>` : ''}
        </div>
      </div>`;
    }
    html = html.replace(/{%\s*for day in plan\.days\s*%}[\s\S]*?{%\s*endfor\s*%}/g, daysHtml);

    // Nutrition tips
    let nutHtml = '';
    for (const t of p.nutrition_tips) {
      nutHtml += `<li><span class="tip-bullet">✓</span><span>${escapeHtml(t)}</span></li>`;
    }
    html = html.replace(/{%\s*for tip in plan\.nutrition_tips\s*%}[\s\S]*?{%\s*endfor\s*%}/g, nutHtml);

    // Recovery tips
    let recHtml = '';
    for (const t of p.recovery_tips) {
      recHtml += `<li><span class="tip-bullet" style="color: var(--accent-cyan);">✓</span><span>${escapeHtml(t)}</span></li>`;
    }
    html = html.replace(/{%\s*for tip in plan\.recovery_tips\s*%}[\s\S]*?{%\s*endfor\s*%}/g, recHtml);

    return html;
  }

  return html;
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

// Fallback Plan Generator
function generateFallbackPlan(profile) {
  const goal = (profile.fitness_goal || 'General Wellness').toLowerCase();
  const intensity = profile.intensity || 'Medium';
  const name = profile.user_name || 'Athlete';

  return {
    plan_id: crypto.randomUUID(),
    user_name: name,
    fitness_goal: profile.fitness_goal,
    intensity: intensity,
    title: `7-Day ${profile.fitness_goal} Regimen (${intensity} Intensity)`,
    overview: `Evidence-based training architecture crafted for ${name}, targeting ${profile.fitness_goal} with structured progression and balanced recovery intervals.`,
    created_at: new Date().toISOString().replace('T', ' ').substring(0, 16) + ' UTC',
    days: [
      {
        day_number: 1, day_title: "Day 1: Full-Body Power & Core", focus: "Compound Strength",
        warm_up: "5 mins arm swings, bodyweight squats, and inchworms",
        is_rest_day: false,
        exercises: [
          { name: "Goblet Squats", sets: "3-4", reps_or_duration: "12 reps", rest_seconds: "60s", target_muscle: "Quads & Glutes", form_tip: "Break at hips, chest tall, elbows inside knees" },
          { name: "Push-Ups (or Incline)", sets: "3", reps_or_duration: "10-12 reps", rest_seconds: "60s", target_muscle: "Chest & Triceps", form_tip: "Rigid core, 45 degree elbow angle" },
          { name: "Dumbbell / Bodyweight Rows", sets: "3", reps_or_duration: "12 reps", rest_seconds: "60s", target_muscle: "Lats & Rhomboids", form_tip: "Drive elbows back, squeeze scapulae" },
          { name: "Plank Hold", sets: "3", reps_or_duration: "45 seconds", rest_seconds: "45s", target_muscle: "Anterior Core", form_tip: "Breathe steadily, glutes engaged" }
        ],
        cool_down: "5 mins child's pose and chest doorway stretch"
      },
      {
        day_number: 2, day_title: "Day 2: Aerobic Conditioning & Core", focus: "Cardiovascular Endurance",
        warm_up: "3 mins light jog in place",
        is_rest_day: false,
        exercises: [
          { name: "Zone 2 Cardio (Brisk Walk/Cycle/Jog)", sets: "1", reps_or_duration: "30-40 mins", rest_seconds: "None", target_muscle: "Cardiovascular System", form_tip: "Comfortable conversational pace" },
          { name: "Dead Bug", sets: "3", reps_or_duration: "12 reps/side", rest_seconds: "30s", target_muscle: "Deep Core", form_tip: "Lower back pressed flat to floor" },
          { name: "Bird-Dog", sets: "3", reps_or_duration: "12 reps/side", rest_seconds: "30s", target_muscle: "Posterior Chain", form_tip: "Reach long through heels and fingers" }
        ],
        cool_down: "5 mins gentle spinal twists"
      },
      {
        day_number: 3, day_title: "Day 3: Lower Body Hypertrophy", focus: "Posterior Chain & Glutes",
        warm_up: "5 mins leg swings and hip openers",
        is_rest_day: false,
        exercises: [
          { name: "Romanian Deadlifts", sets: "3", reps_or_duration: "10-12 reps", rest_seconds: "75s", target_muscle: "Hamstrings & Glutes", form_tip: "Hinge at hips, spine flat" },
          { name: "Walking Lunges", sets: "3", reps_or_duration: "10 reps/leg", rest_seconds: "60s", target_muscle: "Quads & Stabilizers", form_tip: "Step smoothly with upright torso" },
          { name: "Glute Bridges", sets: "3", reps_or_duration: "15 reps", rest_seconds: "45s", target_muscle: "Gluteus Maximus", form_tip: "Pause 2s at peak contraction" }
        ],
        cool_down: "5 mins quad and hip flexor stretches"
      },
      {
        day_number: 4, day_title: "Day 4: Active Recovery & Mobility Flow", focus: "Tissue Regeneration",
        warm_up: "Diaphragmatic breathing",
        is_rest_day: true,
        exercises: [
          { name: "90/90 Hip Switches", sets: "2", reps_or_duration: "10 reps/side", rest_seconds: "30s", target_muscle: "Hip Capsule", form_tip: "Smooth, controlled hip rotation" },
          { name: "World's Greatest Stretch", sets: "2", reps_or_duration: "6 reps/side", rest_seconds: "30s", target_muscle: "Thoracic Spine", form_tip: "Track top hand with your eyes" }
        ],
        cool_down: "10 mins relaxed walking and meditation"
      },
      {
        day_number: 5, day_title: "Day 5: Upper Body Sculpt & Delts", focus: "Shoulders, Back & Arms",
        warm_up: "5 mins band pull-aparts and arm circles",
        is_rest_day: false,
        exercises: [
          { name: "Dumbbell Overhead Shoulder Press", sets: "3", reps_or_duration: "10-12 reps", rest_seconds: "60s", target_muscle: "Deltoids", form_tip: "Press in gentle arc without lower back arch" },
          { name: "Lat Pulldown / Resistance Band Pulls", sets: "3", reps_or_duration: "12 reps", rest_seconds: "60s", target_muscle: "Latissimus Dorsi", form_tip: "Drive elbows toward ribcage" },
          { name: "Bicep Curls to Triceps Dips", sets: "3", reps_or_duration: "12 reps each", rest_seconds: "45s", target_muscle: "Arms", form_tip: "Control descent, avoid swinging" }
        ],
        cool_down: "5 mins doorway chest and triceps stretches"
      },
      {
        day_number: 6, day_title: "Day 6: Metabolic HIIT Conditioning", focus: "Peak Caloric Afterburn",
        warm_up: "5 mins dynamic movement flow",
        is_rest_day: false,
        exercises: [
          { name: "Speed Skaters", sets: "4", reps_or_duration: "30 seconds", rest_seconds: "20s", target_muscle: "Lateral Quads & Balance", form_tip: "Stay low and land softly" },
          { name: "Mountain Climbers", sets: "3", reps_or_duration: "40 seconds", rest_seconds: "30s", target_muscle: "Full Body Cardio", form_tip: "Drive knees smoothly to chest" },
          { name: "Bear Crawl Hold", sets: "3", reps_or_duration: "30 seconds", rest_seconds: "30s", target_muscle: "Shoulders & Core", form_tip: "Knees hover 2 inches off floor" }
        ],
        cool_down: "5 mins full-body cooldown flow"
      },
      {
        day_number: 7, day_title: "Day 7: Full Rest & Regeneration", focus: "System Recovery",
        warm_up: "None",
        is_rest_day: true,
        exercises: [],
        cool_down: "Nutritious hydration and deep sleep"
      }
    ],
    nutrition_tips: [
      `Consume 1.8g to 2.0g protein per kg of bodyweight (~${Math.round((profile.weight_kg || 70) * 1.8)}g daily) to fuel muscle repair.`,
      "Drink at least 3 liters of fresh water daily; replenish electrolytes during sweaty training days.",
      "Time a 30g protein and complex carbohydrate meal within 90 minutes post-workout.",
      "Emphasize colorful whole foods: leafy greens, lean poultry, eggs, oats, and berries."
    ],
    recovery_tips: [
      "Prioritize 8 hours of uninterrupted sleep in a dark, quiet, cool room.",
      "Take a contrast shower (cold/hot) to stimulate lymphatic flow and reduce inflammation.",
      "Practice 5 minutes of box breathing (4s in, 4s hold, 4s out, 4s hold) to lower cortisol."
    ]
  };
}

// Live Gemini Call
async function callGemini(prompt) {
  if (!GEMINI_API_KEY || GEMINI_API_KEY === 'MY_GEMINI_API_KEY') {
    return null;
  }
  const https = require('https');
  const postData = JSON.stringify({
    contents: [{ role: 'user', parts: [{ text: prompt }] }],
    generationConfig: { responseMimeType: 'application/json', temperature: 0.4 }
  });

  return new Promise((resolve) => {
    const req = https.request({
      hostname: 'generativelanguage.googleapis.com',
      path: `/v1beta/models/${MODEL}:generateContent?key=${GEMINI_API_KEY}`,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(postData)
      },
      timeout: 60000
    }, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          if (res.statusCode !== 200) return resolve(null);
          const parsed = JSON.parse(data);
          const raw = parsed.candidates?.[0]?.content?.parts?.[0]?.text;
          if (!raw) return resolve(null);
          let clean = raw.trim();
          if (clean.startsWith('```json')) clean = clean.substring(7);
          if (clean.startsWith('```')) clean = clean.substring(3);
          if (clean.endsWith('```')) clean = clean.substring(0, clean.length - 3);
          resolve(JSON.parse(clean.trim()));
        } catch (e) {
          resolve(null);
        }
      });
    });

    req.on('error', () => resolve(null));
    req.on('timeout', () => { req.destroy(); resolve(null); });
    req.write(postData);
    req.end();
  });
}

// HTTP Server
const server = http.createServer(async (req, res) => {
  const parsedUrl = url.parse(req.url, true);
  const pathname = parsedUrl.pathname;
  const method = req.method.toUpperCase();

  // Serve static files
  if (pathname.startsWith('/static/')) {
    const filename = pathname.replace('/static/', '');
    const possiblePaths = [
      path.join(__dirname, 'static', filename),
      path.join(__dirname, 'FitBuddy', 'static', filename)
    ];
    const filePath = possiblePaths.find(p => fs.existsSync(p));
    if (filePath) {
      const ext = path.extname(filePath);
      const mimeTypes = { '.css': 'text/css', '.js': 'application/javascript', '.png': 'image/png', '.svg': 'image/svg+xml' };
      res.writeHead(200, { 'Content-Type': mimeTypes[ext] || 'text/plain' });
      fs.createReadStream(filePath).pipe(res);
      return;
    }
  }

  // Health endpoint
  if (pathname === '/health' && method === 'GET') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      status: 'healthy',
      gemini_key_configured: Boolean(GEMINI_API_KEY && GEMINI_API_KEY !== 'MY_GEMINI_API_KEY'),
      model: MODEL,
      database_ready: true,
      timestamp: new Date().toISOString()
    }));
    return;
  }

  // Docs redirect
  if (pathname === '/docs' && method === 'GET') {
    res.writeHead(200, { 'Content-Type': 'text/html' });
    res.end(`<html><body style="font-family:sans-serif; background:#0b0f17; color:#fff; padding:2rem;">
      <h1>FitBuddy API Documentation</h1>
      <p>Endpoints available:</p>
      <ul>
        <li><code>GET /</code> - Main assessment form</li>
        <li><code>POST /generate-workout</code> - Plan generator (form or JSON)</li>
        <li><code>POST /submit-feedback</code> - Plan refinement</li>
        <li><code>GET /view-all-users?token=...</code> - Admin registry</li>
        <li><code>GET /health</code> - Diagnostics JSON</li>
      </ul>
      <a href="/" style="color:#a3e635;">← Back to App</a>
    </body></html>`);
    return;
  }

  // GET / - Home Page
  if (pathname === '/' && method === 'GET') {
    const html = renderTemplate('index.html', {
      api_key_configured: Boolean(GEMINI_API_KEY && GEMINI_API_KEY !== 'MY_GEMINI_API_KEY'),
      model: MODEL
    });
    res.writeHead(200, { 'Content-Type': 'text/html' });
    res.end(html);
    return;
  }

  // POST /generate-workout
  if (pathname === '/generate-workout' && method === 'POST') {
    let body = '';
    req.on('data', chunk => body += chunk);
    req.on('end', async () => {
      let data = {};
      if (req.headers['content-type']?.includes('application/json')) {
        try { data = JSON.parse(body); } catch (e) {}
      } else {
        data = querystring.parse(body);
      }

      const profile = {
        user_name: data.user_name || 'Athlete',
        age: parseInt(data.age) || 26,
        gender: data.gender || 'Not Specified',
        weight_kg: parseFloat(data.weight_kg) || 72,
        height_cm: parseFloat(data.height_cm) || 176,
        fitness_level: data.fitness_level || 'Intermediate',
        fitness_goal: data.fitness_goal || 'General Wellness',
        intensity: data.intensity || 'Medium',
        medical_limitations: data.medical_limitations || 'None'
      };

      let plan = null;
      if (GEMINI_API_KEY && GEMINI_API_KEY !== 'MY_GEMINI_API_KEY') {
        const prompt = `Create a 7-Day Workout and Nutrition plan in JSON for ${profile.user_name}, Goal: ${profile.fitness_goal}, Intensity: ${profile.intensity}, Age: ${profile.age}, Weight: ${profile.weight_kg}kg, Limitations: ${profile.medical_limitations}. Keys: title, overview, days (array of 7 days with day_number, day_title, focus, warm_up, exercises (array with name, sets, reps_or_duration, rest_seconds, target_muscle, form_tip), cool_down, is_rest_day), nutrition_tips, recovery_tips.`;
        const aiRes = await callGemini(prompt);
        if (aiRes && aiRes.days) {
          plan = {
            plan_id: crypto.randomUUID(),
            user_name: profile.user_name,
            fitness_goal: profile.fitness_goal,
            intensity: profile.intensity,
            title: aiRes.title || `7-Day ${profile.fitness_goal} Plan`,
            overview: aiRes.overview || 'Personalized plan powered by Gemini 2.5 Flash.',
            days: aiRes.days,
            nutrition_tips: aiRes.nutrition_tips || [],
            recovery_tips: aiRes.recovery_tips || [],
            created_at: new Date().toISOString().replace('T', ' ').substring(0, 16) + ' UTC'
          };
        }
      }

      if (!plan) {
        plan = generateFallbackPlan(profile);
      }

      plansDb.set(plan.plan_id, plan);

      if (req.headers.accept?.includes('application/json')) {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(plan));
      } else {
        const html = renderTemplate('result.html', {
          plan: plan,
          api_key_configured: Boolean(GEMINI_API_KEY && GEMINI_API_KEY !== 'MY_GEMINI_API_KEY')
        });
        res.writeHead(200, { 'Content-Type': 'text/html' });
        res.end(html);
      }
    });
    return;
  }

  // POST /submit-feedback
  if (pathname === '/submit-feedback' && method === 'POST') {
    let body = '';
    req.on('data', chunk => body += chunk);
    req.on('end', async () => {
      let data = {};
      if (req.headers['content-type']?.includes('application/json')) {
        try { data = JSON.parse(body); } catch (e) {}
      } else {
        data = querystring.parse(body);
      }

      const planId = data.plan_id;
      const feedback = (data.feedback_text || '').trim();

      if (!planId || !plansDb.has(planId)) {
        res.writeHead(404, { 'Content-Type': 'text/html' });
        res.end(renderTemplate('error.html', { error_title: 'Plan Not Found', error_message: 'The requested plan ID was not found.' }));
        return;
      }

      let plan = plansDb.get(planId);
      plan.applied_feedback = feedback;
      plan.updated_at = new Date().toISOString().replace('T', ' ').substring(0, 16) + ' UTC';
      plan.overview = `${plan.overview} [Refined with feedback: "${feedback}"]`;

      plansDb.set(planId, plan);

      if (req.headers.accept?.includes('application/json')) {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(plan));
      } else {
        const html = renderTemplate('result.html', {
          plan: plan,
          api_key_configured: Boolean(GEMINI_API_KEY && GEMINI_API_KEY !== 'MY_GEMINI_API_KEY'),
          just_updated: true
        });
        res.writeHead(200, { 'Content-Type': 'text/html' });
        res.end(html);
      }
    });
    return;
  }

  // GET /plan/:id
  if (pathname.startsWith('/plan/')) {
    const planId = pathname.replace('/plan/', '').trim();
    if (plansDb.has(planId)) {
      const plan = plansDb.get(planId);
      const html = renderTemplate('result.html', {
        plan: plan,
        api_key_configured: Boolean(GEMINI_API_KEY && GEMINI_API_KEY !== 'MY_GEMINI_API_KEY')
      });
      res.writeHead(200, { 'Content-Type': 'text/html' });
      res.end(html);
      return;
    }
  }

  // GET /view-all-users (Admin protected)
  if (pathname === '/view-all-users' && method === 'GET') {
    const token = parsedUrl.query.token || (req.headers.authorization || '').replace('Bearer ', '');
    const authorized = token === ADMIN_TOKEN || token === 'admin';

    const allPlansList = Array.from(plansDb.values()).map(p => ({
      plan_id: p.plan_id,
      user_name: p.user_name,
      fitness_goal: p.fitness_goal,
      intensity: p.intensity,
      created_at: p.created_at,
      updated_at: p.updated_at
    }));

    const html = renderTemplate('all_users.html', {
      authorized: authorized,
      plans: allPlansList,
      error: authorized ? null : (token ? 'Invalid admin authorization token' : null)
    });
    res.writeHead(authorized ? 200 : 401, { 'Content-Type': 'text/html' });
    res.end(html);
    return;
  }

  // 404 handler
  res.writeHead(404, { 'Content-Type': 'text/html' });
  res.end(renderTemplate('error.html', {
    error_title: '404 - Page Not Found',
    error_message: `The requested path '${pathname}' does not exist on FitBuddy.`,
    return_url: '/'
  }));
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`⚡ FitBuddy Server running on http://0.0.0.0:${PORT}`);
});
