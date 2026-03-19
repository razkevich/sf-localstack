export interface RequestLogEntry {
  id: string;
  timestamp: string;
  method: string;
  path: string;
  statusCode: number;
  durationMs: number;
  requestBody: string;
  responseBody: string;
}
