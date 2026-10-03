package work.kitae104.myshop.auth.dto;

import work.kitae104.myshop.user.UserResponse;

public record TokenResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {

    public static TokenResponse bearer(String accessToken, long expiresIn, UserResponse user) {
        return new TokenResponse(accessToken, "Bearer", expiresIn, user);
    }
}
