package com.dequeue.common.seed;

import com.dequeue.vendor.entity.*;
import com.dequeue.staff.entity.*;
import com.dequeue.menu.entity.*;
import com.dequeue.profile.entity.VendorProfile;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bson.Document;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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
        if (mongoTemplate.findAll(Vendor.class).size() > 0) {
            logger.info("Database already seeded. Skipping seed process.");
            return;
        }

        logger.info("Starting data seed process...");

        // 1. Create Vendor
        Vendor vendor = new Vendor();
        vendor.setShopName("Chai Corner");
        vendor.setOwnerName("Admin User");
        vendor.setVendorCode("chai-corner-x7k2");
        vendor.setPhone("+911234567890");
        vendor.setShopStatus(ShopStatus.OPEN);
        vendor.setActive(true);
        vendor = mongoTemplate.save(vendor);
        String vendorId = vendor.getId();

        // 2. Create Departments
        Department adminDept = new Department();
        adminDept.setVendorId(vendorId); adminDept.setName("Admin"); adminDept.setActive(true);
        adminDept = mongoTemplate.save(adminDept);

        Department kitchenDept = new Department();
        kitchenDept.setVendorId(vendorId); kitchenDept.setName("Kitchen"); kitchenDept.setActive(true);
        kitchenDept = mongoTemplate.save(kitchenDept);

        Department counterDept = new Department();
        counterDept.setVendorId(vendorId); counterDept.setName("Counter"); counterDept.setActive(true);
        counterDept = mongoTemplate.save(counterDept);

        // 3. Create Staff — use setters so enums serialize correctly
        Staff adminStaff = new Staff();
        adminStaff.setVendorId(vendorId);
        adminStaff.setDepartmentId(adminDept.getId());
        adminStaff.setName("Admin User");
        adminStaff.setEmail("admin@dequeue.com");
        adminStaff.setPassword(passwordEncoder.encode("admin123"));
        adminStaff.setRole(Role.ADMIN);           // stored as enum → "ADMIN"
        adminStaff.setStatus(StaffStatus.ACTIVE); // stored as enum → "ACTIVE"
        adminStaff.setPermissions(new ArrayList<>());
        mongoTemplate.save(adminStaff);

        Staff kitchenStaff = new Staff();
        kitchenStaff.setVendorId(vendorId);
        kitchenStaff.setDepartmentId(kitchenDept.getId());
        kitchenStaff.setName("Kitchen Staff 1");
        kitchenStaff.setEmail("kitchen@chaicorner.com");
        kitchenStaff.setPassword(passwordEncoder.encode("kitchen123"));
        kitchenStaff.setRole(Role.KITCHEN_STAFF);
        kitchenStaff.setStatus(StaffStatus.ACTIVE);
        kitchenStaff.setPermissions(new ArrayList<>());
        mongoTemplate.save(kitchenStaff);

        Staff counterStaff = new Staff();
        counterStaff.setVendorId(vendorId);
        counterStaff.setDepartmentId(counterDept.getId());
        counterStaff.setName("Counter Staff 1");
        counterStaff.setEmail("counter@chaicorner.com");
        counterStaff.setPassword(passwordEncoder.encode("counter123"));
        counterStaff.setRole(Role.COUNTER_STAFF);
        counterStaff.setStatus(StaffStatus.ACTIVE);
        counterStaff.setPermissions(new ArrayList<>());
        mongoTemplate.save(counterStaff);

        // 4. Create Categories
        Category hotBev = new Category();
        hotBev.setVendorId(vendorId); hotBev.setName("Hot Beverages"); hotBev.setSortOrder(1); hotBev.setActive(true);
        hotBev = mongoTemplate.save(hotBev);

        Category coldBev = new Category();
        coldBev.setVendorId(vendorId); coldBev.setName("Cold Beverages"); coldBev.setSortOrder(2); coldBev.setActive(true);
        coldBev = mongoTemplate.save(coldBev);

        Category snacks = new Category();
        snacks.setVendorId(vendorId); snacks.setName("Snacks"); snacks.setSortOrder(3); snacks.setActive(true);
        snacks = mongoTemplate.save(snacks);

        // 5. Create Customization Groups
        CustomizationGroup sizeGroup = buildCustomizationGroup(vendorId, "Size", SelectionType.SINGLE, Arrays.asList(
            buildOption("Small", BigDecimal.ZERO, 0), buildOption("Medium", BigDecimal.ZERO, 1), buildOption("Large", BigDecimal.ZERO, 2)));
        sizeGroup = mongoTemplate.save(sizeGroup);

        CustomizationGroup sugarGroup = buildCustomizationGroup(vendorId, "Sugar", SelectionType.SINGLE, Arrays.asList(
            buildOption("Less", BigDecimal.ZERO, 0), buildOption("Normal", BigDecimal.ZERO, 1), buildOption("Extra", BigDecimal.ZERO, 2)));
        sugarGroup = mongoTemplate.save(sugarGroup);

        CustomizationGroup typeGroup = buildCustomizationGroup(vendorId, "Type", SelectionType.SINGLE, Arrays.asList(
            buildOption("Hot", BigDecimal.ZERO, 0), buildOption("Cold", BigDecimal.ZERO, 1), buildOption("Iced", BigDecimal.ZERO, 2)));
        typeGroup = mongoTemplate.save(typeGroup);

        CustomizationGroup extrasGroup = buildCustomizationGroup(vendorId, "Extras", SelectionType.MULTIPLE, Arrays.asList(
            buildOption("Extra Chutney", new BigDecimal("5.00"), 0), buildOption("Cheese", new BigDecimal("10.00"), 1)));
        extrasGroup = mongoTemplate.save(extrasGroup);

        // 6. Create Menu Items
        mongoTemplate.save(buildMenuItem(vendorId, hotBev.getId(),   "Masala Chai",    new BigDecimal("1.50"), 5, 1, Arrays.asList(sizeGroup.getId(),  sugarGroup.getId())));
        mongoTemplate.save(buildMenuItem(vendorId, hotBev.getId(),   "Coffee",          new BigDecimal("2.00"), 5, 2, Arrays.asList(sizeGroup.getId(),  typeGroup.getId())));
        mongoTemplate.save(buildMenuItem(vendorId, snacks.getId(),   "Vada Pav",        new BigDecimal("1.00"), 3, 1, Arrays.asList(extrasGroup.getId())));
        mongoTemplate.save(buildMenuItem(vendorId, snacks.getId(),   "Samosa",          new BigDecimal("0.75"), 3, 2, new ArrayList<>()));
        mongoTemplate.save(buildMenuItem(vendorId, coldBev.getId(),  "Cold Coffee",     new BigDecimal("2.50"), 5, 1, new ArrayList<>()));
        mongoTemplate.save(buildMenuItem(vendorId, coldBev.getId(),  "Fresh Lime Soda", new BigDecimal("1.50"), 3, 2, Arrays.asList(typeGroup.getId())));

        // 7. Create Vendor Settings (no enum fields — raw doc is safe)
        mongoTemplate.getCollection("vendor_settings").insertOne(
            new Document("vendorId", vendorId).append("isAcceptingOrders", true).append("autoAcceptOrders", false));

        // 8. Create Vendor Profile
        mongoTemplate.getCollection("vendor_profiles").insertOne(
            new Document("vendorId", vendorId)
                .append("shopName", "Chai Corner").append("ownerName", "Admin User")
                .append("description", "Best chai and snacks in town!")
                .append("logoUrl", "https://example.com/logo.png")
                .append("bannerUrl", "https://example.com/banner.png"));

        // 9. Create QR Metadata
        mongoTemplate.getCollection("qr_metadata").insertOne(
            new Document("vendorId", vendorId)
                .append("vendorCode", "chai-corner-x7k2")
                .append("qrUrl", "http://localhost:8080/api/v1/public/qr/v/chai-corner-x7k2"));

        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        logger.info("  ✅ Seed data inserted successfully");
        logger.info("  Admin Email    : admin@dequeue.com");
        logger.info("  Admin Password : admin123");
        logger.info("  Vendor Code    : chai-corner-x7k2");
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private CustomizationGroup buildCustomizationGroup(String vendorId, String name,
                                                        SelectionType type, List<CustomizationOption> options) {
        CustomizationGroup g = new CustomizationGroup();
        g.setVendorId(vendorId); g.setName(name); g.setSelectionType(type);
        g.setRequired(false); g.setOptions(options);
        return g;
    }

    private CustomizationOption buildOption(String name, BigDecimal additionalPrice, int sortOrder) {
        CustomizationOption opt = new CustomizationOption();
        opt.setName(name); opt.setAdditionalPrice(additionalPrice);
        opt.setAvailable(true); opt.setSortOrder(sortOrder);
        return opt;
    }

    private MenuItem buildMenuItem(String vendorId, String categoryId, String name,
                                   BigDecimal price, int prepTime, int sortOrder,
                                   List<String> groupIds) {
        MenuItem item = new MenuItem();
        item.setVendorId(vendorId); item.setCategoryId(categoryId);
        item.setName(name); item.setPrice(price);
        item.setPreparationTime(prepTime); item.setSortOrder(sortOrder);
        item.setAvailable(true); item.setVisible(true);
        item.setCustomizationGroupIds(groupIds); item.setTags(new ArrayList<>());
        return item;
    }
}
