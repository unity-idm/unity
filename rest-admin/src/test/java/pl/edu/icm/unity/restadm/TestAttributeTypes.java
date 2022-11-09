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

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import eu.unicore.util.httpclient.HttpResponseHandler;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.rest.TestRESTBase;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.ImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.IntegerAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.types.basic.AttributeType;


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
		createUsernameUserWithRole("System Manager");
		super.deployEndpoint(RESTAdminEndpoint.NAME, 
				"restAdmin", "/restadm");
		createTestContents();
		
		HttpClient client = getClient();
		HttpHost host = new HttpHost("https", "localhost", 53456);
;		HttpClientContext localcontext = getClientContext(host);

		HttpGet resolve = new HttpGet("/restadm/v1/attributeTypes");
		ClassicHttpResponse response = client.execute(host, resolve, localcontext, HttpResponseHandler.INSTANCE);
		String contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getCode());
		System.out.println("Attribute types:\n" + formatJson(contents));
	}

	@Test
	public void testAddUpdateRemoveAT() throws Exception
	{
		setupPasswordAuthn();
		createUsernameUserWithRole("System Manager");
		super.deployEndpoint(RESTAdminEndpoint.NAME, 
				"restAdmin", "/restadm");
		
		HttpClient client = getClient();
		HttpHost host = new HttpHost("https", "localhost", 53456);
		HttpClientContext localcontext = getClientContext(host);

		
		HttpPost addAT = new HttpPost("/restadm/v1/attributeType");
		
		AttributeType sAttributeType = new AttributeType("stringA", StringAttributeSyntax.ID);
		Map<String, String> meta = new HashMap<>();
		meta.put(EntityNameMetadataProvider.NAME, "");
		sAttributeType.setMetadata(meta);
		sAttributeType.setMaxElements(1);
		sAttributeType.setMinElements(1);
		
		addAT.setEntity(new StringEntity(JsonUtil.toJsonString(sAttributeType), ContentType.APPLICATION_JSON));
		ClassicHttpResponse response = client.execute(host, addAT, localcontext, HttpResponseHandler.INSTANCE);
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		assertNull(response.getEntity());
		assertNotNull(aTypeMan.getAttributeTypesAsMap().get("stringA"));

		sAttributeType.setMetadata(new HashMap<>());
		sAttributeType.setMaxElements(10);
		HttpPut updateAT = new HttpPut("/restadm/v1/attributeType");
		updateAT.setEntity(new StringEntity(JsonUtil.toJsonString(sAttributeType), 
				ContentType.APPLICATION_JSON));
		response = client.execute(host, updateAT, localcontext, HttpResponseHandler.INSTANCE);
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		assertNull(response.getEntity());
		AttributeType updated = aTypeMan.getAttributeTypesAsMap().get("stringA");
		assertNotNull(updated);
		assertEquals(10, updated.getMaxElements());
		assertTrue(updated.getMetadata().isEmpty());

		
		HttpDelete remove = new HttpDelete("/restadm/v1/attributeType/stringA?withInstances=true");
		client.execute(host, remove, localcontext, HttpResponseHandler.INSTANCE);
		assertNull(aTypeMan.getAttributeTypesAsMap().get("stringA"));
	}
	
	protected void createTestContents() throws Exception
	{
		AttributeType sAttributeType = new AttributeType("stringA", StringAttributeSyntax.ID);
		Map<String, String> meta = new HashMap<>();
		meta.put(EntityNameMetadataProvider.NAME, "");
		sAttributeType.setMetadata(meta);
		sAttributeType.setMaxElements(1);
		sAttributeType.setMinElements(1);
		aTypeMan.addAttributeType(sAttributeType);
		aTypeMan.addAttributeType(new AttributeType("intA", IntegerAttributeSyntax.ID));
		aTypeMan.addAttributeType(new AttributeType("floatA", FloatingPointAttributeSyntax.ID));
		EnumAttributeSyntax enumSyntax = new EnumAttributeSyntax("V1", "V2");
		AttributeType enumAT = new AttributeType("enumA", EnumAttributeSyntax.ID);
		enumAT.setValueSyntaxConfiguration(enumSyntax.getSerializedConfiguration());
		aTypeMan.addAttributeType(enumAT);
		aTypeMan.addAttributeType(new AttributeType("jpegA", ImageAttributeSyntax.ID));
		aTypeMan.addAttributeType(new AttributeType("emailA", VerifiableEmailAttributeSyntax.ID));
	}
	
	public String formatJson(String contents) throws JsonProcessingException, IOException
	{
		JsonNode n = m.readTree(contents);
		return m.writeValueAsString(n);
	}
}
