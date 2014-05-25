/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.saml.ecp;

import java.io.IOException;
import java.io.Reader;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import eu.unicore.samly2.SAMLBindings;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.saml.SAMLResponseValidatorUtil;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.saml.xmlbeans.soap.Body;
import pl.edu.icm.unity.saml.xmlbeans.soap.Envelope;
import pl.edu.icm.unity.saml.xmlbeans.soap.EnvelopeDocument;
import pl.edu.icm.unity.saml.xmlbeans.soap.Header;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.remote.RemoteVerificatorUtil;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.utils.Log;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * Responsible for parsing HTTP POST with SAML response.
 * @author K. Benedyczak
 */
public class ECPStep2Handler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, ECPStep2Handler.class);
	private SAMLSPProperties samlProperties;
	private ECPContextManagement samlContextManagement;
	private SAMLResponseValidatorUtil responseValidatorUtil;
	private RemoteVerificatorUtil remoteVerificatorUtil;
	
	public ECPStep2Handler(SAMLSPProperties samlProperties, 
			ECPContextManagement samlContextManagement, String myAddress,
			ReplayAttackChecker replayAttackChecker, IdentityResolver identityResolver,
			TranslationProfileManagement profileManagement, AttributesManagement attrMan)
	{
		this.samlProperties = samlProperties;
		this.samlContextManagement = samlContextManagement;
		this.responseValidatorUtil = new SAMLResponseValidatorUtil(samlProperties, 
				replayAttackChecker, myAddress);
		this.remoteVerificatorUtil = new RemoteVerificatorUtil(identityResolver, 
				profileManagement, attrMan);
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
		
		try
		{
			processSamlResponse(respDoc, ctx);
		} catch (Exception e)
		{
			log.warn("Error while processing SAML response", e);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
			return;
		}
		
		//TODO - generate and return token
		
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
	
	private AuthenticationResult processSamlResponse(ResponseDocument responseDoc, ECPAuthnState ctx) 
			throws ServletException, AuthenticationException
	{
		String key = findIdPKey(responseDoc);
		String groupAttr = samlProperties.getValue(key + SAMLSPProperties.IDP_GROUP_MEMBERSHIP_ATTRIBUTE);
		String profile = samlProperties.getValue(key + SAMLSPProperties.IDP_TRANSLATION_PROFILE);
		RemotelyAuthenticatedInput input = responseValidatorUtil.verifySAMLResponse(responseDoc, 
				ctx.getRequestId(), SAMLBindings.PAOS, groupAttr);
		return remoteVerificatorUtil.getResult(input, profile);
	}
	
	private String findIdPKey(ResponseDocument responseDoc) throws ServletException
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
