/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.samlidp.ecp;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.security.KeyStoreException;
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
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import pl.edu.icm.unity.rest.jwt.JWTUtils;
import pl.edu.icm.unity.saml.ecp.ECPConstants;
import pl.edu.icm.unity.saml.ecp.ECPEndpointFactory;
import pl.edu.icm.unity.saml.idp.ws.SamlIdPSoapEndpointFactory;
import pl.edu.icm.unity.saml.xmlbeans.ecp.RelayStateDocument;
import pl.edu.icm.unity.saml.xmlbeans.soap.Envelope;
import pl.edu.icm.unity.saml.xmlbeans.soap.EnvelopeDocument;
import pl.edu.icm.unity.saml.xmlbeans.soap.Header;
import pl.edu.icm.unity.samlidp.AbstractTestIdpBase;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.server.translation.TranslationCondition;
import pl.edu.icm.unity.server.translation.in.IdentityEffectMode;
import pl.edu.icm.unity.server.translation.in.InputTranslationProfile;
import pl.edu.icm.unity.server.translation.in.InputTranslationProfile.ProfileMode;
import pl.edu.icm.unity.server.translation.in.InputTranslationRule;
import pl.edu.icm.unity.stdext.tactions.in.MapIdentityActionFactory;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;

import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;

import eu.emi.security.authn.x509.helpers.BinaryCertChainValidator;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpUtils;

public class TestECP extends AbstractTestIdpBase
{
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
	public void setup()
	{
		super.setup();
		try
		{
			List<AuthenticatorSet> authnCfg = new ArrayList<AuthenticatorSet>();
			endpointMan.deploy(ECPEndpointFactory.NAME, "endpointECP", new I18nString("endpointECP"),
					"/ecp", "desc", 
					authnCfg, ECP_ENDP_CFG, REALM_NAME);
			List<EndpointDescription> endpoints = endpointMan.getEndpoints();
			assertEquals(2, endpoints.size());
			
			List<InputTranslationRule> rules = new ArrayList<InputTranslationRule>();
			MapIdentityActionFactory factory = new MapIdentityActionFactory(idTypesReg);
			
			InputTranslationRule mapId = new InputTranslationRule(
					factory.getInstance("userName", "attr['unity:identity:userName']", 
							"cr-pass", IdentityEffectMode.CREATE_OR_MATCH.toString()), 
					new TranslationCondition());
			rules.add(mapId);
			InputTranslationProfile testP = new InputTranslationProfile("testP", rules, ProfileMode.UPDATE_ONLY);
			profilesMan.addProfile(testP);
			
		} catch (Exception e)
		{
			throw new RuntimeException(e);
		}
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
			ReadOnlyJWTClaimsSet claims = JWTUtils.parseAndValidate(resp, pkiMan.getCredential("MAIN"));
			System.out.println("GOT:\n" + claims.toJSONObject().toJSONString());
			Assert.assertTrue(claims.getIssuer().contains("https://localhost:52443"));
		} else
			Assert.fail("No HTTP response");
	}
	
	private EnvelopeDocument sendToIdP(EnvelopeDocument envDoc) throws KeyStoreException, IOException, XmlException
	{
		String authnWSUrl = "https://localhost:52443/saml" + SamlIdPSoapEndpointFactory.SERVLET_PATH +
				"/AuthenticationService";
		
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
		
		HttpResponse response = httpclient.execute(targetHost, httpPost, context);
		HttpEntity entity = response.getEntity();
		if (entity != null) 
		{
			String xml = EntityUtils.toString(entity);
			EnvelopeDocument envDocRet = EnvelopeDocument.Factory.parse(xml);
			System.out.println(envDocRet.xmlText(new XmlOptions().setSavePrettyPrint()));
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
