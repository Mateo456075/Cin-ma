package fr.turgot.utils;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public final class JPAUtil {
    private JPAUtil() {
    }

    public static EntityManagerFactory getEntityManagerFactory() {
        return EntityManagerFactoryHolder.INSTANCE;
    }

    public static void shutdown() {
        synchronized (JPAUtil.class) {
            EntityManagerFactory factory = EntityManagerFactoryHolder.INSTANCE;
            if (factory.isOpen()) {
                factory.close();
            }
        }
    }

    private static class EntityManagerFactoryHolder {
        private static final EntityManagerFactory INSTANCE =
                Persistence.createEntityManagerFactory("cynemma");
    }
}
