/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.authn.LocalAuthenticationState;
import pl.edu.icm.unity.types.basic.AttributeExt;
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
		assertEquals(5, supportedSyntaxes.length);
		checkArray(supportedSyntaxes, StringAttributeSyntax.ID, EnumAttributeSyntax.ID);
	}

	@Test
	public void testCreateAttribute() throws Exception
	{
		setupMockAuthn();
		groupsMan.addGroup(new Group("/test"));
		Identity id = idsMan.addIdentity(new IdentityParam(X500Identity.ID, "cn=golbi", true, true), "crMock", 
				LocalAuthenticationState.disabled, false);
		EntityParam entity = new EntityParam(id.getEntityId());
		
		StringAttribute systemA = new StringAttribute(SystemAttributeTypes.CREDENTIALS_STATE, "/", 
				AttributeVisibility.full, "asdsadsa"); 
		try
		{
			attrsMan.setAttribute(entity, systemA, true);
			fail("Updated immutable attribute");
		} catch (IllegalAttributeTypeException e) {}
		try
		{
			attrsMan.removeAttribute(entity, "/", systemA.getName());
			fail("Removed immutable attribute");
		} catch (IllegalAttributeTypeException e) {}
		
		
		attrsMan.addAttributeType(new AttributeType("tel", new StringAttributeSyntax()));

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
		
		Collection<AttributeExt<?>> allAts = attrsMan.getAttributes(entity, null, null);
		assertEquals(2, allAts.size());
		Collection<AttributeExt<?>> gr1Ats = attrsMan.getAttributes(entity, "/", null);
		assertEquals(1, gr1Ats.size());
		assertEquals(at2, gr1Ats.iterator().next());
		Collection<AttributeExt<?>> nameAts = attrsMan.getAttributes(entity, null, "tel");
		assertEquals(2, nameAts.size());
		Collection<AttributeExt<?>> specificAts = attrsMan.getAttributes(entity, "/test", "tel");
		assertEquals(1, specificAts.size());
		assertEquals(at1, specificAts.iterator().next());

		
		attrsMan.removeAttribute(entity, "/", "tel");
		gr1Ats = attrsMan.getAttributes(entity, "/", null);
		assertEquals(0, gr1Ats.size());
		Collection<AttributeExt<?>> gr2Ats = attrsMan.getAttributes(entity, "/test", null);
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
		
		allAts = attrsMan.getAllAttributes(entity, true, null, null);
		assertEquals(3, allAts.size());
		assertEquals("333", getAttributeByName(allAts, "tel").getValues().get(0));
		assertEquals(AttributeVisibility.local, allAts.iterator().next().getVisibility());
		
		
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
	
	private AttributeType createSimpleAT(String name)
	{
		AttributeType at = new AttributeType();
		at.setValueType(new StringAttributeSyntax());
		at.setDescription("desc");
		at.setFlags(0);
		at.setMaxElements(5);
		at.setMinElements(1);
		at.setName(name);
		at.setSelfModificable(true);
		at.setVisibility(AttributeVisibility.local);
		return at;
	}
	
	@Test
	public void testCreateType() throws Exception
	{
		int automaticAttributes = 1;
		final int sa = systemAttributeTypes.getSystemAttributes().size()+automaticAttributes;
		List<AttributeType> ats = attrsMan.getAttributeTypes();
		assertEquals(sa, ats.size());

		AttributeType at = createSimpleAT("some");
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
		setupMockAuthn();
		//remove one without attributes
		attrsMan.removeAttributeType("some", false);
		ats = attrsMan.getAttributeTypes();
		assertEquals(sa+1, ats.size());

		//recreate and add an attribute
		attrsMan.addAttributeType(at);
		Identity id = idsMan.addIdentity(new IdentityParam(X500Identity.ID, "cn=golbi", true, true), "crMock", 
				LocalAuthenticationState.disabled, false);
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
		
		//add and update
		at = createSimpleAT("some");
		attrsMan.addAttributeType(at);
		at.setDescription("updated");
		at.setMaxElements(100);
		attrsMan.updateAttributeType(at);
		
		ats = attrsMan.getAttributeTypes();
		assertEquals(ats.toString(), sa+2, ats.size());
		at2 = getAttributeTypeByName(ats, "some");
		assertEquals(at.getDescription(), at2.getDescription());
		assertEquals(at.getFlags(), at2.getFlags());
		assertEquals(at.getMaxElements(), at2.getMaxElements());
		assertEquals(at.getMinElements(), at2.getMinElements());
		assertEquals(at.getName(), at2.getName());
		assertEquals(at.getValueType().getValueSyntaxId(), at2.getValueType().getValueSyntaxId());
		assertEquals(at.getVisibility(), at2.getVisibility());
		
		//try to set wrong settings via update
		try
		{
			at.setMaxElements(100);
			at.setMinElements(200);
			attrsMan.updateAttributeType(at);
			fail("Managed to update attr setting wrong min/max");
		} catch (IllegalAttributeTypeException e) {/*OK*/}
		
		//check if update works with instances, when the change is not conflicting
		List<String> vals = new ArrayList<String>();
		Collections.addAll(vals, "1", "2", "3");
		at1 = new StringAttribute("some", "/", AttributeVisibility.local, vals);
		attrsMan.setAttribute(entity, at1, false);
		at.setMinElements(1);
		at.setMaxElements(3);
		attrsMan.updateAttributeType(at);
		
		//and the same when the update of the type is conflicting
		try
		{
			at.setMaxElements(2);
			attrsMan.updateAttributeType(at);
			fail("Managed to update attr setting restrictions incompatible with instances");
		} catch (IllegalAttributeTypeException e) {/*OK*/}
	}
	
	/**
	 * Tests {@link StringAttributeSyntax}, at the same time checking if the generic, wrapping infrastructure is
	 * working correctly.
	 */
	@Test
	public void testStringAT() throws Exception
	{
		setupMockAuthn();
		Identity id = idsMan.addIdentity(new IdentityParam(X500Identity.ID, "cn=golbi", true, true), "crMock", 
				LocalAuthenticationState.disabled, false);
		EntityParam entity = new EntityParam(id.getEntityId());
		
		AttributeType at = createSimpleAT("some");
		at.setMinElements(1);
		at.setMaxElements(2);
		at.setUniqueValues(true);
		StringAttributeSyntax stringSyntax = new StringAttributeSyntax();
		stringSyntax.setMaxLength(8);
		stringSyntax.setMinLength(5);
		stringSyntax.setRegexp("MA.*g");
		at.setValueType(stringSyntax);
		attrsMan.addAttributeType(at);
		
		List<String> vals = new ArrayList<String>();
		Collections.addAll(vals, "MA__g", "MA_ _ _g");
		StringAttribute atOK = new StringAttribute("some", "/", AttributeVisibility.local, vals);
		attrsMan.setAttribute(entity, atOK, false);
		
		//now try to break restrictions:
		// - unique
		vals.clear();
		Collections.addAll(vals, "MA__g", "MA__g");
		try
		{
			attrsMan.setAttribute(entity, atOK, true);
			fail("Managed to add attribute with duplicated values");
		} catch (IllegalAttributeValueException e) {/*OK*/}

		// - values limit
		vals.add("_MA_g");
		try
		{
			attrsMan.setAttribute(entity, atOK, true);
			fail("Managed to add attribute with too many values");
		} catch (IllegalAttributeValueException e) {/*OK*/}
		
		// - min len limit
		vals.clear();
		Collections.addAll(vals, "MA_g");
		try
		{
			attrsMan.setAttribute(entity, atOK, true);
			fail("Managed to add attribute with too short value");
		} catch (IllegalAttributeValueException e) {/*OK*/}
		
		// - max len limit
		vals.clear();
		Collections.addAll(vals, "MA__________g");
		try
		{
			attrsMan.setAttribute(entity, atOK, true);
			fail("Managed to add attribute with too long value");
		} catch (IllegalAttributeValueException e) {/*OK*/}
		
		// - regexp 

		vals.clear();
		Collections.addAll(vals, "M____g");
		try
		{
			attrsMan.setAttribute(entity, atOK, true);
			fail("Managed to add attribute with not matching value");
		} catch (IllegalAttributeValueException e) {/*OK*/}
		
		//try to update the type so that syntax won't match instances
		stringSyntax.setRegexp("MA..g");
		at.setValueType(stringSyntax);
		try
		{
			attrsMan.updateAttributeType(at);
			fail("Managed to update attribute type to confliction with instances");
		} catch (IllegalAttributeTypeException e) {/*OK*/}
	}
	
	@Test
	public void testMultipleInstancesSameSyntax() throws Exception
	{
		AttributeType at1 = new AttributeType("at1", new StringAttributeSyntax());
		((StringAttributeSyntax)at1.getValueType()).setMaxLength(6);
		attrsMan.addAttributeType(at1);

		AttributeType at2 = new AttributeType("at2", new StringAttributeSyntax());
		((StringAttributeSyntax)at2.getValueType()).setMaxLength(600);
		attrsMan.addAttributeType(at2);
		
		List<AttributeType> ats = attrsMan.getAttributeTypes();
		AttributeType at1B = getAttributeTypeByName(ats, "at1");
		AttributeType at2B = getAttributeTypeByName(ats, "at2");
		assertEquals(6, ((StringAttributeSyntax)at1B.getValueType()).getMaxLength());
		assertEquals(600, ((StringAttributeSyntax)at2B.getValueType()).getMaxLength());
	}
}



















