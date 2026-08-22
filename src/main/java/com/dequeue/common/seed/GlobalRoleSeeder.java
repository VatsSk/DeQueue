package com.dequeue.common.seed;

import com.dequeue.order.entity.OrderStatus;
import com.dequeue.rbac.entity.OrderVisibility;
import com.dequeue.rbac.entity.RbacRole;
import com.dequeue.rbac.repository.RbacRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Seeds the 4 global platform roles on every startup.
 * - If a role does not exist, it is created (without vendorId).
 * - If a role exists with a stale vendorId field, the field is removed.
 * Runs before DataSeeder (Order 1).
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class GlobalRoleSeeder implements CommandLineRunner {

    private final RbacRoleRepository roleRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) {
        seedRole(
                "ROLE_VENDOR_ADMIN",
                "Full access for the vendor administrator",
                List.of("menu.view", "menu.edit", "staff.view", "staff.edit",
                        "order.view", "order.accept", "order.prepare", "order.ready",
                        "order.complete", "order.cancel", "order.print", "report.view", "qr.view"),
                Arrays.asList(OrderStatus.values())
        );

        seedRole(
                "ROLE_VENDOR_MANAGER",
                "Full access for the vendor manager",
                List.of("menu.view", "menu.edit", "staff.view", "staff.edit",
                        "order.view", "order.accept", "order.prepare", "order.ready",
                        "order.complete", "order.cancel", "order.print", "report.view", "qr.view"),
                Arrays.asList(OrderStatus.values())
        );

        seedRole(
                "ROLE_VENDOR_KITCHEN",
                "Kitchen staff can see and progress orders",
                List.of("order.view", "order.prepare", "order.ready"),
                List.of(OrderStatus.ACCEPTED, OrderStatus.PREPARING, OrderStatus.READY)
        );

        seedRole(
                "ROLE_VENDOR_COUNTER",
                "Counter staff can complete ready orders",
                List.of("order.view", "order.complete", "order.cancel", "order.print"),
                List.of(OrderStatus.READY)
        );

        log.info("GlobalRoleSeeder: 4 global roles verified/seeded.");
    }

    private void seedRole(String name, String description, List<String> permissions,
                          List<OrderStatus> visibilityStatuses) {
        roleRepository.findByName(name).ifPresentOrElse(existing -> {
            // Update permissions, visibility and unset vendorId
            Query query = new Query(Criteria.where("_id").is(existing.getId()));
            Update update = new Update()
                .unset("vendorId")
                .set("permissions", permissions)
                .set("orderVisibility", OrderVisibility.builder().statuses(visibilityStatuses).build());
            mongoTemplate.updateFirst(query, update, RbacRole.class);
            log.info("GlobalRoleSeeder: role '{}' already exists — updated permissions and removed vendorId if any.", name);
        }, () -> {
            RbacRole role = RbacRole.builder()
                    .name(name)
                    .description(description)
                    .permissions(permissions)
                    .orderVisibility(OrderVisibility.builder().statuses(visibilityStatuses).build())
                    .active(true)
                    .build();
            roleRepository.save(role);
            log.info("GlobalRoleSeeder: created global role '{}'.", name);
        });
    }
}
