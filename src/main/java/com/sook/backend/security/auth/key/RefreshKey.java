package com.sook.backend.security.auth.key;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Component
@Qualifier("refreshKey")
@Slf4j
public class RefreshKey implements JwtKey {

	@Value("${app.auth.refreshTokenExpiry}")
	private Long refreshTokenDuration;

	private final Key key;
	private final JwtParser parser;

	public RefreshKey(@Value("${app.auth.refreshTokenSecret}") String refreshTokenSecret) {
		this.key = createKey(refreshTokenSecret);
		this.parser = Jwts.parserBuilder()
			.setSigningKey(key)
			.build();
	}

	Key createKey(String secret) {
		byte[] secretBytes = Base64.getEncoder()
			.encode(secret.getBytes());
		return Keys.hmacShaKeyFor(secretBytes);
	}

	@Override
	public String generateTokenWith(Claims claims) {
		Date now = new Date();
		return Jwts.builder()
			.setClaims(claims)
			.setIssuedAt(now)
			.setExpiration(new Date(now.getTime() + refreshTokenDuration))
			.signWith(key, SignatureAlgorithm.HS512)
			.compact();
	}

	@Override
	public Claims parse(String token) {
		return parser.parseClaimsJws(token)
			.getBody();
	}

	@Override
	public boolean validate(String token) {
		try {
			parse(token);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			log.warn(e.getMessage());
			return false;
		}
	}
}
