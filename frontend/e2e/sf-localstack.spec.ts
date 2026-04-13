import { test, expect } from '@playwright/test'

function uniqueUser() {
  const id = Math.random().toString(36).substring(2, 10)
  return { username: `e2e_${id}`, email: `${id}@test.dev`, password: 'testpass123' }
}

async function registerAndLogin(baseURL: string) {
  const user = uniqueUser()
  const regRes = await fetch(`${baseURL}/api/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(user),
  })
  const regData = await regRes.json()
  return { ...user, accessToken: regData.accessToken, userId: regData.user?.id }
}

async function loginViaUI(page: any, username: string, password: string) {
  await page.goto('/')
  await page.locator('input[type="text"]').first().fill(username)
  await page.locator('input[type="password"]').fill(password)
  await page.getByRole('button', { name: 'Log In' }).click()
  await expect(page.getByText('Object Manager')).toBeVisible({ timeout: 10000 })
}

test.describe('Authentication', () => {
  test('shows login page when not authenticated', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByRole('button', { name: 'Log In' })).toBeVisible()
    await expect(page.locator('input[type="text"]').first()).toBeVisible()
  })

  test('can register a new account', async ({ page }) => {
    await page.goto('/')
    await page.getByRole('button', { name: 'Register' }).click()
    const user = uniqueUser()
    await page.locator('input[type="text"]').first().fill(user.username)
    await page.locator('input[type="email"]').fill(user.email)
    await page.locator('input[type="password"]').fill(user.password)
    await page.getByRole('button', { name: /register/i }).click()
    await expect(page.getByText('Object Manager')).toBeVisible({ timeout: 10000 })
  })

  test('can login with existing account', async ({ page, baseURL }) => {
    const user = await registerAndLogin(baseURL!)
    await loginViaUI(page, user.username, user.password)
  })

  test('shows error for invalid credentials', async ({ page }) => {
    await page.goto('/')
    await page.locator('input[type="text"]').first().fill('nonexistent')
    await page.locator('input[type="password"]').fill('wrong')
    await page.getByRole('button', { name: 'Log In' }).click()
    await expect(page.getByText(/invalid|failed/i)).toBeVisible({ timeout: 5000 })
  })
})

test.describe('Object Manager', () => {
  test.beforeEach(async ({ page, baseURL }) => {
    const user = await registerAndLogin(baseURL!)
    await loginViaUI(page, user.username, user.password)
  })

  test('shows all 8 standard objects', async ({ page }) => {
    for (const obj of ['Account', 'Contact', 'Lead', 'Opportunity', 'Case', 'User', 'Task', 'Event']) {
      await expect(page.locator(`button:has-text("${obj}")`).first()).toBeVisible()
    }
  })

  test('search filters objects', async ({ page }) => {
    await page.getByPlaceholder('Search objects...').fill('Account')
    await expect(page.locator('button:has-text("Account")').first()).toBeVisible()
    await expect(page.locator('button:has-text("Lead")')).not.toBeVisible()
  })

  test('can create custom object', async ({ page }) => {
    await page.getByRole('button', { name: /New Custom Object/i }).first().click()
    await page.locator('input[placeholder="e.g. Invoice"]').first().fill('Widget')
    await expect(page.locator('input[placeholder="e.g. Invoice__c"]').first()).toHaveValue('Widget__c')
    await page.getByRole('button', { name: 'Save' }).click()
    await page.waitForTimeout(2000)
    await expect(page.getByText('Widget__c')).toBeVisible({ timeout: 10000 })
  })
})

test.describe('Account CRUD', () => {
  test.beforeEach(async ({ page, baseURL }) => {
    const user = await registerAndLogin(baseURL!)
    await loginViaUI(page, user.username, user.password)
    await page.locator('button:has-text("Account")').first().click()
    await expect(page.locator('h1:has-text("Accounts"), [class*="header"]:has-text("Accounts")')).toBeVisible({ timeout: 5000 })
  })

  test('shows empty Account list for new user', async ({ page }) => {
    await expect(page.getByText('No Account records to display')).toBeVisible()
  })

  test('Account Name is first field in create form', async ({ page }) => {
    await page.getByRole('button', { name: 'New' }).click()
    await expect(page.getByText('New Account')).toBeVisible()
    const firstLabel = page.locator('form label').first()
    await expect(firstLabel).toContainText('Account Name')
  })

  test('can create Account record', async ({ page }) => {
    await page.getByRole('button', { name: 'New' }).click()
    await page.locator('form input[type="text"]').first().fill('Test Corp')
    await page.getByRole('button', { name: 'Save' }).click()
    await expect(page.getByText('Test Corp')).toBeVisible({ timeout: 10000 })
  })

  test('can click record to view detail', async ({ page }) => {
    await page.getByRole('button', { name: 'New' }).click()
    await page.locator('form input[type="text"]').first().fill('Detail Corp')
    await page.getByRole('button', { name: 'Save' }).click()
    await expect(page.locator('td:has-text("Detail Corp")').first()).toBeVisible({ timeout: 10000 })
    await page.locator('td:has-text("Detail Corp")').first().click()
    await page.waitForTimeout(1000)
    // Detail view should show the record name somewhere
    await expect(page.locator('text=Detail Corp').first()).toBeVisible()
  })
})

test.describe('Fields View', () => {
  test.beforeEach(async ({ page, baseURL }) => {
    const user = await registerAndLogin(baseURL!)
    await loginViaUI(page, user.username, user.password)
    await page.locator('button:has-text("Account")').first().click()
    await expect(page.locator('h1:has-text("Accounts"), [class*="header"]:has-text("Accounts")')).toBeVisible({ timeout: 5000 })
  })

  test('can switch to Fields view', async ({ page }) => {
    await page.getByRole('button', { name: 'Fields' }).click()
    await expect(page.getByText('48 fields')).toBeVisible()
  })

  test('Fields view shows field columns', async ({ page }) => {
    await page.getByRole('button', { name: 'Fields' }).click()
    await expect(page.getByText('API NAME')).toBeVisible()
    await expect(page.getByText('LABEL')).toBeVisible()
    await expect(page.getByText('AccountNumber')).toBeVisible()
  })

  test('can switch back to Records view', async ({ page }) => {
    await page.getByRole('button', { name: 'Fields' }).click()
    await expect(page.getByText('48 fields')).toBeVisible()
    await page.getByRole('button', { name: 'Records' }).click()
    await expect(page.getByText('0 records')).toBeVisible()
  })
})

test.describe('Navigation', () => {
  test.beforeEach(async ({ page, baseURL }) => {
    const user = await registerAndLogin(baseURL!)
    await loginViaUI(page, user.username, user.password)
  })

  test('can navigate to Metadata', async ({ page }) => {
    await page.locator('nav button:has-text("Metadata")').click()
    await expect(page.getByText('0 resources')).toBeVisible({ timeout: 5000 })
  })

  test('can navigate to Bulk Jobs', async ({ page }) => {
    await page.locator('nav button:has-text("Bulk Jobs")').click()
    await expect(page.getByText('0 jobs')).toBeVisible({ timeout: 5000 })
  })

  test('can navigate to API Log', async ({ page }) => {
    await page.locator('nav button:has-text("API Log")').click()
    await expect(page.locator('h1:has-text("API Log"), [class*="header"]:has-text("API Log")')).toBeVisible({ timeout: 5000 })
  })

  test('can navigate to Setup', async ({ page }) => {
    await page.locator('nav button:has-text("Setup")').click()
    await expect(page.getByText('Org Settings')).toBeVisible({ timeout: 5000 })
  })
})

test.describe('OAuth Web Login Page', () => {
  test('authorize endpoint renders login form', async ({ page, baseURL }) => {
    await registerAndLogin(baseURL!)
    await page.goto(`${baseURL}/services/oauth2/authorize?response_type=code&client_id=TestClient&redirect_uri=http://localhost:1717/OauthRedirect&state=e2etest`)
    await expect(page.locator('#username')).toBeVisible()
    await expect(page.locator('#password')).toBeVisible()
    await expect(page.locator('button[type="submit"]')).toBeVisible()
  })

  test('authorize with invalid creds shows error', async ({ page, baseURL }) => {
    await registerAndLogin(baseURL!)
    await page.goto(`${baseURL}/services/oauth2/authorize?response_type=code&client_id=TestClient&redirect_uri=http://localhost:1717/OauthRedirect&state=e2etest`)
    await page.locator('#username').fill('wronguser')
    await page.locator('#password').fill('wrongpass')
    await page.locator('button[type="submit"]').click()
    await expect(page.getByText('Invalid username or password')).toBeVisible()
  })
})

