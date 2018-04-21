/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

public class TestAttributeClasses extends DBIntegrationTestBase
{
	private EntityParam entity;
	private Group groupA;
	
	@Autowired
	private AttributeClassManagement acMan;
	
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
	 * - assign AC to group
	 * - try to remove assigned (should fail)
	 * - remove group assignment
	 * - remove AC
	 */
	@Test
	public void testACManagement() throws Exception
	{
		setupStateForConditions();

		try
		{
			acMan.removeAttributeClass("foo");
			fail("Managed to remove an AC which doesn't exist");
		} catch (IllegalArgumentException e) {}

		
		AttributesClass ac = new AttributesClass("ac1", "desc", Collections.singleton("a2"), 
				 new HashSet<String>(), false, new HashSet<String>(0));
		AttributesClass ac2 = new AttributesClass("ac2", "desc2", new HashSet<String>(), 
				 new HashSet<String>(), true, Collections.singleton(ac.getName()));
		acMan.addAttributeClass(ac);
		Collection<AttributesClass> acs = acMan.getAttributeClasses().values();
		assertEquals(1, acs.size());
		AttributesClass returnedAc = acs.iterator().next();
		areEqual(ac, returnedAc);
		
		try
		{
			acMan.addAttributeClass(ac);			
			fail("Managed to add an AC with an already used name");
		} catch (WrongArgumentException e) {}
		
		
		acMan.addAttributeClass(ac2);
		acs = acMan.getAttributeClasses().values();
		assertEquals(2, acs.size());
		Iterator<AttributesClass> acsIt = acs.iterator();
		returnedAc = acsIt.next();
		if (!returnedAc.getName().equals(ac2.getName()))
			returnedAc = acsIt.next();
		areEqual(ac2, returnedAc);

		try
		{
			acMan.removeAttributeClass(ac.getName());
			fail("Managed to remove an AC used as a parent AC.");
		} catch (SchemaConsistencyException e) {}
		
		groupsMan.addMemberFromParent("/A", entity);
		acMan.setEntityAttributeClasses(entity, "/A", Collections.singleton(ac2.getName()));
		
		try
		{
			acMan.removeAttributeClass(ac2.getName());
			fail("Managed to remove a used AC");
		} catch (SchemaConsistencyException e) {}

		acMan.setEntityAttributeClasses(entity, "/A", new HashSet<String>());
		
		groupA.setAttributesClasses(Collections.singleton(ac2.getName()));
		groupsMan.updateGroup("/A", groupA);
		try
		{
			acMan.removeAttributeClass(ac2.getName());
			fail("Managed to remove a group-used AC");
		} catch (SchemaConsistencyException e) {}
		
		groupA.setAttributesClasses(new HashSet<String>());
		groupsMan.updateGroup("/A", groupA);
		
		acMan.removeAttributeClass(ac2.getName());
	}

	@Test
	public void unusedACIsUpdated() throws Exception
	{
		setupStateForConditions();

		AttributesClass ac = new AttributesClass("ac1", "desc", Sets.newHashSet("a2"), 
				 Sets.newHashSet("a1"), false, Sets.newHashSet());
		acMan.addAttributeClass(ac);
		
		AttributesClass acUp1 = new AttributesClass("ac1", "desc2", Sets.newHashSet("a3"), 
				 Sets.newHashSet("a2"), false, Sets.newHashSet());
		
		acMan.updateAttributeClass(acUp1);
		Map<String, AttributesClass> attributeClasses = acMan.getAttributeClasses();
		AttributesClass updated = attributeClasses.get("ac1");
		assertEquals("desc2", updated.getDescription());
		assertEquals("ac1", updated.getName());
		assertEquals(Sets.newHashSet("a2"), updated.getMandatory());
		assertEquals(Sets.newHashSet("a3", "a2"), updated.getAllowed());
	}
	
