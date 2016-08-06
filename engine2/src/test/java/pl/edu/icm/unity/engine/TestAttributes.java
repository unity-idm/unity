/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import pl.edu.icm.unity.engine.credential.CredentialAttributeTypeProvider;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.IntegerAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

public class TestAttributes extends DBIntegrationTestBase
{
	@Test
	public void testSyntaxes() throws Exception
	{
		String[] supportedSyntaxes = aTypeMan.getSupportedAttributeValueTypes();
		Arrays.sort(supportedSyntaxes);
		assertEquals(6, supportedSyntaxes.length);
		checkArray(supportedSyntaxes, StringAttributeSyntax.ID, EnumAttributeSyntax.ID);
	}

	@Test
	public void testCreateAttribute() throws Exception
	{
		groupsMan.addGroup(new Group("/test"));
		Identity id = idsMan.addEntity(new IdentityParam(X500Identity.ID, "cn=golbi"), "crMock", 
				EntityState.valid, false);
		EntityParam entity = new EntityParam(id.getEntityId());
		
		StringAttribute systemA = new StringAttribute(
				CredentialAttributeTypeProvider.CREDENTIAL_REQUIREMENTS, "/", 
				"asdsadsa"); 
		try
		{
			attrsMan.setAttribute(entity, systemA, true);
			fail("Updated immutable attribute");
		} catch (SchemaConsistencyException e) {}
		try
		{
			attrsMan.removeAttribute(entity, "/", systemA.getName());
			fail("Removed immutable attribute");
		} catch (SchemaConsistencyException e) {}
		
		
		aTypeMan.addAttributeType(new AttributeType("tel", StringAttributeSyntax.ID));

		StringAttribute at1 = new StringAttribute("tel", "/test", "123456");
		try
		{
			attrsMan.setAttribute(entity, at1, false);
			fail("Added attribute in a group where entity is not a member");
		} catch (IllegalGroupValueException e) {}
		groupsMan.addMemberFromParent("/test", entity);
		attrsMan.setAttribute(entity, at1, false);
		
		StringAttribute at2 = new StringAttribute("tel", "/", "1234");
		attrsMan.setAttribute(entity, at2, false);
		
		Collection<AttributeExt> allAts = attrsMan.getAttributes(entity, null, null);
		assertEquals(2, allAts.size());
		Collection<AttributeExt> gr1Ats = attrsMan.getAttributes(entity, "/", null);
		assertEquals(1, gr1Ats.size());
		AttributeExt retrievedA = gr1Ats.iterator().next(); 
		assertEquals(at2, retrievedA);
		assertNotNull(retrievedA.getUpdateTs());
		assertNotNull(retrievedA.getCreationTs());
		assertNull(retrievedA.getRemoteIdp());
		assertNull(retrievedA.getTranslationProfile());
		
		Collection<AttributeExt> nameAts = attrsMan.getAttributes(entity, null, "tel");
		assertEquals(2, nameAts.size());
		Collection<AttributeExt> specificAts = attrsMan.getAttributes(entity, "/test", "tel");
		assertEquals(1, specificAts.size());
		assertEquals(at1, specificAts.iterator().next());

		
		attrsMan.removeAttribute(entity, "/", "tel");
		gr1Ats = attrsMan.getAttributes(entity, "/", null);
		assertEquals(0, gr1Ats.size());
		Collection<AttributeExt> gr2Ats = attrsMan.getAttributes(entity, "/test", null);
		assertEquals(1, gr2Ats.size());
		allAts = attrsMan.getAttributes(entity, null, null);
		assertEquals(1, allAts.size());

		groupsMan.removeMember("/test", entity);
		try
		{
			attrsMan.getAttributes(entity, "/test", null);
			fail("no error when asking for attributes in group where the user is not a member");
		} catch (IllegalGroupValueException e) {}
		groupsMan.addMemberFromParent("/test", entity);
		
		gr2Ats = attrsMan.getAttributes(entity, "/test", null);
		assertEquals(0, gr2Ats.size());
		allAts = attrsMan.getAttributes(entity, null, null);
		assertEquals(0, allAts.size());
		
		attrsMan.setAttribute(entity, at1, false);
		groupsMan.removeGroup("/test", true);
		
		allAts = attrsMan.getAttributes(entity, null, null);
		assertEquals(allAts.toString(), 0, allAts.size());

	
		attrsMan.setAttribute(entity, at2, false);
		allAts = attrsMan.getAttributes(entity, null, null);
		assertEquals(1, allAts.size());
		
		retrievedA = allAts.iterator().next();
		Date created = retrievedA.getCreationTs(); 
		Date updated = retrievedA.getUpdateTs(); 
		assertEquals(created, updated);
		
		at2.setValues(Collections.singletonList("333"));
		try
		{
			attrsMan.setAttribute(entity, at2, false);
			fail("updated existing attribute without update flag");
		} catch (IllegalAttributeValueException e) {}
		attrsMan.setAttribute(entity, at2, true);
		
		allAts = attrsMan.getAttributes(entity, null, null);
		assertEquals(0, allAts.size());
		
		allAts = attrsMan.getAllAttributes(entity, true, "/", "tel", false);
		assertEquals(1, allAts.size());
		retrievedA = allAts.iterator().next();
		assertEquals(created, retrievedA.getCreationTs());
		assertNotEquals(updated, retrievedA.getUpdateTs());
		assertNotNull(retrievedA.getUpdateTs());
		
		allAts = attrsMan.getAllAttributes(entity, true, null, null, false);
		assertEquals(2, allAts.size());
		assertEquals("333", getAttributeByName(allAts, "tel").getValues().get(0));

		allAts = attrsMan.getAllAttributes(entity, false, "/", "tel", false);
		assertEquals(1, allAts.size());
		assertEquals("333", getAttributeByName(allAts, "tel").getValues().get(0));
		
		AttributeType atHidden = new AttributeType("hiddenTel", StringAttributeSyntax.ID);
		aTypeMan.addAttributeType(atHidden);
		StringAttribute aHidden = new StringAttribute("hiddenTel", "/", "123456");
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
		at.setValueSyntax(StringAttributeSyntax.ID);
		at.setDescription(new I18nString("desc"));
		at.setFlags(0);
		at.setMaxElements(5);
		at.setMinElements(1);
		at.setName(name);
		at.setSelfModificable(true);
		return at;
	}
	
