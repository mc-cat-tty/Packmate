package ru.serega6531.packmate;

import org.pcap4j.core.PcapNativeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import ru.serega6531.packmate.model.enums.CaptureMode;
import ru.serega6531.packmate.pcap.FilePcapWorker;
import ru.serega6531.packmate.pcap.LivePcapWorker;
import ru.serega6531.packmate.pcap.PcapWorker;
import ru.serega6531.packmate.service.ServicesService;
import ru.serega6531.packmate.service.StreamService;

import java.net.UnknownHostException;

@Configuration
@EnableWebSecurity
@EnableScheduling
@EnableWebSocket
public class ApplicationConfiguration extends WebSecurityConfigurerAdapter implements WebSocketConfigurer {

    @Value("${account-login}")
    private String login;

    @Value("${account-password}")
    private String password;

    private final WebSocketHandler webSocketHandler;

    @Autowired
    public ApplicationConfiguration(WebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @Bean(destroyMethod = "stop")
    @Autowired
    public PcapWorker pcapWorker(ServicesService servicesService,
                                 StreamService streamService,
                                 @Value("${local-ip}") String localIpString,
                                 @Value("${interface-name}") String interfaceName,
                                 @Value("${pcap-file}") String filename,
                                 @Value("${capture-mode}") CaptureMode captureMode) throws PcapNativeException, UnknownHostException {
        if(captureMode == CaptureMode.LIVE) {
            return new LivePcapWorker(servicesService, streamService, localIpString, interfaceName);
        } else {
            return new FilePcapWorker(servicesService, streamService, localIpString, filename);
        }
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
                .withUser(login)
                .password(passwordEncoder().encode(password))
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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/api/ws")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}
