/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ws.authn.ext;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrieval;
import pl.edu.icm.unity.rest.authn.ext.HttpBasicRetrievalBase;
import pl.edu.icm.unity.stdext.credential.pass.PasswordExchange;
import pl.edu.icm.unity.ws.authn.WebServiceAuthentication;

/**
 * Credential retrieval using username and password from the HTTP Basic Authn.
 * Here we only return a proper name, implementation in parent.
 * @author K. Benedyczak
 */
@Component("SOAPHttpBasicRetrieval")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class HttpBasicRetrieval extends HttpBasicRetrievalBase implements CredentialRetrieval, WebServiceAuthentication
{
	public static final String NAME = "cxf-httpbasic";
	public static final String DESC = "CXFHttpBasicRetrievalFactory.desc";

	public HttpBasicRetrieval()
	{
		super(WebServiceAuthentication.NAME);
	}
	
	@Component("SOAPHttpBasicRetrievalFactory")
	public static class Factory extends AbstractCredentialRetrievalFactory<HttpBasicRetrieval>
	{
		@Autowired
		public Factory(ObjectFactory<HttpBasicRetrieval> factory)
		{
			super(NAME, DESC, WebServiceAuthentication.NAME, factory, PasswordExchange.ID);
		}
	}
}
