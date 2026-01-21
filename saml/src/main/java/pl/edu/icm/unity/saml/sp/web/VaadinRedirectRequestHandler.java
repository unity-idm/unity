/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinSession;
import pl.edu.icm.unity.saml.sp.RemoteAuthnContext;
import io.imunity.vaadin.auth.idp.AbstractRedirectRequestHandler;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Used to trigger authn request redirect. 
 * Delegates to {@link RedirectRequestHandler} via dynamic proxy.
 */
class VaadinRedirectRequestHandler extends AbstractRedirectRequestHandler
{
	static final String REMOTE_AUTHN_CONTEXT = SAMLRetrieval.class.getName() + ".REMOTE_AUTHN_CONTEXT";
	private final RedirectRequestHandler redirectRequestHandler;

	VaadinRedirectRequestHandler(RedirectRequestHandler redirectRequestHandler)
	{
		super(REMOTE_AUTHN_CONTEXT);
		this.redirectRequestHandler = redirectRequestHandler;
	}
	
	@Override
	protected boolean handleRequestInternal(Object contextO, VaadinSession vaadinSession,
	                                        VaadinRequest request, VaadinResponse response) throws IOException
	{
		RemoteAuthnContext context = (RemoteAuthnContext)contextO;
		HttpServletResponse proxiedResponse = VaadinResponseToServletProxy
				.getProxiedResponse((VaadinServletResponse) response);
		return redirectRequestHandler.handleRequest(context, proxiedResponse);
	}
}
