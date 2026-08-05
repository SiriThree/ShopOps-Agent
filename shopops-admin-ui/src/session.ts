import type { CurrentUser } from "./types";

export const TOKEN_KEY = "shopops.auth.token";
export const USER_KEY = "shopops.auth.user";
export const SHOP_CONTEXT_KEY = "shopops.context.shopId";
export const CONTEXT_CHANGED_EVENT = "shopops:context-changed";

export function readCurrentUser(): CurrentUser | null {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || "null") as CurrentUser | null;
  } catch {
    return null;
  }
}

export function currentPermissions(): Set<string> {
  return new Set(readCurrentUser()?.permissions || []);
}

export function hasPermission(permission?: string): boolean {
  if (!permission) return true;
  const user = readCurrentUser();
  if (!user) return false;
  if ((user.roles || []).some((role) => role === "ADMIN" || role === "TENANT_ADMIN")) return true;
  return (user.permissions || []).includes(permission);
}

export function switchShop(shopId: string): void {
  const user = readCurrentUser();
  const accessible = (user?.accessibleShopIds || []).map(String);
  if (accessible.length && !accessible.includes(String(shopId))) {
    throw new Error("当前用户无权切换到该店铺");
  }
  localStorage.setItem(SHOP_CONTEXT_KEY, String(shopId));
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify({ ...user, shopId: String(shopId) }));
  }
  sessionStorage.clear();
  window.dispatchEvent(new CustomEvent(CONTEXT_CHANGED_EVENT, { detail: { shopId: String(shopId) } }));
}

export function clearSession(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  localStorage.removeItem(SHOP_CONTEXT_KEY);
  sessionStorage.clear();
}
