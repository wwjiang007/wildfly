/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.logging;

import static org.jboss.logging.Logger.Level.INFO;

import java.lang.invoke.MethodHandles;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author Pedro Igor
 */
@MessageLogger(projectCode = "WFLYPL", length = 4)
public interface PicketLinkLogger extends BasicLogger {

    PicketLinkLogger ROOT_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), PicketLinkLogger.class, "org.wildfly.extension.picketlink");

    // General Messages 1-49
    @LogMessage(level = INFO)
    @Message(id = 1, value = "Activating PicketLink %s Subsystem")
    void activatingSubsystem(String name);

//    @LogMessage(level = INFO)
//    @Message(id = 2, value = "Configuring PicketLink Federation for deployment [%s]")
//    void federationConfiguringDeployment(String deploymentName);

    @LogMessage(level = INFO)
    @Message(id = 3, value = "Bound [%s] to [%s]")
    void boundToJndi(String alias, String jndiName);

//    @LogMessage(level = INFO)
//    @Message(id = 4, value = "Ignoring unexpected event type [%s]")
//    void federationIgnoringAuditEvent(PicketLinkAuditEventType eventType);

//    @LogMessage(level = ERROR)
//    @Message(id = 5, value = "Error while configuring the metrics collector. Metrics will not be collected.")
//    void federationErrorCollectingMetric(@Cause Throwable t);

//    @Message(id = 6, value = "No writer provided for element %s. Check if a writer is registered in PicketLinkSubsystemWriter.")
//    IllegalStateException noModelElementWriterProvided(String modelElement);

    @Message(id = 7, value = "Could not load module [%s].")
    RuntimeException moduleCouldNotLoad(String s, @Cause Throwable t);

//    @Message(id = 8, value = "Unexpected element [%s].")
//    XMLStreamException parserUnexpectedElement(String modelName);

    @Message(id = 9, value = "Could not load class [%s].")
    RuntimeException couldNotLoadClass(String mappingClass, @Cause Throwable e);

    @Message(id = 10, value = "No type provided for %s. You must specify a class-name or code.")
    OperationFailedException typeNotProvided(String elementName);

//    @Message(id = 11, value = "Failed to get metrics %s.")
//    OperationFailedException failedToGetMetrics(String reason);

    @Message(id = 12, value = "Attribute [%s] is not longer supported.")
    OperationFailedException attributeNoLongerSupported(String attributeName);

    @Message(id = 13, value = "[%s] can only have [%d] child of type [%s].")
    OperationFailedException invalidChildTypeOccurrence(String parentPathElement, int maxOccurs, String elementName);

    @Message(id = 14, value = "Invalid attribute [%s] definition for [%s]. Only one of the following attributes are allowed: [%s].")
    OperationFailedException invalidAlternativeAttributeOccurrence(String attributeName, String pathElement, String attributeNames);

    @Message(id = 15, value = "Required attribute [%s] for [%s].")
    OperationFailedException requiredAttribute(String attributeName, String configuration);

    @Message(id = 16, value = "[%s] requires one of the given attributes [%s].")
    OperationFailedException requiredAlternativeAttributes(String pathElement, String attributeNames);

    @Message(id = 17, value = "Type [%s] already defined.")
    IllegalStateException typeAlreadyDefined(String clazz);

    @Message(id = 18, value = "[%s] can not be empty.")
    OperationFailedException emptyResource(String parentPathElement);

    @Message(id = 19, value = "[%s] requires child [%s].")
    OperationFailedException requiredChild(String parentPathElement, String childPathElement);

    // IDM Messages 50-99
//    @Message(id = 50, value = "Entities module not found [%s].")
//    SecurityConfigurationException idmJpaEntityModuleNotFound(String entityModuleName);

//    @Message(id = 51, value = "Could not configure JPA store.")
//    SecurityConfigurationException idmJpaStartFailed(@Cause Throwable e);

//    @Message(id = 52, value = "Could not lookup EntityManagerFactory [%s].")
//    SecurityConfigurationException idmJpaEMFLookupFailed(String entityManagerFactoryJndiName);

//    @Message(id = 53, value = "Could not create transactional EntityManager.")
//    SecurityConfigurationException idmJpaFailedCreateTransactionEntityManager(@Cause Exception e);

    @Message(id = 54, value = "You must provide at least one identity configuration.")
    OperationFailedException idmNoIdentityConfigurationProvided();

    @Message(id = 55, value = "You must provide at least one identity store for identity configuration [%s].")
    OperationFailedException idmNoIdentityStoreProvided(String identityConfiguration);

    @Message(id = 56, value = "No supported type provided.")
    OperationFailedException idmNoSupportedTypesDefined();

    @Message(id = 57, value = "No mapping was defined.")
    OperationFailedException idmLdapNoMappingDefined();

    // Federation Messages - 100-150
//    @Message(id = 100, value = "No Identity Provider configuration found for federation [%s]. ")
//    IllegalStateException federationIdentityProviderNotConfigured(String federationAlias);

    @Message(id = 101, value = "No type provided for the handler. You must specify a class-name or code.")
    OperationFailedException federationHandlerTypeNotProvided();

//    @Message(id = 102, value = "Could not parse default STS configuration.")
//    RuntimeException federationCouldNotParseSTSConfig(@Cause Throwable t);

//    @Message(id = 104, value = "Could not configure SAML Metadata to deployment [%s].")
//    IllegalStateException federationSAMLMetadataConfigError(String deploymentName, @Cause ProcessingException e);

    @Message(id = 105, value = "The migrate operation can not be performed: the server must be in admin-only mode")
    OperationFailedException migrateOperationAllowedOnlyInAdminOnly();

    @Message(id = 106, value = "Migration failed, see results for more details.")
    String migrationFailed();

    @Message(id = 107, value = "Cannot migrate non-empty picketlink-federation subsystem configuration.")
    OperationFailedException cannotMigrateNonEmptyConfiguration();
}
