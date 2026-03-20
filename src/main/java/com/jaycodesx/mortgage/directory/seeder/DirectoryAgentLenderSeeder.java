package com.jaycodesx.mortgage.directory.seeder;

import com.jaycodesx.mortgage.directory.model.DirectoryAgent;
import com.jaycodesx.mortgage.directory.model.DirectoryLender;
import com.jaycodesx.mortgage.directory.model.DirectoryLocation;
import com.jaycodesx.mortgage.directory.repository.DirectoryAgentRepository;
import com.jaycodesx.mortgage.directory.repository.DirectoryLenderRepository;
import com.jaycodesx.mortgage.directory.repository.DirectoryLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Configuration
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = false)
public class DirectoryAgentLenderSeeder {

    private static final Logger log = LoggerFactory.getLogger(DirectoryAgentLenderSeeder.class);

    // Agent name pools
    private static final String[] AGENT_FIRST = {
        "James", "Sarah", "Michael", "Jennifer", "David", "Lisa",
        "Robert", "Patricia", "William", "Linda", "Richard", "Barbara",
        "Thomas", "Susan", "Charles", "Jessica", "Christopher", "Karen"
    };
    private static final String[] AGENT_LAST = {
        "Martinez", "Johnson", "Williams", "Brown", "Davis", "Miller",
        "Wilson", "Moore", "Taylor", "Anderson", "Thomas", "Jackson",
        "Harris", "White", "Garcia", "Robinson", "Lewis", "Walker"
    };
    private static final String[] AGENT_COMPANY = {
        "Harbor Home Group", "Summit Realty Partners", "Pacific Coast Brokers",
        "Landmark Real Estate", "BlueSky Mortgage Advisors", "Cornerstone Realty Group",
        "Keystone Property Group", "Meridian Home Advisors"
    };
    private static final String[] AGENT_SPECIALTY = {
        "Purchase", "Refinance", "FHA & VA", "Jumbo & Luxury"
    };

    // Lender pools
    private static final String[] LENDER_INSTITUTION = {
        "Harbor Federal Bank", "Summit Mortgage", "Pacific Lending Group",
        "Cornerstone Bank", "BlueSky Financial", "Landmark Home Loans",
        "Keystone Mortgage Group", "Meridian Lending Partners"
    };
    private static final String[] LENDER_CONTACT_FIRST = {
        "Amanda", "Brian", "Catherine", "Daniel", "Elena", "Frank",
        "Grace", "Henry", "Irene", "Jason", "Katherine", "Liam"
    };
    private static final String[] LENDER_CONTACT_LAST = {
        "Chen", "Patel", "Rodriguez", "Kim", "Nguyen", "O'Brien",
        "Kowalski", "Hassan", "Okonkwo", "Yamamoto", "Ferreira", "Singh"
    };
    private static final String[] LENDER_LOAN_TYPES = {
        "Conventional, FHA, VA",
        "Conventional, Jumbo",
        "FHA, VA, USDA",
        "Conventional, FHA, VA, Jumbo"
    };
    private static final int[] LENDER_MIN_CREDIT = { 620, 640, 660, 680 };

    @Bean
    public CommandLineRunner seedAgentsAndLenders(
            DirectoryLocationRepository locationRepository,
            DirectoryAgentRepository agentRepository,
            DirectoryLenderRepository lenderRepository
    ) {
        return args -> {
            if (agentRepository.count() > 0) {
                log.info("Directory agents already seeded ({}), skipping.", agentRepository.count());
                return;
            }

            List<DirectoryLocation> locations = locationRepository.findAll();
            if (locations.isEmpty()) {
                log.warn("No directory locations found — skipping agent/lender seed.");
                return;
            }

            log.info("Seeding agents and lenders for {} counties...", locations.size());

            List<DirectoryAgent> agents = new ArrayList<>();
            List<DirectoryLender> lenders = new ArrayList<>();

            for (int i = 0; i < locations.size(); i++) {
                DirectoryLocation loc = locations.get(i);
                String state = loc.getStateCode();
                String county = loc.getCountyName();

                agents.add(buildAgent(i, 0, state, county));
                agents.add(buildAgent(i, 1, state, county));

                lenders.add(buildLender(i, 0, state, county));
                lenders.add(buildLender(i, 1, state, county));

                // Batch flush every 500 counties
                if (agents.size() >= 1000) {
                    agentRepository.saveAll(agents);
                    lenderRepository.saveAll(lenders);
                    agents.clear();
                    lenders.clear();
                }
            }

            // Flush remainder
            if (!agents.isEmpty()) {
                agentRepository.saveAll(agents);
                lenderRepository.saveAll(lenders);
            }

            log.info("Seeded {} agents and {} lenders across {} counties.",
                    agentRepository.count(), lenderRepository.count(), locations.size());
        };
    }

