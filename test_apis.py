import requests
import json

BASE_URL = "http://localhost:8080/api/v1"
test_results = []

def log_result(endpoint, method, status, message):
    res = f"| `{method}` | `{endpoint}` | {'✅ Pass' if status < 400 else '❌ Fail'} | {status} - {message} |"
    print(res)
    test_results.append(res)

print("Starting API Integration Tests...\n")
print("| Method | Endpoint | Status | Details |")
print("|--------|----------|--------|---------|")

# 1. Test Auth Register
register_url = f"{BASE_URL}/auth/register"
register_payload = {
    "shopName": "Test Shop Auto",
    "ownerName": "Auto Tester",
    "email": "auto@test.com",
    "phone": "9999999999",
    "password": "password123",
    "address": "123 Auto St"
}
try:
    r = requests.post(register_url, json=register_payload)
    if r.status_code == 200 or r.status_code == 201:
        log_result("/auth/register", "POST", r.status_code, "Registration successful")
        token = r.json().get("accessToken")
    else:
        log_result("/auth/register", "POST", r.status_code, r.text[:50])
        token = None
except Exception as e:
    log_result("/auth/register", "POST", 500, str(e))
    token = None

headers = {"Authorization": f"Bearer {token}"} if token else {}

# 2. Test Auth Login
login_url = f"{BASE_URL}/auth/login"
login_payload = {
    "email": "auto@test.com",
    "password": "password123"
}
try:
    r = requests.post(login_url, json=login_payload)
    log_result("/auth/login", "POST", r.status_code, "Login successful" if r.status_code == 200 else r.text[:50])
except Exception as e:
    log_result("/auth/login", "POST", 500, str(e))

# 3. Test Staff / Vendors using token
try:
    r = requests.get(f"{BASE_URL}/vendors/me", headers=headers)
    log_result("/vendors/me", "GET", r.status_code, "Fetched vendor profile" if r.status_code == 200 else r.text[:50])
except Exception as e:
    log_result("/vendors/me", "GET", 500, str(e))

try:
    r = requests.get(f"{BASE_URL}/staff", headers=headers)
    log_result("/staff", "GET", r.status_code, "Fetched staff list" if r.status_code == 200 else r.text[:50])
except Exception as e:
    log_result("/staff", "GET", 500, str(e))

try:
    r = requests.get(f"{BASE_URL}/dashboard/stats", headers=headers)
    log_result("/dashboard/stats", "GET", r.status_code, "Fetched stats" if r.status_code == 200 else r.text[:50])
except Exception as e:
    log_result("/dashboard/stats", "GET", 500, str(e))

print("\nTests complete.")
with open("test_report.md", "w") as f:
    f.write("# API Test Report\n\n")
    f.write("| Method | Endpoint | Status | Details |\n")
    f.write("|--------|----------|--------|---------|\n")
    f.write("\n".join(test_results))
