package br.com.maqpro.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  @Bean
  BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService users(
      @Value("${app.admin-username}") String username,
      @Value("${app.admin-password}") String password) {
    if (password.length() < 12)
      throw new IllegalArgumentException("ADMIN_PASSWORD deve ter pelo menos 12 caracteres");
    return new InMemoryUserDetailsManager(
        User.withUsername(username)
            .password(new BCryptPasswordEncoder().encode(password))
            .roles("ADMIN")
            .build());
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        HttpMethod.GET, "/api/equipment/**", "/api/config", "/api/auth/csrf")
                    .permitAll()
                    .requestMatchers("/api/auth/login", "/error")
                    .permitAll()
                    .anyRequest()
                    .hasRole("ADMIN"))
        .formLogin(
            f ->
                f.loginProcessingUrl("/api/auth/login")
                    .successHandler((q, s, a) -> s.setStatus(204))
                    .failureHandler((q, s, e) -> s.sendError(401, "Credenciais inválidas")))
        .logout(
            l ->
                l.logoutUrl("/api/auth/logout")
                    .logoutSuccessHandler((q, s, a) -> s.setStatus(204))
                    .deleteCookies("JSESSIONID"))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint((q, s, x) -> s.sendError(401))
                    .accessDeniedHandler((q, s, x) -> s.sendError(403)))
        .build();
  }
}
