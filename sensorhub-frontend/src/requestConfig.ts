import type { RequestConfig } from '@umijs/max';

interface ResponseStructure {
  code?: number;
  data?: unknown;
  message?: string;
}

export const requestConfig: RequestConfig = {
  baseURL: process.env.NODE_ENV === 'development' ? 'http://localhost:8223' : '',
  withCredentials: true,
  responseInterceptors: [
    (response) => {
      const payload = response.data as ResponseStructure;
      if (typeof payload?.code === 'number' && payload.code !== 0) {
        throw new Error(payload.message || 'Request failed');
      }
      return response;
    },
  ],
};
