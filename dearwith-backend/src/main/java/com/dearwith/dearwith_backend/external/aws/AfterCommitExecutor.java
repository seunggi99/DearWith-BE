package com.dearwith.dearwith_backend.external.aws;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
public class AfterCommitExecutor {
    public void run(Runnable task) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    try { task.run(); } catch (Throwable t) {
                        log.error("[after-commit] task failed: {}", t, t);
                    }
                }
            });
        } else {
            try { task.run(); } catch (Throwable t) {
                log.error("[after-commit] immediate task failed: {}", t, t);
            }
        }
    }
}
