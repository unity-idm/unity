/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.attr.ImageType;
import pl.edu.icm.unity.base.attr.UnityImage;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityScheduledOperation;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.entity.Identity;
import pl.edu.icm.unity.base.entity.IdentityParam;
import pl.edu.icm.unity.base.entity.IdentityTaV;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.verifiable.VerifiableEmail;
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
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;


public class TestWrite extends RESTAdminTestBase
{
	@Test
	public void setRemoveAttribute() throws Exception
	{
		Identity identity = idsMan.addEntity(new IdentityParam("userName", "userC"), 
				"cr-pass", EntityState.valid);
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
		aTypeMan.addAttributeType(new AttributeType("jpegA", ImageAttributeSyntax.ID));
		
		setSingleAttribute(entityId, StringAttribute.of("stringA", "/", "value1"));

		setSingleAttribute(entityId, IntegerAttribute.of("intA", "/", 123));

		setSingleAttribute(entityId, FloatingPointAttribute.of("floatA", "/", 123.1));

		setSingleAttribute(entityId, EnumAttribute.of("enumA", "/", "V1"));

		setSingleAttribute(entityId, VerifiableEmailAttribute.of(
				"emailA", "/", new VerifiableEmail("some@example.com"),
				new VerifiableEmail("some2@example.com", new ConfirmationInfo(true))));
		
		BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
		setSingleAttribute(entityId,ImageAttribute.of("jpegA", "/", new UnityImage(image, ImageType.JPG)));

		HttpDelete removeAttribute = new HttpDelete("/restadm/v1/entity/" + entityId + "/attribute/stringA");
		try(ClassicHttpResponse response = client.executeOpen(host, removeAttribute, getClientContext(host))){
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}
		assertEquals(0, attrsMan.getAttributes(new EntityParam(entityId), "/", "stringA").size());
	}

	@Test
	public void setAttributes() throws Exception
	{
		Identity identity = idsMan.addEntity(new IdentityParam("userName", "userC"), 
				"cr-pass", EntityState.valid);
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
		aTypeMan.addAttributeType(new AttributeType("jpegA", ImageAttributeSyntax.ID));
		

		
		HttpPut setAttribute = new HttpPut("/restadm/v1/entity/" + entityId + "/attributes");
		
		Attribute a1 = StringAttribute.of("stringA", "/", "value1");
		Attribute a2 = EnumAttribute.of("enumA", "/", "V1");
		
		List<Attribute> params = Lists.newArrayList(a1, a2);
		setAttribute.setEntity(new StringEntity(m.writeValueAsString(params), ContentType.APPLICATION_JSON));
		
		try(ClassicHttpResponse response = client.executeOpen(host, setAttribute, getClientContext(host))){
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}
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
		try(ClassicHttpResponse response = client.executeOpen(host, setAttribute, getClientContext(host))){
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}
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
		String contents = executeQuery(addEntity);
		ObjectNode root = (ObjectNode) m.readTree(contents);
		long entityId = root.get("entityId").asLong();
		Entity entity = idsMan.getEntity(new EntityParam(entityId));
		Identity emailId = getIdentityByType(entity.getIdentities(), EmailIdentity.ID);
		assertTrue(emailId.isConfirmed());
		assertEquals("user@example.com", emailId.getValue());
		
		HttpGet resolve = new HttpGet("/restadm/v1/resolve/email/user%40example.com");
		contents = executeQuery(resolve);
		JsonNode n = m.readTree(contents);
		System.out.println("User's info:\n" + m.writeValueAsString(n));
	}
	
