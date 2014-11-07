/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.bouncycastle.util.encoders.Base64;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.saml.SAMLProcessingException;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.web.EopException;
import pl.edu.icm.unity.saml.validator.WebAuthRequestValidator;
import pl.edu.icm.unity.server.utils.Log;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.exceptions.SAMLValidationException;

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
public class SamlParseServlet extends HttpServlet
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SamlParseServlet.class);
	
	/**
	 * Under this key the SAMLContext object is stored in the session.
	 */
	public static final String SESSION_SAML_CONTEXT = "samlAuthnContextKey";
	
	/**
	 * key used by hold on form to mark that the new authn session should be started even 
	 * when an existing auth is in progress. 
	 */
	public static final String REQ_FORCE = "force";
	protected SamlIdpProperties samlConfig;
	protected String endpointAddress;
	protected String samlUiServletPath;
	protected ErrorHandler errorHandler;

	public SamlParseServlet(SamlIdpProperties samlConfig, String endpointAddress,
			String samlUiServletPath, ErrorHandler errorHandler)
	{
		super();
		this.samlConfig = samlConfig;
		this.endpointAddress = endpointAddress;
		this.samlUiServletPath = samlUiServletPath;
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
			//We can have the old session expired or order to forcefully close it.
			String force = request.getParameter(REQ_FORCE);
			if ((force == null || force.equals("false")) && !context.isExpired())
			{
				if (log.isTraceEnabled())
					log.trace("Request to SAML consumer address, with SAML input and we have " +
							"SAML login in progress, redirecting to hold on page: " + 
							request.getRequestURI());
				errorHandler.showHoldOnPage(samlRequestStr, 
						request.getParameter(SAMLConstants.RELAY_STATE),
						request.getMethod(), response);
				return;
			} else
			{
				if (log.isTraceEnabled())
					log.trace("Request to SAML consumer address, with SAML input and we are " +
							"forced to break the previous SAML login: " + 
							request.getRequestURI());
				session.removeAttribute(SESSION_SAML_CONTEXT);
			}
		}
		
		if (log.isTraceEnabled())
			log.trace("Request to protected address, with SAML input, will be processed: " + 
					request.getRequestURI());
		try
		{
			AuthnRequestDocument samlRequest = parse(request);
			if (log.isTraceEnabled())
				log.trace("Parsed SAML request:\n" + samlRequest.xmlText());
			context = createSamlContext(request, samlRequest);
			validate(context, response);
		} catch (SAMLProcessingException e)
		{
			if (log.isDebugEnabled())
				log.debug("Processing of SAML input failed", e);
			errorHandler.showErrorPage(e, (HttpServletResponse) response);
			return;
		}
		
		session.setAttribute(SESSION_SAML_CONTEXT, context);
		if (log.isTraceEnabled())
			log.trace("Request with SAML input handled successfully");
		response.sendRedirect(samlUiServletPath);
		//request.getRequestDispatcher(samlUiServletPath).forward(request, response);
	}
	
	protected SAMLAuthnContext createSamlContext(HttpServletRequest request, AuthnRequestDocument samlRequest)
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
				decodedReq = new String(Base64.decode(samlRequest));
			else if (req.getMethod().equals("GET"))
				decodedReq = inflateSAMLRequest(samlRequest);
			else
				throw new SAMLProcessingException("Received a request which is neither POST nor GET");
		} catch (Exception e)
		{
			throw new SAMLProcessingException("Received a request which can't be translated into XML form", e);
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

	protected void validate(SAMLAuthnContext context, HttpServletResponse servletResponse) 
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
			//security measure: if the request is invalid (usually not trusted) don't send the response,
			//as it may happen that the response URL is evil.
			if (e.getCause() != null && e.getCause() instanceof SAMLValidationException)
				throw new SAMLProcessingException(e);
			errorHandler.commitErrorResponse(context, e, servletResponse);
		}
	}
	
	protected String inflateSAMLRequest(String samlRequest) throws Exception
	{
		byte[] third = Base64.decode(samlRequest);
		Inflater decompressor = new Inflater(true);
		decompressor.setInput(third, 0, third.length);
		ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
		InflaterOutputStream os = new InflaterOutputStream(baos, decompressor);
		os.write(third);
		os.finish();
		os.close();
		return new String(baos.toByteArray(), Constants.UTF);
	}
}
