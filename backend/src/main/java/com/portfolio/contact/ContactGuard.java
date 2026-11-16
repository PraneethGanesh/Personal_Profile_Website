package com.portfolio.contact;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Profile("api")
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ContactGuard extends OncePerRequestFilter {
    private record Window(long expires, int count) {}
    private final Map<String, Window> clients = new HashMap<>();
    private final int limit;
    public ContactGuard(@Value("${contact.rate-limit-per-minute}") int limit) {
        if (limit < 1) throw new IllegalArgumentException("Rate limit must be positive");
        this.limit = limit;
    }
    private synchronized boolean allow(String address) {
        long now = System.currentTimeMillis();
        clients.entrySet().removeIf(e -> e.getValue().expires() <= now);
        var window = clients.get(address);
        if (window == null) {
            if (clients.size() >= 10000) return false;
            window = new Window(now + 60000, 0);
        }
        if (window.count() >= limit) return false;
        clients.put(address, new Window(window.expires(), window.count() + 1));
        return true;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!request.getRequestURI().equals(request.getContextPath() + "/api/contact") || !request.getMethod().equals("POST")) {
            chain.doFilter(request, response); return;
        }
        // Do not trust client-supplied X-Forwarded-For. See README before adding a reverse proxy.
        if (!allow(request.getRemoteAddr())) {
            response.setHeader("Retry-After", "60"); response.sendError(429); return;
        }
        byte[] body = request.getInputStream().readNBytes(32769);
        if (body.length > 32768) { response.sendError(413); return; }
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override public ServletInputStream getInputStream() {
                var input = new ByteArrayInputStream(body);
                return new ServletInputStream() {
                    @Override public int read() { return input.read(); }
                    @Override public int read(byte[] b, int off, int len) { return input.read(b, off, len); }
                    @Override public boolean isFinished() { return input.available() == 0; }
                    @Override public boolean isReady() { return true; }
                    @Override public void setReadListener(ReadListener listener) { throw new UnsupportedOperationException(); }
                };
            }
        }, response);
    }
}
