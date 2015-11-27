/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web;

import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.web.filter.SamlParseServlet;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;

/**
 * Methods responsible for SAML context handling
 * @author K. Benedyczak
 */
public class SAMLContextSupport
{
	
	public static boolean hasContext()
	{
		VaadinSession vSession = VaadinSession.getCurrent();
		if (vSession != null)
		{
			WrappedSession httpSession = vSession.getSession();
			SAMLAuthnContext ret = (SAMLAuthnContext) httpSession.getAttribute(
					SamlParseServlet.SESSION_SAML_CONTEXT);
			return ret != null;
		}
		return false;
	}
	
	public static SAMLAuthnContext getContext()
	{
		WrappedSession httpSession = VaadinSession.getCurrent().getSession();
		SAMLAuthnContext ret = (SAMLAuthnContext) httpSession.getAttribute(
				SamlParseServlet.SESSION_SAML_CONTEXT);
		if (ret == null)
			throw new IllegalStateException("No SAML context in UI");
		return ret;
	}
	
	public static void cleanContext()
	{
		VaadinSession vSession = VaadinSession.getCurrent();
		if (vSession != null)
		{
			vSession.setAttribute(ResponseDocument.class, null);
			WrappedSession httpSession = vSession.getSession();
			httpSession.removeAttribute(SamlParseServlet.SESSION_SAML_CONTEXT);
		}
	}
}
