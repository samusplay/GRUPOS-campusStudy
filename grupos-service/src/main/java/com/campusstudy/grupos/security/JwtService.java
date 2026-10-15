package com.campusstudy.grupos.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
//clase para sacar losc almis y obtener el  usuario id
//validamos si la firma es valida y que contentaga los campos
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        // Mismo secreto y misma forma que users-auth (bytes UTF-8) → misma llave HS256
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Valida firma y expiración, y devuelve el usuario_id del claim "sub".
     * Lanza JwtException si el token es inválido o expiró,
     * y NumberFormatException si el sub no es un número.
     */
    public Long extraerUsuarioId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.parseLong(claims.getSubject());
    }
}