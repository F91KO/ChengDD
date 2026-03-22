export interface ApiResponseEnvelope<T> {
  code: number;
  message: string;
  data: T;
  request_id?: string;
}

export function isApiResponseEnvelope(value: unknown): value is ApiResponseEnvelope<unknown> {
  if (!value || typeof value !== 'object') {
    return false;
  }

  const candidate = value as Partial<ApiResponseEnvelope<unknown>>;
  return (
    typeof candidate.code === 'number'
    && typeof candidate.message === 'string'
    && 'data' in candidate
  );
}

type ApiClientErrorOptions = {
  code?: number;
  status?: number;
  requestId?: string;
};

export class ApiClientError extends Error {
  readonly code?: number;
  readonly status?: number;
  readonly requestId?: string;

  constructor(message: string, options: ApiClientErrorOptions = {}) {
    super(message);
    this.name = 'ApiClientError';
    this.code = options.code;
    this.status = options.status;
    this.requestId = options.requestId;
  }
}

