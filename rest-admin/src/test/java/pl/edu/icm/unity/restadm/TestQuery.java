/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import eu.emi.security.authn.x509.impl.KeystoreCertChainValidator;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpUtils;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttribute;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.IntegerAttribute;
import pl.edu.icm.unity.stdext.attr.IntegerAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.JpegImageAttribute;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;

public class TestQuery extends DBIntegrationTestBase
{
	private ObjectMapper m = new ObjectMapper();
	
	{
		m.enable(SerializationFeature.INDENT_OUTPUT);
	}
	
	@Test
	public void testQuery() throws Exception
	{

		setupPasswordAuthn();
		createUsernameUser("System Manager");
		deployEndpoint();
		long e = createTestContents();
		
		DefaultHttpClient client = getClient();
		HttpHost host = new HttpHost("localhost", 53456, "https");
		BasicHttpContext localcontext = getClientContext(client, host);

		HttpGet getGroups = new HttpGet("/restadm/v1/entity/"+e+"/groups");
		HttpResponse response = client.execute(host, getGroups, localcontext);
		String contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		System.out.println("User's groups:\n" + contents);
		
		HttpGet getEntity = new HttpGet("/restadm/v1/entity/"+e);
		response = client.execute(host, getEntity, localcontext);
		contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		System.out.println("User's info:\n" + formatJson(contents));
		
		HttpGet getGroupContents = new HttpGet("/restadm/v1/group/example%2Fsub");
		response = client.execute(host, getGroupContents, localcontext);
		contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		System.out.println("Group's /example/sub contents:\n" + formatJson(contents));

		HttpGet getAttributes = new HttpGet("/restadm/v1/entity/" + e + "/attributes?group=%2Fexample");
		response = client.execute(host, getAttributes, localcontext);
		contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		System.out.println("Attributes in /example:\n" + formatJson(contents));
		
	}
	
	protected long createTestContents() throws Exception
	{
		groupsMan.addGroup(new Group("/example"));
		groupsMan.addGroup(new Group("/example/sub"));
		Identity id = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tested"), "cr-pass", 
				EntityState.valid, false);
		EntityParam e = new EntityParam(id);
		groupsMan.addMemberFromParent("/example", e);
		groupsMan.addMemberFromParent("/example/sub", e);
		
		attrsMan.addAttributeType(new AttributeType("stringA", new StringAttributeSyntax()));
		attrsMan.addAttributeType(new AttributeType("intA", new IntegerAttributeSyntax()));
		attrsMan.addAttributeType(new AttributeType("floatA", new FloatingPointAttributeSyntax()));
		attrsMan.addAttributeType(new AttributeType("enumA", new EnumAttributeSyntax("V1", "V2")));
		attrsMan.addAttributeType(new AttributeType("jpegA", new JpegImageAttributeSyntax()));
		
		attrsMan.setAttribute(e, new StringAttribute("stringA", "/example", 
				AttributeVisibility.full, "value"), false);
		attrsMan.setAttribute(e, new IntegerAttribute("intA", "/example", 
				AttributeVisibility.full, 12), false);
		attrsMan.setAttribute(e, new FloatingPointAttribute("floatA", "/example", 
				AttributeVisibility.full, 12.9), false);
		attrsMan.setAttribute(e, new JpegImageAttribute("jpegA", "/example", AttributeVisibility.full, 
				new BufferedImage(100, 50, BufferedImage.TYPE_INT_ARGB)), false);
		attrsMan.setAttribute(e, new EnumAttribute("enumA", "/example", 
				AttributeVisibility.full, "V1"), false);
		return id.getEntityId();
	}
	
	protected void deployEndpoint() throws Exception
	{
		AuthenticationRealm realm = new AuthenticationRealm("testr", "", 
				10, 100, -1, 600);
		realmsMan.addRealm(realm);
		
		List<AuthenticatorSet> authnCfg = new ArrayList<AuthenticatorSet>();
		authnCfg.add(new AuthenticatorSet(Collections.singleton("ApassREST")));
		endpointMan.deploy(RESTAdminEndpointFactory.NAME, 
				"restAdmin", "/restadm", "desc", authnCfg, "", realm.getName());
		List<EndpointDescription> endpoints = endpointMan.getEndpoints();
		assertEquals(1, endpoints.size());

		httpServer.start();
	}
	
	protected BasicHttpContext getClientContext(DefaultHttpClient client, HttpHost host)
	{
		client.getCredentialsProvider().setCredentials(
				new AuthScope(host.getHostName(), host.getPort()),
				new UsernamePasswordCredentials("user1", "mockPassword1"));
		AuthCache authCache = new BasicAuthCache();
		BasicScheme basicAuth = new BasicScheme();
		authCache.put(host, basicAuth);
		BasicHttpContext localcontext = new BasicHttpContext();
		localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);
		return localcontext;
	}
	
	protected DefaultHttpClient getClient() throws Exception
	{
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setValidator(new KeystoreCertChainValidator("src/test/resources/demoTruststore.jks", 
				"unicore".toCharArray(), "JKS", -1));
		clientCfg.setSslEnabled(true);
		clientCfg.setSslAuthn(false);
		return (DefaultHttpClient) HttpUtils.createClient("https://localhost:53456", clientCfg);
	}
	
	@Override
	protected void setupPasswordAuthn() throws Exception
	{
		super.setupPasswordAuthn();
		authnMan.createAuthenticator("ApassREST", "password with rest-httpbasic", 
				null, "", "credential1");
	}
	
	public String formatJson(String contents) throws JsonProcessingException, IOException
	{
		JsonNode n = m.readTree(contents);
		return m.writeValueAsString(n);
	}
}
