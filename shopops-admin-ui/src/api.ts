import axios from "axios";

const TOKEN_KEY = "shopops.auth.token";
const USER_KEY = "shopops.auth.user";

export type RequestContext = {
  tenantId: string;
  shopId: string;
  userId: string;
  roles: string;
};

export function readStoredContext(): Partial<RequestContext> {
  try {
    const user = JSON.parse(localStorage.getItem(USER_KEY) || "null");
    if (!user) {
      return {};
    }
    return {
      tenantId: user.tenantId ? String(user.tenantId) : undefined,
      shopId: user.shopId ? String(user.shopId) : undefined,
      userId: user.userId ? String(user.userId) : undefined,
      roles: Array.isArray(user.roles) && user.roles.length ? user.roles.join(",") : undefined
    };
  } catch {
    return {};
  }
}

export const apiClient = axios.create({
  baseURL: "",
  timeout: 10000
});

export async function apiGet<T>(path: string, context: RequestContext): Promise<T> {
  const response = await apiClient.get(path, { headers: buildHeaders(context) });
  return unwrap<T>(response.data);
}

export async function apiPost<T>(path: string, payload: unknown, context: RequestContext): Promise<T> {
  const response = await apiClient.post(path, payload, { headers: buildHeaders(context) });
  return unwrap<T>(response.data);
}

export function buildHeaders(context: RequestContext) {
  const headers: Record<string, string> = {
    "X-Tenant-Id": context.tenantId || "1",
    "X-Shop-Id": context.shopId || "1",
    "X-User-Id": context.userId || "1",
    "X-User-Roles": context.roles || "ADMIN,OPERATOR"
  };
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return headers;
}

function unwrap<T>(body: { code?: number; message?: string; data?: T } | T): T {
  if (body && typeof body === "object" && "code" in body) {
    if (body.code && body.code !== 200) {
      throw new Error(body.message || `API ${body.code}`);
    }
    return body.data as T;
  }
  return body as T;
}
