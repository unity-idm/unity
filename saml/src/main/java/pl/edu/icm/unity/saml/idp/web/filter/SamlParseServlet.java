/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web.filter;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.hc.core5.net.URIBuilder;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.messages.RedirectedMessage;
import eu.unicore.samly2.messages.SAMLVerifiableElement;
import eu.unicore.samly2.messages.XMLExpandedMessage;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.utils.RoutingServlet;
import pl.edu.icm.unity.saml.SAMLProcessingException;
import pl.edu.icm.unity.saml.SamlHttpRequestServlet;
import pl.edu.icm.unity.saml.idp.SAMLIdPConfiguration;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.web.SamlSessionService;
import pl.edu.icm.unity.saml.metadata.cfg.RemoteMetaManager;
import pl.edu.icm.unity.saml.validator.WebAuthRequestValidator;
import pl.edu.icm.unity.webui.LoginInProgressService.SignInContextKey;
import pl.edu.icm.unity.webui.idpcommon.EopException;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;

/**
 * Low level servlet performing the initial SAML handling. Supports both POST and HTTP-Redirect (GET) 
 * SAML profiles. 
 * <p>
 * The servlet retrieves the SAML request, parses it, validates and if everything is correct 
 * stores it in the session and forwards the processing to the Vaadin part. In case of problems a SAML error is returned
 * to the requester or error page is displayed if the SAML requester can not be established (e.g. no request
 * or request can not be parsed).
 * @author K. Benedyczak
 */
