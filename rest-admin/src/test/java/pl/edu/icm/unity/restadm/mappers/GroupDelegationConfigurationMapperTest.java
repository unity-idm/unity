/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.basic.RestGroupDelegationConfiguration;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;

public class GroupDelegationConfigurationMapperTest
		extends MapperTestBase<GroupDelegationConfiguration, RestGroupDelegationConfiguration>
{

	@Override
	protected GroupDelegationConfiguration getFullAPIObject()
	{
		return new GroupDelegationConfiguration(true, false, "logo", "regForm", "enqForm", "enqForm2", List.of("at1"));
	}

	@Override
	protected RestGroupDelegationConfiguration getFullRestObject()
	{
		return RestGroupDelegationConfiguration.builder()
				.withEnabled(true)
				.withEnableSubprojects(false)
				.withLogoUrl("logo")
				.withRegistrationForm("regForm")
				.withSignupEnquiryForm("enqForm")
				.withMembershipUpdateEnquiryForm("enqForm2")
				.withAttributes(List.of("at1"))
				.build();
	}

	@Override
	protected Pair<Function<GroupDelegationConfiguration, RestGroupDelegationConfiguration>, Function<RestGroupDelegationConfiguration, GroupDelegationConfiguration>> getMapper()
	{
		return Pair.of(GroupDelegationConfigurationMapper::map, GroupDelegationConfigurationMapper::map);
	}

}
