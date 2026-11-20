package fr.turgot.servlet.employe;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

final class ServletTestSupport {
    private ServletTestSupport() {
    }

    static ServletContext servletContext() {
        return proxy(ServletContext.class, (method, arguments) -> {
            if (method.getName().equals("getRequestDispatcher")) {
                return proxy(RequestDispatcher.class, (dispatcherMethod, dispatcherArguments) -> null);
            }
            return null;
        });
    }

    static ServletConfig servletConfig(ServletContext servletContext) {
        return proxy(ServletConfig.class, (method, arguments) -> switch (method.getName()) {
            case "getServletContext" -> servletContext;
            case "getServletName" -> "test-servlet";
            case "getInitParameterNames" -> Collections.emptyEnumeration();
            default -> null;
        });
    }

    static HttpSession session(Map<String, Object> attributes) {
        return proxy(HttpSession.class, (method, arguments) -> switch (method.getName()) {
            case "getAttribute" -> attributes.get(arguments[0]);
            case "setAttribute" -> {
                attributes.put((String) arguments[0], arguments[1]);
                yield null;
            }
            case "removeAttribute" -> attributes.remove(arguments[0]);
            case "getAttributeNames" -> Collections.enumeration(attributes.keySet());
            case "getId" -> "test-session";
            default -> null;
        });
    }

    static HttpServletRequest request(
            Map<String, String> parameters,
            HttpSession session,
            ServletContext servletContext,
            String contextPath) {
        Map<String, Object> attributes = new HashMap<>();
        return proxy(HttpServletRequest.class, (method, arguments) -> switch (method.getName()) {
            case "getParameter" -> parameters.get(arguments[0]);
            case "getSession" -> session;
            case "getServletContext" -> servletContext;
            case "getContextPath" -> contextPath;
            case "setAttribute" -> {
                attributes.put((String) arguments[0], arguments[1]);
                yield null;
            }
            case "getAttribute" -> attributes.get(arguments[0]);
            case "removeAttribute" -> attributes.remove(arguments[0]);
            case "getAttributeNames" -> Collections.enumeration(attributes.keySet());
            default -> null;
        });
    }

    static HttpServletResponse response(ResponseState state) {
        return proxy(HttpServletResponse.class, (method, arguments) -> {
            switch (method.getName()) {
                case "sendError" -> {
                    state.errorStatus = (Integer) arguments[0];
                    state.errorMessage = arguments.length > 1 ? (String) arguments[1] : null;
                }
                case "sendRedirect" -> state.redirect = (String) arguments[0];
                case "setStatus" -> state.status = (Integer) arguments[0];
                default -> {
                }
            }
            return null;
        });
    }

    private static <T> T proxy(Class<T> type, ProxyInvocation invocation) {
        InvocationHandler handler = (proxy, method, arguments) -> {
            if (method.getDeclaringClass() == Object.class) {
                return objectMethod(proxy, method, arguments);
            }
            return invocation.invoke(method, arguments == null ? new Object[0] : arguments);
        };
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(), new Class<?>[] {type}, handler));
    }

    private static Object objectMethod(Object proxy, Method method, Object[] arguments) {
        return switch (method.getName()) {
            case "toString" -> proxy.getClass().getInterfaces()[0].getSimpleName() + " test proxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == arguments[0];
            default -> null;
        };
    }

    @FunctionalInterface
    private interface ProxyInvocation {
        Object invoke(Method method, Object[] arguments) throws Throwable;
    }

    static final class ResponseState {
        Integer errorStatus;
        String errorMessage;
        String redirect;
        Integer status;
    }
}
