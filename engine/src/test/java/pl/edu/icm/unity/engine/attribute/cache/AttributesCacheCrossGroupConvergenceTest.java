/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.attribute.AttributeStatement;
import pl.edu.icm.unity.base.attribute.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.engine.api.attributes.AttributeCacheValidationReport;
import pl.edu.icm.unity.engine.api.attributes.AttributeCacheValidationService;
import pl.edu.icm.unity.engine.api.attributes.GroupValidationReport;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;

/**
 * Real statements depending on other groups, with the background refresh running: after a change of the source
 * group's statements, the dependent group's cache must converge to the live evaluation.
 */
public class AttributesCacheCrossGroupConvergenceTest extends AttributesCacheIntegrationTestBase
{
	@Autowired
	private AttributeCacheValidationService validationService;

	private Group groupA;
	private Group groupAB;
	private long entity;
	private long notInSubgroup;

	@BeforeEach
	public void setup() throws Exception
	{
		aTypeMan.addAttributeType(new AttributeType("source", StringAttributeSyntax.ID));
		aTypeMan.addAttributeType(new AttributeType("derived", StringAttributeSyntax.ID));
		groupA = new Group("/A");
		groupsMan.addGroup(groupA);
		groupAB = new Group("/A/B");
		groupsMan.addGroup(groupAB);
		entity = addEntity("e1", "/A", "/A/B");
		notInSubgroup = addEntity("e2", "/A");
		startBackgroundRefresh();
	}

	@Test
	public void parentResultsFollowChangesOfSubgroupStatements() throws Exception
	{
		setStatements(groupA, new AttributeStatement("eattr['source'] != null", "/A/B", ConflictResolution.skip,
				"derived", "eattr['source'] + '-fromSubgroup'"));

		setStatements(groupAB, fixedStatement("source", "/A/B", "v1"));
		awaitFreshCachedValues(entity, "/A", "derived", List.of("v1-fromSubgroup"));

		setStatements(groupAB, fixedStatement("source", "/A/B", "v2"));
		awaitFreshCachedValues(entity, "/A", "derived", List.of("v2-fromSubgroup"));

		setStatements(groupAB);
		awaitFreshCachedValues(entity, "/A", "derived", null);

		awaitFreshCachedValues(notInSubgroup, "/A", "derived", null);
		assertCacheMatchesLiveEvaluation();
	}

	@Test
	public void subgroupResultsFollowChangesOfParentStatements() throws Exception
	{
		setStatements(groupAB, new AttributeStatement("eattr['source'] != null", "/A", ConflictResolution.skip,
				"derived", "eattr['source'] + '-fromParent'"));

		setStatements(groupA, fixedStatement("source", "/A", "v1"));
		awaitFreshCachedValues(entity, "/A/B", "derived", List.of("v1-fromParent"));

		setStatements(groupA, fixedStatement("source", "/A", "v2"));
		awaitFreshCachedValues(entity, "/A/B", "derived", List.of("v2-fromParent"));

		setStatements(groupA);
		awaitFreshCachedValues(entity, "/A/B", "derived", null);

		assertCacheMatchesLiveEvaluation();
	}

	private void assertCacheMatchesLiveEvaluation()
	{
		Awaitility.await().pollInSameThread().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
		{
			AttributeCacheValidationReport report = validationService.validate(Optional.empty());
			assertThat(report.groups()).allSatisfy(this::assertConsistent);
		});
	}

	private void assertConsistent(GroupValidationReport group)
	{
		assertThat(group.entitiesWithPendingUpdate()).as("pending in %s", group.group()).isEmpty();
		assertThat(group.entitiesWithMismatches()).as("mismatches in %s", group.group()).isEmpty();
	}

	private AttributeStatement fixedStatement(String attribute, String group, String value)
	{
		return AttributeStatement.getFixedEverybodyStatement(StringAttribute.of(attribute, group, value));
	}

	private void setStatements(Group group, AttributeStatement... statements) throws Exception
	{
		group.setAttributeStatements(statements);
		groupsMan.updateGroup(group.getPathEncoded(), group);
	}
}
