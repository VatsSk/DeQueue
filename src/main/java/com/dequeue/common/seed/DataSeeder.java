package com.dequeue.common.seed;

import com.dequeue.menu.entity.*;
import com.dequeue.order.entity.OrderStatus;
import com.dequeue.profile.entity.VendorProfile;
import com.dequeue.rbac.entity.OrderVisibility;
import com.dequeue.rbac.entity.RbacPermission;
import com.dequeue.rbac.entity.RbacRole;
import com.dequeue.staff.entity.*;
import com.dequeue.vendor.entity.*;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);
    private final MongoTemplate mongoTemplate;
    private final BCryptPasswordEncoder passwordEncoder;

    public DataSeeder(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Checking if seed data is needed...");
        
        // Check if any core data already exists
        long vendorCount = mongoTemplate.count(new org.springframework.data.mongodb.core.query.Query(), Vendor.class);
        long staffCount = mongoTemplate.count(new org.springframework.data.mongodb.core.query.Query(), Staff.class);
        
        if (vendorCount > 0 || staffCount > 0) {
            logger.info("Database already contains data (Vendors: {}, Staff: {}). Skipping seed process.", vendorCount, staffCount);
            return;
        }

        logger.info("Starting data seed process...");

        // ── 1. Seed Platform Permissions ─────────────────────────────────
        Map<String, RbacPermission> permMap = seedPermissions();
        logger.info("Seeded {} permissions", permMap.size());

        // ── 2. Create Vendor ──────────────────────────────────────────────
        Vendor vendor = new Vendor();
        vendor.setShopName("Chai Corner");
        vendor.setOwnerName("Admin User");
        vendor.setVendorCode("chai-corner-x7k2");
        vendor.setPhone("+911234567890");
        vendor.setShopStatus(ShopStatus.OPEN);
        vendor.setActive(true);
        vendor = mongoTemplate.save(vendor);
        String vendorId = vendor.getId();

        // ── 3. Create Departments ─────────────────────────────────────────
        Department adminDept = saveDept(vendorId, "Admin");
        Department kitchenDept = saveDept(vendorId, "Kitchen");
        Department counterDept = saveDept(vendorId, "Counter");

        // ── 4. Seed Roles ─────────────────────────────────────────────────
        // Vendor Admin — all permissions, all statuses
        List<String> allPermIds = new ArrayList<>(permMap.values().stream()
                .map(RbacPermission::getId).collect(Collectors.toList()));
        RbacRole adminRole = saveRole(vendorId, "Vendor Admin",
                "Full access for the vendor administrator",
                allPermIds,
                Arrays.asList(OrderStatus.values()));

        // Kitchen Staff — order.view, order.prepare, order.ready
        List<String> kitchenPermIds = permIds(permMap, "order.view", "order.prepare", "order.ready");
        RbacRole kitchenRole = saveRole(vendorId, "Kitchen Staff",
                "Kitchen staff can see and progress orders",
                kitchenPermIds,
                List.of(OrderStatus.ACCEPTED, OrderStatus.PREPARING, OrderStatus.READY));

        // Counter Staff — order.view, order.complete
        List<String> counterPermIds = permIds(permMap, "order.view", "order.complete");
        RbacRole counterRole = saveRole(vendorId, "Counter Staff",
                "Counter staff can complete ready orders",
                counterPermIds,
                List.of(OrderStatus.READY));

        logger.info("Seeded 3 roles for vendor: {}", vendorId);

        // ── 5. Create Staff ───────────────────────────────────────────────
        saveStaff(vendorId, "Admin User",        "admin@dequeue.com",     "admin123",   adminDept,   adminRole, true);
        saveStaff(vendorId, "Kitchen Staff 1",   "kitchen@chaicorner.com","kitchen123", kitchenDept, kitchenRole, false);
        saveStaff(vendorId, "Counter Staff 1",   "counter@chaicorner.com","counter123", counterDept, counterRole, false);

        // ── 6. Create Categories ──────────────────────────────────────────
        Category hotBev  = saveCategory(vendorId, "Hot Beverages",  1);
        Category coldBev = saveCategory(vendorId, "Cold Beverages", 2);
        Category snacks  = saveCategory(vendorId, "Snacks",         3);

        // ── 7. Customization Groups ───────────────────────────────────────
        CustomizationGroup sizeGroup  = buildCustGroup(vendorId, "Size",   SelectionType.SINGLE,
                List.of(opt("Small", 0), opt("Medium", 0), opt("Large", 0)));
        sizeGroup = mongoTemplate.save(sizeGroup);

        CustomizationGroup sugarGroup = buildCustGroup(vendorId, "Sugar",  SelectionType.SINGLE,
                List.of(opt("Less", 0), opt("Normal", 0), opt("Extra", 0)));
        sugarGroup = mongoTemplate.save(sugarGroup);

        CustomizationGroup typeGroup  = buildCustGroup(vendorId, "Type",   SelectionType.SINGLE,
                List.of(opt("Hot", 0), opt("Cold", 0), opt("Iced", 0)));
        typeGroup = mongoTemplate.save(typeGroup);

        CustomizationGroup extrasGroup = buildCustGroup(vendorId, "Extras", SelectionType.MULTIPLE,
                List.of(opt("Extra Chutney", 5), opt("Cheese", 10)));
        extrasGroup = mongoTemplate.save(extrasGroup);

        // ── 8. Menu Items ─────────────────────────────────────────────────
        mongoTemplate.save(menuItem(vendorId, hotBev.getId(),  "Masala Chai",     new BigDecimal("1.50"), 5, 1, List.of(sizeGroup.getId(),  sugarGroup.getId())));
        mongoTemplate.save(menuItem(vendorId, hotBev.getId(),  "Coffee",           new BigDecimal("2.00"), 5, 2, List.of(sizeGroup.getId(),  typeGroup.getId())));
        mongoTemplate.save(menuItem(vendorId, snacks.getId(),  "Vada Pav",         new BigDecimal("1.00"), 3, 1, List.of(extrasGroup.getId())));
        mongoTemplate.save(menuItem(vendorId, snacks.getId(),  "Samosa",           new BigDecimal("0.75"), 3, 2, new ArrayList<>()));
        mongoTemplate.save(menuItem(vendorId, coldBev.getId(), "Cold Coffee",      new BigDecimal("2.50"), 5, 1, new ArrayList<>()));
        mongoTemplate.save(menuItem(vendorId, coldBev.getId(), "Fresh Lime Soda",  new BigDecimal("1.50"), 3, 2, List.of(typeGroup.getId())));

        // ── 9. Vendor Settings ────────────────────────────────────────────
        mongoTemplate.getCollection("vendor_settings").insertOne(
                new Document("vendorId", vendorId)
                        .append("isAcceptingOrders", true)
                        .append("autoAcceptOrders", false));

        // ── 10. Vendor Profile ────────────────────────────────────────────
        mongoTemplate.getCollection("vendor_profiles").insertOne(
                new Document("vendorId", vendorId)
                        .append("shopName", "Chai Corner")
                        .append("ownerName", "Admin User")
                        .append("description", "Best chai and snacks in town!")
                        .append("logoUrl", "https://example.com/logo.png")
                        .append("bannerUrl", "https://example.com/banner.png"));

        // ── 11. QR Metadata ───────────────────────────────────────────────
        mongoTemplate.getCollection("qr_metadata").insertOne(
                new Document("vendorId", vendorId)
                        .append("vendorCode", "chai-corner-x7k2")
                        .append("qrUrl", "http://localhost:8080/api/v1/public/qr/v/chai-corner-x7k2"));

        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        logger.info("  ✅ Seed data inserted successfully");
        logger.info("  Admin Email    : admin@dequeue.com  / Password: admin123");
        logger.info("  Kitchen Email  : kitchen@chaicorner.com / Password: kitchen123");
        logger.info("  Counter Email  : counter@chaicorner.com / Password: counter123");
        logger.info("  Vendor Code    : chai-corner-x7k2");
        logger.info("  Roles seeded   : Vendor Admin, Kitchen Staff, Counter Staff");
        logger.info("  Permissions    : {} platform permissions seeded", permMap.size());
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ─── Permission seeding ───────────────────────────────────────────────────

    private Map<String, RbacPermission> seedPermissions() {
        String[][] perms = {
            // resource, action, description
            {"order",  "view",    "View orders in the queue"},
            {"order",  "accept",  "Accept a pending order"},
            {"order",  "prepare", "Start preparing an accepted order"},
            {"order",  "ready",   "Mark a preparing order as ready"},
            {"order",  "complete","Complete a ready order"},
            {"order",  "cancel",  "Cancel an order"},
            {"menu",   "view",    "View menu items and categories"},
            {"menu",   "create",  "Create menu items"},
            {"menu",   "update",  "Update menu items"},
            {"menu",   "delete",  "Delete menu items"},
            {"staff",  "view",    "View staff members"},
            {"staff",  "create",  "Create staff members"},
            {"staff",  "update",  "Update staff members"},
            {"staff",  "delete",  "Delete staff members"},
            {"role",   "view",    "View vendor roles"},
            {"role",   "create",  "Create vendor roles"},
            {"role",   "update",  "Update vendor roles"},
            {"role",   "delete",  "Delete vendor roles"},
            {"report", "view",    "View reports and analytics"},
        };

        Map<String, RbacPermission> result = new LinkedHashMap<>();
        for (String[] p : perms) {
            try {
                // Check if permission already exists
                org.springframework.data.mongodb.core.query.Query query = 
                    new org.springframework.data.mongodb.core.query.Query();
                query.addCriteria(
                    org.springframework.data.mongodb.core.query.Criteria.where("resource").is(p[0])
                    .and("action").is(p[1])
                );
                
                RbacPermission existing = mongoTemplate.findOne(query, RbacPermission.class);
                
                if (existing != null) {
                    // Permission already exists, use it
                    result.put(p[0] + "." + p[1], existing);
                } else {
                    // Create new permission
                    RbacPermission perm = RbacPermission.builder()
                            .resource(p[0])
                            .action(p[1])
                            .description(p[2])
                            .active(true)
                            .build();
                    perm = mongoTemplate.save(perm);
                    result.put(p[0] + "." + p[1], perm);
                }
            } catch (Exception e) {
                logger.warn("Failed to seed permission {}:{} - {}", p[0], p[1], e.getMessage());
            }
        }
        return result;
    }

    // ─── Role helpers ─────────────────────────────────────────────────────────

    private RbacRole saveRole(String vendorId, String name, String description,
                               List<String> permissionIds, List<OrderStatus> visibilityStatuses) {
        RbacRole role = RbacRole.builder()
                .vendorId(vendorId)
                .name(name)
                .description(description)
                .permissionIds(permissionIds)
                .orderVisibility(OrderVisibility.builder().statuses(visibilityStatuses).build())
                .active(true)
                .build();
        return mongoTemplate.save(role);
    }

    private List<String> permIds(Map<String, RbacPermission> permMap, String... keys) {
        List<String> ids = new ArrayList<>();
        for (String key : keys) {
            if (permMap.containsKey(key)) ids.add(permMap.get(key).getId());
        }
        return ids;
    }

    // ─── Staff helper ─────────────────────────────────────────────────────────

    private void saveStaff(String vendorId, String name, String email, String password,
                            Department dept, RbacRole role, boolean isPlatformAdmin) {
        Staff staff = new Staff();
        staff.setVendorId(vendorId);
        staff.setName(name);
        staff.setEmail(email);
        staff.setPassword(passwordEncoder.encode(password));
        staff.setRoleIds(List.of(role.getId()));
        staff.setDepartmentIds(List.of(dept.getId()));
        staff.setStatus(StaffStatus.ACTIVE);
        staff.setPlatformAdmin(isPlatformAdmin);
        mongoTemplate.save(staff);
    }

    // ─── Other helpers ────────────────────────────────────────────────────────

    private Department saveDept(String vendorId, String name) {
        Department d = new Department();
        d.setVendorId(vendorId);
        d.setName(name);
        d.setActive(true);
        return mongoTemplate.save(d);
    }

    private Category saveCategory(String vendorId, String name, int sortOrder) {
        Category c = new Category();
        c.setVendorId(vendorId);
        c.setName(name);
        c.setSortOrder(sortOrder);
        c.setActive(true);
        return mongoTemplate.save(c);
    }

    private CustomizationGroup buildCustGroup(String vendorId, String name,
                                               SelectionType type, List<CustomizationOption> options) {
        CustomizationGroup g = new CustomizationGroup();
        g.setVendorId(vendorId);
        g.setName(name);
        g.setSelectionType(type);
        g.setRequired(false);
        g.setOptions(options);
        return g;
    }

    private CustomizationOption opt(String name, double price) {
        CustomizationOption o = new CustomizationOption();
        o.setName(name);
        o.setAdditionalPrice(BigDecimal.valueOf(price));
        o.setAvailable(true);
        return o;
    }

    private MenuItem menuItem(String vendorId, String categoryId, String name,
                               BigDecimal price, int prepTime, int sortOrder, List<String> groupIds) {
        MenuItem item = new MenuItem();
        item.setVendorId(vendorId);
        item.setCategoryId(categoryId);
        item.setName(name);
        item.setPrice(price);
        item.setPreparationTime(prepTime);
        item.setSortOrder(sortOrder);
        item.setAvailable(true);
        item.setVisible(true);
        item.setCustomizationGroupIds(groupIds);
        item.setTags(new ArrayList<>());
        return item;
    }
}
