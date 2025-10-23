---
description: Run all tests and verify implementation
---

Run the full test suite and analyze the results:

1. Execute `./gradlew test` to run all tests
2. Check if any tests failed
3. If tests failed:
   - Analyze the failure reasons from test results in build/test-results/test/
   - Identify which component failed (PointService, PointController, UserPoint, etc.)
   - Suggest fixes based on the failure
4. If all tests passed:
   - Confirm all tests passed successfully
   - Show a summary of test counts by class

Do NOT modify any code unless explicitly asked. Just report the test results and provide analysis.