	@Test
	public void addRemoveIdentityAndEntity() throws Exception
	{
		HttpPost addEntity = new HttpPost("/restadm/v1/entity/identity/userName/userA?credentialRequirement=cr-pass");
		String contents = executeQuery(addEntity);
		ObjectNode root = (ObjectNode) m.readTree(contents);
		long entityId = root.get("entityId").asLong();
		assertTrue(checkIdentity("userA"));
		System.out.println("Added entity:\n" + contents);
		
		HttpPost addIdentity = new HttpPost("/restadm/v1/entity/" + entityId + "/identity/userName/userB");
		try(ClassicHttpResponse response = client.executeOpen(host, addIdentity, getClientContext(host))){
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}
		assertTrue(checkIdentity("userB"));
		System.out.println("Added identity");

		
		HttpDelete deleteIdentity = new HttpDelete("/restadm/v1/entity/identity/userName/userB");
		try(ClassicHttpResponse response = client.executeOpen(host, deleteIdentity, getClientContext(host))){
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}
		assertFalse(checkIdentity("userB"));
		System.out.println("Removed identity");
		

		HttpPut setCredentialAdm = new HttpPut("/restadm/v1/entity/" + entityId + "/credential-adm/credential1");
		PasswordToken pass = new PasswordToken("newpass");
		pass.setQuestion(1);
		pass.setAnswer("Some answer");
	        setCredentialAdm.setEntity(new StringEntity(pass.toJson(), ContentType.APPLICATION_JSON));
		try(ClassicHttpResponse response = client.executeOpen(host, setCredentialAdm, getClientContext(host))){
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}
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
	    try(ClassicHttpResponse response = client.executeOpen(host, setCredential, getClientContext(host))){
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}
		System.out.println("Set entity credential (user) with new secret:\n" + m.writeValueAsString(arrayNode));
		
		
		groupsMan.addGroup(new Group("/example"));

		HttpPost addMember = new HttpPost("/restadm/v1/group/%2Fexample/entity/" + entityId);
		try(ClassicHttpResponse response = client.executeOpen(host, addMember, getClientContext(host))){
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}
		assertTrue(idsMan.getGroups(new EntityParam(entityId)).containsKey("/example"));
		System.out.println("Added entity to group");

		
		HttpDelete removeMember = new HttpDelete("/restadm/v1/group/%2Fexample/entity/" + entityId);
		try(ClassicHttpResponse response = client.executeOpen(host, removeMember, getClientContext(host))){
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}
		assertFalse(idsMan.getGroups(new EntityParam(entityId)).containsKey("/example"));
		System.out.println("Removed membership");

		
		HttpDelete deleteEntity = new HttpDelete("/restadm/v1/entity/" + entityId);
		try(ClassicHttpResponse response = client.executeOpen(host, deleteEntity, getClientContext(host))){
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}
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
		String contents = executeQuery(addEntity);
		ObjectNode root = (ObjectNode) m.readTree(contents);
		long entityId = root.get("entityId").asLong();
		assertTrue(checkIdentity("userA"));
		System.out.println("Added entity:\n" + contents);
		
		long time = System.currentTimeMillis() + 20000;
		HttpPut scheduleRemoval = new HttpPut("/restadm/v1/entity/" + entityId + "/admin-schedule?when=" + 
				time + "&operation=REMOVE");
		try(ClassicHttpResponse response = client.executeOpen(host, scheduleRemoval, getClientContext(host))){
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}
		
		Entity entity = idsMan.getEntity(new EntityParam(entityId));
		assertEquals(new Date(time), entity.getEntityInformation().getScheduledOperationTime());
		assertEquals(EntityScheduledOperation.REMOVE, entity.getEntityInformation().getScheduledOperation());
		
		HttpPut scheduleRemovalWrong = new HttpPut("/restadm/v1/entity/" + entityId + "/admin-schedule?when=" + 
				time + "&operation=WRONG");
		try(ClassicHttpResponse response = client.executeOpen(host, scheduleRemovalWrong, getClientContext(host))){
			assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getCode());
		}
	}

	@Test
	public void scheduleRemovalByUserWorks() throws Exception
	{
		HttpPost addEntity = new HttpPost("/restadm/v1/entity/identity/userName/userA?credentialRequirement=cr-pass");
		String contents = executeQuery(addEntity);
		ObjectNode root = (ObjectNode) m.readTree(contents);
		long entityId = root.get("entityId").asLong();
		assertTrue(checkIdentity("userA"));
		System.out.println("Added entity:\n" + contents);
		
		long time = System.currentTimeMillis() + 20000;
		HttpPut scheduleRemoval = new HttpPut("/restadm/v1/entity/" + entityId + "/removal-schedule?when=" + 
				time);
		try(ClassicHttpResponse response = client.executeOpen(host, scheduleRemoval, getClientContext(host))){
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}
		Entity entity = idsMan.getEntity(new EntityParam(entityId));
		assertEquals(new Date(time), entity.getEntityInformation().getRemovalByUserTime());
		assertEquals(EntityState.onlyLoginPermitted, entity.getEntityInformation().getState());
	}
	
	@Test
	public void canTriggerScriptInvocation() throws Exception
	{
		HttpPost trigger = new HttpPost("/restadm/v1/triggerEvent/test_event");
		trigger.setEntity(new StringEntity("user-triggered", ContentType.APPLICATION_JSON));
		try(ClassicHttpResponse response = client.executeOpen(host, trigger, getClientContext(host))){
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}
		
		Entity entity = idsMan.getEntity(new EntityParam(new IdentityTaV(UsernameIdentity.ID, "user-triggered")));
		assertThat(entity, is(notNullValue()));
	}
	
	@Test
	public void shouldChangeEntityStatus() throws Exception
	{
		Identity identity = idsMan.addEntity(new IdentityParam("userName", "userC"), 
				"cr-pass", EntityState.valid);
		long entityId = identity.getEntityId();
		Entity entity = idsMan.getEntity(new EntityParam((entityId)));
		assertThat(entity, is(notNullValue()));
		assertThat(entity.getEntityInformation().getEntityState(), is(EntityState.valid));
		
		HttpPut changeStatus = new HttpPut("/restadm/v1/entity/" + entityId + "/status/" + EntityState.disabled.toString());		
		try(ClassicHttpResponse response = client.executeOpen(host, changeStatus, getClientContext(host))){
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}
		
		entity = idsMan.getEntity(new EntityParam((entityId)));
		assertThat(entity, is(notNullValue()));
		assertThat(entity.getEntityInformation().getEntityState(), is(EntityState.disabled));
	}
}
