package com.fennixs.coreapi.persistence;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fennixs.coreapi.security.FennixsOidcUser;

class RlsTransactionManager extends JpaTransactionManager {
    RlsTransactionManager(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    @Override
    protected void doBegin(@NonNull Object transaction, @NonNull TransactionDefinition definition) {
        super.doBegin(transaction, definition);
        UUID userId = currentUserId();

        if (userId == null) return;

        try {
            DataSource dataSource = getDataSource();
            if (dataSource == null
                    || !(TransactionSynchronizationManager.getResource(dataSource) instanceof ConnectionHolder holder))
                throw new IllegalStateException("No JDBC connection is bound to the transaction");
            try (PreparedStatement statement =
                    holder.getConnection().prepareStatement("SELECT set_config('app.user_id', ?, true)")) {
                statement.setString(1, userId.toString());
                statement.execute();
            }
        } catch (SQLException | RuntimeException ex) {
            try {
                if (TransactionSynchronizationManager.getResource(obtainEntityManagerFactory())
                        instanceof EntityManagerHolder emHolder) {
                    EntityTransaction tx = emHolder.getEntityManager().getTransaction();
                    if (tx.isActive()) tx.rollback();
                }
            } catch (RuntimeException rollbackEx) {
                ex.addSuppressed(rollbackEx);
            } finally {
                doCleanupAfterCompletion(transaction);
            }
            throw new CannotCreateTransactionException("Could not set app.user_id", ex);
        }
    }

    private static @Nullable UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof FennixsOidcUser user
                ? user.getUserId()
                : null;
    }
}
