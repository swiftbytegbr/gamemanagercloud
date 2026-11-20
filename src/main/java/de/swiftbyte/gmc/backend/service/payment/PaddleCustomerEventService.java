package de.swiftbyte.gmc.backend.service.payment;


import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.repository.UserRepository;
import de.swiftbyte.paddle.client.PaddleCustomerClient;
import de.swiftbyte.paddle.client.dto.PaddleAddress;
import de.swiftbyte.paddle.client.dto.PaddleBusiness;
import de.swiftbyte.paddle.client.dto.PaddleCustomer;
import de.swiftbyte.paddle.spring.event.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.HashMap;

@Slf4j
@Service
public class PaddleCustomerEventService {

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private PaddleCustomerClient paddleCustomerClient;

    @Async
    @EventListener
    public void handleCustomerCreated(PaddleCustomerCreatedEvent event) {
        processCustomerEvent(event.getPayload());
    }

    @Async
    @EventListener
    public void handleCustomerUpdated(PaddleCustomerUpdatedEvent event) {
        processCustomerEvent(event.getPayload());
    }

    @Async
    @EventListener
    public void handleBusinessCreated(PaddleBusinessCreatedEvent event) {
        processBusinessEvent(event.getPayload());
    }

    @Async
    @EventListener
    public void handleBusinessUpdated(PaddleBusinessUpdatedEvent event) {
        processBusinessEvent(event.getPayload());
    }

    @Async
    @EventListener
    public void handleAddressCreated(PaddleAddressCreatedEvent event) {
        processAddressEvent(event.getPayload());
    }

    @Async
    @EventListener
    public void handleAddressUpdated(PaddleAddressUpdatedEvent event) {
        processAddressEvent(event.getPayload());
    }

    private void processCustomerEvent(PaddleCustomer customer) {
        if (customer == null) return;

        String userId = extractUserId(customer.getCustomData());
        String email = safeEmail(customer.getEmail());

        // Prefer user_id linkage
        if (userId != null) {
            userRepository.findById(userId).ifPresentOrElse(user -> {
                boolean changed = false;

                // Email sync (only if provided and changed)
                if (email != null && !Objects.equals(user.getEmail(), email)) {
                    user.setEmail(email);
                    changed = true;
                }

                // Name sync (update only if different)
                String incomingName = customer.getName();
                User.PersonalInformation pi = user.getPersonalInformation();
                if (pi == null) pi = new User.PersonalInformation();
                if (!Objects.equals(pi.getName(), incomingName)) {
                    pi.setName(incomingName);
                    user.setPersonalInformation(pi);
                    changed = true;
                }

                if (changed) userRepository.save(user);
            }, () -> linkByEmailAndBackfillUserId(customer, email));
            return;
        }

        // No user_id present; link by email and backfill
        linkByEmailAndBackfillUserId(customer, email);
    }

    private void processBusinessEvent(PaddleBusiness business) {
        if (business == null) return;

        Optional<User> user = findLinkedUserForResource(business.getCustomData(), getCustomerIdFrom(business));
        if (user.isEmpty()) return;

        String userId = user.get().getId();
        updateBusinessCustomDataUserId(business, userId);
        updateUserFromBusiness(userId, business);
    }

    private void processAddressEvent(PaddleAddress address) {
        if (address == null) return;

        Optional<User> user = findLinkedUserForResource(address.getCustomData(), getCustomerIdFrom(address));
        if (user.isEmpty()) return;

        String userId = user.get().getId();
        updateAddressCustomDataUserId(address, userId);
        updateUserFromAddress(userId, address);
    }

    private Optional<User> findLinkedUserForResource(Map<String, Object> customData, String customerId) {
        String uid = extractUserId(customData);
        if (uid != null) {
            Optional<User> byId = userRepository.findById(uid);
            if (byId.isPresent()) return byId;
        }
        if (customerId == null) return Optional.empty();
        return resolveUserFromCustomer(customerId);
    }

    private void linkByEmailAndBackfillUserId(PaddleCustomer customer, String email) {
        if (email == null) return;
        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isEmpty()) return;

        User user = byEmail.get();
        boolean changed = false;
        if (!Objects.equals(user.getPaddleCustomerId(), customer.getId())) {
            user.setPaddleCustomerId(customer.getId());
            changed = true;
        }
        // Name sync: update only if different
        User.PersonalInformation pi = user.getPersonalInformation();
        if (pi == null) pi = new User.PersonalInformation();
        String incomingName = customer.getName();
        if (!Objects.equals(pi.getName(), incomingName)) {
            pi.setName(incomingName);
            user.setPersonalInformation(pi);
            changed = true;
        }
        if (changed) userRepository.save(user);

