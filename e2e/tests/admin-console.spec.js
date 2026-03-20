import { expect, test } from '@playwright/test'

test('admin console supports direct seeded login and summary access', async ({ page }) => {
  await page.goto('/admin/')

  await expect(page.getByRole('heading', { name: /Sign in to Mortgage Desk/i })).toBeVisible()
  await page.getByRole('button', { name: /Sign in as seeded admin/i }).click()

  await expect(page.getByRole('heading', { name: /Quote funnel/i })).toBeVisible()
  await expect(page.getByRole('heading', { name: /Daily quotes/i })).toBeVisible()
})

test('admin workspace is available after login', async ({ page }) => {
  await page.goto('/admin/')
  await page.getByRole('button', { name: /Sign in as seeded admin/i }).click()
  await expect(page.getByRole('heading', { name: /Quote funnel/i })).toBeVisible()

  await page.getByRole('button', { name: /^Workspace$/i }).click()
  await expect(page.getByRole('heading', { name: /Borrower workspace/i })).toBeVisible()
  await expect(page.getByRole('heading', { name: /Loan workspace/i })).toBeVisible()
})
