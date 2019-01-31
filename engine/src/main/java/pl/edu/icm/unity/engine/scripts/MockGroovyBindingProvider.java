/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.scripts;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import org.apache.logging.log4j.Logger;

import groovy.lang.Binding;
import pl.edu.icm.unity.base.event.Event;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.BulkProcessingManagement;
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
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.UserImportManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;

/**
 * Provides a mock Groovy context. The context has the same members as in 
 * the case of regular context and of the same types, but all 
 * are mocked and only print details about their invocation. 
 *
 * @author golbi
 */
public class MockGroovyBindingProvider
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER, MockGroovyBindingProvider.class);
	
	public static Binding getBinding(Event event)
	{
		UnityMessageSource unityMessageSource = mock(UnityMessageSource.class, 
				withSettings().verboseLogging());
		UnityServerConfiguration config = mock(UnityServerConfiguration.class, 
				withSettings().verboseLogging());
		BulkProcessingManagement bulkProcessingManagement = mock(BulkProcessingManagement.class, 
				withSettings().verboseLogging());
		PreferencesManagement preferencesManagement = mock(PreferencesManagement.class, 
				withSettings().verboseLogging());
		UserImportManagement userImportManagement = mock(UserImportManagement.class, 
				withSettings().verboseLogging());
		AttributeTypeSupport attributeTypeSupport = mock(AttributeTypeSupport.class, 
				withSettings().verboseLogging());
		IdentityTypeSupport identityTypeSupport = mock(IdentityTypeSupport.class, 
				withSettings().verboseLogging());
		IdentityTypesManagement identityTypesManagement = mock(IdentityTypesManagement.class, 
				withSettings().verboseLogging());
		AttributeClassManagement attributeClassManagement = mock(AttributeClassManagement.class, 
				withSettings().verboseLogging());
		AttributesManagement attributesManagement = mock(AttributesManagement.class, 
				withSettings().verboseLogging());
		AttributeTypeManagement attributeTypeManagement = mock(AttributeTypeManagement.class, 
				withSettings().verboseLogging());
		AuthenticatorManagement authenticatorManagement = mock(AuthenticatorManagement.class, 
				withSettings().verboseLogging());
		CredentialManagement credentialManagement = mock(CredentialManagement.class, 
				withSettings().verboseLogging());
		CredentialRequirementManagement credentialRequirementManagement = 
				mock(CredentialRequirementManagement.class, 
				withSettings().verboseLogging());
		EndpointManagement endpointManagement = mock(EndpointManagement.class, 
				withSettings().verboseLogging());
		EnquiryManagement enquiryManagement = mock(EnquiryManagement.class, 
				withSettings().verboseLogging());
		EntityCredentialManagement entityCredentialManagement = mock(EntityCredentialManagement.class, 
				withSettings().verboseLogging());
		EntityManagement entityManagement = mock(EntityManagement.class, 
				withSettings().verboseLogging());
		GroupsManagement groupsManagement = mock(GroupsManagement.class, 
				withSettings().verboseLogging());
		InvitationManagement invitationManagement = mock(InvitationManagement.class, 
				withSettings().verboseLogging());
		MessageTemplateManagement messageTemplateManagement = mock(MessageTemplateManagement.class, 
				withSettings().verboseLogging());
		NotificationsManagement notificationsManagement = mock(NotificationsManagement.class, 
				withSettings().verboseLogging());
		RealmsManagement realmsManagement = mock(RealmsManagement.class, 
				withSettings().verboseLogging());
		RegistrationsManagement registrationsManagement = mock(RegistrationsManagement.class, 
				withSettings().verboseLogging());
		TranslationProfileManagement translationProfileManagement = mock(TranslationProfileManagement.class, 
				withSettings().verboseLogging());
		GroupDelegationConfigGenerator groupDelegationConfigGenerator = mock(GroupDelegationConfigGenerator.class, 
				withSettings().verboseLogging());;
		
		Binding binding = new Binding();
		binding.setVariable("config", config);
		binding.setVariable("attributeClassManagement", attributeClassManagement);
		binding.setVariable("attributesManagement", attributesManagement);
		binding.setVariable("attributeTypeManagement", attributeTypeManagement);
		binding.setVariable("authenticatorManagement", authenticatorManagement);
		binding.setVariable("bulkProcessingManagement", bulkProcessingManagement);
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
		binding.setVariable("preferencesManagement", preferencesManagement);
		binding.setVariable("realmsManagement", realmsManagement);
		binding.setVariable("registrationsManagement", registrationsManagement);
		binding.setVariable("translationProfileManagement", translationProfileManagement);
		binding.setVariable("userImportManagement", userImportManagement);
		binding.setVariable("msgSrc", unityMessageSource);
		binding.setVariable("attributeTypeSupport", attributeTypeSupport);
		binding.setVariable("identityTypeSupport", identityTypeSupport);
		binding.setVariable("groupDelegationConfigGenerator", groupDelegationConfigGenerator);
		binding.setVariable("isColdStart", true);
		binding.setVariable("event", event.getTrigger());
		binding.setVariable("context", event.getContents());
		binding.setVariable("log", LOG);
		return binding;
	}
}
