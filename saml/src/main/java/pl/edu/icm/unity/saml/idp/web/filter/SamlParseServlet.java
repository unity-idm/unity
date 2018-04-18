/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web.filter;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.exceptions.SAMLServerException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.utils.RoutingServlet;
import pl.edu.icm.unity.saml.SAMLProcessingException;
import pl.edu.icm.unity.saml.SamlHttpServlet;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.metadata.cfg.RemoteMetaManager;
import pl.edu.icm.unity.saml.validator.WebAuthRequestValidator;
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
public class SamlParseServlet extends SamlHttpServlet
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SamlParseServlet.class);
	
	/**
	 * Under this key the SAMLContext object is stored in the session.
	 */
	public static final String SESSION_SAML_CONTEXT = "samlAuthnContextKey";
	
	protected RemoteMetaManager samlConfigProvider;
	protected String endpointAddress;
	protected String samlDispatcherServletPath;
	protected ErrorHandler errorHandler;

	public SamlParseServlet(RemoteMetaManager samlConfigProvider, String endpointAddress,
			String samlDispatcherServletPath, ErrorHandler errorHandler)
	{
		super(true, false, false);
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
		processSamlRequest(request, response);
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
		processSamlRequest(request, response);
	}

	protected void processSamlRequest(HttpServletRequest request, HttpServletResponse response) 
			throws IOException, ServletException
	{
		try
		{
			processSamlRequestInterruptible(request, response);
		} catch (EopException e)
		{
			//OK
		}
	}
	
	protected void processSamlRequestInterruptible(HttpServletRequest request, HttpServletResponse response) 
			throws IOException, ServletException, EopException
	{
		log.trace("Starting SAML request processing");
		SamlIdpProperties samlConfig = (SamlIdpProperties) samlConfigProvider.getVirtualConfiguration();
		HttpSession session = request.getSession();
		SAMLAuthnContext context = (SAMLAuthnContext) session.getAttribute(SESSION_SAML_CONTEXT); 

		String samlRequestStr = request.getParameter(SAMLConstants.REQ_SAML_REQUEST);
		//do we have a new request?
		if (samlRequestStr == null)
		{
			if (log.isTraceEnabled())
				log.trace("Request to SAML endpoint address, without SAML input, error: " + 
						request.getRequestURI());
			errorHandler.showErrorPage(new SAMLProcessingException("No SAML request"), 
					(HttpServletResponse) response);
			return;
		}
		//ok, we do have a new request. 

		
		//is there processing in progress?
		if (context != null)
		{
			if (log.isTraceEnabled() && !context.isExpired())
				log.trace("Request to SAML consumer address, with SAML input and we are " +
						"forced to break the previous SAML login: " + 
						request.getRequestURI());
			session.removeAttribute(SESSION_SAML_CONTEXT);
		}
		
		if (log.isTraceEnabled())
			log.trace("Got request with SAML input to: " + request.getRequestURI());
		try
		{
			AuthnRequestDocument samlRequest = parse(request);
			if (log.isTraceEnabled())
				log.trace("Parsed SAML request:\n" + samlRequest.xmlText());
			context = createSamlContext(request, samlRequest, samlConfig);
			validate(context, response, samlConfig);
		} catch (SAMLProcessingException e)
		{
			if (log.isDebugEnabled())
				log.debug("Processing of SAML input failed", e);
			errorHandler.showErrorPage(e, (HttpServletResponse) response);
			return;
		}
		
		session.setAttribute(SESSION_SAML_CONTEXT, context);
		RoutingServlet.clean(request);
		if (log.isTraceEnabled())
			log.trace("Request with SAML input handled successfully");
		//Note - this is intended, even taking into account the overhead. We don't want to pass alongside
		//original HTTP query params, we want a clean URL in HTTP redirect case. In HTTP POST case it is
		//even more important: web browser would warn the user about doubled POST.
		response.sendRedirect(samlDispatcherServletPath);
	}
	
	protected SAMLAuthnContext createSamlContext(HttpServletRequest request, AuthnRequestDocument samlRequest,
			SamlIdpProperties samlConfig)
	{
		SAMLAuthnContext ret = new SAMLAuthnContext(samlRequest, samlConfig);
		String rs = request.getParameter(SAMLConstants.RELAY_STATE);
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
			SamlIdpProperties samlConfig) 
			throws SAMLProcessingException, IOException, EopException
	{
		WebAuthRequestValidator validator = new WebAuthRequestValidator(endpointAddress, 
				samlConfig.getAuthnTrustChecker(), samlConfig.getRequestValidity(), 
				samlConfig.getReplayChecker());
		samlConfig.configureKnownRequesters(validator);
		try
		{
			validator.validate(context.getRequestDocument());
		} catch (SAMLServerException e)
		{
			errorHandler.commitErrorResponse(context, e, servletResponse);
		}
	}
}