    private DirectoryAgent buildAgent(int countyIdx, int slot, String state, String county) {
        int seed = countyIdx * 7 + slot * 31;

        String firstName = AGENT_FIRST[(seed) % AGENT_FIRST.length];
        String lastName  = AGENT_LAST[(seed + 5) % AGENT_LAST.length];
        String company   = AGENT_COMPANY[(seed + 2) % AGENT_COMPANY.length];
        String specialty = AGENT_SPECIALTY[(seed + slot) % AGENT_SPECIALTY.length];

        String slug = (firstName.charAt(0) + lastName).toLowerCase();
        String emailDomain = company.toLowerCase()
                .replaceAll("[^a-z0-9]", "").substring(0, Math.min(12, company.replaceAll("[^a-zA-Z0-9]","").length()));
        String email = slug + "." + (countyIdx + 1) + "@" + emailDomain + ".com";
        String phone = formatPhone(5550000 + (countyIdx * 2 + slot) % 9999);
        String license = String.format("AG-%s-%05d", state, countyIdx * 2 + slot + 1);
        int responseHours = 1 + (seed % 23);
        BigDecimal rating = BigDecimal.valueOf(4.0 + (seed % 10) * 0.1).setScale(2);

        DirectoryAgent agent = new DirectoryAgent();
        agent.setStateCode(state);
        agent.setCountyName(county);
        agent.setFirstName(firstName);
        agent.setLastName(lastName);
        agent.setCompanyName(company);
        agent.setEmail(email);
        agent.setPhone(phone);
        agent.setLicenseNumber(license);
        agent.setNmlsId(String.valueOf(1000000 + countyIdx * 2 + slot + 1));
        agent.setSpecialty(specialty);
        agent.setAvgResponseHours(responseHours);
        agent.setRating(rating);
        agent.setRankingScore(60 + (seed % 41));   // 60–100
        agent.setActive(true);
        agent.setLanguages("English");
        return agent;
    }

    private DirectoryLender buildLender(int countyIdx, int slot, String state, String county) {
        int seed = countyIdx * 11 + slot * 37;

        String institution = LENDER_INSTITUTION[(seed) % LENDER_INSTITUTION.length];
        String contactFirst = LENDER_CONTACT_FIRST[(seed + 3) % LENDER_CONTACT_FIRST.length];
        String contactLast  = LENDER_CONTACT_LAST[(seed + 7) % LENDER_CONTACT_LAST.length];
        String loanTypes    = LENDER_LOAN_TYPES[(seed + slot) % LENDER_LOAN_TYPES.length];
        int minCredit       = LENDER_MIN_CREDIT[seed % LENDER_MIN_CREDIT.length];

        String slug = (contactFirst.charAt(0) + contactLast).toLowerCase();
        String emailDomain = institution.toLowerCase()
                .replaceAll("[^a-z0-9]", "").substring(0, Math.min(14, institution.replaceAll("[^a-zA-Z0-9]","").length()));
        String email = slug + "." + (countyIdx + 1) + "@" + emailDomain + ".com";
        String phone = formatPhone(5554000 + (countyIdx * 2 + slot) % 9999);
        String license = String.format("LN-%s-%05d", state, countyIdx * 2 + slot + 1);
        int slaHours = 2 + (seed % 22);
        BigDecimal rating = BigDecimal.valueOf(4.0 + (seed % 10) * 0.1).setScale(2);

        DirectoryLender lender = new DirectoryLender();
        lender.setStateCode(state);
        lender.setCountyName(county);
        lender.setInstitutionName(institution);
        lender.setContactName(contactFirst + " " + contactLast);
        lender.setEmail(email);
        lender.setPhone(phone);
        lender.setLicenseNumber(license);
        lender.setNmlsId(String.valueOf(2000000 + countyIdx * 2 + slot + 1));
        lender.setLoanTypes(loanTypes);
        lender.setMinCreditScore(minCredit);
        lender.setAvgSlaHours(slaHours);
        lender.setRating(rating);
        lender.setRankingScore(60 + (seed % 41));  // 60–100
        lender.setActive(true);
        lender.setLanguages("English");
        return lender;
    }

    private String formatPhone(long n) {
        long num = Math.abs(n) % 10000;
        return String.format("(555) %03d-%04d", (num / 100), num % 100);
    }
}