public class SamlParseServlet extends SamlHttpRequestServlet
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SamlParseServlet.class);
	
	protected RemoteMetaManager samlConfigProvider;
	protected String endpointAddress;
	protected String samlDispatcherServletPath;
	protected ErrorHandler errorHandler;

	public SamlParseServlet(RemoteMetaManager samlConfigProvider, String endpointAddress,
			String samlDispatcherServletPath, ErrorHandler errorHandler)
	{
		super(false);
		this.samlConfigProvider = samlConfigProvider;
		this.endpointAddress = endpointAddress;
		this.samlDispatcherServletPath = samlDispatcherServletPath;
		this.errorHandler = errorHandler;
	}

	/**
	 * GET handling -> SAML Redirect binding.
	 * {@inheritDoc}
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException
	{
		log.trace("Received GET request to the SAML IdP endpoint");
		processSamlRequest(request, response, true);
	}

	/**
	 * POST handling -> SAML POST binding
	 * {@inheritDoc}
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException
	{
		log.trace("Received POST request to the SAML IdP endpoint");
		processSamlRequest(request, response, false);
	}

	protected void processSamlRequest(HttpServletRequest request, HttpServletResponse response, boolean isHTTPGet) 
			throws IOException, ServletException
	{
		try
		{
			processSamlRequestInterruptible(request, response, isHTTPGet);
		} catch (EopException e)
		{
			//OK
		}
	}
	
	protected void processSamlRequestInterruptible(HttpServletRequest request, HttpServletResponse response, boolean isHTTPGet) 
			throws IOException, ServletException, EopException
	{
		log.trace("Starting SAML request processing");
		SAMLIdPConfiguration samlIdPConfiguration = samlConfigProvider.getSAMLIdPConfiguration();

		String samlRequestStr = request.getParameter(SAMLConstants.REQ_SAML_REQUEST);
		//do we have a new request?
		if (samlRequestStr == null)
		{
			if (log.isTraceEnabled())
				log.trace("Request to SAML endpoint address, without SAML input, error: " + 
						request.getRequestURI());
			errorHandler.showErrorPage(new SAMLProcessingException("No SAML request"), 
					response);
			return;
		}
		//ok, we do have a new request. 

		
		SAMLAuthnContext context; 
		if (log.isTraceEnabled())
			log.trace("Got request with SAML input to: " + request.getRequestURI());
		try
		{
			AuthnRequestDocument samlRequest = parse(request);
			if (log.isTraceEnabled())
				log.trace("Parsed SAML request:\n" + samlRequest.xmlText());
			context = createSamlContext(request, samlRequest, samlIdPConfiguration, isHTTPGet);
			validate(context, response, samlIdPConfiguration);
		} catch (SAMLProcessingException e)
		{
			if (log.isDebugEnabled())
				log.warn("Processing of SAML input failed", e);
			errorHandler.showErrorPage(e, response);
			return;
		}
		
		SignInContextKey contextKey = SamlSessionService.setContext(request.getSession(), context);
		RoutingServlet.clean(request);
		if (log.isTraceEnabled())
			log.trace("Request with SAML input handled successfully");
		//Note - this is intended, even taking into account the overhead. We don't want to pass alongside
		//original HTTP query params, we want a clean URL in HTTP redirect case. In HTTP POST case it is
		//even more important: web browser would warn the user about doubled POST.
		response.sendRedirect(samlDispatcherServletPath + getQueryToAppend(contextKey));
	}
	
	private String getQueryToAppend(SignInContextKey contextKey)
	{
		URIBuilder b = new URIBuilder();
		if (!SignInContextKey.DEFAULT.equals(contextKey))
		{
			b.addParameter(SamlSessionService.URL_PARAM_CONTEXT_KEY, contextKey.key);
		}
		String query = null;
		try
		{
			query = b.build().getRawQuery();
		} catch (URISyntaxException e)
		{
			log.error("Can't re-encode URL query params, shouldn't happen", e);
		}
		return query == null ? "" : "?" + query;
	}
	
	protected SAMLAuthnContext createSamlContext(HttpServletRequest httpReq, AuthnRequestDocument samlRequest,
		SAMLIdPConfiguration samlIdPConfiguration, boolean isHTTPGet)
	{
		SAMLVerifiableElement verifiableMessage = isHTTPGet ? 
				new RedirectedMessage(httpReq.getQueryString()) 
				: new XMLExpandedMessage(samlRequest, samlRequest.getAuthnRequest());
		SAMLAuthnContext ret = new SAMLAuthnContext(samlRequest, samlIdPConfiguration, verifiableMessage);
		String rs = httpReq.getParameter(SAMLConstants.RELAY_STATE);
		if (rs != null)
			ret.setRelayState(rs);
		return ret;
	}
	
	protected AuthnRequestDocument parse(HttpServletRequest req) throws SAMLProcessingException
	{
		String samlRequest = req.getParameter(SAMLConstants.REQ_SAML_REQUEST);
		if (samlRequest == null)
		{
			throw new SAMLProcessingException("Received an HTTP request, without SAML request (no " + 
					SAMLConstants.REQ_SAML_REQUEST + " parameter)");
		}
		String decodedReq;
		try
		{
			if (req.getMethod().equals("POST"))
				decodedReq = extractRequestFromPostBinding(samlRequest);
			else if (req.getMethod().equals("GET"))
				decodedReq = extractRequestFromRedirectBinding(samlRequest);
			else
				throw new SAMLProcessingException("Received a request which is neither POST nor GET");
		} catch (Exception e)
		{
			throw new SAMLProcessingException("Received a request which can't be decoded", e);
		}
		
		AuthnRequestDocument reqDoc;
		try
		{
			reqDoc = AuthnRequestDocument.Factory.parse(decodedReq);
		} catch (XmlException e)
		{
			throw new SAMLProcessingException("Received a nonparseable SAML request", e);
		}
		
		return reqDoc;
	}

	protected void validate(SAMLAuthnContext context, HttpServletResponse servletResponse,
			SAMLIdPConfiguration samlConfig)
			throws SAMLProcessingException, IOException, EopException
	{
		WebAuthRequestValidator validator = new WebAuthRequestValidator(endpointAddress, 
				samlConfig.getAuthnTrustChecker(), samlConfig.requestValidityPeriod,
				samlConfig.getReplayChecker());
		samlConfig.configureKnownRequesters(validator);
		try
		{
			validator.validate(context.getRequestDocument(), context.getVerifiableElement());
		} catch (SAMLServerException e)
		{
			errorHandler.commitErrorResponse(context, e, servletResponse);
		}
	}

	@Override
	protected void postProcessRequest(boolean isGet, HttpServletRequest req, HttpServletResponse resp,
			String samlRequest, String relayState) throws IOException 
	{
	}
}