	@Test
	public void usedACIsUpdatedWithoutRestricting() throws Exception
	{
		setupStateForConditions();

		AttributesClass ac = new AttributesClass("ac1", "desc", Sets.newHashSet("a2"), 
				 Sets.newHashSet("a1"), false, Sets.newHashSet());
		acMan.addAttributeClass(ac);
		
		//let's add child so we are allowed to do only non restrictive udpates
		AttributesClass acChild = new AttributesClass("acChild", "desc", Collections.singleton("a2"), 
				 Sets.newHashSet("a1"), false, Collections.singleton("ac1"));
		acMan.addAttributeClass(acChild);
		
		AttributesClass acUp1 = new AttributesClass("ac1", "desc", Sets.newHashSet("a2", "a3"), 
				 Sets.newHashSet("a1"), false, Sets.newHashSet());		
		acMan.updateAttributeClass(acUp1);

		AttributesClass acUp2 = new AttributesClass("ac1", "desc", Sets.newHashSet("a2", "a3"), 
				 Sets.newHashSet(), true, Sets.newHashSet());		
		acMan.updateAttributeClass(acUp2);
	}
		
	@Test
	public void usedACIsNotUpdatedWhenRestricting() throws Exception
	{
		setupStateForConditions();

		AttributesClass ac = new AttributesClass("ac1", "desc", Sets.newHashSet("a2"), 
				 Sets.newHashSet("a1"), false, Sets.newHashSet());
		acMan.addAttributeClass(ac);
		
		//let's add child so we are allowed to do only non restrictive udpates
		AttributesClass acChild = new AttributesClass("acChild", "desc", Collections.singleton("a2"), 
				 Sets.newHashSet("a1"), false, Collections.singleton("ac1"));
		acMan.addAttributeClass(acChild);
		
		AttributesClass acUp1 = new AttributesClass("ac1", "desc", Sets.newHashSet(), 
				 Sets.newHashSet("a1"), false, Sets.newHashSet());
		try
		{
			acMan.updateAttributeClass(acUp1);
			fail("Should get an exception");
		} catch (SchemaConsistencyException e)
		{
			//ok, expected
		}

		AttributesClass acUp2 = new AttributesClass("ac1", "desc", Sets.newHashSet("a2"), 
				 Sets.newHashSet("a1", "a2"), true, Sets.newHashSet());		
		try
		{
			acMan.updateAttributeClass(acUp2);
			fail("Should get an exception");
		} catch (SchemaConsistencyException e)
		{
			//ok, expected
		}
	}

