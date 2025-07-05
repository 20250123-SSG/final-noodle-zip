package noodlezip.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;


@EnableWebSecurity
@Configuration
@Slf4j
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // URL 접근 제어( 인가 설정 )
        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers("/",
                                 "/check-login-id",
                                 "/check-email",
                                 "/verify-email",
                                 "/signup",
                                 "/login",
                                 "/images/**",
                                 "/css/**",
                                 "/img/**",
                                 "/js/**",
                                 "/assets/**",
                                 "/v3/api-docs/**",
                                 "/swagger-ui/**",
                                 "/send-verification-code",
                                 "/fragments/**",
                                 "/search/**",
                                 "/store/**"
                                  ).permitAll()
                    .requestMatchers("/admin/**").hasAnyAuthority("ADMIN")
                    .requestMatchers("/user/**").hasAnyAuthority("NORMAL")
                    .requestMatchers("/board/**", "/comments/**").permitAll()
                    .anyRequest().authenticated();
        });

        // 로그인 처리 설정( 인증 설정 ) => custom
        http.formLogin(form -> {
            form.loginPage("/") // custom loginPage URL
                    .loginProcessingUrl("/login") // login Process URL(POST)
                    .usernameParameter("userId") // 파라미터중 인증처리 사용 아이디
                    .passwordParameter("userPwd") // 파라미터중 인증처리 사용 비밀번호
                    .successHandler((req, resp, auth) -> {
                        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN"))) {
                            resp.sendRedirect("/admin/main"); // 응답 -> /admin/main
                        } else {
                            resp.sendRedirect("/"); // 응답 -> /main
                        }
                    })
                    .failureHandler((req, resp, exce) -> {
                        log.info("exception : {}", exce.getMessage());
                        resp.sendRedirect("/login?error");
                    });
        });

        // 로그아웃 처리 설정( 인증 설정 )
        http.logout(logout -> {
            logout.logoutUrl("/logout")
                    .logoutSuccessUrl("/login?logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID");
        });


        http.csrf(csrf -> {
            csrf.disable();
        });

        return http.build();

    }
}