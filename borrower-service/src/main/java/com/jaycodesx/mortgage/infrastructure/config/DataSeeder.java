package com.jaycodesx.mortgage.infrastructure.config;

import com.jaycodesx.mortgage.borrower.model.Borrower;
import com.jaycodesx.mortgage.borrower.repository.BorrowerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
public class DataSeeder {

    @Bean
    @ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = false)
    CommandLineRunner seedBorrowers(BorrowerRepository borrowerRepository) {
        return args -> {
            if (borrowerRepository.count() > 0) {
                return;
            }

            List<Borrower> borrowers = List.of(
                    createBorrower("Jay", "Carter", "jay.carter@jaycodesx.dev", randomInt(620, 810)),
                    createBorrower("Jay", "Miller", "jay.miller@jaycodesx.dev", randomInt(600, 790)),
                    createBorrower("Jay", "Nguyen", "jay.nguyen@jaycodesx.dev", randomInt(640, 830)),
                    createBorrower("Jay", "Patel", "jay.patel@jaycodesx.dev", randomInt(610, 800))
            );

            borrowerRepository.saveAll(borrowers);
        };
    }

    private Borrower createBorrower(String firstName, String lastName, String email, int creditScore) {
        Borrower borrower = new Borrower();
        borrower.setFirstName(firstName);
        borrower.setLastName(lastName);
        borrower.setEmail(email);
        borrower.setCreditScore(creditScore);
        return borrower;
    }

    private int randomInt(int minInclusive, int maxInclusive) {
        return ThreadLocalRandom.current().nextInt(minInclusive, maxInclusive + 1);
    }
}
