import axios, { AxiosError } from "axios";
import { clearSession, readCurrentUser, SHOP_CONTEXT_KEY, TOKEN_KEY } from "./session";

export type RequestContext = { tenantId: string; shopId: string; userId: string; roles: string };
export type ApiFailure = { status?: number; code?: number | string; message: string; requestId?: string };

export function readStoredContext(): Partial<RequestContext> {
  const user = readCurrentUser();
  if (!user) return {};
  const selectedShop = localStorage.getItem(SHOP_CONTEXT_KEY);
  return {
    tenantId: user.tenantId ? String(user.tenantId) : undefined,
    shopId: selectedShop || (user.shopId ? String(user.shopId) : undefined),
    userId: user.userId ? String(user.userId) : undefined,
    roles: Array.isArray(user.roles) && user.roles.length ? user.roles.join(",") : undefined
  };
}

export const apiClient = axios.create({ baseURL: "", timeout: 10000, withCredentials: false });

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      clearSession();
      if (!window.location.pathname.endsWith("/auth.html")) {
        window.location.assign(`/admin/auth.html?returnUrl=${encodeURIComponent(window.location.href)}`);
      }
    }
    return Promise.reject(normalizeApiError(error));
  }
);

export async function apiGet<T>(path: string, context: RequestContext): Promise<T> {
  const response = await apiClient.get(path, { headers: buildHeaders(context) });
  return unwrap<T>(response.data);
}
export async function apiPost<T>(path: string, payload: unknown, context: RequestContext): Promise<T> {
  const response = await apiClient.post(path, payload, { headers: buildHeaders(context) });
  return unwrap<T>(response.data);
}

export function buildHeaders(context: RequestContext) {
  const headers: Record<string, string> = {};
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    headers.Authorization = `Bearer ${token}`;
    if (context.shopId) headers["X-Shop-Id"] = context.shopId;
    if (context.roles) headers["X-User-Roles"] = context.roles;
    return headers;
  }
  // Header identity is retained only for the explicitly enabled dev profile.
  headers["X-Tenant-Id"] = context.tenantId || "1";
  headers["X-Shop-Id"] = context.shopId || "1";
  headers["X-User-Id"] = context.userId || "1";
  headers["X-User-Roles"] = context.roles || "ADMIN,OPERATOR";
  return headers;
}

function unwrap<T>(body: { code?: number; message?: string; data?: T; requestId?: string } | T): T {
  if (body && typeof body === "object" && "code" in body) {
    if (body.code && body.code !== 200) {
      throw { code: body.code, message: body.message || `API ${body.code}`, requestId: body.requestId } satisfies ApiFailure;
    }
    return body.data as T;
  }
  return body as T;
}

function normalizeApiError(error: AxiosError): ApiFailure {
  const body = error.response?.data as { code?: number | string; message?: string; requestId?: string } | undefined;
  return {
    status: error.response?.status,
    code: body?.code,
    message: body?.message || error.message || "请求失败",
    requestId: body?.requestId
  };
}
