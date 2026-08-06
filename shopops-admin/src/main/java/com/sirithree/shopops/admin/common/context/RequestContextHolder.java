package com.sirithree.shopops.admin.common.context;

public final class RequestContextHolder {
    private static final ThreadLocal<RequestContext> HOLDER = new ThreadLocal<>();

    private RequestContextHolder() {
    }

    public static void set(RequestContext context) {
        HOLDER.set(context);
    }

    public static RequestContext current() {
        RequestContext context = HOLDER.get();
        if (context == null) {
            throw new IllegalStateException("请求上下文未初始化");
        }
        return context;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
