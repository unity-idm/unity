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
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.imunity.rest.api.types.basic.RestAttributeType;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.utils.JsonUtil;
import pl.edu.icm.unity.rest.TestRESTBase;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.ImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.IntegerAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;


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
		HttpGet resolve = new HttpGet("/restadm/v1/attributeTypes");
		String contents = client.execute(host, resolve, getClientContext(host), new BasicHttpClientResponseHandler());
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
		
		HttpPost addAT = new HttpPost("/restadm/v1/attributeType");
		
		Map<String, String> meta = new HashMap<>();
		meta.put(EntityNameMetadataProvider.NAME, "");
		
		RestAttributeType sAttributeType= RestAttributeType.builder()
				.withName("stringA")
				.withSyntaxId(StringAttributeSyntax.ID)
				.withMetadata(meta)
				.withMaxElements(1)
				.withMinElements(1)
				.build();
		
		addAT.setEntity(new StringEntity(JsonUtil.toJsonString(sAttributeType), ContentType.APPLICATION_JSON));
		try(ClassicHttpResponse response = client.executeOpen(host, addAT, getClientContext(host))){
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}
		assertNotNull(aTypeMan.getAttributeTypesAsMap().get("stringA"));
		
		sAttributeType= RestAttributeType.builder()
				.withName("stringA")
				.withSyntaxId(StringAttributeSyntax.ID)
				.withMetadata(new HashMap<>())
				.withMaxElements(10)
				.withMinElements(1)
				.build();
		
		HttpPut updateAT = new HttpPut("/restadm/v1/attributeType");
		updateAT.setEntity(new StringEntity(JsonUtil.toJsonString(sAttributeType), 
				ContentType.APPLICATION_JSON));
		try(ClassicHttpResponse response = client.executeOpen(host, updateAT, getClientContext(host))){
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}
		AttributeType updated = aTypeMan.getAttributeTypesAsMap().get("stringA");
		assertNotNull(updated);
		assertEquals(10, updated.getMaxElements());
		assertTrue(updated.getMetadata().isEmpty());

		
		HttpDelete remove = new HttpDelete("/restadm/v1/attributeType/stringA?withInstances=true");
		client.execute(host, remove, getClientContext(host), new BasicHttpClientResponseHandler());
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
