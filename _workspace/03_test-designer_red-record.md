# RED: user time limit HTTP behavior

- Test: TimeLimitControllerTest.put_replacesCurrentUsersLimitWithoutCreatingAnotherRow
- Command: ./gradlew test --tests 'com.detoxmate.applock.controller.TimeLimitControllerTest.put_replacesCurrentUsersLimitWithoutCreatingAnotherRow'
- Location: isolated committed baseline at /private/tmp/detoxmate-time-limit-baseline-9qq957xr
- Result: compileTestJava succeeded; Spring context loaded; 1 test failed.
- Failure: java.lang.AssertionError: Status expected:<200> but was:<404>
- Cause: existing implementation has no PUT /me/time-limit. This is requirements RED. Initial user-WIP compile failure was separately recorded and not used as RED.
- No assistant production edits before this RED. Test copied unchanged into baseline; source remains in the real working tree for GREEN.
- HTTP test includes real user/JWT/DB and exact JSON response plus per-user row count, without new implementation class dependency.

## Additional RED: FK physical user deletion

Command: ./gradlew test --tests 'com.detoxmate.applock.repository.TimeLimitRepositoryTest.deleteUser_removesOnlyOwnersTimeLimit'
Current working tree: compileTestJava succeeded; one test failed at userRepository.flush with DataIntegrityViolationException / H2 FK constraint. Manual migration expects ON DELETE CASCADE, but the initial JPA mapping did not create it. This is behavior RED before adding @OnDelete. SQL defines physical deletion only; existing soft-withdrawal policy is not changed.
