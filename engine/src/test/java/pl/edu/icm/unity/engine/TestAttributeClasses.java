/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import org.junit.Test;

import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

public class TestAttributeClasses extends DBIntegrationTestBase
{
	private EntityParam entity;
	private Group groupA;
	
	/**
	 * - try to remove unknown AC
	 * - simple create AC
	 * - check if correctly returned
	 * - try to add with existing name (should fail)
	 * - add with parent
	 * - check if both correctly returned
	 * - try to remove AC which is a parent of other AC (should fail)
	 * - assign AC to entity
	 * - try to remove assigned (should fail)
	 * - remove assignment
	 * - remove AC
	 */
	@Test
	public void testACManagement() throws Exception
	{
		setupStateForConditions();

		try
		{
			attrsMan.removeAttributeClass("foo");
			fail("Managed to remove an AC which doesn't exist");
		} catch (WrongArgumentException e) {}

		
		AttributesClass ac = new AttributesClass("ac1", "desc", Collections.singleton("a2"), 
				 new HashSet<String>(), false, null);
		AttributesClass ac2 = new AttributesClass("ac2", "desc2", new HashSet<String>(), 
				 new HashSet<String>(), true, ac.getName());
		attrsMan.addAttributeClass(ac);
		Collection<AttributesClass> acs = attrsMan.getAttributeClasses();
		assertEquals(1, acs.size());
		AttributesClass returnedAc = acs.iterator().next();
		areEqual(ac, returnedAc);
		
		try
		{
			attrsMan.addAttributeClass(ac);			
			fail("Managed to add an AC with an already used name");
		} catch (WrongArgumentException e) {}
		
		
		attrsMan.addAttributeClass(ac2);
		acs = attrsMan.getAttributeClasses();
		assertEquals(2, acs.size());
		Iterator<AttributesClass> acsIt = acs.iterator();
		returnedAc = acsIt.next();
		if (!returnedAc.getName().equals(ac2.getName()))
			returnedAc = acsIt.next();
		areEqual(ac2, returnedAc);

		try
		{
			attrsMan.removeAttributeClass(ac.getName());
			fail("Managed to remove an AC used as a parent AC.");
		} catch (WrongArgumentException e) {}
		
		
		attrsMan.assignAttributeClasses(entity, "/A", Collections.singleton(ac2.getName()));
		
		try
		{
			attrsMan.removeAttributeClass(ac2.getName());
			fail("Managed to remove a used AC");
		} catch (WrongArgumentException e) {}

		attrsMan.assignAttributeClasses(entity, "/A", new HashSet<String>());
		
		attrsMan.removeAttributeClass(ac2.getName());
	}
	
	/**
	 * - create AC with allowed and mandatory attrs
	 * - create entity without any attributes
	 * - try to assign AC to the entity (without mandatory attrs so should fail)
	 * - add required attrs to entity and add some not allowed attrs 
	 * - try to assign AC to the entity (should fail as disallowed are assigned)
	 * - remove disallowed attrs
	 * - assign AC
	 * - try to remove required attribute (should fail)
	 * - try to add disallowed attribute (should fail)
	 */
	public void testACEffectiveness() throws Exception
	{
		setupStateForConditions();
		AttributesClass ac = new AttributesClass("ac1", "desc", Collections.singleton("a2"), 
				Collections.singleton("a1"), false, null);
		attrsMan.addAttributeClass(ac);
		try
		{
			attrsMan.assignAttributeClasses(entity, "/", Collections.singleton(ac.getName()));
			fail("Managed to assign AC to an entity without mandatory attr");
		} catch (WrongArgumentException e) {}
		attrsMan.setAttribute(entity, new StringAttribute("a1", "/", AttributeVisibility.local), false);
		attrsMan.setAttribute(entity, new StringAttribute("a3", "/", AttributeVisibility.local), false);
		try
		{
			attrsMan.assignAttributeClasses(entity, "/", Collections.singleton(ac.getName()));
			fail("Managed to assign AC to an entity with not allowed attr");
		} catch (WrongArgumentException e) {}
		
		attrsMan.removeAttribute(entity, "/", "a3");
		attrsMan.assignAttributeClasses(entity, "/", Collections.singleton(ac.getName()));
		
		try
		{
			attrsMan.removeAttribute(entity, "/", "a1");
			fail("Managed to remove a mandatory attribute");
		} catch (IllegalAttributeTypeException e) {}
		
		try
		{
			attrsMan.setAttribute(entity, new StringAttribute("a3", "/", AttributeVisibility.local), false);
			fail("Managed to add a disallowed attribute");
		} catch (IllegalAttributeTypeException e) {}
	}
	
	private void areEqual(AttributesClass ac, AttributesClass returnedAc)
	{
		assertEquals(ac.getName(), returnedAc.getName());
		assertEquals(ac.getDescription(), returnedAc.getDescription());
		assertEquals(ac.isAllowArbitrary(), returnedAc.isAllowArbitrary());
		assertEquals(ac.getMandatory(), returnedAc.getMandatory());
		assertEquals(ac.getAllowed(), returnedAc.getAllowed());
		assertEquals(ac.getParentClassName(), returnedAc.getParentClassName());
	}
	
	private void setupStateForConditions() throws Exception
	{
		setupMockAuthn();
		
		AttributeType at = createSimpleAT("a1");
		attrsMan.addAttributeType(at);
		AttributeType at1 = createSimpleAT("a2");
		at1.setMaxElements(Integer.MAX_VALUE);
		attrsMan.addAttributeType(at1);
		AttributeType at2 = createSimpleAT("a3");
		attrsMan.addAttributeType(at2);
		
		groupA = new Group("/A");
		groupsMan.addGroup(groupA);
		
		Identity id = idsMan.addEntity(new IdentityParam(X500Identity.ID, "cn=test", true), "crMock", 
				EntityState.disabled, false);
		entity = new EntityParam(id);
		groupsMan.addMemberFromParent("/A", entity);
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
}



















