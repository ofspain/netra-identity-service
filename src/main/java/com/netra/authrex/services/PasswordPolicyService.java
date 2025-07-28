package com.netra.authrex.services;

import com.netra.authrex.exceptions.PasswordRotationException;
import com.netra.commons.database.EnhancedBeanPropertyRowMapper;
import com.netra.commons.models.Identity;
import com.netra.commons.models.PasswordHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PasswordPolicyService {

    private final JdbcClient jdbcClient;
    private final PasswordEncoder passwordEncoder;

    @Value("${max.days.allowed.password.lifetime:90}")
    private int passwordRotationDays;
    @Value("${app.password.history-size:5}")
    private int passwordHistorySize;

    public boolean isPasswordAllowed(Identity identity, String newRawPassword) {
        // Check against password history
        if (isPasswordInHistory(identity, newRawPassword)) {
            return false;
        }

        // Check password rotation interval
        validatePasswordRotationInterval(identity);

        return true;
    }

    private boolean isPasswordInHistory(Identity identity, String newRawPassword) {
        List<PasswordHistory> history = jdbcClient.sql("""
                SELECT * FROM get_password_history(?, ?)
                """)
                .param(1, identity.getId())
                .param(2, passwordHistorySize)
                .query(new EnhancedBeanPropertyRowMapper<PasswordHistory>())
                .list();

        return history.stream()
                .anyMatch(entry -> passwordEncoder.matches(newRawPassword, entry.getHashedPassword()));
    }

    private void validatePasswordRotationInterval(Identity identity) {
        if (identity.getPasswordLastChanged() != null &&
                identity.getPasswordLastChanged().isAfter(LocalDateTime.now().minusDays(passwordRotationDays))) {
            throw new PasswordRotationException(
                    "Password must be changed after " + passwordRotationDays + " days. Last changed on: " +
                            identity.getPasswordLastChanged());
        }
    }

    public void recordPasswordChange(Identity identity, String newHashedPassword) {
        jdbcClient.sql("""
                CALL record_password_change(?, ?, ?)
                """)
                .param(1, identity.getId())
                .param(2, newHashedPassword)
                .param(3, passwordHistorySize)
                .update();
    }
}
