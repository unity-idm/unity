/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import pl.edu.icm.unity.exceptions.EngineException;
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
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityScheduledOperation;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.basic.VerifiableEmail;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;


public class TestWrite extends RESTAdminTestBase
{
	@Test
	public void setRemoveAttribute() throws Exception
	{
		Identity identity = idsMan.addEntity(new IdentityParam("userName", "userC"), 
				"cr-pass", EntityState.valid, false);
		long entityId = identity.getEntityId();
		aTypeMan.addAttributeType(new AttributeType("stringA", StringAttributeSyntax.ID));
		aTypeMan.addAttributeType(new AttributeType("intA", IntegerAttributeSyntax.ID));
		aTypeMan.addAttributeType(new AttributeType("floatA", FloatingPointAttributeSyntax.ID));
		EnumAttributeSyntax enumSyntax = new EnumAttributeSyntax("V1", "V2");
		AttributeType enumAT = new AttributeType("enumA", EnumAttributeSyntax.ID);
		enumAT.setValueSyntaxConfiguration(enumSyntax.getSerializedConfiguration());
		aTypeMan.addAttributeType(enumAT);
		AttributeType email =  new AttributeType("emailA", VerifiableEmailAttributeSyntax.ID);
		email.setMaxElements(2);
		aTypeMan.addAttributeType(email);
		aTypeMan.addAttributeType(new AttributeType("jpegA", JpegImageAttributeSyntax.ID));
		
		setSingleAttribute(entityId, StringAttribute.of("stringA", "/", "value1"));

		setSingleAttribute(entityId, IntegerAttribute.of("intA", "/", 123));

		setSingleAttribute(entityId, FloatingPointAttribute.of("floatA", "/", 123.1));

		setSingleAttribute(entityId, EnumAttribute.of("enumA", "/", "V1"));

		setSingleAttribute(entityId, VerifiableEmailAttribute.of(
				"emailA", "/", new VerifiableEmail("some@example.com"),
				new VerifiableEmail("some2@example.com", new ConfirmationInfo(true))));
		
		BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
		setSingleAttribute(entityId, JpegImageAttribute.of(
				"jpegA", "/", image));

		HttpDelete removeAttribute = new HttpDelete("/restadm/v1/entity/" + entityId + "/attribute/stringA");
		HttpResponse response = client.execute(host, removeAttribute, localcontext);
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
		assertEquals(0, attrsMan.getAttributes(new EntityParam(entityId), "/", "stringA").size());
	}

	@Test
	public void setAttributes() throws Exception
	{
		Identity identity = idsMan.addEntity(new IdentityParam("userName", "userC"), 
				"cr-pass", EntityState.valid, false);
		long entityId = identity.getEntityId();
		aTypeMan.addAttributeType(new AttributeType("stringA", StringAttributeSyntax.ID));
		aTypeMan.addAttributeType(new AttributeType("intA", IntegerAttributeSyntax.ID));
		aTypeMan.addAttributeType(new AttributeType("floatA", FloatingPointAttributeSyntax.ID));
		EnumAttributeSyntax enumSyntax = new EnumAttributeSyntax("V1", "V2");
		AttributeType enumAT = new AttributeType("enumA", EnumAttributeSyntax.ID);
		enumAT.setValueSyntaxConfiguration(enumSyntax.getSerializedConfiguration());
		aTypeMan.addAttributeType(enumAT);
		AttributeType email =  new AttributeType("emailA", VerifiableEmailAttributeSyntax.ID);
		email.setMaxElements(2);
		aTypeMan.addAttributeType(email);
		aTypeMan.addAttributeType(new AttributeType("jpegA", JpegImageAttributeSyntax.ID));
		

		
		HttpPut setAttribute = new HttpPut("/restadm/v1/entity/" + entityId + "/attributes");
		
		Attribute a1 = StringAttribute.of("stringA", "/", "value1");
		Attribute a2 = EnumAttribute.of("enumA", "/", "V1");
		
		List<Attribute> params = Lists.newArrayList(a1, a2);
		setAttribute.setEntity(new StringEntity(m.writeValueAsString(params), ContentType.APPLICATION_JSON));
		
		HttpResponse response = client.execute(host, setAttribute, localcontext);
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
		assertEquals(1, attrsMan.getAttributes(new EntityParam(entityId), a1.getGroupPath(), 
				a1.getName()).size());
		assertEquals(a1.getValues().size(), attrsMan.getAttributes(new EntityParam(entityId), a1.getGroupPath(), 
				a1.getName()).iterator().next().getValues().size());
		System.out.println("Bulk set attributes:\n" + m.writeValueAsString(params));
	}
	
