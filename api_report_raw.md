# DeQueue API Report

This report lists all API endpoints discovered in the application, along with their HTTP methods and required roles.

## AuthController.java

### `POST /api/v1/auth/login`
- **Method Name:** `login`
- **Roles Required:** `None (Public or Authenticated Default)`

### `POST /api/v1/auth/register`
- **Method Name:** `register`
- **Roles Required:** `None (Public or Authenticated Default)`

### `POST /api/v1/auth/refresh`
- **Method Name:** `refresh`
- **Roles Required:** `None (Public or Authenticated Default)`

### `POST /api/v1/auth/logout`
- **Method Name:** `logout`
- **Roles Required:** `None (Public or Authenticated Default)`

### `GET /api/v1/auth/me`
- **Method Name:** `getCurrentUser`
- **Roles Required:** `None (Public or Authenticated Default)`

## DashboardController.java

### `GET /api/v1/dashboard`
- **Method Name:** `getDashboard`
- **Roles Required:** `isAuthenticated() (Inherited from class)`

### `GET /api/v1/dashboard/stats`
- **Method Name:** `getStats`
- **Roles Required:** `isAuthenticated() (Inherited from class)`

### `GET /api/v1/dashboard/recent-orders`
- **Method Name:** `getRecentOrders`
- **Roles Required:** `isAuthenticated() (Inherited from class)`

## GeofenceController.java

### `GET /api/v1/geofence/`
- **Method Name:** `getGeofenceSettings`
- **Roles Required:** `None (Public or Authenticated Default)`

### `PUT /api/v1/geofence/`
- **Method Name:** `updateGeofenceSettings`
- **Roles Required:** `None (Public or Authenticated Default)`

### `POST /api/v1/geofence/validate`
- **Method Name:** `validateLocation`
- **Roles Required:** `None (Public or Authenticated Default)`

## ImageController.java

### `POST /api/v1/images/upload`
- **Method Name:** `upload`
- **Roles Required:** `None (Public or Authenticated Default)`

### `DELETE /api/v1/images/{publicId}`
- **Method Name:** `delete`
- **Roles Required:** `None (Public or Authenticated Default)`

## CategoryController.java

### `GET /api/v1/categories`
- **Method Name:** `getCategories`
- **Roles Required:** `None (Public or Authenticated Default)`

### `GET /api/v1/categories/{id}`
- **Method Name:** `getCategory`
- **Roles Required:** `None (Public or Authenticated Default)`

### `POST /api/v1/categories`
- **Method Name:** `createCategory`
- **Roles Required:** `None (Public or Authenticated Default)`

### `PUT /api/v1/categories/{id}`
- **Method Name:** `updateCategory`
- **Roles Required:** `None (Public or Authenticated Default)`

### `DELETE /api/v1/categories/{id}`
- **Method Name:** `deleteCategory`
- **Roles Required:** `None (Public or Authenticated Default)`

### `PUT /api/v1/categories/sort`
- **Method Name:** `updateSort`
- **Roles Required:** `None (Public or Authenticated Default)`

## CustomizationController.java

### `GET /api/v1/customizations`
- **Method Name:** `getGroups`
- **Roles Required:** `None (Public or Authenticated Default)`

### `GET /api/v1/customizations/{id}`
- **Method Name:** `getGroup`
- **Roles Required:** `None (Public or Authenticated Default)`

### `POST /api/v1/customizations`
- **Method Name:** `createGroup`
- **Roles Required:** `None (Public or Authenticated Default)`

### `PUT /api/v1/customizations/{id}`
- **Method Name:** `updateGroup`
- **Roles Required:** `None (Public or Authenticated Default)`

### `DELETE /api/v1/customizations/{id}`
- **Method Name:** `deleteGroup`
- **Roles Required:** `None (Public or Authenticated Default)`

## MenuController.java

### `GET /api/v1/menu/items`
- **Method Name:** `getItems`
- **Roles Required:** `None (Public or Authenticated Default)`

### `GET /api/v1/menu/items/{id}`
- **Method Name:** `getItem`
- **Roles Required:** `None (Public or Authenticated Default)`

### `POST /api/v1/menu/items`
- **Method Name:** `createItem`
- **Roles Required:** `None (Public or Authenticated Default)`

### `PUT /api/v1/menu/items/{id}`
- **Method Name:** `updateItem`
- **Roles Required:** `None (Public or Authenticated Default)`

### `DELETE /api/v1/menu/items/{id}`
- **Method Name:** `deleteItem`
- **Roles Required:** `None (Public or Authenticated Default)`