        try {
            Map<String, Object> custom = new HashMap<>();
            if (customer.getCustomData() != null) custom.putAll(customer.getCustomData());
            if (!Objects.equals(custom.get("user_id"), user.getId())) {
                custom.put("user_id", user.getId());
                PaddleCustomer payload = new PaddleCustomer();
                payload.setCustomData(custom);
                paddleCustomerClient.updateCustomer(customer.getId(), payload).complete();
            }
        } catch (Exception e) {
            log.warn("Failed updating Paddle customer customData with user_id: {}", e.getMessage());
        }
    }

    private String safeEmail(String email) {
        if (email == null) return null;
        String e = email.trim().toLowerCase();
        return e.isEmpty() ? null : e;
    }


    private Optional<User> resolveUserFromCustomer(String customerId) {
        try {
            PaddleCustomer customer = paddleCustomerClient.getCustomer(customerId).complete();
            if (customer == null) return Optional.empty();
            String uid = extractUserId(customer.getCustomData());
            if (uid != null) {
                Optional<User> byId = userRepository.findById(uid);
                if (byId.isPresent()) return byId;
            }
            String email = safeEmail(customer.getEmail());
            if (email != null) {
                return userRepository.findByEmail(email);
            }
        } catch (Exception e) {
            log.warn("Failed resolving user from Paddle customer {}: {}", customerId, e.getMessage());
        }
        return Optional.empty();
    }

    private String getCustomerIdFrom(Object payload) {
        if (payload == null) return null;
        try {
            // Direct getter
            Object val = payload.getClass().getMethod("getCustomerId").invoke(payload);
            if (val != null) return String.valueOf(val);
        } catch (Exception ignored) {
        }
        try {
            // Nested customer object with id
            Object customer = payload.getClass().getMethod("getCustomer").invoke(payload);
            if (customer != null) {
                Object id = customer.getClass().getMethod("getId").invoke(customer);
                if (id != null) return String.valueOf(id);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void updateBusinessCustomDataUserId(PaddleBusiness business, String userId) {
        try {
            Map<String, Object> custom = new HashMap<>();
            if (business.getCustomData() != null) custom.putAll(business.getCustomData());
            if (Objects.equals(custom.get("user_id"), userId)) return; // nothing to do
            custom.put("user_id", userId);
            PaddleBusiness payload = new PaddleBusiness();
            payload.setCustomData(custom);
            paddleCustomerClient.updateBusiness(business.getCustomerId(), business.getId(), payload).complete();
        } catch (Exception e) {
            log.warn("Failed updating business customData with user_id: {}", e.getMessage());
        }
    }

    private void updateAddressCustomDataUserId(PaddleAddress address, String userId) {
        try {
            Map<String, Object> custom = new HashMap<>();
            if (address.getCustomData() != null) custom.putAll(address.getCustomData());
            if (Objects.equals(custom.get("user_id"), userId)) return; // nothing to do
            custom.put("user_id", userId);
            PaddleAddress payload = new PaddleAddress();
            payload.setCustomData(custom);
            paddleCustomerClient.updateAddress(address.getCustomerId(), address.getId(), payload).complete();
        } catch (Exception e) {
            log.warn("Failed updating address customData with user_id: {}", e.getMessage());
        }
    }

    private String extractUserId(Map<String, Object> customData) {
        if (customData == null) return null;
        Object raw = customData.get("user_id");
        if (raw == null) raw = customData.get("userId");
        return raw != null ? String.valueOf(raw) : null;
    }

    private void updateUserFromBusiness(String userId, PaddleBusiness business) {
        Optional<User> optional = userRepository.findById(userId);
        if (optional.isEmpty()) return;

        User user = optional.get();
        User.CompanyInformation company = user.getCompanyInformation();
        if (company == null) company = new User.CompanyInformation();

        boolean changed = false;

        String companyName = business.getName();
        if (!Objects.equals(company.getCompanyName(), companyName)) {
            company.setCompanyName(companyName);
            changed = true;
        }

        String vat = business.getTaxIdentifier();
        if (!vat.equals(company.getVatNumber()) ) {
            company.setVatNumber(vat);
            changed = true;
        }

        if (changed) {
            user.setCompanyInformation(company);
            userRepository.save(user);
        }
    }

    private void updateUserFromAddress(String userId, PaddleAddress payload) {
        Optional<User> optional = userRepository.findById(userId);
        if (optional.isEmpty()) return;

        User user = optional.get();
        User.PersonalInformation info = user.getPersonalInformation();
        if (info == null) info = new User.PersonalInformation();

        User.PersonalInformation.Address address = info.getBillingAddress();
        if (address == null) address = new User.PersonalInformation.Address();

        boolean changed = false;

        if (!Objects.equals(address.getStreet(), payload.getFirstLine())) {
            address.setStreet(payload.getFirstLine());
            changed = true;
        }
        if (!Objects.equals(address.getCity(), payload.getCity())) {
            address.setCity(payload.getCity());
            changed = true;
        }
        if (!Objects.equals(address.getRegion(), payload.getRegion())) {
            address.setRegion(payload.getRegion());
            changed = true;
        }
        if (!Objects.equals(address.getPostalCode(), payload.getPostalCode())) {
            address.setPostalCode(payload.getPostalCode());
            changed = true;
        }
        if (!Objects.equals(address.getCountryCode(), payload.getCountryCode())) {
            address.setCountryCode(payload.getCountryCode());
            changed = true;
        }
        if (changed) {
            info.setBillingAddress(address);
            user.setPersonalInformation(info);
            userRepository.save(user);
        }
    }
}
