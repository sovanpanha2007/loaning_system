package src.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import src.controller.LoaningSystem;

@Configuration
public class BeanConfig {

    // Single shared instance for the whole application: LoaningSystem is internally
    // thread-safe (see the writeLock comment in LoaningSystem.java) and holds one SQLite
    // connection for its lifetime, so a singleton bean here is what makes it safe and
    // correct for many concurrent HTTP requests to share.
    @Bean(destroyMethod = "close")
    public LoaningSystem loaningSystem(
            @Value("${lms.bank-name:KH Bank}") String bankName,
            @Value("${lms.interest-rate:0.05}") double interestRate,
            @Value("${lms.db-file:loaning_system.db}") String dbFile) {
        return new LoaningSystem(bankName, interestRate, dbFile);
    }
}