### `PATCH /api/v1/menu/items/{id}/availability`
- **Method Name:** `toggleAvailability`
- **Roles Required:** `None (Public or Authenticated Default)`

### `PATCH /api/v1/menu/items/{id}/visibility`
- **Method Name:** `toggleVisibility`
- **Roles Required:** `None (Public or Authenticated Default)`

### `PUT /api/v1/menu/items/sort`
- **Method Name:** `updateSort`
- **Roles Required:** `None (Public or Authenticated Default)`

## PublicMenuController.java

### `GET /api/v1/public/menu/{vendorCode}/categories`
- **Method Name:** `getMenu`
- **Roles Required:** `None (Public or Authenticated Default)`

### `GET /api/v1/public/menu/{vendorCode}/items/{itemId}`
- **Method Name:** `getItem`
- **Roles Required:** `None (Public or Authenticated Default)`

## OrderController.java

### `GET /api/v1/orders`
- **Method Name:** `listOrders`
- **Roles Required:** `isAuthenticated() (Inherited from class)`

### `GET /api/v1/orders/{id}`
- **Method Name:** `getOrder`
- **Roles Required:** `isAuthenticated() (Inherited from class)`

### `PATCH /api/v1/orders/{id}/status`
- **Method Name:** `updateStatus`
- **Roles Required:** `isAuthenticated() (Inherited from class)`

### `GET /api/v1/orders/active`
- **Method Name:** `getActiveOrders`
- **Roles Required:** `isAuthenticated() (Inherited from class)`

### `GET /api/v1/orders/today`
- **Method Name:** `getTodaySummary`
- **Roles Required:** `isAuthenticated() (Inherited from class)`

### `GET /api/v1/orders/history`
- **Method Name:** `getHistory`
- **Roles Required:** `isAuthenticated() (Inherited from class)`

## PublicOrderController.java

### `POST /api/v1/public/orders/{vendorCode}`
- **Method Name:** `placeOrder`
- **Roles Required:** `None (Public or Authenticated Default)`

### `GET /api/v1/public/orders/{vendorCode}/track/{queueNumber}`
- **Method Name:** `trackOrder`
- **Roles Required:** `None (Public or Authenticated Default)`

### `GET /api/v1/public/orders/{vendorCode}/active`
- **Method Name:** `getActiveSessionOrders`
- **Roles Required:** `None (Public or Authenticated Default)`

### `POST /api/v1/public/orders/{vendorCode}/custom`
- **Method Name:** `placeCustomOrder`
- **Roles Required:** `None (Public or Authenticated Default)`

## ProfileController.java

### `GET /api/v1/profile/`
- **Method Name:** `getProfile`
- **Roles Required:** `hasAnyRole('ADMIN', 'MANAGER') (Inherited from class)`

### `PUT /api/v1/profile/`
- **Method Name:** `updateProfile`
- **Roles Required:** `hasAnyRole('ADMIN', 'MANAGER') (Inherited from class)`

### `PATCH /api/v1/profile/logo`
- **Method Name:** `uploadLogo`
- **Roles Required:** `hasAnyRole('ADMIN', 'MANAGER') (Inherited from class)`

### `PATCH /api/v1/profile/banner`
- **Method Name:** `uploadBanner`
- **Roles Required:** `hasAnyRole('ADMIN', 'MANAGER') (Inherited from class)`

## PublicQrController.java

### `GET /api/v1/public/qr/v/{vendorCode}`
- **Method Name:** `redirect`
- **Roles Required:** `None (Public or Authenticated Default)`

## QrController.java

### `GET /api/v1/qr`
- **Method Name:** `get`
- **Roles Required:** `None (Public or Authenticated Default)`

### `POST /api/v1/qr/generate`
- **Method Name:** `generate`
- **Roles Required:** `None (Public or Authenticated Default)`

### `GET /api/v1/qr/download`
- **Method Name:** `download`
- **Roles Required:** `None (Public or Authenticated Default)`

## PublicQueueController.java

### `GET /api/v1/public/queue/{vendorCode}`
- **Method Name:** `getLiveQueue`
- **Roles Required:** `None (Public or Authenticated Default)`

### `GET /api/v1/public/queue/{vendorCode}/position/{queueNumber}`
- **Method Name:** `getPosition`
- **Roles Required:** `None (Public or Authenticated Default)`

### `GET /api/v1/public/queue/{vendorCode}/poll`
- **Method Name:** `pollQueue`
- **Roles Required:** `None (Public or Authenticated Default)`

## QueueController.java

