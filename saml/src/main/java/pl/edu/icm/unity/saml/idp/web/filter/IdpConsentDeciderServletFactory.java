/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web.filter;

public interface IdpConsentDeciderServletFactory
{
	IdpConsentDeciderServlet getInstance(String uiServletPath, String authenticationUIServletPath);
}