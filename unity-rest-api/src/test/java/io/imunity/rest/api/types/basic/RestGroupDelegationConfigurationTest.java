/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.List;

public class RestGroupDelegationConfigurationTest extends RestTypeBase<RestGroupDelegationConfiguration>
{

	@Override
	protected String getJson()
	{
		return "{\"enabled\":true,\"enableSubprojects\":true,\"logoUrl\":\"logoUrl\",\"registrationForm\":\"reg\",\"signupEnquiryForm\":\"enq\","
				+ "\"membershipUpdateEnquiryForm\":\"menq\",\"attributes\":[\"attr1\",\"attr2\"]}";
	}

	@Override
	protected RestGroupDelegationConfiguration getObject()
	{
		return RestGroupDelegationConfiguration.builder()
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
