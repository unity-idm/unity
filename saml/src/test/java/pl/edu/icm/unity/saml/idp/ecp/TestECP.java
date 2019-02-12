/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.ecp;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.collect.Lists;
import com.nimbusds.jwt.JWTClaimsSet;

import eu.emi.security.authn.x509.helpers.BinaryCertChainValidator;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpUtils;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
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
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;

public class TestECP extends AbstractTestIdpBase
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, TestECP.class);
	
	private static final String ECP_ENDP_CFG = 
			"unity.saml.requester.requesterEntityId=http://ecpSP.example.com\n" +
			"unity.saml.requester.metadataPath=metadata\n" +
			"unity.saml.requester.remoteIdp.1.address=http://localhost:52443/\n" +
			"unity.saml.requester.remoteIdp.1.samlId=http://example-saml-idp.org\n" +
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
	
	@Before
	@Override
	public void setup() throws Exception
	{
		super.setup();
		
		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpointECP"),
					"desc",	Lists.newArrayList(), ECP_ENDP_CFG, REALM_NAME);
		
		endpointMan.deploy(ECPEndpointFactory.NAME, "endpointECP", "/ecp", cfg);
		List<ResolvedEndpoint> endpoints = endpointMan.getEndpoints();
		assertEquals(2, endpoints.size());
		log.info("Deployed endpoints: {}", endpoints);
		
		List<InputTranslationRule> rules = new ArrayList<>();
		MapIdentityActionFactory factory = new MapIdentityActionFactory(idTypesReg);
			
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
		System.out.println("\n\nSending modified reesponse back to SP\n\n");
		System.out.println(envDoc2.xmlText(new XmlOptions().setSavePrettyPrint()));
		
		String ecpUrl = "https://localhost:52443/ecp" + ECPEndpointFactory.SERVLET_PATH;
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setValidator(new BinaryCertChainValidator(true));
		clientCfg.setSslEnabled(true);
		HttpPost httpPost = new HttpPost(ecpUrl);
		httpPost.setEntity(new StringEntity(envDoc2.xmlText(), ContentType.APPLICATION_XML));
		
		HttpClient httpclient = HttpUtils.createClient(ecpUrl, clientCfg);
		HttpResponse response = httpclient.execute(httpPost);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
		Assert.assertTrue(response.getFirstHeader("Content-Type").getValue().startsWith("application/jwt"));
		HttpEntity entity = response.getEntity();
		if (entity != null) 
		{
			String resp = EntityUtils.toString(entity);
			System.out.println(resp);
			JWTClaimsSet claims = JWTUtils.parseAndValidate(resp, pkiMan.getCredential("MAIN"));
			System.out.println("GOT:\n" + claims.toJSONObject().toJSONString());
			Assert.assertTrue(claims.getIssuer().contains("https://localhost:52443"));
		} else
			Assert.fail("No HTTP response");
	}
	
	private EnvelopeDocument sendToIdP(EnvelopeDocument envDoc) throws Exception
	{
		String authnWSUrl = "https://localhost:52443/saml/saml2idp-soap/AuthenticationService";
		
		EnvelopeDocument envDoc2 = EnvelopeDocument.Factory.newInstance();
		envDoc2.addNewEnvelope().setBody(envDoc.getEnvelope().getBody());
		
		DefaultClientConfiguration clientCfg = getClientCfg();
		clientCfg.setSslAuthn(false);

		HttpClient httpclient = HttpUtils.createClient(authnWSUrl, clientCfg);
		
		HttpHost targetHost = new HttpHost("localhost", 52443, "https");
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(
				new AuthScope(targetHost.getHostName(), targetHost.getPort()),
				new UsernamePasswordCredentials("user1", "mockPassword1"));
		AuthCache authCache = new BasicAuthCache();
		BasicScheme basicAuth = new BasicScheme();
		authCache.put(targetHost, basicAuth);
		HttpClientContext context = HttpClientContext.create();
		context.setCredentialsProvider(credsProvider);
		context.setAuthCache(authCache);

		HttpPost httpPost = new HttpPost(authnWSUrl);
		
		System.out.println("\n\nSending modified request to IDP\n\n");
		System.out.println(envDoc2.xmlText(new XmlOptions().setSavePrettyPrint()));
		
		
		httpPost.setEntity(new StringEntity(envDoc2.xmlText(), ContentType.APPLICATION_XML));
		
		List<ResolvedEndpoint> endpoints = endpointMan.getEndpoints();
		log.info("Deployed endpoints: {}", endpoints);
		
		HttpResponse response = httpclient.execute(targetHost, httpPost, context);
		HttpEntity entity = response.getEntity();
		if (entity != null) 
		{
			String xml = EntityUtils.toString(entity);
			log.info("Response: {}", xml);
			EnvelopeDocument envDocRet = EnvelopeDocument.Factory.parse(xml);
			return envDocRet;
		} else
		{
			Assert.fail("No HTTP response");
			return null;
		}
	}
	
	private EnvelopeDocument getSamlRequest() throws ClientProtocolException, IOException, XmlException
	{
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
		HttpResponse response = httpclient.execute(httpget);
		HttpEntity entity = response.getEntity();
		if (entity != null) 
		{
			String xml = EntityUtils.toString(entity);
			EnvelopeDocument envDoc = EnvelopeDocument.Factory.parse(xml);
			System.out.println(envDoc.xmlText(new XmlOptions().setSavePrettyPrint()));
			return envDoc;
		}
		Assert.fail("No HTTP response");
		return null;
	}

}
