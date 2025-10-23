---
description: Verify refactoring by running tests and checking git status
---

After a refactoring, verify everything still works:

1. Run `./gradlew test` to ensure all tests pass
2. Check `git status` to see what files changed
3. Show a diff of the main changed files
4. Confirm:
   - All tests pass
   - Changes are intentional and correct
   - No unintended side effects

If tests fail, explain what broke and suggest how to fix it.
If tests pass, summarize the refactoring impact and confirm it's safe to commit.
