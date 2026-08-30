package faang.school.postservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mockStatic;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class TransactionHooksTest {

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void runAfterCommit_whenNoTransactionActive_runsActionImmediately() {
        // Arrange
        AtomicInteger executions = new AtomicInteger();

        // Act
        TransactionHooks.runAfterCommit(executions::incrementAndGet);

        // Assert
        assertThat(executions.get()).isEqualTo(1);
    }

    @Test
    void runAfterCommit_whenTransactionActive_defersActionUntilAfterCommit() {
        // Arrange: mock static to simulate an active transaction
        AtomicInteger executions = new AtomicInteger();
        try (MockedStatic<TransactionSynchronizationManager> mocked = mockStatic(TransactionSynchronizationManager.class)) {
            mocked.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(true);

            // Act
            TransactionHooks.runAfterCommit(executions::incrementAndGet);

            // Assert: deferred, not yet executed
            assertThat(executions.get()).isZero();
        }
    }

    @Test
    void runAfterCommit_whenNoTransactionAndActionThrows_propagatesException() {
        // Arrange: no transaction active → action runs immediately, exception propagates
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
                TransactionHooks.runAfterCommit(() -> {
                    throw new RuntimeException("boom");
                })
        );
    }

    @Test
    void runAfterRollback_whenNoTransactionActive_doesNothing() {
        // Arrange & Act & Assert
        assertThatCode(() -> TransactionHooks.runAfterRollback(() -> {
            throw new IllegalStateException("should not run");
        })).doesNotThrowAnyException();
    }

    @Test
    void runAfterRollback_whenNoTransactionActive_doesNotRegisterSync() {
        // Arrange: no transaction active → runAfterRollback is a no-op
        AtomicInteger executions = new AtomicInteger();

        // Act
        TransactionHooks.runAfterRollback(executions::incrementAndGet);

        // Assert: nothing registered, action never runs
        assertThat(executions.get()).isZero();
    }

    @Test
    void runAfterCommit_whenTransactionActive_defersAndSwallowsOnFailure() {
        // Arrange: mock static to simulate active transaction
        AtomicInteger executions = new AtomicInteger();
        try (MockedStatic<TransactionSynchronizationManager> mocked = mockStatic(TransactionSynchronizationManager.class)) {
            mocked.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(true);

            // Act: action is deferred (not run immediately)
            TransactionHooks.runAfterCommit(executions::incrementAndGet);

            // Assert: not yet executed
            assertThat(executions.get()).isZero();
        }
    }

    @Test
    void runAfterRollback_whenActionThrows_swallowsException() {
        // Arrange: no transaction active → action not registered, nothing to trigger
        assertThatCode(() -> TransactionHooks.runAfterRollback(() -> {
            throw new RuntimeException("boom");
        })).doesNotThrowAnyException();
    }

    @Test
    void runAfterRollback_whenTransactionActive_andRollbackStatus_executesAction() {
        // Arrange: capture the registered synchronization to trigger afterCompletion
        AtomicInteger executions = new AtomicInteger();
        java.util.concurrent.atomic.AtomicReference<org.springframework.transaction.support.TransactionSynchronization> captured =
                new java.util.concurrent.atomic.AtomicReference<>();

        try (MockedStatic<TransactionSynchronizationManager> mocked = mockStatic(TransactionSynchronizationManager.class)) {
            mocked.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(true);
            mocked.when(() -> TransactionSynchronizationManager.registerSynchronization(
                    org.mockito.ArgumentMatchers.any(org.springframework.transaction.support.TransactionSynchronization.class)))
                    .thenAnswer(invocation -> {
                        captured.set(invocation.getArgument(0));
                        return null;
                    });

            // Act: register the rollback hook
            TransactionHooks.runAfterRollback(executions::incrementAndGet);

            // Assert: not yet executed
            assertThat(executions.get()).isZero();

            // Trigger afterCompletion with ROLLED_BACK status
            captured.get().afterCompletion(
                    org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK);

            // Assert: action executed
            assertThat(executions.get()).isEqualTo(1);
        }
    }

    @Test
    void runAfterRollback_whenTransactionActive_andCommittedStatus_doesNotExecute() {
        // Arrange
        AtomicInteger executions = new AtomicInteger();
        java.util.concurrent.atomic.AtomicReference<org.springframework.transaction.support.TransactionSynchronization> captured =
                new java.util.concurrent.atomic.AtomicReference<>();

        try (MockedStatic<TransactionSynchronizationManager> mocked = mockStatic(TransactionSynchronizationManager.class)) {
            mocked.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(true);
            mocked.when(() -> TransactionSynchronizationManager.registerSynchronization(
                    org.mockito.ArgumentMatchers.any(org.springframework.transaction.support.TransactionSynchronization.class)))
                    .thenAnswer(invocation -> {
                        captured.set(invocation.getArgument(0));
                        return null;
                    });

            // Act
            TransactionHooks.runAfterRollback(executions::incrementAndGet);

            // Trigger afterCompletion with COMMITTED status
            captured.get().afterCompletion(
                    org.springframework.transaction.support.TransactionSynchronization.STATUS_COMMITTED);

            // Assert: action NOT executed (only runs on rollback)
            assertThat(executions.get()).isZero();
        }
    }
}