	private void setSingleAttribute(long entityId, Attribute a) throws EngineException, 
		UnsupportedCharsetException, ClientProtocolException, IOException
	{
		HttpPut setAttribute = new HttpPut("/restadm/v1/entity/" + entityId + "/attribute");
		setAttribute.setEntity(new StringEntity(m.writeValueAsString(a), ContentType.APPLICATION_JSON));
		HttpResponse response = client.execute(host, setAttribute, localcontext);
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
		assertEquals(1, attrsMan.getAttributes(new EntityParam(entityId), a.getGroupPath(), 
				a.getName()).size());
		assertEquals(a.getValues().size(), attrsMan.getAttributes(new EntityParam(entityId), a.getGroupPath(), 
				a.getName()).iterator().next().getValues().size());
		System.out.println("Set attribute:\n" + m.writeValueAsString(a));
	}

	@Test
	public void addEmailIdentityPreservesMetadata() throws Exception
	{
		HttpPost addEntity = new HttpPost("/restadm/v1/entity/identity/email/"
				+ "user%40example.com%5BCONFIRMED%5D?credentialRequirement=cr-pass");
		HttpResponse response = client.execute(host, addEntity, localcontext);
		String contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		ObjectNode root = (ObjectNode) m.readTree(contents);
		long entityId = root.get("entityId").asLong();
		Entity entity = idsMan.getEntity(new EntityParam(entityId));
		Identity emailId = getIdentityByType(entity.getIdentities(), EmailIdentity.ID);
		assertTrue(emailId.isConfirmed());
		assertEquals("user@example.com", emailId.getValue());
		
		HttpGet resolve = new HttpGet("/restadm/v1/resolve/email/user%40example.com");
		response = client.execute(host, resolve, localcontext);
		contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		JsonNode n = m.readTree(contents);
		System.out.println("User's info:\n" + m.writeValueAsString(n));
	}
	
	@Test
	public void addRemoveIdentityAndEntity() throws Exception
	{
		HttpPost addEntity = new HttpPost("/restadm/v1/entity/identity/userName/userA?credentialRequirement=cr-pass");
		HttpResponse response = client.execute(host, addEntity, localcontext);
		String contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		ObjectNode root = (ObjectNode) m.readTree(contents);
		long entityId = root.get("entityId").asLong();
		assertTrue(checkIdentity("userA"));
		System.out.println("Added entity:\n" + contents);
		

		HttpPost addIdentity = new HttpPost("/restadm/v1/entity/" + entityId + "/identity/userName/userB");
		response = client.execute(host, addIdentity, localcontext);
		assertNull(response.getEntity());
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
		assertTrue(checkIdentity("userB"));
		System.out.println("Added identity");

		
		HttpDelete deleteIdentity = new HttpDelete("/restadm/v1/entity/identity/userName/userB");
		response = client.execute(host, deleteIdentity, localcontext);
		assertNull(response.getEntity());
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
		assertFalse(checkIdentity("userB"));
		System.out.println("Removed identity");
		

		HttpPut setCredentialAdm = new HttpPut("/restadm/v1/entity/" + entityId + "/credential-adm/credential1");
		PasswordToken pass = new PasswordToken("newpass");
		pass.setQuestion(1);
		pass.setAnswer("Some answer");
	        setCredentialAdm.setEntity(new StringEntity(pass.toJson(), ContentType.APPLICATION_JSON));
		response = client.execute(host, setCredentialAdm, localcontext);
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
		assertNull(response.getEntity());
		System.out.println("Set entity credential (adm) with new secret:\n" + pass.toJson());

		HttpPut setCredential = new HttpPut("/restadm/v1/entity/" + entityId + "/credential/credential1");
		PasswordToken pass2 = new PasswordToken("newpass2");
		pass2.setQuestion(0);
		pass2.setAnswer("Some answer2");
		ArrayNode arrayNode = m.createArrayNode();
		arrayNode.add(pass2.toJson());
		arrayNode.add(pass.toJson());
	        setCredential.setEntity(new StringEntity(m.writeValueAsString(arrayNode), 
	        		ContentType.APPLICATION_JSON));
		response = client.execute(host, setCredential, localcontext);
		assertNull(response.getEntity());
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
		System.out.println("Set entity credential (user) with new secret:\n" + m.writeValueAsString(arrayNode));
		
		
		groupsMan.addGroup(new Group("/example"));

		HttpPost addMember = new HttpPost("/restadm/v1/group/%2Fexample/entity/" + entityId);
		response = client.execute(host, addMember, localcontext);
		assertNull(response.getEntity());
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
		assertTrue(idsMan.getGroups(new EntityParam(entityId)).containsKey("/example"));
		System.out.println("Added entity to group");

		
		HttpDelete removeMember = new HttpDelete("/restadm/v1/group/%2Fexample/entity/" + entityId);
		response = client.execute(host, removeMember, localcontext);
		assertNull(response.getEntity());
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
		assertFalse(idsMan.getGroups(new EntityParam(entityId)).containsKey("/example"));
		System.out.println("Removed membership");

		
		HttpDelete deleteEntity = new HttpDelete("/restadm/v1/entity/" + entityId);
		response = client.execute(host, deleteEntity, localcontext);
		assertNull(response.getEntity());
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
		System.out.println("Removed entity");
		
		try
		{
			idsMan.getEntity(new EntityParam(entityId));
			fail("Entity not removed");
		} catch (IllegalArgumentException e)
		{
			//OK
		}
	}
	
