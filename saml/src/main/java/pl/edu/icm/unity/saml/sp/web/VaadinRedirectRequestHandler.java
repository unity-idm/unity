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

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Used to trigger authn request redirect. 
 * Delegates to {@link RedirectRequestHandler} via dynamic proxy.
 * 
 * @author K. Benedyczak
 */
public class VaadinRedirectRequestHandler extends AbstractRedirectRequestHandler
{
	public static final String REMOTE_AUTHN_CONTEXT = SAMLRetrieval.class.getName() + ".REMOTE_AUTHN_CONTEXT";
	
	public VaadinRedirectRequestHandler()
	{
		super(REMOTE_AUTHN_CONTEXT);
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
