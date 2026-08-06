package com.dequeue.common.security;

import com.dequeue.common.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpServletResponse.SC_FORBIDDEN)
                .message("Forbidden: " + accessDeniedException.getMessage())
                .path(request.getServletPath())
                .timestamp(Instant.now())
                .build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
