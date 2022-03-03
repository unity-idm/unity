/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.saml.ecp;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Supplier;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.xmlbeans.XmlCursor;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.SAMLConstants;
import pl.edu.icm.unity.saml.SAMLHelper;
import pl.edu.icm.unity.saml.metadata.cfg.SPRemoteMetaManager;
import pl.edu.icm.unity.saml.sp.config.SAMLSPConfiguration;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPConfiguration;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPs;
import pl.edu.icm.unity.saml.xmlbeans.ecp.RelayStateDocument;
import pl.edu.icm.unity.saml.xmlbeans.ecp.RelayStateType;
import pl.edu.icm.unity.saml.xmlbeans.ecp.RequestDocument;
import pl.edu.icm.unity.saml.xmlbeans.ecp.RequestType;
import pl.edu.icm.unity.saml.xmlbeans.soap.Body;
import pl.edu.icm.unity.saml.xmlbeans.soap.Envelope;
import pl.edu.icm.unity.saml.xmlbeans.soap.EnvelopeDocument;
import pl.edu.icm.unity.saml.xmlbeans.soap.Header;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.IDPEntryType;
import xmlbeans.org.oasis.saml2.protocol.IDPListType;

/**
 * Responsible for parsing a GET request to the PAOS ECP endpoint and issuance of a 
 * SAML request which can be sent as the response.
 * @author K. Benedyczak
 */
public class ECPStep1Handler
{
	private final SPRemoteMetaManager metadataManager;
	private final String myAddress;
	private final ECPContextManagement samlContextManagement;
	private final Supplier<SAMLSPConfiguration> configProvider;

	public ECPStep1Handler(Supplier<SAMLSPConfiguration> configProvider, SPRemoteMetaManager metadataManager, 
			ECPContextManagement samlContextManagement, String myAddress)
	{
		this.configProvider = configProvider;
		this.metadataManager = metadataManager;
		this.myAddress = myAddress;
		this.samlContextManagement = samlContextManagement;
	}

	protected void processECPGetRequest(HttpServletRequest req, HttpServletResponse resp)
			throws IOException
	{
		try
		{
			verifyRequestHeaders(req);
		} catch (ServletException e)
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
			return;
		}
		
		ECPAuthnState context = new ECPAuthnState();
		EnvelopeDocument envDoc = generateECPEnvelope(context);
		samlContextManagement.addAuthnContext(context);
		
		resp.setContentType(ECPConstants.ECP_CONTENT_TYPE);
		resp.addHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
		resp.addHeader("Pragma", "no-cache");

