/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web.filter;

import pl.edu.icm.unity.types.endpoint.Endpoint;

public interface IdpConsentDeciderServletFactory
{
	IdpConsentDeciderServlet getInstance(String uiServletPath, String authenticationUIServletPath, Endpoint endpoint);
}