/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinServletResponse;
import com.vaadin.server.VaadinSession;

import pl.edu.icm.unity.saml.sp.RemoteAuthnContext;
import pl.edu.icm.unity.webui.authn.remote.AbstractRedirectRequestHandler;

/**
 * Used to trigger authn request redirect. 
 * Delegates to {@link RedirectRequestHandler} via dynamic proxy.
 * 
 * @author K. Benedyczak
 */
public class VaadinRedirectRequestHandler extends AbstractRedirectRequestHandler
{
	public VaadinRedirectRequestHandler()
	{
		super(SAMLRetrieval.REMOTE_AUTHN_CONTEXT);
	}
	
	@Override
	protected boolean handleRequestInternal(Object contextO, VaadinSession vaadinSession,
			VaadinRequest request, VaadinResponse response) throws IOException
	{
		RemoteAuthnContext context = (RemoteAuthnContext)contextO;
		HttpServletResponse proxiedResponse = VaadinResponseToServletProxy
				.getProxiedResponse((VaadinServletResponse) response);
		return RedirectRequestHandler.handleRequest(context, proxiedResponse);
	}
}
