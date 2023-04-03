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
import pl.edu.icm.unity.store.MapperWithMinimalTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.store.types.common.DBI18nString;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.types.basic.GroupProperty;

public class GroupBaseMapperTest extends MapperWithMinimalTestBase<Group, DBGroupBase>
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
	protected DBGroupBase getFullDBObject()
	{
		return DBGroupBase.builder()
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
	protected DBGroupBase getMinDBObject()
	{
		return DBGroupBase.builder()
				.build();
	}

	@Override
	protected Pair<Function<Group, DBGroupBase>, Function<DBGroupBase, Group>> getMapper()
	{
		return Pair.of(GroupMapper::mapBaseGroup, g -> GroupMapper.mapFromBaseGroup(g, "/A/B/C"));
	}
	
	@Test
	public void shouldSupportStringDescription()
	{
		DBGroupBase group = DBGroupBase.builder()
				.withDescription("desc")
				.build();
		Group map = GroupMapper.mapFromBaseGroup(group, "/A/B/C");
		assertThat(map.getDescription()
				.getDefaultValue()).isEqualTo("desc");
	}
}
