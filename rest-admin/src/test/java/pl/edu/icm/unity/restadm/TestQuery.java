/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.stream.Collectors;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.core5.http.HttpHost;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.attr.ImageType;
import pl.edu.icm.unity.attr.UnityImage;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttribute;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.ImageAttribute;
import pl.edu.icm.unity.stdext.attr.ImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.IntegerAttribute;
import pl.edu.icm.unity.stdext.attr.IntegerAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.EntityWithAttributes;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.VerifiableEmail;


public class TestQuery extends RESTAdminTestBase
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, TestQuery.class);
	
	private ObjectMapper m = new ObjectMapper();
	
	{
		m.enable(SerializationFeature.INDENT_OUTPUT);
	}
	
	@Test
	public void resolveOfEmailWithTagsReturnsEntity() throws Exception
	{
		idsMan.addEntity(new IdentityParam(EmailIdentity.ID, "a+zzz@ex.com"), "cr-pass", 
				EntityState.valid);
		
		HttpClient client = getClient();
		HttpHost host = new HttpHost("https", "localhost", 53456);

		HttpGet resolve = new HttpGet("/restadm/v1/resolve/email/a+foo@ex.com");
		String contents = client.execute(host, resolve, getClientContext(host), new BasicHttpClientResponseHandler());
		log.info("User's info:\n" + formatJson(contents));
	}	
	
	@Test
	public void testQuery() throws Exception
	{
		long e = createTestContents();
		
		HttpClient client = getClient();
		HttpHost host = new HttpHost("https", "localhost", 53456);

		HttpGet resolve = new HttpGet("/restadm/v1/resolve/userName/admin");
		String contents = client.execute(host, resolve, getClientContext(host), new BasicHttpClientResponseHandler());
		log.info("User's info:\n" + formatJson(contents));
		
		HttpGet getGroups = new HttpGet("/restadm/v1/entity/"+e+"/groups");
		contents = client.execute(host, getGroups, getClientContext(host), new BasicHttpClientResponseHandler());
		log.info("User's groups:\n" + contents);
		
		HttpGet getGroupContents = new HttpGet("/restadm/v1/group/%2Fexample%2Fsub");
		contents = client.execute(host, getGroupContents, getClientContext(host), new BasicHttpClientResponseHandler());
		log.info("Group's /example/sub contents:\n" + formatJson(contents));

		HttpGet getAttributes = new HttpGet("/restadm/v1/entity/" + e + "/attributes?group=%2Fexample");
		contents = client.execute(host, getAttributes, getClientContext(host), new BasicHttpClientResponseHandler());
		log.info("Attributes in /example:\n" + formatJson(contents));
	}
	
	@Test
	public void fullEntityIsReturned() throws Exception
	{
		long e = createTestContents();
		
		HttpGet getEntity = new HttpGet("/restadm/v1/entity/"+e);
		String contents = executeQuery(getEntity);
		log.info("User's info:\n" + formatJson(contents));
		Entity parsed = m.readValue(contents, Entity.class);
		assertThat(parsed.getId(), is(e));
	}
	
	@Test
	public void fullEntityWithAttributesAndGroupsIsReturned() throws Exception
	{
		long e = createTestContents();

		HttpGet getEntity = new HttpGet("/restadm/v1/entity/" + e + "/record");
		String contents = executeQuery(getEntity);
		log.info("User's info:\n" + formatJson(contents));

		EntityWithAttributes parsed = m.readValue(contents, EntityWithAttributes.class);
		assertThat(parsed.entity.getId(), is(e));
		assertThat(parsed.attributesInGroups.keySet().size(), is(2));
		assertThat(parsed.attributesInGroups.get("/").size(), is(2));
		assertThat(parsed.attributesInGroups.get("/").stream().map(a -> a.getName())
				.collect(Collectors.toSet()), hasItems("emailA", "sys:CredentialRequirements"));
		assertThat(parsed.attributesInGroups.get("/example").stream().map(a -> a.getName()).collect(
				Collectors.toSet()), hasItems("floatA", "emailA", "intA", "jpegA", "enumA", "stringA"));
		assertThat(parsed.groups.keySet(), hasItems("/", "/example", "/example/sub"));
	}
	
	@Test
	public void queryByPersistentIdWorks() throws Exception
	{
		long entityId = createTestContents();
		Entity entity = idsMan.getEntity(new EntityParam(entityId));
		Identity persistent = entity.getIdentities().stream().
			filter(i -> i.getTypeId().equals(PersistentIdentity.ID)).
			findFirst().
			get();
		
		HttpGet getGroups = new HttpGet("/restadm/v1/entity/"+persistent.getValue()+"/groups");
		String contents = executeQuery(getGroups);
		log.info("User's groups:\n" + contents);
	}

	@Test
	public void completeGroupContentsIsReturned() throws Exception
	{
		createTestContents();
		
		HttpGet getEntity = new HttpGet("/restadm/v1/group-members/example");
		String contents = executeQuery(getEntity);
		log.info("Group's /example contents:\n" + formatJson(contents));
		ArrayNode parsed = JsonUtil.parse(contents, ArrayNode.class);
		
		assertThat(parsed.size(), is(1));
		ObjectNode entityData = (ObjectNode) parsed.get(0);
		ArrayNode attributes = (ArrayNode) entityData.get("attributes");
		assertThat(attributes.size(), is(6));
	}
	
	protected long createTestContents() throws Exception
	{
		aTypeMan.addAttributeType(new AttributeType("stringA", StringAttributeSyntax.ID));
		aTypeMan.addAttributeType(new AttributeType("intA", IntegerAttributeSyntax.ID));
		aTypeMan.addAttributeType(new AttributeType("floatA", FloatingPointAttributeSyntax.ID));
		EnumAttributeSyntax enumSyntax = new EnumAttributeSyntax("V1", "V2");
		AttributeType enumAT = new AttributeType("enumA", EnumAttributeSyntax.ID);
		enumAT.setValueSyntaxConfiguration(enumSyntax.getSerializedConfiguration());
		aTypeMan.addAttributeType(enumAT);
		aTypeMan.addAttributeType(new AttributeType("jpegA", ImageAttributeSyntax.ID));
		aTypeMan.addAttributeType(new AttributeType("emailA", VerifiableEmailAttributeSyntax.ID));
		
		Group example = new Group("/example");
		AttributeStatement addAttr = new AttributeStatement("eattr contains 'emailA'", "/", ConflictResolution.skip, 
				"emailA", "eattr['emailA']");
		example.setAttributeStatements(new AttributeStatement[] {addAttr});
		groupsMan.addGroup(example);
		groupsMan.addGroup(new Group("/example/sub"));
		Identity id = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tested"), "cr-pass", 
				EntityState.valid);
		EntityParam e = new EntityParam(id);
		groupsMan.addMemberFromParent("/example", e);
		groupsMan.addMemberFromParent("/example/sub", e);
				
		attrsMan.createAttribute(e, StringAttribute.of("stringA", "/example", 
				"value"));
		attrsMan.createAttribute(e, IntegerAttribute.of("intA", "/example", 
				12));
		attrsMan.createAttribute(e, FloatingPointAttribute.of("floatA", "/example", 
				12.9));
		attrsMan.createAttribute(e, ImageAttribute.of("jpegA", "/example", 
				new UnityImage(new BufferedImage(100, 50, BufferedImage.TYPE_INT_ARGB), ImageType.JPG)));
		attrsMan.createAttribute(e, EnumAttribute.of("enumA", "/example", 
				"V1"));
		attrsMan.createAttribute(e, VerifiableEmailAttribute.of("emailA", "/", 
				new VerifiableEmail("some@example.com")));
		return id.getEntityId();
	}
	
	public String formatJson(String contents) throws JsonProcessingException, IOException
	{
		JsonNode n = m.readTree(contents);
		return m.writeValueAsString(n);
	}
}