		PrintWriter writer = resp.getWriter();
		envDoc.save(writer);
		writer.flush();
	}

	private EnvelopeDocument generateECPEnvelope(ECPAuthnState context)
	{
		EnvelopeDocument envDoc = EnvelopeDocument.Factory.newInstance();
		Envelope env = envDoc.addNewEnvelope();
		
		TrustedIdPs trustedIdPs = metadataManager.getTrustedIdPs();
		
		Header header = env.addNewHeader();
		XmlCursor curH = header.newCursor();
		curH.toFirstContentToken();
		SAMLSPConfiguration samlspConfiguration = configProvider.get();
		generateEcpHeaders(samlspConfiguration, trustedIdPs, curH, context);
		generatePaosHeader(curH);
		curH.dispose();
		
		Body body = env.addNewBody();
		XmlCursor curBody = body.newCursor();
		curBody.toFirstContentToken();
		generateSamlRequest(samlspConfiguration, curBody, context);
		curBody.dispose();
		
		return envDoc;
	}

	private void generateSamlRequest(SAMLSPConfiguration samlConfig, XmlCursor curBody, ECPAuthnState context)
	{
		boolean sign = samlConfig.signRequestByDefault;
		String requesterId = samlConfig.requesterSamlId; 
		String requestedNameFormat = samlConfig.defaultRequestedNameFormat;
		X509Credential credential = sign ? samlConfig.requesterCredential : null;
		
		AuthnRequestDocument authnRequestDoc = SAMLHelper.createSAMLRequest(myAddress, sign, requesterId, null,
				requestedNameFormat, true, credential);
		context.setRequestId(authnRequestDoc.getAuthnRequest().getID()); 

		XmlCursor curR = authnRequestDoc.getAuthnRequest().newCursor();
		curR.copyXml(curBody);
		curR.dispose();
	}
	
	private void generatePaosHeader(XmlCursor curH)
	{
		pl.edu.icm.unity.saml.xmlbeans.paos.RequestDocument paosReqDoc = 
				pl.edu.icm.unity.saml.xmlbeans.paos.RequestDocument.Factory.newInstance();
		pl.edu.icm.unity.saml.xmlbeans.paos.RequestType paosReq = paosReqDoc.addNewRequest();
		
		paosReq.setMustUnderstand(true);
		paosReq.setActor("http://schemas.xmlsoap.org/soap/actor/next");
		paosReq.setService(ECPConstants.ECP_PROFILE);
		paosReq.setResponseConsumerURL(myAddress);
		
		XmlCursor curPa = paosReq.newCursor();
		curPa.copyXml(curH);
		curPa.dispose();
	}
	
	private void generateEcpHeaders(SAMLSPConfiguration samlConfig, TrustedIdPs trustedIdPs, XmlCursor curH, ECPAuthnState context)
	{
		RequestDocument ecpRequestDoc = RequestDocument.Factory.newInstance();
		RequestType ecpReq = ecpRequestDoc.addNewRequest();
		ecpReq.setActor("http://schemas.xmlsoap.org/soap/actor/next");
		ecpReq.setMustUnderstand(true);
		
		NameIDType issuer = ecpReq.addNewIssuer();
		String requestrId = samlConfig.requesterSamlId;
		issuer.setFormat(SAMLConstants.NFORMAT_ENTITY);
		issuer.setStringValue(requestrId);
		
		IDPListType idps = ecpReq.addNewIDPList();
		for (TrustedIdPConfiguration idpConfig: trustedIdPs.getAll())
		{
			String idpId = idpConfig.samlId;
			String idpName = idpConfig.name.getDefaultValue(); 
			IDPEntryType idp = idps.addNewIDPEntry();
			idp.setProviderID(idpId);
			idp.setName(idpName);
		}
		
		RelayStateDocument relayStateDoc = RelayStateDocument.Factory.newInstance();
		RelayStateType relayState = relayStateDoc.addNewRelayState();
		relayState.setActor("http://schemas.xmlsoap.org/soap/actor/next");
		relayState.setMustUnderstand(true);
		relayState.setStringValue(context.getRelayState());
		XmlCursor curER = ecpReq.newCursor();
		curER.copyXml(curH);
		curER.dispose();
		
		XmlCursor curRS = relayState.newCursor();
		curRS.copyXml(curH);
		curRS.dispose();
	}
	
	private void verifyRequestHeaders(HttpServletRequest req) throws ServletException
	{
		String accept = req.getHeader("Accept");
		if (accept == null)
			throw new ServletException("No Accept header in request, what is mandatory for this service.");
		if (!accept.contains(ECPConstants.ECP_CONTENT_TYPE))
			throw new ServletException("Client must be able to accept " + ECPConstants.ECP_CONTENT_TYPE
					+ " what was not advertised in the Accept header.");
		
		String paos = req.getHeader("PAOS");
		if (paos == null)
			throw new ServletException("No PAOS header in request, what is mandatory for this service.");
		if (!paos.startsWith(ECPConstants.PAOS_VERSION))
			throw new ServletException("PAOS version incorrect, supported version is " + 
					ECPConstants.PAOS_VERSION);
		if (!paos.contains('\"' + ECPConstants.ECP_PROFILE + '\"'))
			throw new ServletException("PAOS header must include support for the ECP profile, "
					+ "which is missing.");
		
	}
}
