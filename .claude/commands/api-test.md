---
description: Test all API endpoints from point-api.http
---

Test the Point API endpoints:

1. Check if application is running on port 8080
2. If not running, start it with `./gradlew bootRun` in background
3. Wait for the application to start (check for "Started TddApplication" in logs)
4. Execute all API test scenarios from point-api.http:
   - Valid charge (5000원)
   - Point retrieval
   - Valid use (1300원)
   - Insufficient balance case (should fail with 500)
   - History retrieval
   - Invalid charge unit (3000원 - should fail with 500)
   - Invalid use unit (150원 - should fail with 500)
5. Report which tests passed and which failed
6. For failures, explain why they failed and what the expected behavior is

Keep the application running in background for further testing.
