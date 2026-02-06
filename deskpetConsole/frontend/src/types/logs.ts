export type LogLevel = 'info' | 'warning' | 'error'

export interface LogEntry {
  id: string
  timestamp: string
  level: LogLevel
  module: string
  message: string
}

export interface AlertEntry {
  id: string
  timestamp: string
  level: 'warning' | 'critical'
  source: string
  message: string
  status: 'active' | 'resolved'
}
