/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.authn.LocalAuthenticationState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

public class TestAttributes extends DBIntegrationTestBase
{
	@Test
	public void testSyntaxes() throws Exception
	{
		String[] supportedSyntaxes = attrsMan.getSupportedAttributeValueTypes();
		Arrays.sort(supportedSyntaxes);
		assertEquals(2, supportedSyntaxes.length);
		checkArray(supportedSyntaxes, StringAttributeSyntax.ID, EnumAttributeSyntax.ID);
	}

	@Test
	public void testCreateAttribute() throws Exception
	{
		setupAuthn();
		groupsMan.addGroup(new Group("/test"));
		Identity id = idsMan.addIdentity(new IdentityParam(X500Identity.ID, "cn=golbi", true, true), "crMock", 
				LocalAuthenticationState.disabled);
		attrsMan.addAttributeType(new AttributeType("tel", new StringAttributeSyntax()));
		EntityParam entity = new EntityParam(id.getEntityId());

		StringAttribute at1 = new StringAttribute("tel", "/test", AttributeVisibility.full, "123456");
		try
		{
			attrsMan.setAttribute(entity, at1, false);
			fail("Added attribute in a group where entity is not a member");
		} catch (IllegalGroupValueException e) {}
		groupsMan.addMemberFromParent("/test", entity);
		attrsMan.setAttribute(entity, at1, false);
		
		StringAttribute at2 = new StringAttribute("tel", "/", AttributeVisibility.full, "1234");
		attrsMan.setAttribute(entity, at2, false);
		
		List<Attribute<?>> allAts = attrsMan.getAttributes(entity, null, null);
		assertEquals(2, allAts.size());
		List<Attribute<?>> gr1Ats = attrsMan.getAttributes(entity, "/", null);
		assertEquals(1, gr1Ats.size());
		assertEquals(at2, gr1Ats.get(0));
		List<Attribute<?>> nameAts = attrsMan.getAttributes(entity, null, "tel");
		assertEquals(2, nameAts.size());
		List<Attribute<?>> specificAts = attrsMan.getAttributes(entity, "/test", "tel");
		assertEquals(1, specificAts.size());
		assertEquals(at1, specificAts.get(0));

		
		attrsMan.removeAttribute(entity, "/", "tel");
		gr1Ats = attrsMan.getAttributes(entity, "/", null);
		assertEquals(0, gr1Ats.size());
		List<Attribute<?>> gr2Ats = attrsMan.getAttributes(entity, "/test", null);
		assertEquals(1, gr2Ats.size());
		allAts = attrsMan.getAttributes(entity, null, null);
		assertEquals(1, allAts.size());

		groupsMan.removeGroup("/test", true);
		
		allAts = attrsMan.getAttributes(entity, null, null);
		assertEquals(0, allAts.size());

	
		attrsMan.setAttribute(entity, at2, false);
		allAts = attrsMan.getAttributes(entity, null, null);
		assertEquals(1, allAts.size());
		
		at2.setVisibility(AttributeVisibility.local);
		at2.setValues(Collections.singletonList("333"));
		try
		{
			attrsMan.setAttribute(entity, at2, false);
			fail("updated existing attribute without update flag");
		} catch (IllegalAttributeValueException e) {}
		attrsMan.setAttribute(entity, at2, true);
		
		allAts = attrsMan.getAttributes(entity, null, null);
		assertEquals(0, allAts.size());
		
		allAts = attrsMan.getAllAttributes(entity, null, null);
		assertEquals(3, allAts.size());
		assertEquals("333", getAttributeByName(allAts, "tel").getValues().get(0));
		assertEquals(AttributeVisibility.local, allAts.get(0).getVisibility());
		
		
		AttributeType atHidden = new AttributeType("hiddenTel", new StringAttributeSyntax());
		atHidden.setVisibility(AttributeVisibility.local);
		attrsMan.addAttributeType(atHidden);
		StringAttribute aHidden = new StringAttribute("hiddenTel", "/", AttributeVisibility.full, "123456");
		try
		{
			attrsMan.setAttribute(entity, aHidden, false);
			fail("Managed to lift hidden flag per attribute");
		} catch (IllegalAttributeTypeException e) {}

		idsMan.removeEntity(entity);
	}
	
	@Test
	public void testCreateType() throws Exception
	{
		final int sa = systemAttributeTypes.getSystemAttributes().size();
		List<AttributeType> ats = attrsMan.getAttributeTypes();
		assertEquals(sa, ats.size());

		AttributeType at = new AttributeType();
		at.setValueType(new StringAttributeSyntax());
		at.setDescription("desc");
		at.setFlags(0);
		at.setMaxElements(5);
		at.setMinElements(1);
		at.setName("some");
		at.setSelfModificable(true);
		at.setVisibility(AttributeVisibility.local);
		attrsMan.addAttributeType(at);
		
		ats = attrsMan.getAttributeTypes();
		assertEquals(sa+1, ats.size());
		AttributeType at2 = getAttributeTypeByName(ats, "some");
		
		assertEquals(at.getDescription(), at2.getDescription());
		assertEquals(at.getFlags(), at2.getFlags());
		assertEquals(at.getMaxElements(), at2.getMaxElements());
		assertEquals(at.getMinElements(), at2.getMinElements());
		assertEquals(at.getName(), at2.getName());
		assertEquals(at.getValueType().getValueSyntaxId(), at2.getValueType().getValueSyntaxId());
		assertEquals(at.getVisibility(), at2.getVisibility());
		
		at.setName("some2");
		try
		{
			at.setFlags(100);
			attrsMan.addAttributeType(at);
			fail("Managed to add attr with flags");
		} catch (IllegalAttributeTypeException e) {/*OK*/}
		at.setFlags(0);

		try
		{
			at.setMaxElements(100);
			at.setMinElements(200);
			attrsMan.addAttributeType(at);
			fail("Managed to add attr with wrong min/max");
		} catch (IllegalAttributeTypeException e) {/*OK*/}
		at.setMinElements(0);

		try
		{
			at.setName("some");
			attrsMan.addAttributeType(at);
			fail("Managed to add attr with duplicated name");
		} catch (IllegalAttributeTypeException e) {/*OK*/}
		
		//warning - this adds one more attribute type (with credential)
		setupAuthn();
		//remove one without attributes
		attrsMan.removeAttributeType("some", false);
		ats = attrsMan.getAttributeTypes();
		assertEquals(sa+1, ats.size());

		//recreate and add an attribute
		attrsMan.addAttributeType(at);
		Identity id = idsMan.addIdentity(new IdentityParam(X500Identity.ID, "cn=golbi", true, true), "crMock", 
				LocalAuthenticationState.disabled);
		EntityParam entity = new EntityParam(id.getEntityId());
		StringAttribute at1 = new StringAttribute("some", "/", AttributeVisibility.local, "123456");
		attrsMan.setAttribute(entity, at1, false);
		
		//remove one with attributes
		try
		{
			attrsMan.removeAttributeType("some", false);
			fail("Managed to remove attr type with values");
		} catch (IllegalAttributeTypeException e) {/*OK*/}
		
		attrsMan.removeAttributeType("some", true);
		ats = attrsMan.getAttributeTypes();
		assertEquals(sa+1, ats.size());

		
	}
}
