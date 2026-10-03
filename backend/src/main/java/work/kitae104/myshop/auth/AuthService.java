package work.kitae104.myshop.auth;

import work.kitae104.myshop.auth.dto.LoginRequest;
import work.kitae104.myshop.auth.dto.SignupRequest;
import work.kitae104.myshop.auth.dto.TokenResponse;
import work.kitae104.myshop.common.ApiException;
import work.kitae104.myshop.security.JwtTokenProvider;
import work.kitae104.myshop.user.Role;
import work.kitae104.myshop.user.User;
import work.kitae104.myshop.user.UserRepository;
import work.kitae104.myshop.user.UserResponse;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public UserResponse signup(SignupRequest request) {
        String email = request.email().trim().toLowerCase();
        // BCrypt 는 72바이트까지만 처리합니다 (한글은 글자당 3바이트).
        if (request.password().getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "비밀번호가 너무 깁니다. 영문 72자, 한글 24자 이내로 입력해 주세요.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
        }
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .name(request.name().trim())
                .role(Role.USER)
                .build();
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));
        String token = tokenProvider.createAccessToken(user.getEmail(), user.getRole().name());
        return TokenResponse.bearer(token, tokenProvider.getValiditySeconds(), UserResponse.from(user));
    }
}
