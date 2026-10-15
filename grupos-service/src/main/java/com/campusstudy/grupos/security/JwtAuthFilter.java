package com.campusstudy.grupos.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
//lee el header y guarda el id
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String USUARIO_ID = "usuarioId";
    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    // Solo protege las rutas del servicio; el resto (ej. /actuator) pasa sin token
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/grupos");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith(BEARER)) {
            responderNoAutorizado(response);
            return;
        }

        Long usuarioId;
        try {
            usuarioId = jwtService.extraerUsuarioId(header.substring(BEARER.length()).trim());
        } catch (JwtException | IllegalArgumentException ex) {
            // Firma inválida, expirado, mal formado o sub no numérico
            responderNoAutorizado(response);
            return;
        }

        request.setAttribute(USUARIO_ID, usuarioId);
        filterChain.doFilter(request, response);
    }

    // Formato de error del contrato: { "error": "..." }
    private void responderNoAutorizado(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), Map.of("error", "No autorizado"));
    }
}