'use strict';

function buildBrowserCandidates(preferredBrowser) {
  const normalized = (preferredBrowser || '').toLowerCase();
  if (normalized === 'chrome') return ['chrome', 'chromium'];
  if (normalized === 'edge' || normalized === 'msedge') return ['edge', 'chromium'];
  if (normalized === 'chromium') return ['chromium'];

  // Auto strategy by platform: prefer enterprise-managed browsers first.
  if (process.platform === 'win32') return ['edge', 'chrome', 'chromium'];
  if (process.platform === 'darwin') return ['chrome', 'edge', 'chromium'];
  return ['chrome', 'edge', 'chromium'];
}

function browserLaunchOptions(candidate) {
  if (candidate === 'chrome') return { channel: 'chrome', headless: false };
  if (candidate === 'edge') return { channel: 'msedge', headless: false };
  return { headless: false };
}

async function launchBrowserWithFallback(playwright, preferredBrowser) {
  const { chromium } = playwright;
  const candidates = buildBrowserCandidates(preferredBrowser);
  let lastError = null;

  for (const candidate of candidates) {
    try {
      const browser = await chromium.launch(browserLaunchOptions(candidate));
      console.log(`[AutoToken] Browser selezionato: ${candidate}`);
      return browser;
    } catch (err) {
      lastError = err;
      const msg = err && err.message ? err.message : String(err);
      console.warn(`[AutoToken] Browser ${candidate} non disponibile: ${msg.split('\n')[0]}`);
    }
  }

  const finalMsg = lastError && lastError.message ? lastError.message : String(lastError);
  if (finalMsg.includes('Looks like Playwright was just installed or updated')) {
    throw new Error(
      'Nessun browser utilizzabile trovato. Installa il runtime Playwright con: npx playwright install chromium'
    );
  }

  throw new Error(`Impossibile avviare un browser supportato (chrome/edge/chromium): ${finalMsg}`);
}

async function fetchHelpdeskIdToken(options = {}) {
  const {
    helpdeskUrl = 'https://helpdesk.dev.notifichedigitali.it',
    timeoutMs = 180000,
    pollMs = 1500,
    browser = process.env.AUTO_TOKEN_BROWSER || 'auto',
    playwright = null,
  } = options;

  const playwrightInstance = playwright || require('playwright');
  const browserInstance = await launchBrowserWithFallback(playwrightInstance, browser);
  const context = await browserInstance.newContext();
  const page = await context.newPage();

  console.log(`[AutoToken] Apro il browser su ${helpdeskUrl}`);
  await page.goto(helpdeskUrl, { waitUntil: 'domcontentloaded' });
  console.log('[AutoToken] Completa il login SSO nella finestra del browser aperta...');

  const startedAt = Date.now();
  try {
    while ((Date.now() - startedAt) < timeoutMs) {
      if (page.isClosed()) {
        throw new Error('La finestra browser e stata chiusa prima di recuperare il token');
      }

      let token = null;
      try {
        token = await page.evaluate(() => {
          function isJwt(v) {
            return typeof v === 'string' && v.split('.').length === 3;
          }

          function extractToken(storage) {
            if (!storage) return null;
            const candidates = [];
            for (let i = 0; i < storage.length; i++) {
              const key = storage.key(i);
              if (!key) continue;
              if (key.includes('idToken') || key.endsWith('.idToken')) {
                const value = storage.getItem(key);
                if (isJwt(value)) candidates.push(value);
              }
            }
            return candidates.length ? candidates[0] : null;
          }

          return extractToken(window.localStorage) || extractToken(window.sessionStorage);
        });
      } catch (evalErr) {
        const msg = String(evalErr && evalErr.message ? evalErr.message : evalErr);
        const navigationInProgress =
          msg.includes('Execution context was destroyed') ||
          msg.includes('Cannot find context with specified id') ||
          msg.includes('Frame was detached');

        if (!navigationInProgress) {
          throw evalErr;
        }
      }

      if (token) {
        console.log('[AutoToken] idToken trovato, proseguo.');
        await browserInstance.close();
        return token;
      }

      try {
        await page.waitForLoadState('domcontentloaded', { timeout: pollMs });
      } catch (_) {
        await page.waitForTimeout(pollMs);
      }
    }

    throw new Error('Timeout: token non trovato entro il tempo massimo');
  } catch (err) {
    await browserInstance.close();
    throw err;
  }
}

module.exports = {
  fetchHelpdeskIdToken,
};
