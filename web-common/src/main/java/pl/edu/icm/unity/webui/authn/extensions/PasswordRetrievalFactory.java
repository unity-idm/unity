/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.extensions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.authn.CredentialExchange;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.authn.CredentialRetrievalFactory;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.credential.PasswordExchange;
import pl.edu.icm.unity.stdext.credential.PasswordVerificatorFactory;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;

/**
 * Produces password retrievals for the Vaadin authn binding
 * @author K. Benedyczak
 */
@Component
public class PasswordRetrievalFactory implements CredentialRetrievalFactory
{
	public static final String NAME = "web-password";
	
	@Autowired
	private UnityMessageSource msg;
	
	@Autowired
	private CredentialEditorRegistry credEditorReg;
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "WebPasswordRetrievalFactory.desc";
	}

	@Override
	public CredentialRetrieval newInstance()
	{
		return new PasswordRetrieval(msg, credEditorReg.getEditor(PasswordVerificatorFactory.NAME));
	}

	@Override
	public String getSupportedBinding()
	{
		return VaadinAuthentication.NAME;
	}

	@Override
	public boolean isCredentialExchangeSupported(CredentialExchange e)
	{
		return e instanceof PasswordExchange;
	}

}
