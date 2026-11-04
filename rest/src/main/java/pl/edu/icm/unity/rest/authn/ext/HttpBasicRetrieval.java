/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.authn.ext;

import java.util.Set;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.engine.api.authn.CredentialPresentationId;
import io.imunity.engine.api.authn.CredentialPresentationProvider;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrieval;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;
import pl.edu.icm.unity.stdext.credential.pass.PasswordExchange;

/**
 * Credential retrieval using username and password from the HTTP Basic Authn.
 * <p>
 * Real implementation in parent class, here we only report a unique name.
 * @author K. Benedyczak
 */
@PrototypeComponent
public class HttpBasicRetrieval extends HttpBasicRetrievalBase
		implements CredentialRetrieval, JAXRSAuthentication, CredentialPresentationProvider
{
	public static final String NAME = "rest-httpbasic";
	public static final String DESC = "CXFHttpBasicRetrievalFactory.desc";
	public static final CredentialPresentationId PRESENTATION =
			new CredentialPresentationId("urn:unity:credential-presentation:http-basic");
	
	public HttpBasicRetrieval()
	{
		super(JAXRSAuthentication.NAME);
	}

	@Override
	public Set<CredentialPresentationId> getSupportedPresentations()
	{
		return Set.of(PRESENTATION);
	}
	
	@Component
	public static class Factory extends AbstractCredentialRetrievalFactory<HttpBasicRetrieval>
	{
		@Autowired
		public Factory(ObjectFactory<HttpBasicRetrieval> factory)
		{
			super(NAME, DESC, JAXRSAuthentication.NAME, factory, PasswordExchange.ID);
		}
	}

}