test.describe('Tenant Isolation (UI)', () => {
  test('two users see different data', async ({ browser, baseURL }) => {
    const userA = await registerAndLogin(baseURL!)
    const userB = await registerAndLogin(baseURL!)

    // Get OAuth tokens and create data
    const tokenARes = await fetch(`${baseURL}/services/oauth2/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: `grant_type=password&username=${userA.username}&password=${userA.password}`,
    })
    const tokenA = (await tokenARes.json()).access_token

    const tokenBRes = await fetch(`${baseURL}/services/oauth2/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: `grant_type=password&username=${userB.username}&password=${userB.password}`,
    })
    const tokenB = (await tokenBRes.json()).access_token

    await fetch(`${baseURL}/services/data/v60.0/sobjects/Account`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${tokenA}` },
      body: JSON.stringify({ Name: 'UserA Corp' }),
    })
    await fetch(`${baseURL}/services/data/v60.0/sobjects/Account`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${tokenB}` },
      body: JSON.stringify({ Name: 'UserB Corp' }),
    })

    // User A sees only their data
    const ctxA = await browser.newContext()
    const pgA = await ctxA.newPage()
    await loginViaUI(pgA, userA.username, userA.password)
    await pgA.locator('button:has-text("Account")').first().click()
    await expect(pgA.getByText('UserA Corp')).toBeVisible({ timeout: 5000 })
    await expect(pgA.getByText('UserB Corp')).not.toBeVisible()
    await ctxA.close()

    // User B sees only their data
    const ctxB = await browser.newContext()
    const pgB = await ctxB.newPage()
    await loginViaUI(pgB, userB.username, userB.password)
    await pgB.locator('button:has-text("Account")').first().click()
    await expect(pgB.getByText('UserB Corp')).toBeVisible({ timeout: 5000 })
    await expect(pgB.getByText('UserA Corp')).not.toBeVisible()
    await ctxB.close()
  })
})
