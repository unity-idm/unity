/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.registration;

import static pl.edu.icm.unity.server.api.registration.PublicRegistrationURLSupport.REGISTRATION_FRAGMENT_PREFIX;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.api.internal.IdPLoginController;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.wellknownurl.PublicViewProvider;

import com.vaadin.navigator.View;

/**
 * Provides access to public registration forms via well-known links
 * @author K. Benedyczak
 */
@Component
public class PublicRegistrationURLProvider implements PublicViewProvider
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			PublicRegistrationURLProvider.class);
	private RegistrationsManagement regMan;
	private IdentityEditorRegistry identityEditorRegistry;
	private CredentialEditorRegistry credentialEditorRegistry;
	private AttributeHandlerRegistry attributeHandlerRegistry;
	private AttributesManagement attrsMan;
	private AuthenticationManagement authnMan;
	private GroupsManagement groupsMan;
	private UnityMessageSource msg;
	private UnityServerConfiguration cfg;
	private IdPLoginController idpLoginController;
	
	@Autowired
	public PublicRegistrationURLProvider(UnityMessageSource msg,
			@Qualifier("insecure") RegistrationsManagement regMan,
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			@Qualifier("insecure") AttributesManagement attrsMan, 
			@Qualifier("insecure") AuthenticationManagement authnMan,
			@Qualifier("insecure") GroupsManagement groupsMan,
			UnityServerConfiguration cfg, 
			IdPLoginController idpLoginController)
	{
		this.msg = msg;
		this.regMan = regMan;
		this.identityEditorRegistry = identityEditorRegistry;
		this.credentialEditorRegistry = credentialEditorRegistry;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.attrsMan = attrsMan;
		this.authnMan = authnMan;
		this.groupsMan = groupsMan;
		this.cfg = cfg;
		this.idpLoginController = idpLoginController;
	}

	@Override
	public String getViewName(String viewAndParameters)
	{
		if (!viewAndParameters.startsWith(REGISTRATION_FRAGMENT_PREFIX))
			return null;
		String viewName = viewAndParameters.substring(REGISTRATION_FRAGMENT_PREFIX.length());
		return getForm(viewName) == null ? null : viewAndParameters;
	}

	@Override
	public View getView(String viewName)
	{
		RegistrationForm form = getForm(viewName.substring(REGISTRATION_FRAGMENT_PREFIX.length()));
		if (form == null)
			return null;
		return new StandalonePublicFormView(form, msg, regMan, identityEditorRegistry, 
				credentialEditorRegistry, attributeHandlerRegistry, attrsMan, authnMan, 
				groupsMan, cfg, idpLoginController);
	}

	
	private RegistrationForm getForm(String name)
	{
		try
		{
			List<RegistrationForm> forms = regMan.getForms();
			for (RegistrationForm regForm: forms)
				if (regForm.isPubliclyAvailable() && regForm.getName().equals(name))
					return regForm;
		} catch (EngineException e)
		{
			log.error("Can't load registration forms", e);
		}
		return null;
	}
}