	@Test
	public void testCreateType() throws Exception
	{
		Collection<AttributeType> ats = aTypeMan.getAttributeTypes();
		final int sa = ats.size();

		AttributeType at = createSimpleAT("some");
		aTypeMan.addAttributeType(at);
		
		ats = aTypeMan.getAttributeTypes();
		assertEquals(sa+1, ats.size());
		AttributeType at2 = getAttributeTypeByName(ats, "some");
		
		assertThat(at, is(at2));
		
		at.setName("some2");
		try
		{
			at.setFlags(100);
			aTypeMan.addAttributeType(at);
			fail("Managed to add attr with flags");
		} catch (IllegalAttributeTypeException e) {/*OK*/}
		at.setFlags(0);

		try
		{
			at.setMaxElements(100);
			at.setMinElements(200);
			aTypeMan.addAttributeType(at);
			fail("Managed to add attr with wrong min/max");
		} catch (IllegalAttributeTypeException e) {/*OK*/}
		at.setMinElements(0);

		try
		{
			at.setName("some");
			aTypeMan.addAttributeType(at);
			fail("Managed to add attr with duplicated name");
		} catch (IllegalArgumentException e) {/*OK*/}
		
		//remove one without attributes
		aTypeMan.removeAttributeType("some", false);
		ats = aTypeMan.getAttributeTypes();
		assertEquals(sa+1, ats.size());

		//recreate and add an attribute
		aTypeMan.addAttributeType(at);
		Identity id = idsMan.addEntity(new IdentityParam(X500Identity.ID, "cn=golbi"), "crMock", 
				EntityState.disabled, false);
		EntityParam entity = new EntityParam(id.getEntityId());
		StringAttribute at1 = new StringAttribute("some", "/", "123456");
		attrsMan.setAttribute(entity, at1, false);
		
		//remove one with attributes
		try
		{
			aTypeMan.removeAttributeType("some", false);
			fail("Managed to remove attr type with values");
		} catch (IllegalAttributeTypeException e) {/*OK*/}
		
		aTypeMan.removeAttributeType("some", true);
		ats = aTypeMan.getAttributeTypes();
		assertEquals(sa+1, ats.size());
		
		//add and update
		at = createSimpleAT("some");
		aTypeMan.addAttributeType(at);
		at.setDescription(new I18nString("updated"));
		at.setMaxElements(100);
		aTypeMan.updateAttributeType(at);
		
		ats = aTypeMan.getAttributeTypes();
		assertEquals(ats.toString(), sa+2, ats.size());
		at2 = getAttributeTypeByName(ats, "some");
		assertThat(at, is(at2));
		
		//try to set wrong settings via update
		try
		{
			at.setMaxElements(100);
			at.setMinElements(200);
			aTypeMan.updateAttributeType(at);
			fail("Managed to update attr setting wrong min/max");
		} catch (IllegalAttributeTypeException e) {/*OK*/}
		
		//check if update works with instances, when the change is not conflicting
		List<String> vals = new ArrayList<String>();
		Collections.addAll(vals, "1", "2", "3");
		at1 = new StringAttribute("some", "/", vals);
		attrsMan.setAttribute(entity, at1, false);
		at.setMinElements(1);
		at.setMaxElements(3);
		aTypeMan.updateAttributeType(at);
		
		//and the same when the update of the type is conflicting
		try
		{
			at.setMaxElements(2);
			aTypeMan.updateAttributeType(at);
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
		Identity id = idsMan.addEntity(new IdentityParam(X500Identity.ID, "cn=golbi"), "crMock", 
				EntityState.disabled, false);
		EntityParam entity = new EntityParam(id.getEntityId());
		
		AttributeType at = createSimpleAT("some");
		at.setMinElements(1);
		at.setMaxElements(2);
		at.setUniqueValues(true);
		StringAttributeSyntax stringSyntax = new StringAttributeSyntax();
		stringSyntax.setMaxLength(8);
		stringSyntax.setMinLength(5);
		stringSyntax.setRegexp("MA.*g");
		at.setValueSyntax(StringAttributeSyntax.ID);
		at.setValueSyntaxConfiguration(stringSyntax.getSerializedConfiguration());
		aTypeMan.addAttributeType(at);
		
		List<String> vals = new ArrayList<String>();
		Collections.addAll(vals, "MA__g", "MA_ _ _g");
		StringAttribute atOK = new StringAttribute("some", "/", vals);
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
		at.setValueSyntaxConfiguration(stringSyntax.getSerializedConfiguration());
		try
		{
			aTypeMan.updateAttributeType(at);
			fail("Managed to update attribute type to confliction with instances");
		} catch (IllegalAttributeTypeException e) {/*OK*/}
	}
	
	@Test
	public void testMultipleInstancesSameSyntax() throws Exception
	{
		AttributeType at1 = new AttributeType("at1", StringAttributeSyntax.ID);
		at1.setDescription(new I18nString("def"));
		StringAttributeSyntax stringSyntax = new StringAttributeSyntax();
		stringSyntax.setMaxLength(6);
		at1.setValueSyntaxConfiguration(stringSyntax.getSerializedConfiguration());
		aTypeMan.addAttributeType(at1);

		AttributeType at2 = new AttributeType("at2", StringAttributeSyntax.ID);
		at2.setDescription(new I18nString("desc2"));
		StringAttributeSyntax stringSyntax2 = new StringAttributeSyntax();
		stringSyntax2.setMaxLength(600);
		at2.setValueSyntaxConfiguration(stringSyntax2.getSerializedConfiguration());
		aTypeMan.addAttributeType(at2);
		
		Collection<AttributeType> ats = aTypeMan.getAttributeTypes();
		AttributeType at1B = getAttributeTypeByName(ats, "at1");
		AttributeType at2B = getAttributeTypeByName(ats, "at2");
		assertThat(at1B, is(at1));
		assertThat(at2B, is(at2));
	}
	
	@Test
	public void testMetadata() throws Exception
	{
		AttributeType atInt = new AttributeType("at1", IntegerAttributeSyntax.ID);
		atInt.getMetadata().put(EntityNameMetadataProvider.NAME, "");
		try
		{
			aTypeMan.addAttributeType(atInt);
			fail("Managed to add with wrong syntax");
		} catch (IllegalAttributeTypeException e) {};
		
		
		AttributeType at1 = new AttributeType("at1", StringAttributeSyntax.ID);
		at1.setMaxElements(1);
		at1.setMinElements(1);
		at1.getMetadata().put(EntityNameMetadataProvider.NAME, "");
		aTypeMan.addAttributeType(at1);

		AttributeType ret = getAttributeTypeByName(aTypeMan.getAttributeTypes(), "at1");
		assertTrue(ret.getMetadata().containsKey(EntityNameMetadataProvider.NAME));
		
		aTypeMan.removeAttributeType("at1", false);
		at1.getMetadata().remove(EntityNameMetadataProvider.NAME);
		aTypeMan.addAttributeType(at1);
		
		AttributeType at2 = new AttributeType("at2", StringAttributeSyntax.ID);
		at2.setMaxElements(1);
		at2.setMinElements(1);
		aTypeMan.addAttributeType(at2);
		
		at1.getMetadata().put(EntityNameMetadataProvider.NAME, "");
		aTypeMan.updateAttributeType(at1);

		//let's check again - update should work
		aTypeMan.updateAttributeType(at1);
		
		
		at2.getMetadata().put(EntityNameMetadataProvider.NAME, "");
		try
		{
			aTypeMan.updateAttributeType(at2);
			fail("Managed to set 2nd via update");
		} catch (IllegalAttributeTypeException e) {};
		

		AttributeType at3 = new AttributeType("at3", StringAttributeSyntax.ID);
		at3.setMaxElements(1);
		at3.setMinElements(1);
		at3.getMetadata().put(EntityNameMetadataProvider.NAME, "");
		try
		{
			aTypeMan.updateAttributeType(at3);
			fail("Managed to set 2nd via add");
		} catch (IllegalArgumentException e) {};
		
	}

	
	@Test
	public void nameLocalAndSelfFlagsAreModifiableForImmutableType() throws Exception
	{
		Collection<AttributeType> ats = aTypeMan.getAttributeTypes();
		AttributeType crAt = getAttributeTypeByName(ats, 
				CredentialAttributeTypeProvider.CREDENTIAL_REQUIREMENTS);
		assertTrue((crAt.getFlags() | AttributeType.TYPE_IMMUTABLE_FLAG) != 0);
		
		crAt.setSelfModificable(true);
		crAt.setDisplayedName(new I18nString("Foo"));
		crAt.setDescription(new I18nString("FooDesc"));
		crAt.setFlags(0);
		aTypeMan.updateAttributeType(crAt);
		
		ats = aTypeMan.getAttributeTypes();
		crAt = getAttributeTypeByName(ats, CredentialAttributeTypeProvider.CREDENTIAL_REQUIREMENTS);
		assertTrue((crAt.getFlags() | AttributeType.TYPE_IMMUTABLE_FLAG) != 0);
		assertEquals(new I18nString("Foo"), crAt.getDisplayedName());
		assertEquals(new I18nString("FooDesc"), crAt.getDescription());
		assertTrue(crAt.isSelfModificable());
	}
}



















