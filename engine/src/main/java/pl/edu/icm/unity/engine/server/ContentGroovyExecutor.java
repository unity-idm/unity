/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import java.io.File;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.Logger;
import org.codehaus.groovy.control.CompilationFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.google.common.base.Stopwatch;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.BulkProcessingManagement;
import pl.edu.icm.unity.engine.api.ConfirmationConfigurationManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.IdentityTypesManagement;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.NotificationsManagement;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.ServerManagement;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.UserImportManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.initializers.ContentInitConf;
import pl.edu.icm.unity.engine.api.initializers.InitializerType;
import pl.edu.icm.unity.exceptions.InternalException;

/**
 * Executes GROOVY scripts given by user in
 * {@link UnityServerConfiguration#CONTENT_INITIALIZERS} configuration.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
@Component
public class ContentGroovyExecutor
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER, ContentGroovyExecutor.class);
	private GroovyShell shell;
	
	@Autowired
	private UnityServerConfiguration config;
	@Autowired
	private BulkProcessingManagement bulkProcessingManagement;
	@Autowired
	private IdentityTypesManagement identityTypesManagement;
	@Autowired
	private PKIManagement pkiManagement;
	@Autowired
	private PreferencesManagement preferencesManagement;
	@Autowired
	private UserImportManagement userImportManagement;
	@Autowired
	@Qualifier("insecure")
	private AttributeClassManagement attributeClassManagement;
	@Autowired
	@Qualifier("insecure")
	private AttributesManagement attributesManagement;
	@Autowired
	@Qualifier("insecure")
	private AttributeTypeManagement attributeTypeManagement;
	@Autowired
	@Qualifier("insecure")
	private AuthenticatorManagement authenticatorManagement;
	@Autowired
	@Qualifier("insecure")
	private ConfirmationConfigurationManagement confirmationConfigurationManagement;
	@Autowired
	@Qualifier("insecure")
	private CredentialManagement credentialManagement;
	@Autowired
	@Qualifier("insecure")
	private CredentialRequirementManagement credentialRequirementManagement;
	@Autowired
	@Qualifier("insecure")
	private EndpointManagement endpointManagement;
	@Autowired
	@Qualifier("insecure")
	private EnquiryManagement enquiryManagement;
	@Autowired
	@Qualifier("insecure")
	private EntityCredentialManagement entityCredentialManagement;
	@Autowired
	@Qualifier("insecure")
	private EntityManagement entityManagement;
	@Autowired
	@Qualifier("insecure")
	private GroupsManagement groupsManagement;
	@Autowired
	@Qualifier("insecure")
	private InvitationManagement invitationManagement;
	@Autowired
	@Qualifier("insecure")
	private MessageTemplateManagement messageTemplateManagement;
	@Autowired
	@Qualifier("insecure")
	private NotificationsManagement notificationsManagement;
	@Autowired
	@Qualifier("insecure")
	private RealmsManagement realmsManagement;
	@Autowired
	@Qualifier("insecure")
	private RegistrationsManagement registrationsManagement;
	@Autowired
	@Qualifier("insecure")
	private ServerManagement serverManagement;
	@Autowired
	@Qualifier("insecure")
	private TranslationProfileManagement translationProfileManagement;

	@PostConstruct
	public void initialize()
	{
		shell = new GroovyShell(getBinding());
	}

	public void run(ContentInitConf conf)
	{
		if (conf == null || conf.getType() != InitializerType.GROOVY)
			throw new IllegalArgumentException(
					"conf must not be null and must be of " + InitializerType.GROOVY + " type");
		run(conf.getFile());
	}

	private void run(File file)
	{
		LOG.info("Executing {} script: {}", InitializerType.GROOVY, file.toString());
		Stopwatch timer = Stopwatch.createStarted();
		try
		{
			shell.evaluate(file);
		} catch (CompilationFailedException | IOException e)
		{
			throw new InternalException("Failed to initialize content from " + InitializerType.GROOVY 
					+ " script: " + file.toString() + ": reason: " + e.getMessage(), e);
		}
		LOG.info("{} script: {} finished in {}", InitializerType.GROOVY, file.toString(), timer);
	}

	private Binding getBinding()
	{
		Binding binding = new Binding();
		binding.setVariable("config", config);
		binding.setVariable("attributeClassManagement", attributeClassManagement);
		binding.setVariable("attributesManagement", attributesManagement);
		binding.setVariable("attributeTypeManagement", attributeTypeManagement);
		binding.setVariable("authenticatorManagement", authenticatorManagement);
		binding.setVariable("bulkProcessingManagement", bulkProcessingManagement);
		binding.setVariable("confirmationConfigurationManagement", confirmationConfigurationManagement);
		binding.setVariable("credentialManagement", credentialManagement);
		binding.setVariable("credentialRequirementManagement", credentialRequirementManagement);
		binding.setVariable("endpointManagement", endpointManagement);
		binding.setVariable("enquiryManagement", enquiryManagement);
		binding.setVariable("entityCredentialManagement", entityCredentialManagement);
		binding.setVariable("entityManagement", entityManagement);
		binding.setVariable("groupsManagement", groupsManagement);
		binding.setVariable("identityTypesManagement", identityTypesManagement);
		binding.setVariable("invitationManagement", invitationManagement);
		binding.setVariable("messageTemplateManagement", messageTemplateManagement);
		binding.setVariable("notificationsManagement", notificationsManagement);
		binding.setVariable("pkiManagement", pkiManagement);
		binding.setVariable("preferencesManagement", preferencesManagement);
		binding.setVariable("realmsManagement", realmsManagement);
		binding.setVariable("registrationsManagement", registrationsManagement);
		binding.setVariable("serverManagement", serverManagement);
		binding.setVariable("translationProfileManagement", translationProfileManagement);
		binding.setVariable("userImportManagement", userImportManagement);
		binding.setVariable("config", config);
		binding.setVariable("log", LOG);
		return binding;
	}
}
