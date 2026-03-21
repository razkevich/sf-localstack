package co.prodly.sflocalstack.interceptor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GzipRequestDecompressionFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String contentEncoding = request.getHeader("Content-Encoding");
        if ("gzip".equalsIgnoreCase(contentEncoding)) {
            chain.doFilter(new GzipRequestWrapper(request), response);
        } else {
            chain.doFilter(request, response);
        }
    }

    private static class GzipRequestWrapper extends HttpServletRequestWrapper {

        private final byte[] decompressedBody;

        GzipRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            try (GZIPInputStream gzis = new GZIPInputStream(request.getInputStream())) {
                decompressedBody = gzis.readAllBytes();
            }
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bais = new ByteArrayInputStream(decompressedBody);
            return new ServletInputStream() {
                @Override public int read() throws IOException { return bais.read(); }
                @Override public boolean isFinished() { return bais.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener l) {}
            };
        }

        @Override
        public String getHeader(String name) {
            if ("Content-Encoding".equalsIgnoreCase(name)) return null;
            if ("Content-Length".equalsIgnoreCase(name)) return String.valueOf(decompressedBody.length);
            return super.getHeader(name);
        }

        @Override
        public int getContentLength() { return decompressedBody.length; }

        @Override
        public long getContentLengthLong() { return decompressedBody.length; }
    }
}
