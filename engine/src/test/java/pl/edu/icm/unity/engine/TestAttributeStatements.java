/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;

import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.attrstmnt.CopyParentAttributeStatement;
import pl.edu.icm.unity.types.basic.attrstmnt.CopySubgroupAttributeStatement;
import pl.edu.icm.unity.types.basic.attrstmnt.EverybodyStatement;
import pl.edu.icm.unity.types.basic.attrstmnt.HasParentAttributeStatement;
import pl.edu.icm.unity.types.basic.attrstmnt.HasSubgroupAttributeStatement;
import pl.edu.icm.unity.types.basic.attrstmnt.MemberOfStatement;

public class TestAttributeStatements extends DBIntegrationTestBase
{
	private final int systemAttributes = 1;
	private EntityParam entity;
	private Group groupA;
	private Group groupAB;
	private Group groupAD;
	private Group groupAZ;
	private Group groupABC;
	
	@Test
	public void testSimple() throws Exception
	{
		setupStateForConditions();
		
		// test if simple attributes are properly returned

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 0, 0, 0,  0, 0); //a2
	}
	
	@Test
	public void testEverybody() throws Exception
	{
		setupStateForConditions();
		
		// test with one statement added to /A for everybody
		groupA.setAttributeStatements(new AttributeStatement[] {new EverybodyStatement(
				new StringAttribute("a2", "/A", AttributeVisibility.local, "va1"), 
				ConflictResolution.skip)});
		groupsMan.updateGroup("/A", groupA);

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 1, 0, 0,  0, 0); //a2
	}
	
	
	@Test
	public void testMemberOf() throws Exception
	{
		setupStateForConditions();
		// statement at /A/B for all members of /A/B/C (entity is member)
		groupAB.setAttributeStatements(new AttributeStatement[] {new MemberOfStatement(
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "va1"), 
				"/A/B/C", 
				ConflictResolution.skip)});
		groupsMan.updateGroup("/A/B", groupAB);

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 0, 1, 0,  0, 0); //a2

		// condition added to /A/B/C for all members of /A/V (entity is not a member, should be no change)		
		groupABC.setAttributeStatements(new AttributeStatement[] {new MemberOfStatement(
				new StringAttribute("a2", "/A/B/C", AttributeVisibility.local, "va1"), 
				"/A/V",
				ConflictResolution.skip)});
		groupsMan.updateGroup("/A/B/C", groupABC);

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 0, 1, 0,  0, 0); //a2
	}

	@Test
	public void testCopyParentAttr() throws Exception
	{
		setupStateForConditions();
	
		// added to /A/B/C for all having an "a1" attribute in parent. 
		// Entity has this attribute as regular attribute there.
		AttributeStatement statement1 = new CopyParentAttributeStatement(
				new StringAttribute("a1", "/A/B", AttributeVisibility.local), 
				ConflictResolution.skip);
		groupABC.setAttributeStatements(new AttributeStatement[] {statement1});
		groupsMan.updateGroup("/A/B/C", groupABC);

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 1,  0, 0,  //a1
				0, 0, 0, 0,  0, 0); //a2
	}

	
	@Test
	public void testParentAttr() throws Exception
	{
		setupStateForConditions();
	
		// added to /A/B for all having an "a1" attribute in parent. 
		// Entity has this attribute as regular attribute there.
		AttributeStatement statement1 = new HasParentAttributeStatement(
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "va1"), 
				new StringAttribute("a1", "/A", AttributeVisibility.local), 
				ConflictResolution.skip);
		groupAB.setAttributeStatements(new AttributeStatement[] {statement1});
		groupsMan.updateGroup("/A/B", groupAB);

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 0, 1, 0,  0, 0); //a2
	}
	
	@Test
	public void testParentAttr2() throws Exception
	{
		setupStateForConditions();
	
		// added to /A/B for all having an "a2" attribute in parent. 
		// Entity doesn't have this attribute there
		AttributeStatement statement1 = new HasParentAttributeStatement(
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "va1"), 
				new StringAttribute("a2", "/A", AttributeVisibility.local), 
				ConflictResolution.skip);
		groupAB.setAttributeStatements(new AttributeStatement[] {statement1});
		groupsMan.updateGroup("/A/B", groupAB);

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 0, 0, 0,  0, 0); //a2

		// now a2 is added in /A using a statement
		groupA.setAttributeStatements(new AttributeStatement[] {new EverybodyStatement(
				new StringAttribute("a2", "/A", AttributeVisibility.local, "va1"), 
				ConflictResolution.skip)});
		groupsMan.updateGroup("/A", groupA);
		
		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 1, 1, 0,  0, 0); //a2
	}
	
	@Test
	public void testParentAttr3() throws Exception
	{
		setupStateForConditions();
	
		// added to /A/B for all having an "a2" attribute in parent. 
		AttributeStatement statement1 = new HasParentAttributeStatement(
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "va1"), 
				new StringAttribute("a2", "/A", AttributeVisibility.local), 
				ConflictResolution.skip);
		groupAB.setAttributeStatements(new AttributeStatement[] {statement1});
		groupsMan.updateGroup("/A/B", groupAB);

		// added to /A for all having an "a2" attribute in parent. 
		AttributeStatement statement2 = new HasParentAttributeStatement(
				new StringAttribute("a2", "/A", AttributeVisibility.local, "va1"), 
				new StringAttribute("a2", "/", AttributeVisibility.local), 
				ConflictResolution.skip);
		groupA.setAttributeStatements(new AttributeStatement[] {statement2});
		groupsMan.updateGroup("/A", groupA);
		
		// added to / for everybody
		Group root = new Group("/");
		AttributeStatement statement3 = new EverybodyStatement(
				new StringAttribute("a2", "/", AttributeVisibility.local, "va1"), 
				ConflictResolution.skip);
		root.setAttributeStatements(new AttributeStatement[] {statement3});
		groupsMan.updateGroup("/", root);
		
		
		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				1, 1, 1, 0,  0, 0); //a2
	}	
	
	@Test
	public void testCopySubgroupAttr() throws Exception
	{
		setupStateForConditions();

		// added to / for all having an "a1" attribute in /A. 
		// Entity has this attribute as regular attribute there.
		AttributeStatement statement1 = new CopySubgroupAttributeStatement(
				new StringAttribute("a1", "/A", AttributeVisibility.local), 
				ConflictResolution.skip);
		Group groupR = groupsMan.getContents("/", GroupContents.METADATA).getGroup();
		groupR.setAttributeStatements(new AttributeStatement[] {statement1});
		groupsMan.updateGroup("/", groupR);

		//              /  A  AB ABC AD AZ
		testCorrectness(1, 1, 1, 0,  0, 0,  //a1
				0, 0, 0, 0,  0, 0); //a2
	}
	
	@Test
	public void testSubgroupAttr() throws Exception
	{
		setupStateForConditions();
	
		// added to /A for all having an "a1" attribute in /A/B. 
		// Entity has this attribute as regular attribute there.
		AttributeStatement statement1 = new HasSubgroupAttributeStatement(
				new StringAttribute("a2", "/A", AttributeVisibility.local, "va1"), 
				new StringAttribute("a1", "/A/B", AttributeVisibility.local), 
				ConflictResolution.skip);
		groupA.setAttributeStatements(new AttributeStatement[] {statement1});
		groupsMan.updateGroup("/A", groupA);

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 1, 0, 0,  0, 0); //a2
	}
	
	@Test
	public void testSubgroupAttr2() throws Exception
	{
		setupStateForConditions();
	
		// added to /A for all having an "a2" attribute in /A/B. 
		// Entity doesn't have this attribute there
		AttributeStatement statement1 = new HasSubgroupAttributeStatement(
				new StringAttribute("a2", "/A", AttributeVisibility.local, "va1"), 
				new StringAttribute("a2", "/A/B", AttributeVisibility.local), 
				ConflictResolution.skip);
		groupA.setAttributeStatements(new AttributeStatement[] {statement1});
		groupsMan.updateGroup("/A", groupA);

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 0, 0, 0,  0, 0); //a2

		// now a2 is added in /A/B using a statement
		groupAB.setAttributeStatements(new AttributeStatement[] {new EverybodyStatement(
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "va1"), 
				ConflictResolution.skip)});
		groupsMan.updateGroup("/A/B", groupAB);
		
		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 1, 1, 0,  0, 0); //a2
	}
	
	@Test
	public void testSubgroupAttr3() throws Exception
	{
		setupStateForConditions();
	
		// added to /A for all having an "a2" attribute in /A/B. 
		AttributeStatement statement1 = new HasSubgroupAttributeStatement(
				new StringAttribute("a2", "/A", AttributeVisibility.local, "va1"), 
				new StringAttribute("a2", "/A/B", AttributeVisibility.local), 
				ConflictResolution.skip);
		groupA.setAttributeStatements(new AttributeStatement[] {statement1});
		groupsMan.updateGroup("/A", groupA);

		// added to /A/B for all having an "a2" attribute in /A/B/C. 
		AttributeStatement statement2 = new HasSubgroupAttributeStatement(
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "va1"), 
				new StringAttribute("a2", "/A/B/C", AttributeVisibility.local), 
				ConflictResolution.skip);
		groupAB.setAttributeStatements(new AttributeStatement[] {statement2});
		groupsMan.updateGroup("/A/B", groupAB);
		
		// added to /A/B/B for everybody
		AttributeStatement statement3 = new EverybodyStatement(
				new StringAttribute("a2", "/A/B/C", AttributeVisibility.local, "va1"), 
				ConflictResolution.skip);
		groupABC.setAttributeStatements(new AttributeStatement[] {statement3});
		groupsMan.updateGroup("/A/B/C", groupABC);
		
		
		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 1, 1, 1,  0, 0); //a2
	}	

	
	@Test
	public void testSubgroupAttrWithValue() throws Exception
	{
		setupStateForConditions();
	
		// added to /A for all having an "a1" with value "foo" attribute in /A/B. 
		// Entity has this attribute as regular attribute there but with other value.
		AttributeStatement statement1 = new HasSubgroupAttributeStatement(
				new StringAttribute("a2", "/A", AttributeVisibility.local, "va1"), 
				new StringAttribute("a1", "/A/B", AttributeVisibility.local, "foo"), 
				ConflictResolution.skip);
		groupA.setAttributeStatements(new AttributeStatement[] {statement1});
		groupsMan.updateGroup("/A", groupA);

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 0, 0, 0,  0, 0); //a2

		//again using the existing value
		AttributeStatement statement2 = new HasSubgroupAttributeStatement(
				new StringAttribute("a2", "/A", AttributeVisibility.local, "va1"), 
				new StringAttribute("a1", "/A/B", AttributeVisibility.local, "va1"), 
				ConflictResolution.skip);
		groupA.setAttributeStatements(new AttributeStatement[] {statement2});
		groupsMan.updateGroup("/A", groupA);

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 1, 0, 0,  0, 0); //a2
	}
	
	@Test
	public void testParentgroupAttrWithValue() throws Exception
	{
		setupStateForConditions();
	
		// added to /A/B for all having an "a1" with value "foo" attribute in /A. 
		// Entity has this attribute as regular attribute there but with other value.
		AttributeStatement statement1 = new HasParentAttributeStatement(
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "va1"), 
				new StringAttribute("a1", "/A", AttributeVisibility.local, "foo"), 
				ConflictResolution.skip);
		groupAB.setAttributeStatements(new AttributeStatement[] {statement1});
		groupsMan.updateGroup("/A/B", groupAB);

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 0, 0, 0,  0, 0); //a2

		//again using the existing value
		AttributeStatement statement2 = new HasParentAttributeStatement(
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "va1"), 
				new StringAttribute("a1", "/A", AttributeVisibility.local, "va1"), 
				ConflictResolution.skip);
		groupAB.setAttributeStatements(new AttributeStatement[] {statement2});
		groupsMan.updateGroup("/A/B", groupAB);

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 0, 1, 0,  0, 0); //a2
	}
	
	@Test
	public void testConflictResolution() throws Exception
	{
		setupStateForConditions();
		Collection<AttributeExt<?>> aRet;

		// overwrite direct (-> should skip)
		AttributeStatement statement0 = new EverybodyStatement(
				new StringAttribute("a1", "/A/B", AttributeVisibility.local, "updated"), 
				ConflictResolution.overwrite);
		groupAB.setAttributeStatements(new AttributeStatement[] {statement0});
		groupsMan.updateGroup("/A/B", groupAB);
		aRet = attrsMan.getAllAttributes(entity, true, "/A/B", "a1", false);
		assertEquals(1, aRet.size());
		assertEquals(1, aRet.iterator().next().getValues().size());
		assertEquals("va1", aRet.iterator().next().getValues().get(0));
	
		// skip
		AttributeStatement statement1 = new EverybodyStatement(
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "base"), 
				ConflictResolution.skip);
		groupAB.setAttributeStatements(new AttributeStatement[] {statement1});
		groupsMan.updateGroup("/A/B", groupAB);
		aRet = attrsMan.getAllAttributes(entity, true, "/A/B", "a2", false);
		assertEquals(1, aRet.size());
		assertEquals(1, aRet.iterator().next().getValues().size());
		assertEquals("base", aRet.iterator().next().getValues().get(0));
		
		//skip x2
		AttributeStatement statement11 = new EverybodyStatement(
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "updated"), 
				ConflictResolution.skip);
		groupAB.setAttributeStatements(new AttributeStatement[] {statement1, statement11});
		groupsMan.updateGroup("/A/B", groupAB);
		aRet = attrsMan.getAllAttributes(entity, true, "/A/B", "a2", false);
		assertEquals(1, aRet.size());
		assertEquals(1, aRet.iterator().next().getValues().size());
		assertEquals("base", aRet.iterator().next().getValues().get(0));

		// skip and then overwrite
		AttributeStatement statement2 = new EverybodyStatement(
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "updated2"), 
				ConflictResolution.overwrite);
		groupAB.setAttributeStatements(new AttributeStatement[] {statement1, statement11, statement2});
		groupsMan.updateGroup("/A/B", groupAB);
		
		aRet = attrsMan.getAllAttributes(entity, true, "/A/B", "a2", false);
		assertEquals(1, aRet.size());
		assertEquals(1, aRet.iterator().next().getValues().size());
		assertEquals("updated2", aRet.iterator().next().getValues().get(0));
		
		// skip, overwrite, merge which should skip
		AttributeStatement statement31 = new EverybodyStatement(
				new StringAttribute("a1", "/A/D", AttributeVisibility.local, "base"), 
				ConflictResolution.merge);
		AttributeStatement statement32 = new EverybodyStatement(
				new StringAttribute("a1", "/A/D", AttributeVisibility.local, "merge"), 
				ConflictResolution.merge);
		groupAD.setAttributeStatements(new AttributeStatement[] {statement31, statement32});
		groupsMan.updateGroup("/A/D", groupAD);
		
		aRet = attrsMan.getAllAttributes(entity, true, "/A/D", "a1", false);
		assertEquals(1, aRet.size());
		assertEquals(1, aRet.iterator().next().getValues().size());
		assertEquals("base", aRet.iterator().next().getValues().get(0));

		//add two rules to test merge working
		AttributeStatement statement4 = new EverybodyStatement(
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "merge1"), 
				ConflictResolution.skip);
		AttributeStatement statement5 = new EverybodyStatement(
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "merge2"), 
				ConflictResolution.merge);
		groupAB.setAttributeStatements(new AttributeStatement[] {statement4, statement5});
		groupsMan.updateGroup("/A/B", groupAB);

		aRet = attrsMan.getAllAttributes(entity, true, "/A/B", "a2", false);
		assertEquals(1, aRet.size());
		assertEquals(2, aRet.iterator().next().getValues().size());
		assertEquals("merge1", aRet.iterator().next().getValues().get(0));
		assertEquals("merge2", aRet.iterator().next().getValues().get(1));
		
		//additionally add a direct attribute
		attrsMan.setAttribute(entity, new StringAttribute("a2", "/A/B", AttributeVisibility.local, "direct"), false);
		aRet = attrsMan.getAllAttributes(entity, true, "/A/B", "a2", false);
		assertEquals(1, aRet.size());
		assertEquals(aRet.iterator().next().getValues().toString(), 2, aRet.iterator().next().getValues().size());
		assertEquals("direct", aRet.iterator().next().getValues().get(0));
		assertEquals("merge2", aRet.iterator().next().getValues().get(1));
	}
	
	@Test
	public void testCleanup() throws Exception
	{
		setupStateForConditions();
		
		AttributeStatement statement1 = new EverybodyStatement(
				new StringAttribute("a1", "/A", AttributeVisibility.local, "updated"), 
				ConflictResolution.overwrite);
		AttributeStatement statement3 = new HasSubgroupAttributeStatement(
				new StringAttribute("a2", "/A", AttributeVisibility.local, "va1"), 
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "foo"), 
				ConflictResolution.skip);
		groupA.setAttributeStatements(new AttributeStatement[] {statement1, statement3});
		groupsMan.updateGroup("/A", groupA);
		
		assertEquals(0, statementsCleaner.updateGroups());
		
		GroupContents c = groupsMan.getContents("/A", GroupContents.METADATA);
		assertEquals(2, c.getGroup().getAttributeStatements().length);
		
		attrsMan.removeAttributeType("a1", true);
		c = groupsMan.getContents("/A", GroupContents.METADATA);
		assertEquals(1, c.getGroup().getAttributeStatements().length);
		
		assertEquals(1, statementsCleaner.updateGroups());
		
		groupsMan.removeGroup("/A/B", true);
		c = groupsMan.getContents("/A", GroupContents.METADATA);
		assertEquals(0, c.getGroup().getAttributeStatements().length);

		assertEquals(1, statementsCleaner.updateGroups());
		
		c = groupsMan.getContents("/A", GroupContents.METADATA);
		assertEquals(0, c.getGroup().getAttributeStatements().length);
	}

	/**
	 * in child group an attribute is assigned by the statement. This attribute is disallowed by ac. 
	 * At the same time it is a condition attribute for the attribute in a parent group. 
	 * Both attributes should not be assigned.
	 * @throws Exception
	 */
	@Test
	public void testWithAC() throws Exception
	{
		setupStateForConditions();
		
		AttributeStatement statement1 = new EverybodyStatement(
				new StringAttribute("a1", "/A/D", AttributeVisibility.local, "any"), 
				ConflictResolution.skip);
		groupAD.setAttributeStatements(new AttributeStatement[] {statement1});
		groupsMan.updateGroup("/A/D", groupAD);
		
		AttributeStatement statement2 = new HasSubgroupAttributeStatement(
				new StringAttribute("a2", "/A", AttributeVisibility.local, "any"),
				new StringAttribute("a1", "/A/D", AttributeVisibility.local), 
				ConflictResolution.skip);
		groupA.setAttributeStatements(new AttributeStatement[] {statement2});
		groupsMan.updateGroup("/A", groupA);
		
		
		AttributesClass ac = new AttributesClass("ac1", "", Collections.singleton("a2"), 
				new HashSet<String>(), false, new HashSet<String>(0));
		attrsMan.addAttributeClass(ac);
		attrsMan.setEntityAttributeClasses(entity, "/A/D", Collections.singleton(ac.getName()));
		
		//              /  A  AB ABC AD AZ
//		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
//				0, 0, 0, 0,  0, 0); //a2
		
		Collection<AttributeExt<?>> aRet = attrsMan.getAllAttributes(entity, true, "/A", "a1", false);
		assertEquals(aRet.toString(), 1, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/A", "a2", false);
		assertEquals(aRet.toString(), 0, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/A", null, false);
		assertEquals(aRet.toString(), 1, aRet.size());
		
		aRet = attrsMan.getAllAttributes(entity, true, "/A/D", "a1", false);
		assertEquals(aRet.toString(), 0, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/A/D", "a2", false);
		assertEquals(aRet.toString(), 0, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/A/D", null, false);
		assertEquals(aRet.toString(), 1, aRet.size());
	}
	
	@Test
	public void testInvalidArgs() throws Exception
	{
		setupStateForConditions();
	
		AttributeStatement statement1 = new EverybodyStatement(
				new StringAttribute("a1", "/A/D", AttributeVisibility.local, "updated"), 
				ConflictResolution.skip);
		groupA.setAttributeStatements(new AttributeStatement[] {statement1});
		try
		{
			groupsMan.updateGroup("/A", groupA);
			fail("Managed to update group with wrong attribute");
		} catch (IllegalAttributeValueException e) {}
		
		AttributeStatement statement2 = new HasParentAttributeStatement(
				new StringAttribute("a2", "/A", AttributeVisibility.local, "va1"), 
				new StringAttribute("a1", "/A/D", AttributeVisibility.local, "foo"), 
				ConflictResolution.skip);
		groupA.setAttributeStatements(new AttributeStatement[] {statement2});
		try
		{
			groupsMan.updateGroup("/A", groupA);
			fail("Managed to update group with wrong attribute statement 1");
		} catch (IllegalAttributeValueException e) {}
		
		AttributeStatement statement3 = new HasSubgroupAttributeStatement(
				new StringAttribute("a2", "/A", AttributeVisibility.local, "va1"), 
				new StringAttribute("a1", "/A", AttributeVisibility.local, "foo"), 
				ConflictResolution.skip);
		groupA.setAttributeStatements(new AttributeStatement[] {statement3});
		try
		{
			groupsMan.updateGroup("/A", groupA);
			fail("Managed to update group with wrong attribute statement 2");
		} catch (IllegalAttributeValueException e) {}
		
		AttributeStatement statement4 = new EverybodyStatement(
				new StringAttribute(SystemAttributeTypes.CREDENTIAL_REQUIREMENTS, 
						"/A", AttributeVisibility.local, "foo"), 
				ConflictResolution.skip);
		groupA.setAttributeStatements(new AttributeStatement[] {statement4});
		try
		{
			groupsMan.updateGroup("/A", groupA);
			fail("Managed to update group with assignment of immutable attribute");
		} catch (IllegalAttributeTypeException e) {}

	}	
	
	@Test
	public void testCopyFromSubgroup() throws Exception
	{
		setupMockAuthn();
		
		AttributeType at = createSimpleAT("a1");
		attrsMan.addAttributeType(at);
		AttributeType at1 = createSimpleAT("a2");
		at1.setMaxElements(Integer.MAX_VALUE);
		attrsMan.addAttributeType(at1);
		
		groupA = new Group("/A");
		groupsMan.addGroup(groupA);
		
		groupAB = new Group("/A/B");
		groupsMan.addGroup(groupAB);

		Identity id = idsMan.addEntity(new IdentityParam(X500Identity.ID, "cn=golbi"), "crMock", 
				EntityState.disabled, false);
		entity = new EntityParam(id);
		groupsMan.addMemberFromParent("/A", entity);
		
		// added to /A for all having an "a1" attribute in /A/B. 
		AttributeStatement statement1 = new CopySubgroupAttributeStatement(
				new StringAttribute("a1", "/A/B", AttributeVisibility.local), 
				ConflictResolution.skip);
		Group groupR = groupsMan.getContents("/A", GroupContents.METADATA).getGroup();
		groupR.setAttributeStatements(new AttributeStatement[] {statement1});
		groupsMan.updateGroup("/A", groupR);
		
		AttributeStatement statement2 = new EverybodyStatement(
				new StringAttribute("a1", "/A/B", AttributeVisibility.local, "value"), 
				ConflictResolution.skip);
		Group groupR2 = groupsMan.getContents("/A/B", GroupContents.METADATA).getGroup();
		groupR2.setAttributeStatements(new AttributeStatement[] {statement2});
		groupsMan.updateGroup("/A/B", groupR2);
		
		Collection<AttributeExt<?>> aRet = attrsMan.getAllAttributes(entity, true, "/A", "a1", false);
		assertEquals(aRet.toString(), 0, aRet.size());
	}
	
	private void setupStateForConditions() throws Exception
	{
		setupMockAuthn();
		
		AttributeType at = createSimpleAT("a1");
		attrsMan.addAttributeType(at);
		AttributeType at1 = createSimpleAT("a2");
		at1.setMaxElements(Integer.MAX_VALUE);
		attrsMan.addAttributeType(at1);
		
		groupA = new Group("/A");
		groupsMan.addGroup(groupA);
		
		groupAB = new Group("/A/B");
		groupsMan.addGroup(groupAB);

		groupAD = new Group("/A/D");
		groupsMan.addGroup(groupAD);

		groupAZ = new Group("/A/Z");
		groupsMan.addGroup(groupAZ);

		Group groupAV = new Group("/A/V");
		groupsMan.addGroup(groupAV);
		
		groupABC = new Group("/A/B/C");
		groupsMan.addGroup(groupABC);
		
		Identity id = idsMan.addEntity(new IdentityParam(X500Identity.ID, "cn=golbi"), "crMock", 
				EntityState.disabled, false);
		entity = new EntityParam(id);
		groupsMan.addMemberFromParent("/A", entity);
		groupsMan.addMemberFromParent("/A/B", entity);
		groupsMan.addMemberFromParent("/A/Z", entity);
		groupsMan.addMemberFromParent("/A/D", entity);
		groupsMan.addMemberFromParent("/A/B/C", entity);
		
		attrsMan.setAttribute(entity, new StringAttribute("a1", "/A", AttributeVisibility.local, "va1"), false);
		attrsMan.setAttribute(entity, new StringAttribute("a1", "/A/B", AttributeVisibility.local, "va1"), false);
	}
	
	private AttributeType createSimpleAT(String name)
	{
		AttributeType at = new AttributeType();
		at.setValueType(new StringAttributeSyntax());
		at.setDescription(new I18nString("desc"));
		at.setFlags(0);
		at.setMaxElements(5);
		at.setMinElements(1);
		at.setName(name);
		at.setSelfModificable(true);
		at.setVisibility(AttributeVisibility.local);
		return at;
	}
	
	private void testCorrectness(int a1InRoot, int a1InA, int a1InAB, int a1InABC, int a1InAD, int a1InAZ,
			int a2InRoot, int a2InA, int a2InAB, int a2InABC, int a2InAD, int a2InAZ) throws Exception
	{
		Collection<AttributeExt<?>> aRet = attrsMan.getAllAttributes(entity, true, "/", "a1", false);
		assertEquals(aRet.toString(), a1InRoot, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/", "a2", false);
		assertEquals(aRet.toString(), a2InRoot, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/", null, false);
		assertEquals(aRet.toString(), a2InRoot+a1InRoot+systemAttributes, aRet.size());
		
		aRet = attrsMan.getAllAttributes(entity, true, "/A", "a1", false);
		assertEquals(aRet.toString(), a1InA, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/A", "a2", false);
		assertEquals(aRet.toString(), a2InA, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/A", null, false);
		assertEquals(aRet.toString(), a1InA+a2InA, aRet.size());
		
		aRet = attrsMan.getAllAttributes(entity, true, "/A/B", "a1", false);
		assertEquals(aRet.toString(), a1InAB, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/A/B", "a2", false);
		assertEquals(aRet.toString(), a2InAB, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/A/B", null, false);
		assertEquals(aRet.toString(), a1InAB+a2InAB, aRet.size());

		aRet = attrsMan.getAllAttributes(entity, true, "/A/B/C", "a1", false);
		assertEquals(aRet.toString(), a1InABC, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/A/B/C", "a2", false);
		assertEquals(aRet.toString(), a2InABC, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/A/B/C", null, false);
		assertEquals(aRet.toString(), a1InABC+a2InABC, aRet.size());

		aRet = attrsMan.getAllAttributes(entity, true, "/A/D", "a1", false);
		assertEquals(aRet.toString(), a1InAD, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/A/D", "a2", false);
		assertEquals(aRet.toString(), a2InAD, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/A/D", null, false);
		assertEquals(aRet.toString(), a1InAD+a2InAD, aRet.size());
		
		aRet = attrsMan.getAllAttributes(entity, true, "/A/Z", "a1", false);
		assertEquals(aRet.toString(), a1InAZ, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/A/Z", "a2", false);
		assertEquals(aRet.toString(), a2InAZ, aRet.size());
		aRet = attrsMan.getAllAttributes(entity, true, "/A/Z", null, false);
		assertEquals(aRet.toString(), a1InAZ+a2InAZ, aRet.size());
		
		aRet = attrsMan.getAllAttributes(entity, true, null, null, false);
		assertEquals(aRet.toString(), a1InRoot+a1InA+a1InAB+a1InABC+a1InAD+a1InAZ+
				a2InRoot+a2InA+a2InAB+a2InABC+a2InAD+a2InAZ+systemAttributes, aRet.size());

		aRet = attrsMan.getAllAttributes(entity, true, null, "a2", false);
		assertEquals(aRet.toString(), a2InRoot+a2InA+a2InAB+a2InABC+a2InAD+a2InAZ, aRet.size());
		
		aRet = attrsMan.getAllAttributes(entity, true, null, "a1", false);
		assertEquals(aRet.toString(), a1InRoot+a1InA+a1InAB+a1InABC+a1InAD+a1InAZ, aRet.size());
	}
}



















