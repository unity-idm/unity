/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ws.mock;

import jakarta.jws.WebService;
import xmlbeans.org.oasis.saml2.assertion.NameIDDocument;

@WebService
public interface MockWSSEI
{
	public NameIDDocument getAuthenticatedUser();
}
