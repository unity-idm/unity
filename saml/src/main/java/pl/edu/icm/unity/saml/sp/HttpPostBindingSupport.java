/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import org.apache.xmlbeans.impl.util.Base64;

/**
 * Helper class supporting SAML HTTP POST binding.
 * @author K. Benedyczak
 */
public class HttpPostBindingSupport
{
	public static String getHtmlPOSTFormContents(SAMLMessageType messageType, String identityProviderURL, 
			String xmlMessage, String relayState)
	{
		String f = formForm.replace("__ACTION__", identityProviderURL);
		f = f.replace("__RELAYSTATE__", relayState == null ? "" : relayState);
		String encodedReq = new String(Base64.encode(xmlMessage.getBytes()));
		f = f.replace("__SAMLREQUEST__", encodedReq);
		f = f.replace("__MESSAGE_TYPE__", messageType.toString());
		return f;
	}
	
	private static final String formForm = 
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
		"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">" +
		"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">" +
		"<body onload=\"document.forms[0].submit()\">" +
		"<noscript>" +
		"<p>" +
		"<strong>Note:</strong> Since your browser does not support JavaScript," +
		"you must press the Continue button once to proceed." +
		"</p>" +
		"</noscript>" +
		"<form action=\"__ACTION__\" method=\"post\">" +
		"<div>" +
		"<input type=\"hidden\" name=\"RelayState\" value=\"__RELAYSTATE__\"/>" +
		"<input type=\"hidden\" name=\"__MESSAGE_TYPE__\" value=\"__SAMLREQUEST__\"/>" +
		"</div>" +
		"<noscript>" +
		"<div>" +
		"<input type=\"submit\" value=\"Continue\"/>" +
		"</div>" +
		"</noscript>" +
		"</form>" +
		"</body></html>";
}