### `GET /api/v1/queue`
- **Method Name:** `getQueueState`
- **Roles Required:** `isAuthenticated() (Inherited from class)`

### `GET /api/v1/queue/current`
- **Method Name:** `getCurrentOrder`
- **Roles Required:** `isAuthenticated() (Inherited from class)`

### `PATCH /api/v1/queue/next`
- **Method Name:** `moveToNext`
- **Roles Required:** `isAuthenticated() (Inherited from class)`

### `GET /api/v1/queue/stats`
- **Method Name:** `getQueueStats`
- **Roles Required:** `isAuthenticated() (Inherited from class)`

## ReportsController.java

### `GET /api/v1/reports/today`
- **Method Name:** `getTodayReport`
- **Roles Required:** `isAuthenticated() (Inherited from class)`

### `GET /api/v1/reports/orders`
- **Method Name:** `getOrderReport`
- **Roles Required:** `isAuthenticated() (Inherited from class)`

### `GET /api/v1/reports/popular-items`
- **Method Name:** `getPopularItems`
- **Roles Required:** `isAuthenticated() (Inherited from class)`

### `GET /api/v1/reports/peak-hours`
- **Method Name:** `getPeakHours`
- **Roles Required:** `isAuthenticated() (Inherited from class)`

### `GET /api/v1/reports/queue-stats`
- **Method Name:** `getQueueStats`
- **Roles Required:** `isAuthenticated() (Inherited from class)`

### `GET /api/v1/reports/summary`
- **Method Name:** `getSummary`
- **Roles Required:** `isAuthenticated() (Inherited from class)`

## SettingsController.java

### `GET /api/v1/settings/`
- **Method Name:** `getSettings`
- **Roles Required:** `hasAnyRole('ADMIN', 'MANAGER') (Inherited from class)`

### `PUT /api/v1/settings/`
- **Method Name:** `updateAllSettings`
- **Roles Required:** `hasAnyRole('ADMIN', 'MANAGER') (Inherited from class)`

### `PATCH /api/v1/settings/orders`
- **Method Name:** `updateOrderSettings`
- **Roles Required:** `hasAnyRole('ADMIN', 'MANAGER') (Inherited from class)`

### `PATCH /api/v1/settings/queue`
- **Method Name:** `updateQueueSettings`
- **Roles Required:** `hasAnyRole('ADMIN', 'MANAGER') (Inherited from class)`

### `PATCH /api/v1/settings/notifications`
- **Method Name:** `updateNotificationSettings`
- **Roles Required:** `hasAnyRole('ADMIN', 'MANAGER') (Inherited from class)`

### `PATCH /api/v1/settings/display`
- **Method Name:** `updateDisplaySettings`
- **Roles Required:** `hasAnyRole('ADMIN', 'MANAGER') (Inherited from class)`

## DepartmentController.java

### `GET /api/v1/departments`
- **Method Name:** `getAll`
- **Roles Required:** `None (Public or Authenticated Default)`

### `GET /api/v1/departments/{id}`
- **Method Name:** `getById`
- **Roles Required:** `None (Public or Authenticated Default)`

## StaffController.java

### `GET /api/v1/staff`
- **Method Name:** `getAll`
- **Roles Required:** `None (Public or Authenticated Default)`

### `GET /api/v1/staff/{id}`
- **Method Name:** `getById`
- **Roles Required:** `None (Public or Authenticated Default)`

### `GET /api/v1/staff/departments/{departmentId}`
- **Method Name:** `getByDepartment`
- **Roles Required:** `None (Public or Authenticated Default)`

## PublicVendorController.java

### `GET /api/v1/public/vendors/{vendorCode}`
- **Method Name:** `getVendorByCode`
- **Roles Required:** `None (Public or Authenticated Default)`

### `GET /api/v1/public/vendors/{vendorCode}/status`
- **Method Name:** `getVendorStatus`
- **Roles Required:** `None (Public or Authenticated Default)`

## VendorController.java

### `GET /api/v1/vendors/me`
- **Method Name:** `getCurrentVendor`
- **Roles Required:** `isAuthenticated() (Inherited from class)`

### `PUT /api/v1/vendors/me`
- **Method Name:** `updateVendor`
- **Roles Required:** `isAuthenticated() (Inherited from class)`

### `PATCH /api/v1/vendors/me/status`
- **Method Name:** `updateShopStatus`
- **Roles Required:** `isAuthenticated() (Inherited from class)`

### `GET /api/v1/vendors/me/status`
- **Method Name:** `getShopStatus`
- **Roles Required:** `isAuthenticated() (Inherited from class)`
