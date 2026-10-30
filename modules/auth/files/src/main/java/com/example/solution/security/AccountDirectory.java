package com.example.solution.security;

import java.util.*;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Explicit configured accounts; no registration, mail or example business entities. */
@Service
public class AccountDirectory implements UserDetailsService {
    public record Account(String login, String password, List<String> authorities) {}
    private final Map<String, UserDetails> accounts;
    public AccountDirectory(Environment environment, PasswordEncoder encoder) {
        var configured = Binder.get(environment).bind("app.auth.users", Bindable.listOf(Account.class)).orElse(List.of());
        var users = new LinkedHashMap<String, UserDetails>();
        for (var account : configured) {
            if (account.login() == null || !account.login().matches("[a-z0-9_-]{1,50}") || account.password() == null || account.password().isBlank()
                || account.authorities() == null || account.authorities().isEmpty()
                || account.authorities().stream().anyMatch(role -> !role.matches("ROLE_[A-Z0-9_]+"))) {
                throw new IllegalStateException("Invalid configured account");
            }
            var user = User.withUsername(account.login()).password(encoder.encode(account.password()))
                .authorities(account.authorities().toArray(String[]::new)).build();
            if (users.putIfAbsent(account.login(), user) != null) throw new IllegalStateException("Duplicate account");
        }
        if (users.isEmpty()) throw new IllegalStateException("Configure app.auth.users before enabling auth");
        accounts = Map.copyOf(users);
    }
    public Optional<UserDetails> findOneByLogin(String login) { return Optional.ofNullable(accounts.get(login)); }
    @Override public UserDetails loadUserByUsername(String login) {
        var account = findOneByLogin(login).orElseThrow(() -> new UsernameNotFoundException("Account unavailable"));
        // Authentication erases credentials from the returned instance; never expose the stored instance.
        return User.withUserDetails(account).build();
    }
}
