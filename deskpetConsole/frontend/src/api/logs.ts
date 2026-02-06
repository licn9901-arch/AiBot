import type { LogEntry, AlertEntry, LogLevel } from '../types/logs'

const MOCK_LOGS: LogEntry[] = Array.from({ length: 50 }, (_, i) => ({
    id: `log-${i}`,
    timestamp: new Date(Date.now() - i * 60000).toISOString(),
    level: (i % 10 === 0 ? 'error' : i % 5 === 0 ? 'warning' : 'info') as LogLevel,
    module: ['System', 'Network', 'Device', 'Auth'][i % 4]!,
    message: `Log message example ${i} - detailed description of the event`
}))

const MOCK_ALERTS: AlertEntry[] = Array.from({ length: 10 }, (_, i) => ({
    id: `alert-${i}`,
    timestamp: new Date(Date.now() - i * 3600000).toISOString(),
    level: (i % 3 === 0 ? 'critical' : 'warning') as AlertEntry['level'],
    source: ['CPU', 'Memory', 'Disk', 'Network'][i % 4]!,
    message: `System alert ${i}: threshold exceeded`,
    status: i % 2 === 0 ? 'active' : 'resolved'
}))

export async function fetchLogs(params: { level?: string; search?: string } = {}): Promise<LogEntry[]> {
    // Simulate network delay
    await new Promise(resolve => setTimeout(resolve, 500))

    let logs = [...MOCK_LOGS]

    if (params.level) {
        logs = logs.filter(log => log.level === params.level)
    }

    if (params.search) {
        const search = params.search.toLowerCase()
        logs = logs.filter(log =>
            log.message.toLowerCase().includes(search) ||
            log.module.toLowerCase().includes(search)
        )
    }

    return logs
}

export async function fetchAlerts(): Promise<AlertEntry[]> {
    await new Promise(resolve => setTimeout(resolve, 500))
    return [...MOCK_ALERTS]
}
