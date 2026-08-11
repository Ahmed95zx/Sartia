package il.ac.openu.sartia.web;

import il.ac.openu.sartia.dao.Database;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Opens the connection pool when the application deploys and closes it when the
 * application shuts down.
 *
 * <p>Binding the pool to the servlet context lifecycle means the first customer
 * request does not pay for pool construction, and - more importantly - that the
 * pool's threads and sockets are released on undeploy instead of leaking every
 * time the WAR is redeployed during development.
 */
@WebListener
public class StartupListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(StartupListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            Database.init();
            LOG.info("Sartia started");
        } catch (RuntimeException failure) {
            // Logged explicitly: a container will otherwise report only that the
            // listener failed, without the database error that actually caused it.
            LOG.log(Level.SEVERE, "Startup failed - is MySQL running and db/schema.sql applied?", failure);
            throw failure;
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        Database.shutdown();
        LOG.info("Sartia stopped");
    }
}
