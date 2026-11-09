package top.mqxu.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import top.mqxu.security.security.JsonAccessDeniedHandler;
import top.mqxu.security.security.JsonAuthenticationEntryPoint;
import top.mqxu.security.security.JwtAuthenticationFilter;
import top.mqxu.security.security.JwtService;
import top.mqxu.security.security.RequestTraceFilter;
import top.mqxu.security.sms.SmsCodeAuthenticationProvider;
import top.mqxu.security.sms.SmsCodeService;

/**
 * 五条过滤器链。
 * <p>
 * 匹配规则是"从上往下、命中即停"：@Order 小的先匹配，securityMatcher 命中就交给这条链处理。
 * 第 ⑤ 条没有 securityMatcher，兜住所有剩余路径，所以必须放最后（@Order(5) 数字最大）。
 * 把它挪到前面，前四条链永远匹配不到。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * 两个 Provider 各声明"我认哪种凭证"，authenticate() 时按 supports() 挑一个，顺序无所谓。
     * <p>
     * 容器里有两个 UserDetailsService，按类型注入会报 NoUniqueBeanDefinitionException，
     * 所以用 @Qualifier 指定 Bean 名。
     * <p>
     * 注意 new DaoAuthenticationProvider(dbUserDetailsService)——Security 7 把它改成了构造器参数，
     * 老教程里无参构造加 setterUserDetailsService 的写法已经不能用了。
     */
    @Bean
    public AuthenticationManager authenticationManager(
            @Qualifier("dbUserDetailsService") UserDetailsService dbUserDetailsService,
            @Qualifier("phoneUserDetailsService") UserDetailsService phoneUserDetailsService,
            PasswordEncoder passwordEncoder,
            SmsCodeService smsCodeService) {

        DaoAuthenticationProvider passwordProvider =
                new DaoAuthenticationProvider(dbUserDetailsService);
        passwordProvider.setPasswordEncoder(passwordEncoder);

        SmsCodeAuthenticationProvider smsProvider =
                new SmsCodeAuthenticationProvider(smsCodeService, phoneUserDetailsService);

        return new ProviderManager(passwordProvider, smsProvider);
    }

    /**
     * DelegatingPasswordEncoder：按密文的 {bcrypt} / {noop} 前缀选校验算法，
     * 同一个库里可以混着存不同算法的密文，迁移时不用一次性改完所有账号。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * 第一条：全拦截。anyRequest().authenticated() 是全部规则里最宽的一条，必须放最后——
     * authorizeHttpRequests 内部也是从上往下匹配，先写的规则先生效。
     */
    @Bean
    @org.springframework.core.annotation.Order(1)
    public SecurityFilterChain stage1FilterChain(HttpSecurity http,
                                                 AuthenticationManager authenticationManager) {
        http
                .securityMatcher("/stage1/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new JsonAuthenticationEntryPoint())
                        .accessDeniedHandler(new JsonAccessDeniedHandler()))
                .authenticationManager(authenticationManager);
        return http.build();
    }

    /**
     * 第二条：白名单。同一个路径，GET 放行、DELETE 不放行——
     * requestMatchers(HttpMethod.GET, ...) 把方法也纳入匹配条件，
     * 这是"读公开、写受保护"最直接的写法。
     */
    @Bean
    @org.springframework.core.annotation.Order(2)
    public SecurityFilterChain stage2FilterChain(HttpSecurity http,
                                                 AuthenticationManager authenticationManager) {
        http
                .securityMatcher("/stage2/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/stage2/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/stage2/articles/**").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new JsonAuthenticationEntryPoint())
                        .accessDeniedHandler(new JsonAccessDeniedHandler()))
                .authenticationManager(authenticationManager);
        return http.build();
    }

    /**
     * 第三条：角色与权限点。
     * <p>
     * hasRole 不要写 ROLE_ 前缀（框架自动补），hasAuthority 要写完整字符串。
     * 库里存的是 ADMIN，前缀在 SysRole.toAuthority() 里补成 ROLE_ADMIN。
     */
    @Bean
    @org.springframework.core.annotation.Order(3)
    public SecurityFilterChain stage3FilterChain(HttpSecurity http,
                                                 AuthenticationManager authenticationManager) {
        http
                .securityMatcher("/stage3/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/stage3/admin/**").hasRole("ADMIN")
                        .requestMatchers("/stage3/ops/**").hasAnyRole("ADMIN", "AUDITOR")
                        // hasAllRoles 是 Security 7 新增的；在这之前要写
                        // hasRole('ADMIN') and hasRole('AUDITOR')
                        .requestMatchers("/stage3/audit/**").hasAllRoles("ADMIN", "AUDITOR")
                        .requestMatchers(HttpMethod.DELETE, "/stage3/articles/**")
                        .hasAuthority("ARTICLE_DELETE")
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new JsonAuthenticationEntryPoint())
                        .accessDeniedHandler(new JsonAccessDeniedHandler()))
                .authenticationManager(authenticationManager);
        return http.build();
    }

    /**
     * 第四条：表单登录。传统 Web 那一套，有登录页、有 Session、有 CSRF token。
     * <p>
     * 四个坑：
     * <ol>
     *   <li>loginPage 与 loginProcessingUrl 不能同址，否则会死循环；</li>
     *   <li>loginProcessingUrl 与 logoutUrl 要放行；</li>
     *   <li>defaultSuccessUrl 第二个参数传 false，表示"总是跳这里"；</li>
     *   <li>CSRF 用不做掩码的 CsrfTokenRequestAttributeHandler，否则表单值和 Cookie 值对不上。</li>
     * </ol>
     */
    @Bean
    @org.springframework.core.annotation.Order(4)
    public SecurityFilterChain stage4FilterChain(HttpSecurity http,
                                                 AuthenticationManager authenticationManager) {
        http
                .securityMatcher("/stage4/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/stage4/login.html", "/stage4/login",
                                "/stage4/csrf", "/stage4/logout").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/stage4/login.html")
                        .loginProcessingUrl("/stage4/login")
                        .defaultSuccessUrl("/stage4/me", false)
                        .failureUrl("/stage4/login.html?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/stage4/logout")
                        .logoutSuccessUrl("/stage4/login.html?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .csrf(csrf -> csrf
                        // withHttpOnlyFalse：让页面里的 JS 读得到 XSRF-TOKEN cookie
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // 默认的 XorCsrfTokenRequestAttributeHandler 会做 XOR 掩码，
                        // 表单里拿到的值和 Cookie 里的不是同一个字符串，手工比对永远对不上
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new JsonAuthenticationEntryPoint())
                        .accessDeniedHandler(new JsonAccessDeniedHandler()))
                .authenticationManager(authenticationManager);
        return http.build();
    }

    /**
     * 第五条：前后端分离，JWT + CORS，兜底所有剩余路径。
     * <p>
     * 四处要解释：
     * <ul>
     *   <li>OPTIONS 必须放行：跨域预检不带 Authorization，拦住它等于所有跨域请求都失败；</li>
     *   <li>/error 必须放行：参数校验失败会 forward 到 /error，它要登录的话 400 会被覆盖成 401；</li>
     *   <li>formLogin / httpBasic / logout 必须显式 disable：它们默认开着，
     *       会在 AuthorizationFilter 之前直接返回 302 或弹登录框，走不到这里配的规则；</li>
     *   <li>addFilterBefore 的位置：JWT 过滤器放在 UsernamePasswordAuthenticationFilter 之前，
     *       RequestTraceFilter 放在 AuthorizationFilter 之前才能读到真实用户名。</li>
     * </ul>
     */
    @Bean
    @org.springframework.core.annotation.Order(5)
    public SecurityFilterChain apiFilterChain(HttpSecurity http,
                                              AuthenticationManager authenticationManager,
                                              JwtService jwtService) {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/", "/index.html", "/error",
                                "/css/**", "/js/**", "/favicon.ico").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/sms/**",
                                "/api/public/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/articles/**")
                        .hasAuthority("ARTICLE_DELETE")
                        .anyRequest().authenticated())

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .csrf(csrf -> csrf.disable())

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new JsonAuthenticationEntryPoint())
                        .accessDeniedHandler(new JsonAccessDeniedHandler()))

                .cors(Customizer.withDefaults())

                .addFilterBefore(new JwtAuthenticationFilter(jwtService),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new RequestTraceFilter(), AuthorizationFilter.class)

                .authenticationManager(authenticationManager);
        return http.build();
    }
}
