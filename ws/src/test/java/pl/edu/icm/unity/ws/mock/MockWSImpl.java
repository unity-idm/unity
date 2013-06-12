/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ws.mock;

import javax.jws.WebService;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.utils.Log;
import xmlbeans.org.oasis.saml2.assertion.NameIDDocument;

@WebService(endpointInterface = "pl.edu.icm.unity.ws.mock.MockWSSEI",
	serviceName = "MockWS")
public class MockWSImpl implements MockWSSEI
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WS, MockWSImpl.class); 
	
	@Override
	public NameIDDocument getAuthenticatedUser()
	{
		log.info("Got request to the mock WS");
		InvocationContext ctx = InvocationContext.getCurrent();
		String ret = ctx.getAuthenticatedEntity().getAuthenticatedWith().toString();
		log.info("Returning: " + ret);
		NameIDDocument rr = NameIDDocument.Factory.newInstance();
		rr.addNewNameID().setStringValue(ret);
		return rr;
	}
}
