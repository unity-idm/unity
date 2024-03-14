/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import eu.unicore.samly2.messages.RedirectedMessage;
import eu.unicore.samly2.messages.SAMLVerifiableElement;
import eu.unicore.samly2.messages.XMLExpandedMessage;
import io.imunity.vaadin.endpoint.common.RemoteRedirectedAuthnResponseProcessingFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthenticationContextManagement.UnboundRelayStateException;
import pl.edu.icm.unity.engine.api.authn.remote.SharedRemoteAuthenticationContextStore;
import pl.edu.icm.unity.engine.api.utils.URIBuilderFixer;
import pl.edu.icm.unity.saml.SamlHttpResponseServlet;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Custom servlet which awaits SAML authn response from IdP, which should be 
 * attached to the HTTP request.
 * <p>
 * If the response is found it is confronted with the expected data from the SAML authentication context and 
 * if is OK it is recorded in the context so the UI can catch up and further process the response.
 * 
 * @author K. Benedyczak
 */
public class SAMLResponseConsumerServlet extends SamlHttpResponseServlet
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SAMLResponseConsumerServlet.class);
	public static final String PATH = "/spSAMLResponseConsumer";
	
	private final SamlContextManagement contextManagement;
	private final SharedRemoteAuthenticationContextStore remoteAuthnContextStore;
	
	public SAMLResponseConsumerServlet(SamlContextManagement contextManagement, 
			SharedRemoteAuthenticationContextStore remoteAuthnContextStore)
	{
		super(true);
		this.contextManagement = contextManagement;
		this.remoteAuthnContextStore = remoteAuthnContextStore;
	}

	@Override
	protected void postProcessResponse(boolean isGet, HttpServletRequest req, HttpServletResponse resp,
			String samlResponse, String relayState) throws IOException
	{
		RemoteAuthnContext context;
		try
		{
			context = contextManagement.getAndRemoveAuthnContext(relayState);
		} catch (UnboundRelayStateException e)
		{
			log.warn("Got a request to the SAML response consumer endpoint, " +
					"with invalid relay state.");
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Wrong 'RelayState' value");
			return;
		}
		
		Binding binding = isGet ? Binding.HTTP_REDIRECT : Binding.HTTP_POST;
		SAMLVerifiableElement verifiableMessage = isGet ? 
				new RedirectedMessage(req.getQueryString()) : getDocumentSignedMessage(samlResponse);
		context.setResponse(samlResponse, binding, verifiableMessage);
		log.debug("SAML response for authenticator {} was stored in context, redirecting to originating endpoint {}", 
				context.getAuthenticationStepContext().authnOptionId, context.getReturnUrl());

		remoteAuthnContextStore.addAuthnContext(context);
		resp.sendRedirect(getRedirectWithContextIdParam(context.getReturnUrl(), relayState));
	}

	private String getRedirectWithContextIdParam(String returnURL, String relayState) throws IOException
	{
		try
		{
			URIBuilder uriBuilder = URIBuilderFixer.newInstance(returnURL);
			uriBuilder.addParameter(RemoteRedirectedAuthnResponseProcessingFilter.CONTEXT_ID_HTTP_PARAMETER, relayState);
			return uriBuilder.build().toString();
		} catch (URISyntaxException e)
		{
			throw new IOException("Can't build return URL", e);
		}
	}
	
	private XMLExpandedMessage getDocumentSignedMessage(String samlResponse) throws IOException
	{
		ResponseDocument parsedResponse = parseResponse(samlResponse);
		return new XMLExpandedMessage(parsedResponse, parsedResponse.getResponse());
	}
	
	private ResponseDocument parseResponse(String samlResponse) throws IOException
	{
		try
		{
			return ResponseDocument.Factory.parse(samlResponse);
		} catch (XmlException e)
		{
			throw new IOException("The SAML response can not be parsed - XML data is corrupted", e);
		}
	}
}

