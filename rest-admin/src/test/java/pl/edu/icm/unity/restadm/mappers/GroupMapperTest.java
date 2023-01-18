/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.basic.RestAttributeStatement;
import io.imunity.rest.api.types.basic.RestGroup;
import io.imunity.rest.api.types.basic.RestGroupDelegationConfiguration;
import io.imunity.rest.api.types.basic.RestGroupProperty;
import io.imunity.rest.api.types.basic.RestI18nString;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.types.basic.GroupProperty;

public class GroupMapperTest extends MapperTestBase<Group, RestGroup>
{
	@Override
	protected Group getAPIObject()
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
	protected RestGroup getRestObject()
	{
		return RestGroup.builder()
				.withPath("/A/B/C")
				.withAttributesClasses(Set.of("attrClass1", "attrClass2"))
				.withAttributeStatements(new RestAttributeStatement[]
				{ RestAttributeStatement.builder()
						.withCondition("true")
						.withResolution("merge")
						.withExtraGroupName("/extra")
						.withDynamicAttributeName("name")
						.withDynamicAttributeExpression("a")
						.build() })
				.withDisplayedName(RestI18nString.builder()
						.withDefaultValue("groupDisp")
						.build())
				.withI18nDescription(RestI18nString.builder()
						.withDefaultValue("desc")
						.build())
				.withPublicGroup(false)
				.withDelegationConfiguration(RestGroupDelegationConfiguration.builder()
						.withEnabled(true)
						.withEnableSubprojects(false)
						.withLogoUrl("logo")
						.withRegistrationForm("regForm")
						.withSignupEnquiryForm("enqForm")
						.withMembershipUpdateEnquiryForm("enqForm2")
						.withAttributes(List.of("at1"))
						.build())
				.withProperties(List.of(RestGroupProperty.builder()
						.withKey("key")
						.withValue("value")
						.build()))
				.build();
	}

	@Override
	protected Pair<Function<Group, RestGroup>, Function<RestGroup, Group>> getMapper()
	{
		return Pair.of(GroupMapper::map, GroupMapper::map);
	}
}
