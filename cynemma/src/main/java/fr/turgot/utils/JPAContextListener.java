package fr.turgot.utils;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class JPAContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        JPAUtil.getEntityManagerFactory();
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        JPAUtil.shutdown();
    }
}
