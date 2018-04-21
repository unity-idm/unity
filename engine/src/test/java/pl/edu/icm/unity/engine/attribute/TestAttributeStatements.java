/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.credential.CredentialAttributeTypeProvider;
import pl.edu.icm.unity.engine.group.AttributeStatementsCleaner;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

public class TestAttributeStatements extends DBIntegrationTestBase
{
	@Autowired
	protected AttributeClassManagement acMan;
	@Autowired
	private AttributeStatementsCleaner statementsCleaner;
	
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
	public void fixedAttributeIsAssigned() throws Exception
	{
		setupStateForConditions();
		
		// test with one statement added to /A for everybody
		groupA.setAttributeStatements(new AttributeStatement[] {
				AttributeStatement.getFixedEverybodyStatement(
				StringAttribute.of("a2", "/A", "va1"))});
		groupsMan.updateGroup("/A", groupA);

		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 1, 0, 0,  0, 0); //a2
	}
	
	@Test
	public void dynamicAttributeIsAssigned() throws Exception
	{
		setupStateForConditions();
		AttributeStatement statement0 = new AttributeStatement(
				"true", 
				"/A", 
				ConflictResolution.skip, 
				"a2", 
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
		AttributeStatement statement0 = new AttributeStatement(
				"eattr == empty && eattrs == null && " + 
				"attr['a1'] != null && attrs['a1'] != null && " +
				"idsByType.size() > 0 && " +
				"groupName == '/A/B' && groups.size() > 1 && entityId != null", 
				null, 
				ConflictResolution.skip,
				StringAttribute.of("a2", "/A/B", "updated"));
		setStatments(groupAB, statement0);
		
		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 0, 1, 0,  0, 0); //a2
	}

	@Test
	public void downwardsDynamicAttributesAreAvailableInContext() throws Exception
	{
		setupStateForConditions();
		AttributeStatement statementOfABC = new AttributeStatement(
				"true",
				null, 
				ConflictResolution.skip,
				StringAttribute.of("a2", "/A/B/C", "updated"));
		setStatments(groupABC, statementOfABC);
		AttributeStatement statementOfAB = new AttributeStatement(
				"eattr['a2'] != null",
				"/A/B/C", 
				ConflictResolution.skip, 
				"a2", 
				"eattr['a2']");
		setStatments(groupAB, statementOfAB);

		AttributeStatement statementOfRoot = new AttributeStatement(
				"eattr['a2'] != null && eattrs['a2'] != null",
				"/A/B", 
				ConflictResolution.skip, 
				"a2", "eattr['a2']");
		setStatments(new Group("/"), statementOfRoot);
		
		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				1, 0, 1, 1,  0, 0); //a2
	}
	
	@Test
	public void upwardsDynamicAttributesAreAvailableInContext() throws Exception
	{
		setupStateForConditions();
		AttributeStatement statementOfRoot = new AttributeStatement(
				"true",
				null, 
				ConflictResolution.skip,
				StringAttribute.of("a2", "/", "updated"));
		setStatments(new Group("/"), statementOfRoot);

		
		AttributeStatement statementOfAB = new AttributeStatement(
				"eattr['a2'] != null",
				"/", 
				ConflictResolution.skip, 
				"a2", 
				"eattr['a2']");
		setStatments(groupAB, statementOfAB);

		AttributeStatement statementOfABC = new AttributeStatement(
				"eattr['a2'] != null && eattrs['a2'] != null",
				"/A/B", 
				ConflictResolution.skip,
				"a2", "eattr['a2']");
		setStatments(groupABC, statementOfABC);
		
		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				1, 0, 1, 1,  0, 0); //a2
	}
	
	@Test
	public void cyclesAreNotFollowed() throws Exception
	{
		setupStateForConditions();
		
		AttributeStatement statementOfA = new AttributeStatement(
				"eattr['a2'] != null",
				"/A/B", 
				ConflictResolution.skip,
				"a2", 
				"'foo'");
		setStatments(groupA, statementOfA);

		AttributeStatement statementOfAB = new AttributeStatement(
				"eattr['a2'] != null",
				"/A", 
				ConflictResolution.skip, 
				"a2", "'bar'");
		setStatments(groupAB, statementOfAB);
		
		//              /  A  AB ABC AD AZ
		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
				0, 0, 0, 0,  0, 0); //a2
	}
	
	@Test
	public void testConflictResolution() throws Exception
	{
		setupStateForConditions();
		Collection<AttributeExt> aRet;

		// overwrite direct (-> should skip)
		AttributeStatement statement0 = new AttributeStatement("true", null, 
				ConflictResolution.overwrite,
				StringAttribute.of("a1", "/A/B", "updated"));
		
		setStatments(groupAB, statement0);
		aRet = attrsMan.getAllAttributes(entity, true, "/A/B", "a1", false);
		assertEquals(1, aRet.size());
		assertEquals(1, aRet.iterator().next().getValues().size());
		assertEquals("va1", aRet.iterator().next().getValues().get(0));
	
		// skip
		AttributeStatement statement1 = new AttributeStatement("true", null, 
				ConflictResolution.skip,
				StringAttribute.of("a2", "/A/B", "base"));
		setStatments(groupAB, statement1);
		aRet = attrsMan.getAllAttributes(entity, true, "/A/B", "a2", false);
		assertEquals(1, aRet.size());
		assertEquals(1, aRet.iterator().next().getValues().size());
		assertEquals("base", aRet.iterator().next().getValues().get(0));
		
		//skip x2
		AttributeStatement statement11 = new AttributeStatement("true", null, 
				ConflictResolution.skip,
				StringAttribute.of("a2", "/A/B", "updated"));
		setStatments(groupAB, statement1, statement11);
		aRet = attrsMan.getAllAttributes(entity, true, "/A/B", "a2", false);
		assertEquals(1, aRet.size());
		assertEquals(1, aRet.iterator().next().getValues().size());
		assertEquals("base", aRet.iterator().next().getValues().get(0));

		// skip and then overwrite
		AttributeStatement statement2 = new AttributeStatement("true", null, 
				ConflictResolution.overwrite,
				StringAttribute.of("a2", "/A/B", "updated2"));
		groupAB.setAttributeStatements(new AttributeStatement[] {statement1, statement11, statement2});
		groupsMan.updateGroup("/A/B", groupAB);
		
		aRet = attrsMan.getAllAttributes(entity, true, "/A/B", "a2", false);
		assertEquals(1, aRet.size());
		assertEquals(1, aRet.iterator().next().getValues().size());
		assertEquals("updated2", aRet.iterator().next().getValues().get(0));
		
		// skip, overwrite, merge which should skip
		AttributeStatement statement31 = new AttributeStatement("true", null, 
				ConflictResolution.merge,
				StringAttribute.of("a1", "/A/D", "base"));
		AttributeStatement statement32 = new AttributeStatement("true", null, 
				ConflictResolution.merge,
				StringAttribute.of("a1", "/A/D", "merge"));
		groupAD.setAttributeStatements(new AttributeStatement[] {statement31, statement32});
		groupsMan.updateGroup("/A/D", groupAD);
		
		aRet = attrsMan.getAllAttributes(entity, true, "/A/D", "a1", false);
		assertEquals(1, aRet.size());
		assertEquals(1, aRet.iterator().next().getValues().size());
		assertEquals("base", aRet.iterator().next().getValues().get(0));

		//add two rules to test merge working
		AttributeStatement statement4 = new AttributeStatement("true", null, 
				ConflictResolution.skip,
				StringAttribute.of("a2", "/A/B", "merge1"));
		AttributeStatement statement5 = new AttributeStatement("true", null, 
				ConflictResolution.merge,
				StringAttribute.of("a2", "/A/B", "merge2"));
		groupAB.setAttributeStatements(new AttributeStatement[] {statement4, statement5});
		groupsMan.updateGroup("/A/B", groupAB);

		aRet = attrsMan.getAllAttributes(entity, true, "/A/B", "a2", false);
		assertEquals(1, aRet.size());
		assertEquals(2, aRet.iterator().next().getValues().size());
		assertEquals("merge1", aRet.iterator().next().getValues().get(0));
		assertEquals("merge2", aRet.iterator().next().getValues().get(1));
		
		//additionally add a direct attribute
		attrsMan.createAttribute(entity, StringAttribute.of("a2", "/A/B", "direct"));
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
		
		AttributeStatement statement1 = new AttributeStatement("true", null, ConflictResolution.overwrite,
				StringAttribute.of("a1", "/A", "updated"));
		AttributeStatement statement3 = new AttributeStatement("eattr['a1'] != null", "/A/B", 
				ConflictResolution.skip,
				StringAttribute.of("a2", "/A", "va1"));
		groupA.setAttributeStatements(new AttributeStatement[] {statement1, statement3});
		groupsMan.updateGroup("/A", groupA);
		
		statementsCleaner.updateGroups();
		
		assertEquals(2, getGroupstatements("/A").length);
		
		aTypeMan.removeAttributeType("a1", true);
		assertEquals(2, getGroupstatements("/A").length);
		
		statementsCleaner.updateGroups();
		assertEquals(1, getGroupstatements("/A").length);
		
		groupsMan.removeGroup("/A/B", true);
		statementsCleaner.updateGroups();
		assertEquals(0, getGroupstatements("/A").length);
	}

	private AttributeStatement[] getGroupstatements(String group) throws EngineException
	{
		return groupsMan.getContents(group, GroupContents.METADATA).getGroup().getAttributeStatements();
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
		
		AttributeStatement statement1 = AttributeStatement.getFixedEverybodyStatement(
				StringAttribute.of("a1", "/A/D", "any"));
		groupAD.setAttributeStatements(new AttributeStatement[] {statement1});
		groupsMan.updateGroup("/A/D", groupAD);
		
		AttributeStatement statement2 = new AttributeStatement("eattr['a1'] != null", "/A/D",
				ConflictResolution.skip, 
				StringAttribute.of("a2", "/A", "any"));
		groupA.setAttributeStatements(new AttributeStatement[] {statement2});
		groupsMan.updateGroup("/A", groupA);
		
		
		AttributesClass ac = new AttributesClass("ac1", "", Collections.singleton("a2"), 
				new HashSet<String>(), false, new HashSet<String>(0));
		acMan.addAttributeClass(ac);
		acMan.setEntityAttributeClasses(entity, "/A/D", Collections.singleton(ac.getName()));
		
		//              /  A  AB ABC AD AZ
//		testCorrectness(0, 1, 1, 0,  0, 0,  //a1
//				0, 0, 0, 0,  0, 0); //a2
		
		Collection<AttributeExt> aRet = attrsMan.getAllAttributes(entity, true, "/A", "a1", false);
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
	
		AttributeStatement statement1 = AttributeStatement.getFixedEverybodyStatement(
				StringAttribute.of("a1", "/A/D", "updated"));
		groupA.setAttributeStatements(new AttributeStatement[] {statement1});
		try
		{
			groupsMan.updateGroup("/A", groupA);
			fail("Managed to update group with wrong attribute");
		} catch (IllegalAttributeValueException e) {}
		
		AttributeStatement statement3 = new AttributeStatement("eattr['a1'] != null", "/A", 
				ConflictResolution.skip, "a1", null);
		groupA.setAttributeStatements(new AttributeStatement[] {statement3});
		try
		{
			groupsMan.updateGroup("/A", groupA);
			fail("Managed to update group with attribute statement without expression");
		} catch (IllegalAttributeValueException e) {}
		
		AttributeStatement statement4 = AttributeStatement.getFixedEverybodyStatement(
				StringAttribute.of(CredentialAttributeTypeProvider.CREDENTIAL_REQUIREMENTS, 
						"/A", "foo"));
		groupA.setAttributeStatements(new AttributeStatement[] {statement4});
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

		AttributeStatement statement1 = AttributeStatement.getFixedEverybodyStatement(
				StringAttribute.of("a2", "/A/B", "VV"));
		groupAB.setAttributeStatements(new AttributeStatement[] {statement1});
		groupsMan.updateGroup("/A/B", groupAB);


		AttributeStatement statement2 = new AttributeStatement("eattrs contains 'a2'", "/A/B",
				ConflictResolution.skip,
				StringAttribute.of("a2", "/A", "NEW"));
		groupA.setAttributeStatements(new AttributeStatement[] {statement2});
		groupsMan.updateGroup("/A", groupA);


		Collection<AttributeExt> aRet = attrsMan.getAllAttributes(entity2, true, "/A", "a2", false);
		assertThat(aRet.isEmpty(), is(true));
		Collection<AttributeExt> aRet2 = attrsMan.getAllAttributes(entity, true, "/A", "a2", false);
		assertThat(aRet2.size(), is(1));
	}

	
	private void setupStateForConditions() throws Exception
	{
		setupMockAuthn();
		
		AttributeType at1 = createSimpleAT("a1");
		aTypeMan.addAttributeType(at1);
		AttributeType at2 = createSimpleAT("a2");
		at2.setMaxElements(Integer.MAX_VALUE);
		aTypeMan.addAttributeType(at2);
		
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
		
		attrsMan.createAttribute(entity, StringAttribute.of("a1", "/A", "va1"));
		attrsMan.createAttribute(entity, StringAttribute.of("a1", "/A/B", "va1"));
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
	
	private void testCorrectness(int a1InRoot, int a1InA, int a1InAB, int a1InABC, int a1InAD, int a1InAZ,
			int a2InRoot, int a2InA, int a2InAB, int a2InABC, int a2InAD, int a2InAZ) throws Exception
	{
		Collection<AttributeExt> aRet = attrsMan.getAllAttributes(entity, true, "/", "a1", false);
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
	
	private void setStatments(Group group, AttributeStatement... statements) throws EngineException
	{
		group.setAttributeStatements(statements);
		groupsMan.updateGroup(group.toString(), group);
	}
}



















