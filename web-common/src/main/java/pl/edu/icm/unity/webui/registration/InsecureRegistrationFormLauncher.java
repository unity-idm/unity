/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.registration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;



/**
 * Responsible for showing a given registration form dialog. This is a no-authz version of
 * {@link RegistrationFormLauncher}.
 * 
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class InsecureRegistrationFormLauncher extends RegistrationFormLauncher
{
	@Autowired
	public InsecureRegistrationFormLauncher(UnityMessageSource msg,
			@Qualifier("insecure") RegistrationsManagement registrationsManagement,
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			@Qualifier("insecure") AttributesManagement attrsMan, 
			@Qualifier("insecure") AuthenticationManagement authnMan)
	{
		super(msg, registrationsManagement, identityEditorRegistry, credentialEditorRegistry, 
				attributeHandlerRegistry, attrsMan, authnMan);
	}

	@Override
	public void setAddAutoAccept(boolean addAutoAccept)
	{
		throw new IllegalStateException("Can not set the insecure " +
				"registration form launcher into auto accept mode.");
	}
}
