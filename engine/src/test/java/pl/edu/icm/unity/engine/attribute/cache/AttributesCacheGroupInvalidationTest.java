/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.attribute.AttributeStatement;
import pl.edu.icm.unity.base.attribute.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;

/**
 * Checks exactly which (entity, group) pairs get invalidated by changes of group definitions.
 */
public class AttributesCacheGroupInvalidationTest extends AttributesCacheIntegrationTestBase
{
	private long inAandAB;
	private long inAOnly;
	private long unrelated;
	private Group groupA;
	private Group groupAB;

	@BeforeEach
	public void setup() throws Exception
	{
		aTypeMan.addAttributeType(new AttributeType("a1", StringAttributeSyntax.ID));
		groupA = new Group("/A");
		groupsMan.addGroup(groupA);
		groupAB = new Group("/A/B");
		groupsMan.addGroup(groupAB);
		groupsMan.addGroup(new Group("/C"));
		inAandAB = addEntity("e1", "/A", "/A/B");
		inAOnly = addEntity("e2", "/A");
		unrelated = addEntity("e3", "/C");
		clearAllPending();
	}

	@Test
	public void addingSubgroupStatementInvalidatesOnlyItsMembersInAllTheirGroups() throws Exception
	{
		setStatements(groupAB, fixedStatement("/A/B"));

		assertThat(pendingPairs()).containsExactlyInAnyOrder(
				pair(inAandAB, "/"), pair(inAandAB, "/A"), pair(inAandAB, "/A/B"));
	}

	@Test
	public void changingParentStatementInvalidatesSubgroupResultsOfItsMembers() throws Exception
	{
		// /A/B statement depends on /A (upwards), so a change in /A matters for /A/B results
		setStatements(groupAB, new AttributeStatement("eattr['a1'] != null", "/A", ConflictResolution.skip,
				"a1", "'fromParent'"));
		clearAllPending();

		setStatements(groupA, fixedStatement("/A"));

		assertThat(pendingPairs()).containsExactlyInAnyOrder(
				pair(inAandAB, "/"), pair(inAandAB, "/A"), pair(inAandAB, "/A/B"),
				pair(inAOnly, "/"), pair(inAOnly, "/A"));
	}

	@Test
	public void removingSubgroupStatementInvalidatesParentResultsOfItsMembers() throws Exception
	{
		// /A statement depends on /A/B (downwards), so a change in /A/B matters for /A results
		setStatements(groupA, new AttributeStatement("eattr['a1'] != null", "/A/B", ConflictResolution.skip,
				"a1", "'fromSubgroup'"));
		setStatements(groupAB, fixedStatement("/A/B"));
		clearAllPending();

		setStatements(groupAB);

		assertThat(pendingPairs()).containsExactlyInAnyOrder(
				pair(inAandAB, "/"), pair(inAandAB, "/A"), pair(inAandAB, "/A/B"));
		assertThat(pendingPairs()).noneMatch(p -> p.entityId() == inAOnly || p.entityId() == unrelated);
	}

	private AttributeStatement fixedStatement(String group)
	{
		return AttributeStatement.getFixedEverybodyStatement(StringAttribute.of("a1", group, "fixed"));
	}

	private void setStatements(Group group, AttributeStatement... statements) throws Exception
	{
		group.setAttributeStatements(statements);
		groupsMan.updateGroup(group.getPathEncoded(), group);
	}
}
