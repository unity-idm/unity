/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.groups;

import java.util.List;
import java.util.function.Function;

import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;

public class GroupDelegationConfigurationMapperTest
		extends MapperTestBase<GroupDelegationConfiguration, DBGroupDelegationConfiguration>
{

	@Override
	protected GroupDelegationConfiguration getFullAPIObject()
	{
		return new GroupDelegationConfiguration(true, false, "logo", "regForm", "enqForm", "enqForm2", List.of("at1"));
	}

	@Override
	protected DBGroupDelegationConfiguration getFullDBObject()
	{
		return DBGroupDelegationConfiguration.builder()
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
	protected Pair<Function<GroupDelegationConfiguration, DBGroupDelegationConfiguration>, Function<DBGroupDelegationConfiguration, GroupDelegationConfiguration>> getMapper()
	{
		return Pair.of(GroupDelegationConfigurationMapper::map, GroupDelegationConfigurationMapper::map);
	}

}
