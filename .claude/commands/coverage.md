---
description: Generate and analyze code coverage report
---

Generate code coverage report and analyze it:

1. Run `./gradlew test jacocoTestReport` to generate coverage
2. Check if the HTML report was generated at build/reports/jacoco/test/html/index.html
3. Parse the coverage data from build/reports/jacoco/test/jacocoTestReport.xml or HTML
4. Report:
   - Overall line coverage percentage
   - Coverage by package (io.hhplus.tdd.point, etc.)
   - Classes with low coverage (< 80%)
   - Suggestions for improving coverage

Provide actionable recommendations for increasing test coverage.