	@Test
	public void usedACUpdateRestrictingIsDetectedViaParents() throws Exception
	{
		//Given
		setupStateForConditions();

		AttributesClass acParent = new AttributesClass("acParent", "desc", Sets.newHashSet("a2"), 
				 Sets.newHashSet("a1"), false, Sets.newHashSet());
		acMan.addAttributeClass(acParent);

		AttributesClass ac = new AttributesClass("ac1", "desc", Sets.newHashSet("a3"), 
				 Sets.newHashSet(), false, Sets.newHashSet("acParent"));
		acMan.addAttributeClass(ac);
		
		//let's add child so we are allowed to do only non restrictive udpates
		AttributesClass acChild = new AttributesClass("acChild", "desc", Collections.singleton("a2"), 
				 Sets.newHashSet("a1"), false, Collections.singleton("ac1"));
		acMan.addAttributeClass(acChild);
		
		
		//When
		AttributesClass acUp1 = new AttributesClass("ac1", "desc", Sets.newHashSet("a3"), 
				 Sets.newHashSet(), false, Sets.newHashSet());
		try
		{
			acMan.updateAttributeClass(acUp1);
			//Then
			fail("Should get an exception");
		} catch (SchemaConsistencyException e)
		{
			//ok, expected
		}
	}
	
	
	/**
	 * - try to assign non-exiting AC to group (should fail)
	 * - create AC with allowed and mandatory attrs
	 * - create group with ACs
	 * - check if correctly returned
	 * - try to add entity to the group without any attributes (should fail)
	 * - try to add entity to the group with mandatory and not allowed attributes (should fail)
	 * - add entity to the group with mandatory and allowed attributes
	 * - check if attributes were correctly added
	 * - try to add a disallowed attribute (should fail)
	 * - try to remove required attribute (should fail)
	 * - assign an entity-AC in the same group with a new allowed attribute
	 * - add the new allowed attribute
	 */
	@Test
	public void testGroupAC() throws Exception
	{
		setupStateForConditions();
		groupA.setAttributesClasses(Collections.singleton("foo"));
		try
		{
			groupsMan.updateGroup("/A", groupA);
			fail("Managed to assign non-existing AC to a group");
		} catch (IllegalTypeException e) {}

		AttributesClass ac = new AttributesClass("ac1", "desc", Collections.singleton("a2"), 
				Collections.singleton("a1"), false, new HashSet<String>(0));
		acMan.addAttributeClass(ac);
		
		groupA.setAttributesClasses(Collections.singleton("ac1"));
		groupsMan.updateGroup("/A", groupA);
		
		Group retG = groupsMan.getContents("/A", GroupContents.METADATA).getGroup();
		assertEquals(1, retG.getAttributesClasses().size());
		assertEquals(ac.getName(), retG.getAttributesClasses().iterator().next());
		
		try
		{
			groupsMan.addMemberFromParent("/A", entity);
			fail("Managed to add a member to a group which has AC with required attributes");
		} catch (SchemaConsistencyException e) {}
		
		try
		{
			List<Attribute> initialAtrs = new ArrayList<>();
			initialAtrs.add(StringAttribute.of("a1", "/A"));
			initialAtrs.add(StringAttribute.of("a3", "/A"));
			groupsMan.addMemberFromParent("/A", entity, initialAtrs);
			fail("Managed to add a member to a group which has AC with not allowed attributes");
		} catch (SchemaConsistencyException e) {}
		
		List<Attribute> initialAtrs = new ArrayList<>();
		initialAtrs.add(StringAttribute.of("a1", "/A", ""));
		initialAtrs.add(StringAttribute.of("a2", "/A", ""));
		groupsMan.addMemberFromParent("/A", entity, initialAtrs);
		
		Collection<AttributeExt> returnedAtrs = attrsMan.getAllAttributes(entity, false, "/A", null, false);
		assertEquals(2, returnedAtrs.size());
		
		try
		{
			attrsMan.setAttribute(entity, StringAttribute.of("a3", "/A", ""));
			fail("Managed to add an attribute disallowed by group's AC");
		} catch (SchemaConsistencyException e) {}
		
		try
		{
			attrsMan.removeAttribute(entity, "/A", "a1");
			fail("Managed to remove a mandatory attribute in group's AC");
		} catch (SchemaConsistencyException e) {}
		
		AttributesClass ac2 = new AttributesClass("ac2", "desc2", Collections.singleton("a3"), 
				new HashSet<String>(), false, new HashSet<String>(0));
		acMan.addAttributeClass(ac2);
		
		acMan.setEntityAttributeClasses(entity, "/A", Collections.singleton(ac2.getName()));
		attrsMan.setAttribute(entity, StringAttribute.of("a3", "/A", ""));
	}	
	
	/**
	 * - create two ACs ac1 and ac2, each with a different allowed attribute
	 * - assign both to entity
	 * - add an attribute from ac1 to the entity
	 * - try to remove the ac1 (should fail -> entity would be left with disallowed attribute)
	 * - remove the ac2 from entity
	 * - assign the ac2 as a group AC
	 * - add an attribute from ac2
	 * - try to remove ac2 from the group (should fail -> as above)
	 * - add ac2 as entity AC
	 * - remove ac2 from the group 
	 * @throws Exception
	 */
	@Test
	public void testMultiAC() throws Exception
	{
		setupStateForConditions();
		groupsMan.addMemberFromParent("/A", entity);
		
		AttributesClass ac1 = new AttributesClass("ac1", "desc", Collections.singleton("a1"), 
				new HashSet<String>(), false, new HashSet<String>(0));
		acMan.addAttributeClass(ac1);
		AttributesClass ac2 = new AttributesClass("ac2", "desc2", Collections.singleton("a2"), 
				new HashSet<String>(), false, new HashSet<String>(0));
		acMan.addAttributeClass(ac2);
		
		Set<String> acs = new HashSet<>();
		acs.add(ac1.getName());
		acs.add(ac2.getName());
		acMan.setEntityAttributeClasses(entity, "/A", acs);
		
		attrsMan.setAttribute(entity, StringAttribute.of("a1", "/A", ""));

		try
		{
			acMan.removeAttributeClass(ac1.getName());
			fail("Managed to remove an AC leaving an entity with disallowed attribute");
		} catch (SchemaConsistencyException e) {}
		
		acMan.setEntityAttributeClasses(entity, "/A", Collections.singleton(ac1.getName()));
		
		groupA.setAttributesClasses(Collections.singleton(ac2.getName()));
		groupsMan.updateGroup("/A", groupA);
		
		attrsMan.setAttribute(entity, StringAttribute.of("a2", "/A", ""));
		
		try
		{
			groupA.setAttributesClasses(new HashSet<String>());
			groupsMan.updateGroup("/A", groupA);
			fail("Managed to remove an AC from group, leaving a member with disallowed attribute");
		} catch (SchemaConsistencyException e) {}
		
		acMan.setEntityAttributeClasses(entity, "/A", acs);

		groupA.setAttributesClasses(new HashSet<String>());
		groupsMan.updateGroup("/A", groupA);
	}
	
