/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;

import pl.edu.icm.unity.rest.TestRESTBase;
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
import pl.edu.icm.unity.stdext.attr.VerifiableEmail;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.AttributeRepresentation;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;


public class TestQuery extends TestRESTBase
{
	private ObjectMapper m = new ObjectMapper();
	
	{
		m.enable(SerializationFeature.INDENT_OUTPUT);
	}
	
	@Before
	public void init() throws Exception
	{
		setupPasswordAuthn();
		createUsernameUser("System Manager");
		super.deployEndpoint(RESTAdminEndpointFactory.NAME, 
				"restAdmin", "/restadm");
	}
	
	@Test
	public void resolveOfEmailWithTagsReturnsEntity() throws Exception
	{
		idsMan.addEntity(new IdentityParam(EmailIdentity.ID, "a+zzz@ex.com"), "cr-pass", 
				EntityState.valid, false);
		
		HttpClient client = getClient();
		HttpHost host = new HttpHost("localhost", 53456, "https");
		HttpContext localcontext = getClientContext(client, host);

		HttpGet resolve = new HttpGet("/restadm/v1/resolve/email/a+foo@ex.com");
		HttpResponse response = client.execute(host, resolve, localcontext);
		String contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		System.out.println("User's info:\n" + formatJson(contents));
	}	
	
	@Test
	public void testQuery() throws Exception
	{
		long e = createTestContents();
		
		HttpClient client = getClient();
		HttpHost host = new HttpHost("localhost", 53456, "https");
		HttpContext localcontext = getClientContext(client, host);

		HttpGet resolve = new HttpGet("/restadm/v1/resolve/userName/admin");
		HttpResponse response = client.execute(host, resolve, localcontext);
		String contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		System.out.println("User's info:\n" + formatJson(contents));
		
		HttpGet getGroups = new HttpGet("/restadm/v1/entity/"+e+"/groups");
		response = client.execute(host, getGroups, localcontext);
		contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		System.out.println("User's groups:\n" + contents);
		
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
	
	@Test
	public void fullEntityIsReturned() throws Exception
	{
		long e = createTestContents();
		
		HttpGet getEntity = new HttpGet("/restadm/v1/entity/"+e);
		HttpResponse response = executeQuery(getEntity);
		
		String contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		System.out.println("User's info:\n" + formatJson(contents));
		Entity parsed = m.readValue(contents, Entity.class);
		assertThat(parsed.getId(), is(e));
	}
	
	@Test
	public void localAttributesAreReturned() throws Exception
	{
		long e = createTestContents();
		
		HttpGet getAttributes = new HttpGet("/restadm/v1/entity/" + e + "/attributes");
		HttpResponse response = executeQuery(getAttributes);
		
		String contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		List<AttributeRepresentation> parsed = m.readValue(contents, 
				new TypeReference<List<AttributeRepresentation>>(){});
		assertThat(parsed.size(), is(1));
		assertThat(parsed.get(0).getVisibility(), is(AttributeVisibility.local));
		System.out.println("Attributes in /:\n" + formatJson(contents));
	}
	
	private HttpResponse executeQuery(HttpRequest request) throws Exception
	{
		HttpClient client = getClient();
		HttpHost host = new HttpHost("localhost", 53456, "https");
		HttpContext localcontext = getClientContext(client, host);
		return client.execute(host, request, localcontext);
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
		attrsMan.addAttributeType(new AttributeType("emailA", new VerifiableEmailAttributeSyntax()));
		
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
		attrsMan.setAttribute(e, new VerifiableEmailAttribute("emailA", "/example", 
				AttributeVisibility.full, new VerifiableEmail("some@example.com")), false);
		return id.getEntityId();
	}
	
	public String formatJson(String contents) throws JsonProcessingException, IOException
	{
		JsonNode n = m.readTree(contents);
		return m.writeValueAsString(n);
	}
}
