package faang.school.postservice.service;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@UtilityClass
@Slf4j
public class TransactionHooks {

    public static void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runSafely(action, "after-commit");
                }
            });
            return;
        }

        action.run();
    }

    public static void runAfterRollback(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        runSafely(action, "after-rollback");
                    }
                }
            });
        }
    }

    private static void runSafely(Runnable action, String phase) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            log.error("Transaction {} action failed", phase, ex);
        }
    }
}
