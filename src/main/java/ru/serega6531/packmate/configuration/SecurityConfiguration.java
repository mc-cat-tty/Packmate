package ru.serega6531.packmate.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Value("${account-login}")
    private String login;

    @Value("${account-password}")
    private String password;

    private final PasswordEncoder passwordEncoder;

    @Autowired
    public SecurityConfiguration(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
                .withUser(login)
                .password(passwordEncoder.encode(password))
                .authorities("ROLE_USER");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf()
                .disable()
                .authorizeRequests()
                .antMatchers("/site.webmanifest")
                .permitAll()
                .anyRequest().authenticated()
                .and()
                .httpBasic()
                .and()
                .headers()
                .frameOptions()
                .sameOrigin();
    }

    @EventListener
    public void authenticationFailed(AuthenticationFailureBadCredentialsEvent e) {
        log.info("Login failed for user {}, password {}",
                e.getAuthentication().getPrincipal(), e.getAuthentication().getCredentials());
    }

}
