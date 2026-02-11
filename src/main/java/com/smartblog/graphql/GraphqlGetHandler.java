package com.smartblog.graphql;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Intercepts malformed or GET requests to /graphql and returns a friendly response
 * to avoid NoResourceFoundException spam in logs.
 */
@Component
public class GraphqlGetHandler extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/graphql") && "GET".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("GraphQL endpoint expects HTTP POST. Use POST /graphql with JSON body.");
            return;
        }
        // also catch some malformed requests that attempt to load static resource named graphql%20 etc.
        if (uri != null && uri.toLowerCase().contains("graphql" ) && "GET".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("GraphQL endpoint expects HTTP POST. Use POST /graphql with JSON body.");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
