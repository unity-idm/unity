/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.groups;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.junit.Test;

import pl.edu.icm.unity.base.attribute.AttributeStatement;
import pl.edu.icm.unity.base.attribute.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.group.GroupProperty;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.store.MapperWithMinimalTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.store.types.common.DBI18nString;

public class GroupMapperTest extends MapperWithMinimalTestBase<Group, DBGroup>
{
	@Override
	protected Group getFullAPIObject()
	{
		Group group = new Group("/A/B/C");
		group.setAttributesClasses(Set.of("attrClass1", "attrClass2"));
		group.setDescription(new I18nString("desc"));
		group.setDisplayedName(new I18nString("groupDisp"));
		group.setPublic(false);
		group.setDelegationConfiguration(new GroupDelegationConfiguration(true, false, "logo", "regForm", "enqForm",
				"enqForm2", List.of("at1")));
		group.setAttributeStatements(new AttributeStatement[]
		{ new AttributeStatement("true", "/extra", ConflictResolution.merge, "name", "a") });
		group.setProperties(List.of(new GroupProperty("key", "value")));
		return group;
	}

	@Override
	protected DBGroup getFullDBObject()
	{
		return DBGroup.builder()
				.withPath("/A/B/C")
				.withAttributesClasses(Set.of("attrClass1", "attrClass2"))
				.withAttributeStatements(new DBAttributeStatement[]
				{ DBAttributeStatement.builder()
						.withCondition("true")
						.withResolution("merge")
						.withExtraGroupName("/extra")
						.withDynamicAttributeName("name")
						.withDynamicAttributeExpression("a")
						.build() })
				.withDisplayedName(DBI18nString.builder()
						.withDefaultValue("groupDisp")
						.build())
				.withI18nDescription(DBI18nString.builder()
						.withDefaultValue("desc")
						.build())
				.withPublicGroup(false)
				.withDelegationConfiguration(DBGroupDelegationConfiguration.builder()
						.withEnabled(true)
						.withEnableSubprojects(false)
						.withLogoUrl("logo")
						.withRegistrationForm("regForm")
						.withSignupEnquiryForm("enqForm")
						.withMembershipUpdateEnquiryForm("enqForm2")
						.withAttributes(List.of("at1"))
						.build())
				.withProperties(List.of(DBGroupProperty.builder()
						.withKey("key")
						.withValue("value")
						.build()))
				.build();
	}

	@Override
	protected Group getMinAPIObject()
	{
		Group group = new Group("/A/B/C");
		return group;
	}

	@Override
	protected DBGroup getMinDBObject()
	{
		return DBGroup.builder().withPath("/A/B/C")
				.build();
	}

	@Override
	protected Pair<Function<Group, DBGroup>, Function<DBGroup, Group>> getMapper()
	{
		return Pair.of(GroupMapper::map, g -> GroupMapper.map(g));
	}
	
	@Test
	public void shouldSupportStringDescription()
	{
		DBGroup group = DBGroup.builder()
				.withPath("/A/B/C")
				.withDescription("desc")
				.build();
		Group map = GroupMapper.map(group);
		assertThat(map.getDescription()
				.getDefaultValue()).isEqualTo("desc");
	}
}
