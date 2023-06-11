/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.attribute.IllegalAttributeTypeException;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.authz.RoleAttributeTypeProvider;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.IntegerAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;

public class TestAttributeTypes extends DBIntegrationTestBase
{
	@Test
	public void allImplementedSyntaxesAreReported() throws Exception
	{
		String[] supportedSyntaxes = aTypeMan.getSupportedAttributeValueTypes();
		Arrays.sort(supportedSyntaxes);
		assertEquals(13, supportedSyntaxes.length);
		checkArray(supportedSyntaxes, StringAttributeSyntax.ID, EnumAttributeSyntax.ID);
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
	public void createdTypeIsReturned() throws Exception
	{
		Collection<AttributeType> ats = aTypeMan.getAttributeTypes();
		final int sa = ats.size();
		AttributeType at = createSimpleAT("some");

		aTypeMan.addAttributeType(at);
		
		ats = aTypeMan.getAttributeTypes();
		assertEquals(sa+1, ats.size());
		AttributeType at2 = getAttributeTypeByName(ats, "some");
		assertThat(at, is(at2));
	}

	@Test
	public void cantAddTypeWithInvalidFlags() throws Exception
	{
		AttributeType at = createSimpleAT("some2");
		at.setFlags(100);

		Throwable error = catchThrowable(() -> aTypeMan.addAttributeType(at));

		assertThat(error).isInstanceOf(IllegalAttributeTypeException.class);
	}
	
	@Test
	public void cantAddTypeWithMinGreaterThenMaxFlags() throws Exception
	{
		AttributeType at = createSimpleAT("some");
		at.setMaxElements(100);
		at.setMinElements(200);

		Throwable error = catchThrowable(() -> aTypeMan.addAttributeType(at));

		assertThat(error).isInstanceOf(IllegalAttributeTypeException.class);
	}
	
	@Test
	public void cantAddTypeWithDuplicateName() throws Exception
	{
		AttributeType at = createSimpleAT("some");

		aTypeMan.addAttributeType(at);
		Throwable error = catchThrowable(() -> aTypeMan.addAttributeType(at));
		
		assertThat(error).isInstanceOf(IllegalArgumentException.class);
	}
	
	@Test
	public void typeWithoutAttributesCanBeRemoved() throws Exception
	{
		AttributeType at = createSimpleAT("some");
		aTypeMan.addAttributeType(at);
		
		aTypeMan.removeAttributeType("some", false);

		Collection<AttributeType> ats = aTypeMan.getAttributeTypes();
		AttributeType at2 = getAttributeTypeByName(ats, "some");
		assertThat(at2, is(nullValue()));
	}
	
	@Test
	public void typeWithAttributesCantBeRemovedWithoutForce() throws Exception
	{
		setupMockAuthn();
		AttributeType at = createSimpleAT("some");
		aTypeMan.addAttributeType(at);
		Identity id = createUsernameUser("user");
		EntityParam entity = new EntityParam(id.getEntityId());
		Attribute at1 = StringAttribute.of("some", "/", "123456");
		attrsMan.createAttribute(entity, at1);
		
		Throwable error = catchThrowable(() -> aTypeMan.removeAttributeType("some", false));
		
		assertThat(error).isInstanceOf(IllegalAttributeTypeException.class);
	}
	
	@Test
	public void typeWithAttributesCanBeRemovedWithForce() throws Exception
	{
		setupMockAuthn();
		AttributeType at = createSimpleAT("some");
		aTypeMan.addAttributeType(at);
		Identity id = createUsernameUser("user");
		EntityParam entity = new EntityParam(id.getEntityId());
		Attribute at1 = StringAttribute.of("some", "/", "123456");
		attrsMan.createAttribute(entity, at1);
		
		aTypeMan.removeAttributeType("some", true);
		
		Collection<AttributeType> ats = aTypeMan.getAttributeTypes();
		AttributeType at2 = getAttributeTypeByName(ats, "some");
		assertThat(at2, is(nullValue()));
	}
	
	@Test
	public void updatedTypeIsReturned() throws Exception
	{
		AttributeType at = createSimpleAT("some");
		aTypeMan.addAttributeType(at);
		
		at.setDescription(new I18nString("updated"));
		at.setMaxElements(100);
		aTypeMan.updateAttributeType(at);
		
		Collection<AttributeType> ats = aTypeMan.getAttributeTypes();
		AttributeType at2 = getAttributeTypeByName(ats, "some");
		assertThat(at, is(at2));
	}

	@Test
	public void cantUpdateToTypeWithMinGreaterThenMaxFlags() throws Exception
	{
		AttributeType at = createSimpleAT("some");
		aTypeMan.addAttributeType(at);

		at.setMaxElements(100);
		at.setMinElements(200);
		Throwable error = catchThrowable(() -> aTypeMan.updateAttributeType(at));

		assertThat(error).isInstanceOf(IllegalAttributeTypeException.class);
	}
	
	@Test
	public void nonconflictingUpdateWorksWithAttributes() throws Exception
	{
		setupMockAuthn();
		AttributeType at = createSimpleAT("some");
		aTypeMan.addAttributeType(at);
		List<String> vals = Lists.newArrayList("1", "2", "3");
		Identity id = createUsernameUser("user");
		EntityParam entity = new EntityParam(id.getEntityId());
		Attribute at1 = StringAttribute.of("some", "/", vals);
		attrsMan.createAttribute(entity, at1);
		
		at.setMinElements(1);
		at.setMaxElements(3);
		aTypeMan.updateAttributeType(at);
		
		Collection<AttributeType> ats = aTypeMan.getAttributeTypes();
		AttributeType at2 = getAttributeTypeByName(ats, "some");
		assertThat(at, is(at2));
	}	
	
	@Test
	public void conflictingUpdateIsForbiddenWithAttributes() throws Exception
	{
		setupMockAuthn();
		AttributeType at = createSimpleAT("some");
		aTypeMan.addAttributeType(at);
		List<String> vals = Lists.newArrayList("1", "2", "3");
		Identity id = createUsernameUser("user");
		EntityParam entity = new EntityParam(id.getEntityId());
		Attribute at1 = StringAttribute.of("some", "/", vals);
		attrsMan.createAttribute(entity, at1);
		
		at.setMaxElements(2);
		Throwable error = catchThrowable(() -> aTypeMan.updateAttributeType(at));
		
		assertThat(error).isInstanceOf(IllegalAttributeTypeException.class);
	}
	
	/**
	 * Tests {@link StringAttributeSyntax}, at the same time checking if the generic, wrapping infrastructure is
	 * working correctly.
	 */
	@Test
	public void testStringAT() throws Exception
	{
		setupMockAuthn();
		Identity id = createUsernameUser("user");
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
		Attribute atOK = StringAttribute.of("some", "/", vals);
		attrsMan.createAttribute(entity, atOK);
		
		//now try to break restrictions:
		// - unique
		atOK.setValues("MA__g", "MA__g");
		try
		{
			attrsMan.setAttribute(entity, atOK);
			fail("Managed to add attribute with duplicated values");
		} catch (IllegalAttributeValueException e) {/*OK*/}

		// - values limit
		atOK.setValues("MA__g", "MA___g", "MA_____g");
		try
		{
			attrsMan.setAttribute(entity, atOK);
			fail("Managed to add attribute with too many values");
		} catch (IllegalAttributeValueException e) {/*OK*/}
		
		// - min len limit
		atOK.setValues("MA_g");
		try
		{
			attrsMan.setAttribute(entity, atOK);
			fail("Managed to add attribute with too short value");
		} catch (IllegalAttributeValueException e) {/*OK*/}
		
		// - max len limit
		atOK.setValues("MA__________g");
		try
		{
			attrsMan.setAttribute(entity, atOK);
			fail("Managed to add attribute with too long value");
		} catch (IllegalAttributeValueException e) {/*OK*/}
		
		// - regexp 

		atOK.setValues("M____g");
		try
		{
			attrsMan.setAttribute(entity, atOK);
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
				RoleAttributeTypeProvider.AUTHORIZATION_ROLE);
		assertTrue((crAt.getFlags() | AttributeType.TYPE_IMMUTABLE_FLAG) != 0);
		
		crAt.setSelfModificable(true);
		crAt.setDisplayedName(new I18nString("Foo"));
		crAt.setDescription(new I18nString("FooDesc"));
		crAt.setFlags(0);
		aTypeMan.updateAttributeType(crAt);
		
		ats = aTypeMan.getAttributeTypes();
		crAt = getAttributeTypeByName(ats, RoleAttributeTypeProvider.AUTHORIZATION_ROLE);
		assertTrue((crAt.getFlags() | AttributeType.TYPE_IMMUTABLE_FLAG) != 0);
		assertEquals(new I18nString("Foo"), crAt.getDisplayedName());
		assertEquals(new I18nString("FooDesc"), crAt.getDescription());
		assertTrue(crAt.isSelfModificable());
	}
	
	@Test
	public void cantAddTypeIfSyntaxNotExists() throws EngineException
	{
		AttributeType at = createSimpleAT("some");
		at.setValueSyntax("INCORRECT");
		Throwable error = catchThrowable(() -> aTypeMan.addAttributeType(at));
		assertThat(error).isInstanceOf(IllegalArgumentException.class);
	}
	
	@Test
	public void cantAddTypeIfSyntaxConfigIsIncorrect() throws EngineException
	{
		AttributeType at = createSimpleAT("some");
		ObjectNode main = Constants.MAPPER.createObjectNode();
		main.put("minLength", 1);
		main.put("maxLength", 2);
		at.setValueSyntaxConfiguration(main);
		Throwable error = catchThrowable(() -> aTypeMan.addAttributeType(at));
		assertThat(error).isInstanceOf(IllegalArgumentException.class);
	}
}



















