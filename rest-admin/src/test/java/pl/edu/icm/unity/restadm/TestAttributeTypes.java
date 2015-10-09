/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import pl.edu.icm.unity.json.AttributeTypeSerializer;
import pl.edu.icm.unity.rest.TestRESTBase;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.IntegerAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.types.basic.AttributeType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class TestAttributeTypes extends TestRESTBase
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
		super.deployEndpoint(RESTAdminEndpointFactory.NAME, 
				"restAdmin", "/restadm");
		createTestContents();
		
		HttpClient client = getClient();
		HttpHost host = new HttpHost("localhost", 53456, "https");
		HttpContext localcontext = getClientContext(client, host);

		HttpGet resolve = new HttpGet("/restadm/v1/attributeTypes");
		HttpResponse response = client.execute(host, resolve, localcontext);
		String contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		System.out.println("Attribute types:\n" + formatJson(contents));
	}

	@Test
	public void testAddUpdateRemoveAT() throws Exception
	{
		setupPasswordAuthn();
		createUsernameUser("System Manager");
		super.deployEndpoint(RESTAdminEndpointFactory.NAME, 
				"restAdmin", "/restadm");
		
		HttpClient client = getClient();
		HttpHost host = new HttpHost("localhost", 53456, "https");
		HttpContext localcontext = getClientContext(client, host);

		
		HttpPost addAT = new HttpPost("/restadm/v1/attributeType");
		
		AttributeType sAttributeType = new AttributeType("stringA", new StringAttributeSyntax());
		Map<String, String> meta = new HashMap<>();
		meta.put(EntityNameMetadataProvider.NAME, "");
		sAttributeType.setMetadata(meta);
		sAttributeType.setMaxElements(1);
		sAttributeType.setMinElements(1);
		
		AttributeTypeSerializer as = new AttributeTypeSerializer();
		ObjectNode jsonNodeFull = as.toJsonNodeFull(sAttributeType);
		addAT.setEntity(new StringEntity(new ObjectMapper().writeValueAsString(jsonNodeFull), 
				ContentType.APPLICATION_JSON));
		HttpResponse response = client.execute(host, addAT, localcontext);
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
		assertNull(response.getEntity());
		assertNotNull(attrsMan.getAttributeTypesAsMap().get("stringA"));

		sAttributeType.setMetadata(new HashMap<>());
		sAttributeType.setMaxElements(10);
		jsonNodeFull = as.toJsonNodeFull(sAttributeType);
		HttpPut updateAT = new HttpPut("/restadm/v1/attributeType");
		updateAT.setEntity(new StringEntity(new ObjectMapper().writeValueAsString(jsonNodeFull), 
				ContentType.APPLICATION_JSON));
		response = client.execute(host, updateAT, localcontext);
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
		assertNull(response.getEntity());
		AttributeType updated = attrsMan.getAttributeTypesAsMap().get("stringA");
		assertNotNull(updated);
		assertEquals(10, updated.getMaxElements());
		assertTrue(updated.getMetadata().isEmpty());

		
		HttpDelete remove = new HttpDelete("/restadm/v1/attributeType/stringA?withInstances=true");
		client.execute(host, remove, localcontext);
		assertNull(attrsMan.getAttributeTypesAsMap().get("stringA"));
	}
	
	protected void createTestContents() throws Exception
	{
		AttributeType sAttributeType = new AttributeType("stringA", new StringAttributeSyntax());
		Map<String, String> meta = new HashMap<>();
		meta.put(EntityNameMetadataProvider.NAME, "");
		sAttributeType.setMetadata(meta);
		sAttributeType.setMaxElements(1);
		sAttributeType.setMinElements(1);
		attrsMan.addAttributeType(sAttributeType);
		attrsMan.addAttributeType(new AttributeType("intA", new IntegerAttributeSyntax()));
		attrsMan.addAttributeType(new AttributeType("floatA", new FloatingPointAttributeSyntax()));
		attrsMan.addAttributeType(new AttributeType("enumA", new EnumAttributeSyntax("V1", "V2")));
		attrsMan.addAttributeType(new AttributeType("jpegA", new JpegImageAttributeSyntax()));
		attrsMan.addAttributeType(new AttributeType("emailA", new VerifiableEmailAttributeSyntax()));
	}
	
	public String formatJson(String contents) throws JsonProcessingException, IOException
	{
		JsonNode n = m.readTree(contents);
		return m.writeValueAsString(n);
	}
}
