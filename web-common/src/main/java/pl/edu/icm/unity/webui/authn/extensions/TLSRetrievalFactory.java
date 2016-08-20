/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.extensions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.CredentialExchange;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.credential.CertificateExchange;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

/**
 * Produces certificate retrievals for the Vaadin authn binding
 * @author K. Benedyczak
 */
@Component
public class TLSRetrievalFactory implements CredentialRetrievalFactory
{
	public static final String NAME = "web-certificate";
	
	@Autowired
	private UnityMessageSource msg;
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "WebTLSRetrievalFactory.desc";
	}

	@Override
	public CredentialRetrieval newInstance()
	{
		return new TLSRetrieval(msg);
	}

	@Override
	public String getSupportedBinding()
	{
		return VaadinAuthentication.NAME;
	}

	@Override
	public boolean isCredentialExchangeSupported(CredentialExchange e)
	{
		return e instanceof CertificateExchange;
	}
}
