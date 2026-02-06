import { test, expect } from '@playwright/test';

test.describe('Critical Flow', () => {
    test('should navigate to devices and view detail', async ({ page }) => {
        // 1. Mock API
        await page.route('*/**/api/devices', async route => {
            await route.fulfill({
                json: [
                    {
                        device: { deviceId: 'TEST-001', model: 'Bot-V1', remark: 'Test Bot' },
                        session: { connected: true, connectedAt: Date.now() }
                    }
                ]
            });
        });

        await page.route('*/**/api/devices/TEST-001', async route => {
            await route.fulfill({
                json: {
                    device: { deviceId: 'TEST-001', model: 'Bot-V1', remark: 'Test Bot' },
                    session: { connected: true, connectedAt: Date.now() },
                    telemetry: { ts: Date.now(), payload: { battery: 80, temp: 25 } }
                }
            });
        });

        // 2. Visit Home
        await page.goto('/');

        // Check if redirected to dashboard or shows dashboard content
        // Assuming root redirects or dashboard is home
        // Our router has dashboard at /
        await expect(page).toHaveURL('/');

        // 3. Navigate to Devices
        await page.getByRole('link', { name: '设备' }).click();
        await expect(page).toHaveURL('/devices');

        // Check list content
        await expect(page.getByText('TEST-001')).toBeVisible();
        await expect(page.getByText('Test Bot')).toBeVisible();

        // 4. Click detail button
        await page.getByRole('button', { name: '详情' }).first().click();

        // 5. Verify Detail Page
        await expect(page).toHaveURL(/\/devices\/TEST-001/);
        await expect(page.getByText('基础信息')).toBeVisible();
        await expect(page.getByText('Bot-V1')).toBeVisible();

        // Check telemetry
        await expect(page.getByText('最新遥测数据')).toBeVisible();
    });
});
