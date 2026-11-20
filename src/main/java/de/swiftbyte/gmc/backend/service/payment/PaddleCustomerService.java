package de.swiftbyte.gmc.backend.service.payment;


import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.repository.UserRepository;
import de.swiftbyte.paddle.client.PaddleCustomerClient;
import de.swiftbyte.paddle.client.dto.PaddleAddress;
import de.swiftbyte.paddle.client.dto.PaddleBusiness;
import de.swiftbyte.paddle.client.dto.PaddleCustomer;
import de.swiftbyte.paddle.client.exception.PaddleConflictException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PaddleCustomerService {

    @Autowired
    private PaddleCustomerClient client;

    @Autowired
    private UserRepository userRepository;

    /**
     * Syncs the given user's data to Paddle: ensures the customer exists and updates
     * address and business if available. Writes to Paddle (not from Paddle).
     */
    public void syncUserToPaddle(User user) {
        upsertCustomer(user);
        upsertAddress(user);
        upsertBusiness(user);
    }

    /**
     * Synchronously create/update the Paddle customer record and persist customerId on the user.
     * Does not touch address or business. Use this when you only need the customer prepared
     * before selectively syncing address or business.
     */
    public void upsertCustomer(User user) {
        PaddleCustomer customer = new PaddleCustomer();
        customer.setEmail(normalizeEmail(user.getEmail()));
        customer.setCustomData(Map.of("user_id", user.getId()));
        // Push personal name to Paddle customer (simple assignment)
        String name = user.getPersonalInformation() == null ? null : user.getPersonalInformation().getName();
        if (name != null && !name.isBlank()) customer.setName(name);

        customer = saveCustomer(user.getPaddleCustomerId(), customer);

        // Persist the Paddle ID on the user if it was just created
        if (user.getPaddleCustomerId() == null || user.getPaddleCustomerId().isBlank()) {
            user.setPaddleCustomerId(customer.getId());
            userRepository.save(user);
        }
    }

    /**
     * Async convenience wrapper to only update the Paddle customer fields (email/name/customData).
     */
    // ----- Internal helpers (package-private for tests) -----

    void upsertBusiness(User user) {
        if (user.getPaddleCustomerId() == null || user.getPaddleCustomerId().isBlank()) return;

        User.CompanyInformation company = user.getCompanyInformation();
        if (company == null) return;
        if (company.getCompanyName() == null || company.getCompanyName().isBlank()) return;
        if (company.getCountryCode() == null || company.getCountryCode().isBlank()) return;
        if (company.getVatNumber() == null || company.getVatNumber().isBlank()) return;

        PaddleBusiness business = new PaddleBusiness();
        business.setName(company.getCompanyName());
        business.setTaxIdentifier(company.getVatNumber());
        business.setCustomData(Map.of("user_id", user.getId()));
        String cid = user.getPaddleCustomerId();
        List<PaddleBusiness> existing = client.listBusinesses(cid).complete();
        if (existing != null && !existing.isEmpty()) {
            PaddleBusiness target = existing.get(0);
            client.updateBusiness(cid, target.getId(), business).complete();
        } else {
            client.createBusiness(cid, business).complete();
        }

    }

    void upsertAddress(User user) {
        if (user.getPaddleCustomerId() == null || user.getPaddleCustomerId().isBlank()) return;

        User.PersonalInformation info = user.getPersonalInformation();
        if (info == null || info.getBillingAddress() == null || !info.getBillingAddress().isSet()) return;

        User.PersonalInformation.Address addr = info.getBillingAddress();
        PaddleAddress address = new PaddleAddress();
        address.setFirstLine(info.getBillingAddress().getStreet());
        address.setCity(addr.getCity());
        address.setRegion(addr.getRegion());
        address.setPostalCode(addr.getPostalCode());
        address.setCountryCode(addr.getCountryCode());
        address.setCustomData(Map.of("user_id", user.getId()));
        String cid = user.getPaddleCustomerId();
        List<PaddleAddress> existing = client.listAddresses(cid).complete();
        if (existing != null && !existing.isEmpty()) {
            client.updateAddress(cid, existing.get(0).getId(), address).complete();
        } else {
            client.createAddress(cid, address).complete();
        }

    }


    public Optional<PaddleCustomer> findCustomerByEmail(String email) {
        String normalized = normalizeEmail(email);
        List<PaddleCustomer> existing = client.listCustomers(Map.of("email", normalized)).complete();
        return existing.stream().findFirst();
    }


    public PaddleCustomer saveCustomer(String customerId, PaddleCustomer customer) {
        // Ensure normalized email for lookups and writes
        customer.setEmail(normalizeEmail(customer.getEmail()));
        if (customerId != null && !customerId.isBlank()) {
            // For updates, explicitly set status to active
            customer.setStatus("active");
            return client.updateCustomer(customerId, customer).complete();
        }

        List<PaddleCustomer> existing = client.listCustomers(Map.of("email", customer.getEmail(), "status", "active,archived")).complete();
        if (!existing.isEmpty()) {
            PaddleCustomer existingCustomer = existing.get(0);
            customer.setStatus("active");
            return client.updateCustomer(existingCustomer.getId(), customer).complete();
        }

        try {
            return client.createCustomer(customer).complete();
        } catch (PaddleConflictException e) {
            // If Paddle reports the customer already exists, resolve by email and update instead
            List<PaddleCustomer> byEmail = client.listCustomers(Map.of("email", customer.getEmail())).complete();
            if (!byEmail.isEmpty()) {
                PaddleCustomer existingCustomer = byEmail.get(0);
                customer.setStatus("active");
                return client.updateCustomer(existingCustomer.getId(), customer).complete();
            }
            throw e;
        }
    }

    private String normalizeEmail(String email) {
        if (email == null) return null;
        String e = email.trim().toLowerCase();
        return e.isEmpty() ? null : e;
    }


}
