package com.hackathon.hackathon.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;


import java.util.Date;

public class JwtUtil {

    private static final String SECRET_KEY ="AintnowayoutofhereNowayoutofhereMmmhnoNowayoutofhereNowayoutofhereOhaintnowayoutofhereTheniggasongIneedthissongIdroppedtheEaddedtheAandkilledtheRTohealmyscarsDontsingalongUnlessyourpeoplehungfromtreesIhatethissongTillIdroppedtheEaddedtheAandIkilledtheROhaniggasongOhaniggasongUnlessyourpeoplehungfromtreesAndslavedtilldawn";

    public static String generateToken(
            String email,
            String role
    ) {

        return Jwts.builder()

                .subject(email)

                .claim("role", role)

                .issuedAt(new Date())

                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                + 86400000
                        )
                )

                .signWith(
                        io.jsonwebtoken.security.Keys
                                .hmacShaKeyFor(
                                        SECRET_KEY.getBytes()
                                )
                )

                .compact();
    }

    public static Claims extractClaims(
            String token
    ) {

        return Jwts.parser()

                .verifyWith(
                        io.jsonwebtoken.security.Keys
                                .hmacShaKeyFor(
                                        SECRET_KEY.getBytes()
                                )
                )

                .build()

                .parseSignedClaims(token)

                .getPayload();
    }
}