/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming.subsystem;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.naming.NamingContext;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.context.external.ExternalContexts;
import org.jboss.as.naming.context.external.ExternalContextsNavigableSet;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.ExternalContextsProcessor;
import org.jboss.as.naming.deployment.JdkDependenciesProcessor;
import org.jboss.as.naming.deployment.JndiNamingDependencyProcessor;
import org.jboss.as.naming.management.JndiViewExtensionRegistry;
import org.jboss.as.naming.remote.HttpRemoteNamingServerService;
import org.jboss.as.naming.service.DefaultNamespaceContextSelectorService;
import org.jboss.as.naming.service.ExternalContextsService;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.naming.service.NamingStoreService;
import org.jboss.as.naming.subsystem.NamingSubsystemRootResourceDefinition.Capability;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

import static org.jboss.as.naming.logging.NamingLogger.ROOT_LOGGER;

import io.undertow.server.handlers.PathHandler;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 * @author John Bailey
 * @author Eduardo Martins
 */
public class NamingSubsystemAdd extends AbstractBoottimeAddStepHandler {

    private static final String UNDERTOW_HTTP_INVOKER_CAPABILITY_NAME = "org.wildfly.undertow.http-invoker";

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) {
        model.setEmptyObject();
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) {

        ROOT_LOGGER.activatingSubsystem();

        NamingContext.initializeNamingManager();
        final ServiceTarget target = context.getServiceTarget();

        // Create the java: namespace
        target.addService(ContextNames.JAVA_CONTEXT_SERVICE_NAME, new NamingStoreService())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();

        // Create the Naming Service
        final NamingService namingService = new NamingService();
        target.addService(Capability.NAMING_STORE.getDefinition().getCapabilityServiceName(), namingService)
                .addAliases(NamingService.SERVICE_NAME)
                .addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME, NamingStore.class, namingService.getNamingStore())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();

        // Create the java:global namespace
        target.addService(ContextNames.GLOBAL_CONTEXT_SERVICE_NAME, new NamingStoreService())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();

        // Create the java:jboss vendor namespace
        target.addService(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, new NamingStoreService())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();

        // Setup remote naming store
        //we always install the naming store, but we don't install the server unless it has been explicitly enabled
        target.addService(ContextNames.EXPORTED_CONTEXT_SERVICE_NAME, new NamingStoreService())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();

        // add the default namespace context selector service
        DefaultNamespaceContextSelectorService defaultNamespaceContextSelectorService = new DefaultNamespaceContextSelectorService();
        target.addService(DefaultNamespaceContextSelectorService.SERVICE_NAME, defaultNamespaceContextSelectorService)
                .addDependency(ContextNames.GLOBAL_CONTEXT_SERVICE_NAME, NamingStore.class, defaultNamespaceContextSelectorService.getGlobalNamingStore())
                .addDependency(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, NamingStore.class, defaultNamespaceContextSelectorService.getJbossNamingStore())
                .addDependency(ContextNames.EXPORTED_CONTEXT_SERVICE_NAME, NamingStore.class, defaultNamespaceContextSelectorService.getRemoteExposedNamingStore())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();

        target.addService(JndiViewExtensionRegistry.SERVICE_NAME, new JndiViewExtensionRegistry()).install();

        // create the subsystem's external context instance, and install related Service and DUP
        final ExternalContexts externalContexts = new ExternalContextsNavigableSet();
        target.addService(ExternalContextsService.SERVICE_NAME, new ExternalContextsService(externalContexts)).install();

        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            protected void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(NamingExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_NAMING_EXTERNAL_CONTEXTS, new ExternalContextsProcessor(externalContexts));
                // TODO: replace Phase.STRUCTURE_NAMING_EXTERNAL_CONTEXTS + 1 with Phase.STRUCTURE_NAMING_JDK_DEPENDENCIES
                processorTarget.addDeploymentProcessor(NamingExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_NAMING_EXTERNAL_CONTEXTS + 1, new JdkDependenciesProcessor());
                processorTarget.addDeploymentProcessor(NamingExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_JNDI_DEPENDENCIES, new JndiNamingDependencyProcessor());
            }
        }, OperationContext.Stage.RUNTIME);


        if(context.hasOptionalCapability(UNDERTOW_HTTP_INVOKER_CAPABILITY_NAME, NamingService.CAPABILITY_NAME, null)) {
            HttpRemoteNamingServerService httpRemoteNamingServerService = new HttpRemoteNamingServerService();
            context.getServiceTarget().addService(HttpRemoteNamingServerService.SERVICE_NAME, httpRemoteNamingServerService)
                    .addDependency(context.getCapabilityServiceName(UNDERTOW_HTTP_INVOKER_CAPABILITY_NAME, PathHandler.class), PathHandler.class, httpRemoteNamingServerService.getPathHandlerInjectedValue())
                    .addDependency(ContextNames.EXPORTED_CONTEXT_SERVICE_NAME, NamingStore.class, httpRemoteNamingServerService.getNamingStore())
                    .install();
        }
    }
}
