package com.upc.numerix.security.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

//Clase que se encargara de generar y validar los tokens JWT.
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiracion-minutos}")
    private long expiracionMinutos;

    // Duración del token cuando el usuario marca "Recordar contraseña"
    @Value("${jwt.expiracion-recordar-minutos:10080}")
    private long expiracionRecordarMinutos;

    private Key clave() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(clave()).build().parseClaimsJws(token).getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // El rol viaja en el token para que el frontend pueda mostrar el menú correspondiente
    public String generateToken(UserDetails userDetails, String rolUsuario, boolean recordarContrasena) {
        long ahora = System.currentTimeMillis();
        long minutos = recordarContrasena ? expiracionRecordarMinutos : expiracionMinutos;
        return Jwts.builder()
                .setClaims(Map.of("rol", rolUsuario))
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(ahora))
                .setExpiration(new Date(ahora + minutos * 60 * 1000))
                .signWith(clave(), SignatureAlgorithm.HS512)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