	/**
	 * - check if no AC is returned
	 * - try to assign non-exiting AC to entity (should fail)
	 * - create AC with allowed and mandatory attrs
	 * - create entity without any attributes
	 * - try to assign AC to the entity (without mandatory attrs so should fail)
	 * - add required attrs to entity and add some not allowed attrs 
	 * - try to assign AC to the entity (should fail as disallowed are assigned)
	 * - remove disallowed attrs
	 * - assign AC
	 * - check if returned properly
	 * - try to remove required attribute (should fail)
	 * - try to add disallowed attribute (should fail)
	 */
	public void testACEffectiveness() throws Exception
	{
		setupStateForConditions();
		
		Collection<AttributesClass> entityAC = acMan.getEntityAttributeClasses(entity, "/");
		assertEquals(0, entityAC.size());
		
		try
		{
			acMan.setEntityAttributeClasses(entity, "/", Collections.singleton("foo"));
			fail("Managed to assign non existing AC to an entity");
		} catch (SchemaConsistencyException e) {}
		
		AttributesClass ac = new AttributesClass("ac1", "desc", Collections.singleton("a2"), 
				Collections.singleton("a1"), false, null);
		acMan.addAttributeClass(ac);
		try
		{
			acMan.setEntityAttributeClasses(entity, "/", Collections.singleton(ac.getName()));
			fail("Managed to assign AC to an entity without mandatory attr");
		} catch (SchemaConsistencyException e) {}
		attrsMan.createAttribute(entity, StringAttribute.of("a1", "/"));
		attrsMan.createAttribute(entity, StringAttribute.of("a3", "/"));
		try
		{
			acMan.setEntityAttributeClasses(entity, "/", Collections.singleton(ac.getName()));
			fail("Managed to assign AC to an entity with not allowed attr");
		} catch (SchemaConsistencyException e) {}
		
		attrsMan.removeAttribute(entity, "/", "a3");
		acMan.setEntityAttributeClasses(entity, "/", Collections.singleton(ac.getName()));
		entityAC = acMan.getEntityAttributeClasses(entity, "/");
		assertEquals(1, entityAC.size());
		AttributesClass retAc = entityAC.iterator().next();
		areEqual(retAc, retAc);
		
		try
		{
			attrsMan.removeAttribute(entity, "/", "a1");
			fail("Managed to remove a mandatory attribute");
		} catch (SchemaConsistencyException e) {}
		
		try
		{
			attrsMan.createAttribute(entity, StringAttribute.of("a3", "/"));
			fail("Managed to add a disallowed attribute");
		} catch (SchemaConsistencyException e) {}
	}
	
	private void areEqual(AttributesClass ac, AttributesClass returnedAc)
	{
		assertEquals(ac.getName(), returnedAc.getName());
		assertEquals(ac.getDescription(), returnedAc.getDescription());
		assertEquals(ac.isAllowArbitrary(), returnedAc.isAllowArbitrary());
		assertEquals(ac.getMandatory(), returnedAc.getMandatory());
		assertEquals(ac.getAllowed(), returnedAc.getAllowed());
		assertEquals(ac.getParentClasses(), returnedAc.getParentClasses());
	}
	
	private void setupStateForConditions() throws Exception
	{
		setupMockAuthn();
		
		AttributeType at = createSimpleAT("a1");
		aTypeMan.addAttributeType(at);
		AttributeType at1 = createSimpleAT("a2");
		at1.setMaxElements(Integer.MAX_VALUE);
		aTypeMan.addAttributeType(at1);
		AttributeType at2 = createSimpleAT("a3");
		aTypeMan.addAttributeType(at2);
		
		groupA = new Group("/A");
		groupsMan.addGroup(groupA);
		
		Identity id = idsMan.addEntity(new IdentityParam(X500Identity.ID, "cn=test"), "crMock", 
				EntityState.disabled, false);
		entity = new EntityParam(id);
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
}



















