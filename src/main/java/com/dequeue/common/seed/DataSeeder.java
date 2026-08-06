package com.dequeue.common.seed;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Arrays;
import org.bson.Document;

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
        long count = mongoTemplate.getCollection("vendors").countDocuments();
        if (count > 0) {
            logger.info("Database already seeded. Skipping seed process.");
            return;
        }

        logger.info("Starting data seed process...");

        // 1. Create Vendor
        String vendorId = UUID.randomUUID().toString();
        Document vendor = new Document("_id", vendorId)
            .append("shopName", "Chai Corner")
            .append("vendorCode", "chai-corner-x7k2")
            .append("email", "contact@chaicorner.com")
            .append("active", true)
            .append("createdAt", Instant.now());
        mongoTemplate.getCollection("vendors").insertOne(vendor);

        // 2. Create Departments
        String adminDeptId = UUID.randomUUID().toString();
        String kitchenDeptId = UUID.randomUUID().toString();
        String counterDeptId = UUID.randomUUID().toString();
        
        List<Document> departments = Arrays.asList(
            new Document("_id", adminDeptId).append("vendorId", vendorId).append("name", "Admin"),
            new Document("_id", kitchenDeptId).append("vendorId", vendorId).append("name", "Kitchen"),
            new Document("_id", counterDeptId).append("vendorId", vendorId).append("name", "Counter")
        );
        mongoTemplate.getCollection("departments").insertMany(departments);

        // 3. Create Staff
        List<Document> staff = Arrays.asList(
            new Document("_id", UUID.randomUUID().toString())
                .append("vendorId", vendorId)
                .append("departmentId", adminDeptId)
                .append("name", "Admin User")
                .append("email", "admin@dequeue.com")
                .append("password", passwordEncoder.encode("admin123"))
                .append("role", "ADMIN")
                .append("status", "ACTIVE"),
            new Document("_id", UUID.randomUUID().toString())
                .append("vendorId", vendorId)
                .append("departmentId", kitchenDeptId)
                .append("name", "Kitchen Staff 1")
                .append("email", "kitchen@chaicorner.com")
                .append("password", passwordEncoder.encode("kitchen123"))
                .append("role", "KITCHEN_STAFF")
                .append("status", "ACTIVE"),
            new Document("_id", UUID.randomUUID().toString())
                .append("vendorId", vendorId)
                .append("departmentId", counterDeptId)
                .append("name", "Counter Staff 1")
                .append("email", "counter@chaicorner.com")
                .append("password", passwordEncoder.encode("counter123"))
                .append("role", "COUNTER_STAFF")
                .append("status", "ACTIVE")
        );
        mongoTemplate.getCollection("staff").insertMany(staff);

        // 4. Create Categories
        String hotBevId = UUID.randomUUID().toString();
        String coldBevId = UUID.randomUUID().toString();
        String snacksId = UUID.randomUUID().toString();
        
        List<Document> categories = Arrays.asList(
            new Document("_id", hotBevId).append("vendorId", vendorId).append("name", "Hot Beverages"),
            new Document("_id", coldBevId).append("vendorId", vendorId).append("name", "Cold Beverages"),
            new Document("_id", snacksId).append("vendorId", vendorId).append("name", "Snacks")
        );
        mongoTemplate.getCollection("categories").insertMany(categories);

        // 5. Create Customization Groups
        String sizeGroupId = UUID.randomUUID().toString();
        String sugarGroupId = UUID.randomUUID().toString();
        String typeGroupId = UUID.randomUUID().toString();
        String extrasGroupId = UUID.randomUUID().toString();

        List<Document> customizationGroups = Arrays.asList(
            new Document("_id", sizeGroupId).append("vendorId", vendorId).append("name", "Size")
                .append("options", Arrays.asList("Small", "Medium", "Large")),
            new Document("_id", sugarGroupId).append("vendorId", vendorId).append("name", "Sugar")
                .append("options", Arrays.asList("Less", "Normal", "Extra")),
            new Document("_id", typeGroupId).append("vendorId", vendorId).append("name", "Type")
                .append("options", Arrays.asList("Hot", "Cold", "Iced", "Sweet", "Salt", "Mixed")),
            new Document("_id", extrasGroupId).append("vendorId", vendorId).append("name", "Extras")
                .append("options", Arrays.asList("Extra Chutney", "Cheese"))
        );
        mongoTemplate.getCollection("customization_groups").insertMany(customizationGroups);

        // 6. Create Menu Items
        List<Document> menuItems = Arrays.asList(
            new Document("_id", UUID.randomUUID().toString())
                .append("vendorId", vendorId).append("categoryId", hotBevId).append("name", "Masala Chai")
                .append("price", 1.50).append("customizationGroupIds", Arrays.asList(sizeGroupId, sugarGroupId)),
            new Document("_id", UUID.randomUUID().toString())
                .append("vendorId", vendorId).append("categoryId", hotBevId).append("name", "Coffee")
                .append("price", 2.00).append("customizationGroupIds", Arrays.asList(sizeGroupId, typeGroupId)),
            new Document("_id", UUID.randomUUID().toString())
                .append("vendorId", vendorId).append("categoryId", snacksId).append("name", "Vada Pav")
                .append("price", 1.00).append("customizationGroupIds", Arrays.asList(extrasGroupId)),
            new Document("_id", UUID.randomUUID().toString())
                .append("vendorId", vendorId).append("categoryId", snacksId).append("name", "Samosa")
                .append("price", 0.75).append("customizationGroupIds", Arrays.asList()),
            new Document("_id", UUID.randomUUID().toString())
                .append("vendorId", vendorId).append("categoryId", coldBevId).append("name", "Cold Coffee")
                .append("price", 2.50).append("customizationGroupIds", Arrays.asList()),
            new Document("_id", UUID.randomUUID().toString())
                .append("vendorId", vendorId).append("categoryId", coldBevId).append("name", "Fresh Lime Soda")
                .append("price", 1.50).append("customizationGroupIds", Arrays.asList(typeGroupId))
        );
        mongoTemplate.getCollection("menu_items").insertMany(menuItems);

        // 7. Create Vendor Settings
        Document vendorSettings = new Document("_id", UUID.randomUUID().toString())
            .append("vendorId", vendorId)
            .append("isAcceptingOrders", true)
            .append("autoAcceptOrders", false);
        mongoTemplate.getCollection("vendor_settings").insertOne(vendorSettings);

        // 8. Create Vendor Profile
        Document vendorProfile = new Document("_id", UUID.randomUUID().toString())
            .append("vendorId", vendorId)
            .append("description", "Best chai and snacks in town!")
            .append("logoUrl", "https://example.com/logo.png")
            .append("bannerUrl", "https://example.com/banner.png");
        mongoTemplate.getCollection("vendor_profiles").insertOne(vendorProfile);

        // 9. Create QR Metadata
        Document qrMetadata = new Document("_id", UUID.randomUUID().toString())
            .append("vendorId", vendorId)
            .append("vendorCode", "chai-corner-x7k2")
            .append("qrUrl", "https://dequeue.com/qr/chai-corner-x7k2");
        mongoTemplate.getCollection("qr_metadata").insertOne(qrMetadata);

        logger.info("Data seeding completed successfully.");
        logger.info("Admin Credentials - Email: admin@dequeue.com | Password: admin123");
        logger.info("Vendor Code: chai-corner-x7k2");
    }
}

