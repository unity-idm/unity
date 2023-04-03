/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.groups;

import java.util.List;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBGroupDelegationConfigurationTest extends DBTypeTestBase<DBGroupDelegationConfiguration>
{

	@Override
	protected String getJson()
	{
		return "{\"enabled\":true,\"enableSubprojects\":true,\"logoUrl\":\"logoUrl\",\"registrationForm\":\"reg\",\"signupEnquiryForm\":\"enq\","
				+ "\"membershipUpdateEnquiryForm\":\"menq\",\"attributes\":[\"attr1\",\"attr2\"]}";
	}

	@Override
	protected DBGroupDelegationConfiguration getObject()
	{
		return DBGroupDelegationConfiguration.builder()
				.withEnabled(true)
				.withEnableSubprojects(true)
				.withLogoUrl("logoUrl")
				.withMembershipUpdateEnquiryForm("menq")
				.withRegistrationForm("reg")
				.withSignupEnquiryForm("enq")
				.withAttributes(List.of("attr1", "attr2"))
				.build();
	}
}
