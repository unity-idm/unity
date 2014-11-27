/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.slo;

import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.LogoutRequestType;
import xmlbeans.org.oasis.saml2.protocol.LogoutResponseDocument;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.proto.LogoutResponse;

/**
 * Helpers to produce logout responses
 * @author K. Benedyczak
 */
public class LogoutResponseUtils
{
	public static LogoutResponseDocument getErrorResponse(SAMLLogoutContext ctx, SAMLServerException e)
	{
		String id = null;
		LogoutRequestType request = ctx.getRequest();
		if (request != null)
			id = request.getID();
		return new LogoutResponse(getResponseIssuer(ctx.getLocalSessionAuthorityId()), id, e).getXMLBeanDoc();
	}
	
	private static NameIDType getResponseIssuer(String issuer)
	{
		NameIDType ret = NameIDType.Factory.newInstance();
		ret.setFormat(SAMLConstants.NFORMAT_ENTITY);
		ret.setStringValue(issuer);
		return ret;
	}
}
