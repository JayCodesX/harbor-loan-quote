import { expect, test } from '@playwright/test'

test('borrower demo loads and produces a quote', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByRole('heading', { name: /See an estimate in under a minute/i })).toBeVisible()
  await page.getByRole('button', { name: /See result/i }).click()

  await expect(page.getByRole('heading', { name: /Initial quote result/i })).toBeVisible()

  await page.waitForFunction(() => {
    const body = document.body.innerText
    return body.includes('Initial quote result') || body.includes('Payment breakdown')
  })
})

test('borrower demo redirects users to auth before personalization', async ({ page }) => {
  await page.goto('/')
  await page.getByRole('button', { name: /^Sign In$/i }).click()

  await expect(page.getByRole('heading', { name: /Save and refine your quote/i })).toBeVisible()
})
