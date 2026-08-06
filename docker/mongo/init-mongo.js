db = db.getSiblingDB('dequeue_db');

// Create collections
db.createCollection('vendors');
db.createCollection('staff');
db.createCollection('departments');
db.createCollection('categories');
db.createCollection('menu_items');
db.createCollection('customization_groups');
db.createCollection('orders');
db.createCollection('qr_metadata');
db.createCollection('vendor_profiles');
db.createCollection('vendor_settings');
db.createCollection('refresh_tokens');
db.createCollection('audit_logs');

// Create indexes
db.vendors.createIndex({ vendorCode: 1 }, { unique: true });
db.vendors.createIndex({ email: 1 }, { unique: true });

db.staff.createIndex({ email: 1 }, { unique: true });
db.staff.createIndex({ vendorId: 1, departmentId: 1 });

db.categories.createIndex({ vendorId: 1 });

db.menu_items.createIndex({ vendorId: 1 });
db.menu_items.createIndex({ vendorId: 1, categoryId: 1 });

db.customization_groups.createIndex({ vendorId: 1 });

db.orders.createIndex({ vendorId: 1 });
db.orders.createIndex({ vendorId: 1, status: 1 });
db.orders.createIndex({ vendorId: 1, queueNumber: 1 });
db.orders.createIndex({ vendorId: 1, createdAt: 1 });

db.qr_metadata.createIndex({ vendorId: 1 }, { unique: true });
db.qr_metadata.createIndex({ vendorCode: 1 }, { unique: true });

db.vendor_profiles.createIndex({ vendorId: 1 }, { unique: true });

db.vendor_settings.createIndex({ vendorId: 1 }, { unique: true });

db.refresh_tokens.createIndex({ token: 1 }, { unique: true });
db.refresh_tokens.createIndex({ staffId: 1 });

db.audit_logs.createIndex({ vendorId: 1, timestamp: -1 });

print("Database initialized successfully!");
