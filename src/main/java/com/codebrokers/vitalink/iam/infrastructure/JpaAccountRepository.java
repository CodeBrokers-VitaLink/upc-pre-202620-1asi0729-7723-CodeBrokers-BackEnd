package com.codebrokers.vitalink.iam.infrastructure;

import com.codebrokers.vitalink.iam.domain.AccountRepository;
import com.codebrokers.vitalink.iam.domain.UserAccount;
import com.codebrokers.vitalink.shared.exception.ApplicationFailure;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public class JpaAccountRepository implements AccountRepository {
    private final EntityManager entityManager;

    public JpaAccountRepository(EntityManager entityManager) { this.entityManager = entityManager; }

    public Optional<UserAccount> findById(String id) {
        return Optional.ofNullable(entityManager.find(AccountEntity.class, id)).map(AccountEntity::toDomain);
    }

    public Optional<UserAccount> findByEmail(String email) {
        return entityManager.createQuery("select a from AccountEntity a where a.email = :email", AccountEntity.class)
                .setParameter("email", email).getResultStream().findFirst().map(AccountEntity::toDomain);
    }

    public void insert(UserAccount account) {
        try {
            entityManager.persist(new AccountEntity(account));
            entityManager.flush();
        } catch (org.hibernate.exception.ConstraintViolationException ex) {
            if (ex.getConstraintName() != null && ex.getConstraintName().contains("uk_iam_accounts_email")) {
                throw new ApplicationFailure("EMAIL_ALREADY_EXISTS");
            }
            throw ex;
        }
    }
}
