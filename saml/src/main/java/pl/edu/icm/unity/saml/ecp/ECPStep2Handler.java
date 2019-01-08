/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.saml.ecp;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import eu.unicore.samly2.SAMLBindings;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.LoginSession.RememberMeInfo;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultProcessor;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.rest.jwt.endpoint.JWTManagement;
import pl.edu.icm.unity.saml.SAMLResponseValidatorUtil;
import pl.edu.icm.unity.saml.metadata.cfg.RemoteMetaManager;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.saml.xmlbeans.soap.Body;
import pl.edu.icm.unity.saml.xmlbeans.soap.Envelope;
import pl.edu.icm.unity.saml.xmlbeans.soap.EnvelopeDocument;
import pl.edu.icm.unity.saml.xmlbeans.soap.Header;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * Responsible for parsing HTTP POST with SAML response.
 * @author K. Benedyczak
 */
public class ECPStep2Handler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, ECPStep2Handler.class);
	private RemoteMetaManager metadataManager;
	private ECPContextManagement samlContextManagement;
	private RemoteAuthnResultProcessor remoteAuthnProcessor;
	private JWTManagement jwtGenerator;
	private AuthenticationRealm realm;
	private SessionManagement sessionMan;
	private ReplayAttackChecker replayAttackChecker;
	private String myAddress;
	
	public ECPStep2Handler(SAMLECPProperties samlProperties, RemoteMetaManager metadataManager,
			ECPContextManagement samlContextManagement, String myAddress,
			ReplayAttackChecker replayAttackChecker, 
			TokensManagement tokensMan, PKIManagement pkiManagement, 
			RemoteAuthnResultProcessor remoteAuthnProcessor,
			EntityManagement entityMan,
			SessionManagement sessionMan, AuthenticationRealm realm, String address)
	{
		this.metadataManager = metadataManager;
		this.samlContextManagement = samlContextManagement;
		this.remoteAuthnProcessor = remoteAuthnProcessor;
		this.jwtGenerator = new JWTManagement(tokensMan, pkiManagement, entityMan, 
				realm.getName(), address, samlProperties.getJWTProperties());
		this.realm = realm;
		this.sessionMan = sessionMan;
		this.replayAttackChecker = replayAttackChecker;
		this.myAddress = myAddress;
	}


	protected void processECPPostRequest(HttpServletRequest req, HttpServletResponse resp)
			throws IOException
	{
		EnvelopeDocument soapEnvDoc;
		try
		{
			soapEnvDoc = EnvelopeDocument.Factory.parse(req.getReader());
		} catch (XmlException e)
		{
			log.warn("Received contents which can not the parsed as SOAP Envelope.", e);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Received a contents which can not "
					+ "the parsed as SOAP Envelope.");
			return;
		}
		
		Envelope soapEnv = soapEnvDoc.getEnvelope();
		Header soapHeader = soapEnv.getHeader();
		String relayState;
		try
		{
			relayState = processHeader(soapHeader);
		} catch (ServletException e1)
		{
			log.warn("Wrong ECP response header", e1);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e1.getMessage());
			return;
		}
		
		ECPAuthnState ctx;
		try
		{
			ctx = samlContextManagement.getAuthnContext(relayState);
		} catch (WrongArgumentException e)
		{
			log.warn("Received a request with unknown relay state " + relayState);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, 
					"Received a request with unknown relay state");
			return;
		}
		
		Body soapBody = soapEnv.getBody();
		Reader bodyReader = soapBody.newReader();
		ResponseDocument respDoc;
		try
		{
			respDoc = ResponseDocument.Factory.parse(bodyReader);
		} catch (XmlException e)
		{
			log.warn("Received SOAP body contents which can not be parsed as SAML response.", e);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, 
					"Received SOAP body contents which can not be parsed as SAML response.");
			return;
		}
		
		SAMLSPProperties samlProperties = (SAMLSPProperties) metadataManager.getVirtualConfiguration();
		AuthenticationResult authenticationResult;
		try
		{
			authenticationResult = processSamlResponse(samlProperties, respDoc, ctx);
		} catch (Exception e)
		{
			log.warn("Error while processing SAML response", e);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
			return;
		}
		
		if (!authenticationResult.getStatus().equals(Status.success))
		{
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "SAML authentication is unsuccessful");
			return;
		}
		
		AuthenticatedEntity ae = authenticationResult.getAuthenticatedEntity();
		Long entityId = ae.getEntityId();
		
		InvocationContext iCtx = new InvocationContext(null, realm, Collections.emptyList());
		authnSuccess(ae, iCtx);
		InvocationContext.setCurrent(iCtx);
		
		try
		{
			String token = jwtGenerator.generate(new EntityParam(entityId));
		
			resp.setContentType("application/jwt");
			resp.getWriter().write(token);
			resp.flushBuffer();
		} finally
		{
			InvocationContext.setCurrent(null);
		}
	}
	
	private void authnSuccess(AuthenticatedEntity client, InvocationContext ctx)
	{
		if (log.isDebugEnabled())
			log.debug("Client was successfully authenticated: [" + 
					client.getEntityId() + "] " + client.getAuthenticatedWith().toString());
		LoginSession ls = sessionMan.getCreateSession(client.getEntityId(), realm, 
				"", client.getOutdatedCredentialId(), new RememberMeInfo(false, false), null, null);
		ctx.setLoginSession(ls);
		ls.addAuthenticatedIdentities(client.getAuthenticatedWith());
		ls.setRemoteIdP(client.getRemoteIdP());
	}

	
	private String processHeader(Header soapHeader) throws ServletException
	{
		Node headerDom = soapHeader.getDomNode();
		NodeList elements = headerDom.getChildNodes();
		String rs = null;
		for (int i=0; i<elements.getLength(); i++)
		{
			Node elementN = elements.item(i);
			if (!(elementN instanceof Element))
				continue;
			Element element = (Element) elementN;
			String name = element.getLocalName();
			String ns = element.getNamespaceURI();
			if (ECPConstants.ECP_NS.equals(ns) && "RelayState".equals(name))
				rs = extractRelayState(element);
			else
			{
				String mustUnderstand = element.getAttributeNS("http://schemas.xmlsoap.org/soap/envelope/", 
						"mustUnderstand");
				if ("1".equals(mustUnderstand) || "true".equals(mustUnderstand))
					throw new ServletException("Unsupported header which is marked as " +
							"mandatory to understand: " + element.getLocalName());
			}
		}
		if (rs == null)
			throw new ServletException("RelayState was not provided");
		return rs;
	}
	
	private String extractRelayState(Element rsElement) throws ServletException
	{
		Node contents = rsElement.getFirstChild();
		if (contents == null)
			throw new ServletException("RelayState element is malformed or empty");
		return contents.getNodeValue();
	}
	
	private AuthenticationResult processSamlResponse(SAMLSPProperties samlProperties, 
			ResponseDocument responseDoc, ECPAuthnState ctx) 
			throws ServletException, AuthenticationException
	{
		String key = findIdPKey(samlProperties, responseDoc);
		String groupAttr = samlProperties.getValue(key + SAMLSPProperties.IDP_GROUP_MEMBERSHIP_ATTRIBUTE);
		String profile = samlProperties.getValue(key + CommonWebAuthnProperties.TRANSLATION_PROFILE);
		SAMLResponseValidatorUtil responseValidatorUtil = new SAMLResponseValidatorUtil(samlProperties, 
				replayAttackChecker, myAddress);
		RemotelyAuthenticatedInput input = responseValidatorUtil.verifySAMLResponse(responseDoc, 
				ctx.getRequestId(), SAMLBindings.PAOS, groupAttr, key);
		return remoteAuthnProcessor.getResult(input, profile, false, Optional.empty());
	}
	
	private String findIdPKey(SAMLSPProperties samlProperties, ResponseDocument responseDoc) throws ServletException
	{
		NameIDType issuer = responseDoc.getResponse().getIssuer();
		if (issuer == null || issuer.isNil())
			throw new ServletException("Invalid response: no issuer");
		String issuerName = issuer.getStringValue();
		Set<String> idps = samlProperties.getStructuredListKeys(SAMLSPProperties.IDP_PREFIX);
		for (String k: idps)
			if (samlProperties.getValue(k+SAMLSPProperties.IDP_ID).equals(issuerName))
				return k;
		throw new ServletException("The issuer " + issuerName + " is not among trusted issuers");		
	}
}
