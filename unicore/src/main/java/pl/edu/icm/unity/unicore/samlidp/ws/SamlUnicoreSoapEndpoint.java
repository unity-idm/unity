/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.ws;

import eu.unicore.samly2.webservice.SAMLAuthnInterface;
import eu.unicore.samly2.webservice.SAMLQueryInterface;
import pl.edu.icm.unity.saml.idp.ws.SAMLAssertionQueryImpl;
import pl.edu.icm.unity.saml.idp.ws.SamlSoapEndpoint;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

/**
 * Endpoint exposing SAML SOAP binding. This version extends the {@link SamlSoapEndpoint}
 * by exposing a modified implementation of the {@link SAMLAuthnInterface}. The
 * {@link SAMLETDAuthnImpl} is used, which also returns a bootstrap ETD assertion.
 * 
 * @author K. Benedyczak
 */
public class SamlUnicoreSoapEndpoint extends SamlSoapEndpoint
{
	public SamlUnicoreSoapEndpoint(UnityMessageSource msg, EndpointTypeDescription type,
			String servletPath, String metadataServletPath, IdentitiesManagement identitiesMan,
			AttributesManagement attributesMan, PreferencesManagement preferencesMan,
			PKIManagement pkiManagement, ExecutorsService executorsService)
	{
		super(msg, type, servletPath, metadataServletPath, 
				identitiesMan, attributesMan, preferencesMan, 
				pkiManagement, executorsService);
	}


	@Override
	protected void configureServices()
	{
		String endpointURL = getServletUrl(servletPath);
		SAMLAssertionQueryImpl assertionQueryImpl = new SAMLAssertionQueryImpl(samlProperties, 
				endpointURL, attributesMan, identitiesMan, preferencesMan);
		addWebservice(SAMLQueryInterface.class, assertionQueryImpl);
		SAMLETDAuthnImpl authnImpl = new SAMLETDAuthnImpl(samlProperties, endpointURL, 
				identitiesMan, attributesMan, preferencesMan);
		addWebservice(SAMLAuthnInterface.class, authnImpl);
	}
}