	private boolean checkIdentity(String name) throws EngineException
	{
		try
		{
			idsMan.getEntity(new EntityParam(new IdentityTaV("userName", name)));
			return true;
		} catch (IllegalArgumentException e)
		{
			return false;
		}
	}
	
	@Test
	public void scheduleOperationByAdminWorks() throws Exception
	{
		HttpPost addEntity = new HttpPost("/restadm/v1/entity/identity/userName/userA?credentialRequirement=cr-pass");
		HttpResponse response = client.execute(host, addEntity, localcontext);
		String contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		ObjectNode root = (ObjectNode) m.readTree(contents);
		long entityId = root.get("entityId").asLong();
		assertTrue(checkIdentity("userA"));
		System.out.println("Added entity:\n" + contents);
		
		long time = System.currentTimeMillis() + 20000;
		HttpPut scheduleRemoval = new HttpPut("/restadm/v1/entity/" + entityId + "/admin-schedule?when=" + 
				time + "&operation=REMOVE");
		response = client.execute(host, scheduleRemoval, localcontext);
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
		
		Entity entity = idsMan.getEntity(new EntityParam(entityId));
		assertEquals(new Date(time), entity.getEntityInformation().getScheduledOperationTime());
		assertEquals(EntityScheduledOperation.REMOVE, entity.getEntityInformation().getScheduledOperation());
		
		HttpPut scheduleRemovalWrong = new HttpPut("/restadm/v1/entity/" + entityId + "/admin-schedule?when=" + 
				time + "&operation=WRONG");
		response = client.execute(host, scheduleRemovalWrong, localcontext);
		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatusLine().getStatusCode());
	}

	@Test
	public void scheduleRemovalByUserWorks() throws Exception
	{
		HttpPost addEntity = new HttpPost("/restadm/v1/entity/identity/userName/userA?credentialRequirement=cr-pass");
		HttpResponse response = client.execute(host, addEntity, localcontext);
		String contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		ObjectNode root = (ObjectNode) m.readTree(contents);
		long entityId = root.get("entityId").asLong();
		assertTrue(checkIdentity("userA"));
		System.out.println("Added entity:\n" + contents);
		
		long time = System.currentTimeMillis() + 20000;
		HttpPut scheduleRemoval = new HttpPut("/restadm/v1/entity/" + entityId + "/removal-schedule?when=" + 
				time);
		response = client.execute(host, scheduleRemoval, localcontext);
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
		
		Entity entity = idsMan.getEntity(new EntityParam(entityId));
		assertEquals(new Date(time), entity.getEntityInformation().getRemovalByUserTime());
		assertEquals(EntityState.onlyLoginPermitted, entity.getEntityInformation().getState());
	}
	
	@Test
	public void canTriggerScriptInvocation() throws Exception
	{
		HttpPost trigger = new HttpPost("/restadm/v1/triggerEvent/test_event");
		trigger.setEntity(new StringEntity("user-triggered", ContentType.APPLICATION_JSON));
		
		HttpResponse response = client.execute(host, trigger, localcontext);
		assertThat(response.getStatusLine().getStatusCode(), is(Status.NO_CONTENT.getStatusCode()));
		
		Entity entity = idsMan.getEntity(new EntityParam(new IdentityTaV(UsernameIdentity.ID, "user-triggered")));
		assertThat(entity, is(notNullValue()));
	}
}
