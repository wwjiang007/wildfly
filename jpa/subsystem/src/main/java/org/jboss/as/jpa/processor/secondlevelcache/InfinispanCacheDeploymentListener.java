/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.processor.secondlevelcache;

import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;

import java.security.AccessController;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jipijapa.cache.spi.Classification;
import org.jipijapa.cache.spi.Wrapper;
import org.jipijapa.event.spi.EventListener;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;

/**
 * InfinispanCacheDeploymentListener adds Infinispan second level cache dependencies during application deployment.
 *
 * @author Scott Marlow
 * @author Paul Ferraro
 */
public class InfinispanCacheDeploymentListener implements EventListener {

    public static final String CACHE_TYPE = "cachetype";    // shared (Jakarta Persistence) or private (for native applications)
    public static final String CACHE_PRIVATE = "private";
    public static final String CONTAINER = "container";
    public static final String NAME = "name";
    public static final String CACHES = "caches";
    public static final String PENDING_PUTS = "pending-puts";

    public static final String DEFAULT_CACHE_CONTAINER = "hibernate";

    @Override
    public void beforeEntityManagerFactoryCreate(Classification classification, PersistenceUnitMetadata persistenceUnitMetadata) {

    }

    @Override
    public void afterEntityManagerFactoryCreate(Classification classification, PersistenceUnitMetadata persistenceUnitMetadata) {

    }

    @Override
    public Wrapper startCache(Classification classification, Properties properties) throws Exception {
        ServiceContainer target = currentServiceContainer();
        String container = properties.getProperty(CONTAINER);
        String cacheType = properties.getProperty(CACHE_TYPE);
        // TODO Figure out how to access CapabilityServiceSupport from here
        ServiceName containerServiceName = ServiceNameFactory.resolveServiceName(InfinispanServiceDescriptor.CACHE_CONTAINER, container);

        // need a private cache for non-jpa application use
        String name = properties.getProperty(NAME, UUID.randomUUID().toString());

        ServiceBuilder<?> builder = target.addService(ServiceName.JBOSS.append(DEFAULT_CACHE_CONTAINER, name));
        Supplier<EmbeddedCacheManager> manager = builder.requires(containerServiceName);

        if (CACHE_PRIVATE.equals(cacheType)) {
            // If using a private cache, addCacheDependencies(...) is never triggered
            String[] caches = properties.getProperty(CACHES).split("\\s+");
            for (String cache : caches) {
                builder.requires(ServiceNameFactory.resolveServiceName(InfinispanServiceDescriptor.CACHE_CONFIGURATION, container, cache));
            }
        }
        final CountDownLatch latch = new CountDownLatch(1);
        builder.addListener(new LifecycleListener() {
            @Override
            public void handleEvent(final ServiceController<?> controller, final LifecycleEvent event) {
                if (event == LifecycleEvent.UP) {
                    latch.countDown();
                    controller.removeListener(this);
                }
            }
        });
        ServiceController<?> controller = builder.install();
        // Ensure cache configuration services are started
        latch.await();

        return new CacheWrapper(manager.get(), controller);
    }

    @Override
    public void addCacheDependencies(Classification classification, Properties properties) {
        ServiceBuilder<?> builder = CacheDeploymentListener.getInternalDeploymentServiceBuilder();
        CapabilityServiceSupport support = CacheDeploymentListener.getInternalDeploymentCapablityServiceSupport();
        String container = properties.getProperty(CONTAINER);
        for (String cache : properties.getProperty(CACHES).split("\\s+")) {
            // Workaround for legacy default configuration, where the pending-puts cache configuration is missing
            if (cache.equals(PENDING_PUTS) ? support.hasCapability(InfinispanServiceDescriptor.CACHE_CONFIGURATION, container, cache) : true) {
                builder.requires(support.getCapabilityServiceName(InfinispanServiceDescriptor.CACHE_CONFIGURATION, container, cache));
            }
        }
    }

    @Override
    public void stopCache(Classification classification, Wrapper wrapper) {
        // Remove services created in startCache(...)
        ((CacheWrapper) wrapper).close();
    }

    private static class CacheWrapper implements Wrapper, AutoCloseable {

        private final EmbeddedCacheManager embeddedCacheManager;
        private final ServiceController<?> controller;

        CacheWrapper(EmbeddedCacheManager embeddedCacheManager, ServiceController<?> controller) {
            this.embeddedCacheManager = embeddedCacheManager;
            this.controller = controller;
        }

        @Override
        public Object getValue() {
            return this.embeddedCacheManager;
        }

        @Override
        public void close() {
            if (ROOT_LOGGER.isTraceEnabled()) {
                ROOT_LOGGER.tracef("stop second level cache by removing dependency on service '%s'", this.controller.getName().getCanonicalName());
            }
            final CountDownLatch latch = new CountDownLatch(1);
            controller.addListener(new LifecycleListener() {
                @Override
                public void handleEvent(final ServiceController<?> controller, final LifecycleEvent event) {
                    if (event == LifecycleEvent.REMOVED) latch.countDown();
                }
            });
            controller.setMode(ServiceController.Mode.REMOVE);
            try {
                latch.await();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static ServiceContainer currentServiceContainer() {
        if (System.getSecurityManager() == null) {
            return CurrentServiceContainer.getServiceContainer();
        }
        return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
    }
}
