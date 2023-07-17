/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.ecp;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.hc.client5.http.ContextBuilder;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.collect.Lists;
import com.nimbusds.jwt.JWTClaimsSet;

import eu.emi.security.authn.x509.helpers.BinaryCertChainValidator;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpUtils;
import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.translation.ExternalDataParser;
import pl.edu.icm.unity.engine.api.translation.TranslationCondition;
import pl.edu.icm.unity.engine.api.translation.in.IdentityEffectMode;
import pl.edu.icm.unity.engine.translation.in.InputTranslationRule;
import pl.edu.icm.unity.engine.translation.in.action.MapIdentityActionFactory;
import pl.edu.icm.unity.rest.jwt.JWTUtils;
import pl.edu.icm.unity.saml.ecp.ECPConstants;
import pl.edu.icm.unity.saml.ecp.ECPEndpointFactory;
import pl.edu.icm.unity.saml.idp.AbstractTestIdpBase;
import pl.edu.icm.unity.saml.xmlbeans.ecp.RelayStateDocument;
import pl.edu.icm.unity.saml.xmlbeans.soap.Envelope;
import pl.edu.icm.unity.saml.xmlbeans.soap.EnvelopeDocument;
import pl.edu.icm.unity.saml.xmlbeans.soap.Header;

public class TestECP extends AbstractTestIdpBase
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, TestECP.class);
	
	private static final String ECP_ENDP_CFG = 
			"unity.saml.requester.requesterEntityId=http://ecpSP.example.com\n" +
			"unity.saml.requester.metadataPath=metadata\n" +
			"unity.saml.requester.remoteIdp.1.address=http://localhost:52443/\n" +
			"unity.saml.requester.remoteIdp.1.samlId=http://example-saml-idp.org\n" +
			"unity.saml.requester.remoteIdp.1.binding=SOAP\n" +
			"unity.saml.requester.remoteIdp.1.certificate=MAIN\n" +
			"unity.saml.requester.remoteIdp.1.translationProfile=testP\n" + 
			"unity.saml.requester.jwt.credential=MAIN\n" +
			"unity.saml.requester.jwt.tokenTtl=10\n";
	
	@Autowired
	private TranslationProfileManagement profilesMan;
	@Autowired
	private PKIManagement pkiMan;
	@Autowired
	private IdentityTypesRegistry idTypesReg;
	@Autowired
	private ExternalDataParser parser;
	
	@BeforeEach
	@Override
	public void setup() throws Exception
	{
		super.setup();
		
		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpointECP"),
					"desc",	Lists.newArrayList(), ECP_ENDP_CFG, REALM_NAME);
		
		endpointMan.deploy(ECPEndpointFactory.NAME, "endpointECP", "/ecp", cfg);
		List<ResolvedEndpoint> endpoints = endpointMan.getDeployedEndpoints();
		assertEquals(2, endpoints.size());
		log.info("Deployed endpoints: {}", endpoints);
		
		List<InputTranslationRule> rules = new ArrayList<>();
		MapIdentityActionFactory factory = new MapIdentityActionFactory(idTypesReg, parser);
			
		InputTranslationRule mapId = new InputTranslationRule(
				factory.getInstance("userName", "attr['unity:identity:userName']", 
						"cr-pass", IdentityEffectMode.CREATE_OR_MATCH.toString()), 
				new TranslationCondition());
		rules.add(mapId);
		TranslationProfile testP = new TranslationProfile("testP", "", ProfileType.INPUT, rules);
		profilesMan.addProfile(testP);
	}
	
	@Test
	public void testRequest() throws Exception
	{
		EnvelopeDocument samlReqDoc = getSamlRequest();
		EnvelopeDocument samlRespDoc = sendToIdP(samlReqDoc);
		sendResponseToSP(samlRespDoc, samlReqDoc);
	}

	private void sendResponseToSP(EnvelopeDocument samlRespDoc, EnvelopeDocument samlReqDoc) throws Exception
	{
		EnvelopeDocument envDoc2 = EnvelopeDocument.Factory.newInstance();
		Envelope env2 = envDoc2.addNewEnvelope();
		
		Header header = samlReqDoc.getEnvelope().getHeader();
		NodeList nl = ((Element)header.getDomNode()).getElementsByTagNameNS(
				"urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp",
				"RelayState");
		RelayStateDocument rs = RelayStateDocument.Factory.parse(nl.item(0));
		env2.addNewHeader().set(rs);
		
		env2.setBody(samlRespDoc.getEnvelope().getBody());
		System.out.println("\n\nSending modified response back to SP\n\n");
		System.out.println(envDoc2.xmlText(new XmlOptions().setSavePrettyPrint()));
		
		String ecpUrl = "https://localhost:52443/ecp" + ECPEndpointFactory.SERVLET_PATH;
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setValidator(new BinaryCertChainValidator(true));
		clientCfg.setSslEnabled(true);
		HttpPost httpPost = new HttpPost(ecpUrl);
		httpPost.setEntity(new StringEntity(envDoc2.xmlText(), ContentType.APPLICATION_XML));
		
		HttpClient httpclient = HttpUtils.createClient(ecpUrl, clientCfg);
		try(ClassicHttpResponse response = httpclient.executeOpen(null, httpPost, HttpClientContext.create())){
			assertEquals(HttpServletResponse.SC_OK, response.getCode());
			assertTrue(response.getFirstHeader("Content-Type").getValue().startsWith("application/jwt"));
			HttpEntity entity = response.getEntity();
			if (entity != null)
			{
				String resp = EntityUtils.toString(entity);
				System.out.println(resp);
				JWTClaimsSet claims = JWTUtils.parseAndValidate(resp, pkiMan.getCredential("MAIN"));
				System.out.println("GOT:\n" + claims.toString());
				assertTrue(claims.getIssuer().contains("https://localhost:52443"));
				response.close();
			} else
				fail("No HTTP response");
		}
	}
	
	private EnvelopeDocument sendToIdP(EnvelopeDocument envDoc) throws Exception
	{
		String authnWSUrl = "https://localhost:52443/saml/saml2idp-soap/AuthenticationService";
		
		EnvelopeDocument envDoc2 = EnvelopeDocument.Factory.newInstance();
		envDoc2.addNewEnvelope().setBody(envDoc.getEnvelope().getBody());
		
		DefaultClientConfiguration clientCfg = getClientCfg();
		clientCfg.setSslAuthn(false);

		HttpClient httpclient = HttpUtils.createClient(authnWSUrl, clientCfg);
		
		HttpHost targetHost = new HttpHost("https", "localhost", 52443);
		ContextBuilder cb = ContextBuilder.create();
		cb.preemptiveBasicAuth(targetHost, new UsernamePasswordCredentials("user1", "mockPassword1".toCharArray()));
		HttpClientContext context = cb.build();

		HttpPost httpPost = new HttpPost(authnWSUrl);
		
		System.out.println("\n\nSending modified request to IDP\n\n");
		System.out.println(envDoc2.xmlText(new XmlOptions().setSavePrettyPrint()));
		
		
		httpPost.setEntity(new StringEntity(envDoc2.xmlText(), ContentType.APPLICATION_XML));
		
		List<ResolvedEndpoint> endpoints = endpointMan.getDeployedEndpoints();
		log.info("Deployed endpoints: {}", endpoints);
		
		String xml = httpclient.execute(targetHost, httpPost, context, new BasicHttpClientResponseHandler());
		log.info("Response: {}", xml);
		EnvelopeDocument envDocRet = EnvelopeDocument.Factory.parse(xml);
		return envDocRet;
	}
	
	private EnvelopeDocument getSamlRequest() throws Exception	{
		String ecpUrl = "https://localhost:52443/ecp" + ECPEndpointFactory.SERVLET_PATH;
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setValidator(new BinaryCertChainValidator(true));
		clientCfg.setSslEnabled(true);
		
		HttpClient httpclient = HttpUtils.createClient(ecpUrl, clientCfg);
		HttpGet httpget = new HttpGet(ecpUrl);
		httpget.addHeader("Accept", ECPConstants.ECP_CONTENT_TYPE);
		httpget.addHeader("PAOS", "ver=\"urn:liberty:paos:2003-08\"; " +
				"\"urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp\"," +
				"\"urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp:2.0:cb\"," +
				"\"urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp:2.0:hok\"");
		System.out.println("\n\nSending GET request to ECP enabled SP\n\n");
		String xml = httpclient.execute(httpget, new BasicHttpClientResponseHandler());
		EnvelopeDocument envDoc = EnvelopeDocument.Factory.parse(xml);
		System.out.println(envDoc.xmlText(new XmlOptions().setSavePrettyPrint()));
		return envDoc;
	}

}
