/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.hamcrest.CoreMatchers.is;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeStatement2;
import pl.edu.icm.unity.types.basic.AttributeStatement2.ConflictResolution;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

public class TestAttributeStatements extends DBIntegrationTestBase
{
	private final int systemAttributes = 1;
	private EntityParam entity;
	private Group groupA;
	private Group groupAB;
	private Group groupAD;
	private Group groupAZ;
	private Group groupABC;
	private AttributeType at1;
	private AttributeType at2;
	
	
	
	
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
	public void fixedAttributeIsAssigned() throws Exception
	{
		setupStateForConditions();
		
		// test with one statement added to /A for everybody
		groupA.setAttributeStatements(new AttributeStatement2[] {
				AttributeStatement2.getFixedEverybodyStatement(
				new StringAttribute("a2", "/A", AttributeVisibility.local, "va1"))});
		groupsMan.updateGroup("/A", groupA);

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 1, 0, 0,  0, 0); //a2
	}
	
	@Test
	public void dynamicAttributeIsAssigned() throws Exception
	{
		setupStateForConditions();
		AttributeStatement2 statement0 = new AttributeStatement2(
				"true", 
				"/A", 
				ConflictResolution.skip, AttributeVisibility.full,
				at2, 
				"attrs['a1']");
		setStatments(groupAB, statement0);
		
		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 0, 1, 0,  0, 0); //a2
	}

	@Test
	public void mvelContextIsComplete() throws Exception
	{
		setupStateForConditions();
		AttributeStatement2 statement0 = new AttributeStatement2(
				"eattr == empty && eattrs == null && " + 
				"attr['a1'] != null && attrs['a1'] != null && " +
				"idsByType.size() > 0 && " +
				"groupName == '/A/B' && groups.size() > 1 && entityId != null", 
				null, 
				ConflictResolution.skip,
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "updated"));
		setStatments(groupAB, statement0);
		
		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 0, 1, 0,  0, 0); //a2
	}

	@Test
	public void downwardsDynamicAttributesAreAvailableInContext() throws Exception
	{
		setupStateForConditions();
		AttributeStatement2 statementOfABC = new AttributeStatement2(
				"true",
				null, 
				ConflictResolution.skip,
				new StringAttribute("a2", "/A/B/C", AttributeVisibility.local, "updated"));
		setStatments(groupABC, statementOfABC);
		AttributeStatement2 statementOfAB = new AttributeStatement2(
				"eattr['a2'] != null",
				"/A/B/C", 
				ConflictResolution.skip, AttributeVisibility.local,
				at2, 
				"eattr['a2']");
		setStatments(groupAB, statementOfAB);

		AttributeStatement2 statementOfRoot = new AttributeStatement2(
				"eattr['a2'] != null && eattrs['a2'] != null",
				"/A/B", 
				ConflictResolution.skip, AttributeVisibility.local,
				at2, "eattr['a2']");
		setStatments(new Group("/"), statementOfRoot);
		
		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				1, 0, 1, 1,  0, 0); //a2
	}
	
	@Test
	public void upwardsDynamicAttributesAreAvailableInContext() throws Exception
	{
		setupStateForConditions();
		AttributeStatement2 statementOfRoot = new AttributeStatement2(
				"true",
				null, 
				ConflictResolution.skip,
				new StringAttribute("a2", "/", AttributeVisibility.local, "updated"));
		setStatments(new Group("/"), statementOfRoot);

		
		AttributeStatement2 statementOfAB = new AttributeStatement2(
				"eattr['a2'] != null",
				"/", 
				ConflictResolution.skip, AttributeVisibility.local,
				at2, 
				"eattr['a2']");
		setStatments(groupAB, statementOfAB);

		AttributeStatement2 statementOfABC = new AttributeStatement2(
				"eattr['a2'] != null && eattrs['a2'] != null",
				"/A/B", 
				ConflictResolution.skip, AttributeVisibility.local,
				at2, "eattr['a2']");
		setStatments(groupABC, statementOfABC);
		
		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				1, 0, 1, 1,  0, 0); //a2
	}
	
	@Test
	public void cyclesAreNotFollowed() throws Exception
	{
		setupStateForConditions();
		
		AttributeStatement2 statementOfA = new AttributeStatement2(
				"eattr['a2'] != null",
				"/A/B", 
				ConflictResolution.skip, AttributeVisibility.local,
				at2, 
				"'foo'");
		setStatments(groupA, statementOfA);

		AttributeStatement2 statementOfAB = new AttributeStatement2(
				"eattr['a2'] != null",
				"/A", 
				ConflictResolution.skip, AttributeVisibility.local,
				at2, "'bar'");
		setStatments(groupAB, statementOfAB);
		
		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 0, 0, 0,  0, 0); //a2
	}
	
	@Test
	public void testConflictResolution() throws Exception
	{
		setupStateForConditions();
		Collection<AttributeExt<?>> aRet;

		// overwrite direct (-> should skip)
		AttributeStatement2 statement0 = new AttributeStatement2("true", null, 
				ConflictResolution.overwrite,
				new StringAttribute("a1", "/A/B", AttributeVisibility.local, "updated"));
		
		setStatments(groupAB, statement0);
		aRet = attrsMan.getAllAttributes(entity, true, "/A/B", "a1", false);
		assertEquals(1, aRet.size());
		assertEquals(1, aRet.iterator().next().getValues().size());
		assertEquals("va1", aRet.iterator().next().getValues().get(0));
	
		// skip
		AttributeStatement2 statement1 = new AttributeStatement2("true", null, 
				ConflictResolution.skip,
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "base"));
		setStatments(groupAB, statement1);
		aRet = attrsMan.getAllAttributes(entity, true, "/A/B", "a2", false);
		assertEquals(1, aRet.size());
		assertEquals(1, aRet.iterator().next().getValues().size());
		assertEquals("base", aRet.iterator().next().getValues().get(0));
		
		//skip x2
		AttributeStatement2 statement11 = new AttributeStatement2("true", null, 
				ConflictResolution.skip,
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "updated"));
		setStatments(groupAB, statement1, statement11);
		aRet = attrsMan.getAllAttributes(entity, true, "/A/B", "a2", false);
		assertEquals(1, aRet.size());
		assertEquals(1, aRet.iterator().next().getValues().size());
		assertEquals("base", aRet.iterator().next().getValues().get(0));

		// skip and then overwrite
		AttributeStatement2 statement2 = new AttributeStatement2("true", null, 
				ConflictResolution.overwrite,
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "updated2"));
		groupAB.setAttributeStatements(new AttributeStatement2[] {statement1, statement11, statement2});
		groupsMan.updateGroup("/A/B", groupAB);
		
		aRet = attrsMan.getAllAttributes(entity, true, "/A/B", "a2", false);
		assertEquals(1, aRet.size());
		assertEquals(1, aRet.iterator().next().getValues().size());
		assertEquals("updated2", aRet.iterator().next().getValues().get(0));
		
		// skip, overwrite, merge which should skip
		AttributeStatement2 statement31 = new AttributeStatement2("true", null, 
				ConflictResolution.merge,
				new StringAttribute("a1", "/A/D", AttributeVisibility.local, "base"));
		AttributeStatement2 statement32 = new AttributeStatement2("true", null, 
				ConflictResolution.merge,
				new StringAttribute("a1", "/A/D", AttributeVisibility.local, "merge"));
		groupAD.setAttributeStatements(new AttributeStatement2[] {statement31, statement32});
		groupsMan.updateGroup("/A/D", groupAD);
		
		aRet = attrsMan.getAllAttributes(entity, true, "/A/D", "a1", false);
		assertEquals(1, aRet.size());
		assertEquals(1, aRet.iterator().next().getValues().size());
		assertEquals("base", aRet.iterator().next().getValues().get(0));

		//add two rules to test merge working
		AttributeStatement2 statement4 = new AttributeStatement2("true", null, 
				ConflictResolution.skip,
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "merge1"));
		AttributeStatement2 statement5 = new AttributeStatement2("true", null, 
				ConflictResolution.merge,
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "merge2"));
		groupAB.setAttributeStatements(new AttributeStatement2[] {statement4, statement5});
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
		
		AttributeStatement2 statement1 = new AttributeStatement2("true", null, ConflictResolution.overwrite,
				new StringAttribute("a1", "/A", AttributeVisibility.local, "updated"));
		AttributeStatement2 statement3 = new AttributeStatement2("eattr['a1'] != null", "/A/B", 
				ConflictResolution.skip,
				new StringAttribute("a2", "/A", AttributeVisibility.local, "va1"));
		groupA.setAttributeStatements(new AttributeStatement2[] {statement1, statement3});
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
		
		AttributeStatement2 statement1 = AttributeStatement2.getFixedEverybodyStatement(
				new StringAttribute("a1", "/A/D", AttributeVisibility.local, "any"));
		groupAD.setAttributeStatements(new AttributeStatement2[] {statement1});
		groupsMan.updateGroup("/A/D", groupAD);
		
		AttributeStatement2 statement2 = new AttributeStatement2("eattr['a1'] != null", "/A/D",
				ConflictResolution.skip, 
				new StringAttribute("a2", "/A", AttributeVisibility.local, "any"));
		groupA.setAttributeStatements(new AttributeStatement2[] {statement2});
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
	
		AttributeStatement2 statement1 = AttributeStatement2.getFixedEverybodyStatement(
				new StringAttribute("a1", "/A/D", AttributeVisibility.local, "updated"));
		groupA.setAttributeStatements(new AttributeStatement2[] {statement1});
		try
		{
			groupsMan.updateGroup("/A", groupA);
			fail("Managed to update group with wrong attribute");
		} catch (IllegalAttributeValueException e) {}
		
		AttributeStatement2 statement3 = new AttributeStatement2("eattr['a1'] != null", "/A", 
				ConflictResolution.skip, AttributeVisibility.full, at1, null);
		groupA.setAttributeStatements(new AttributeStatement2[] {statement3});
		try
		{
			groupsMan.updateGroup("/A", groupA);
			fail("Managed to update group with attribute statement without expression");
		} catch (IllegalAttributeValueException e) {}
		
		AttributeStatement2 statement4 = AttributeStatement2.getFixedEverybodyStatement(
				new StringAttribute(SystemAttributeTypes.CREDENTIAL_REQUIREMENTS, 
						"/A", AttributeVisibility.local, "foo"));
		groupA.setAttributeStatements(new AttributeStatement2[] {statement4});
		try
		{
			groupsMan.updateGroup("/A", groupA);
			fail("Managed to update group with assignment of immutable attribute");
		} catch (IllegalAttributeTypeException e) {}

	}	

	
	@Test
	public void onlyOneEntityGetsAttributeCopiedFromSubgroupIfAssignedWithStatementInSubgroup() throws Exception
	{
		setupStateForConditions();
		Identity id2 = idsMan.addEntity(new IdentityParam(X500Identity.ID, "cn=golbi2"), "crMock", 
				EntityState.disabled, false);
		EntityParam entity2 = new EntityParam(id2);
		groupsMan.addMemberFromParent("/A", entity2);
		
		AttributeStatement2 statement1 = AttributeStatement2.getFixedEverybodyStatement(
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "VV"));
		groupAB.setAttributeStatements(new AttributeStatement2[] {statement1});
		groupsMan.updateGroup("/A/B", groupAB);
		
		
		AttributeStatement2 statement2 = new AttributeStatement2("eattrs contains 'a2'", "/A/B",
				ConflictResolution.skip, 
				new StringAttribute("a2", "/A", AttributeVisibility.local, "NEW"));
		groupA.setAttributeStatements(new AttributeStatement2[] {statement2});
		groupsMan.updateGroup("/A", groupA);
		
		
		Collection<AttributeExt<?>> aRet2 = attrsMan.getAllAttributes(entity, true, "/A", "a2", false);
		assertThat(aRet2.size(), is(1));
		Collection<AttributeExt<?>> aRet = attrsMan.getAllAttributes(entity2, true, "/A", "a2", false);
		assertThat(aRet.isEmpty(), is(true));
	}

	
	private void setupStateForConditions() throws Exception
	{
		setupMockAuthn();
		
		at1 = createSimpleAT("a1");
		attrsMan.addAttributeType(at1);
		at2 = createSimpleAT("a2");
		at2.setMaxElements(Integer.MAX_VALUE);
		attrsMan.addAttributeType(at2);
		
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
	
	private void setStatments(Group group, AttributeStatement2... statements) throws EngineException
	{
		group.setAttributeStatements(statements);
		groupsMan.updateGroup(group.toString(), group);
	}
}



















