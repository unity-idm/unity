/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import io.imunity.rest.api.types.basic.RestGroupDelegationConfiguration;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;

public class GroupDelegationConfigurationMapper
{
	static RestGroupDelegationConfiguration map(GroupDelegationConfiguration groupDelegationConfiguration)
	{
		return RestGroupDelegationConfiguration.builder()
				.withAttributes(groupDelegationConfiguration.attributes)
				.withEnabled(groupDelegationConfiguration.enabled)
				.withEnableSubprojects(groupDelegationConfiguration.enableSubprojects)
				.withLogoUrl(groupDelegationConfiguration.logoUrl)
				.withMembershipUpdateEnquiryForm(groupDelegationConfiguration.membershipUpdateEnquiryForm)
				.withRegistrationForm(groupDelegationConfiguration.registrationForm)
				.withSignupEnquiryForm(groupDelegationConfiguration.signupEnquiryForm)
				.build();
	}

	static GroupDelegationConfiguration map(RestGroupDelegationConfiguration restGroupDelegationConfiguration)
	{
		return new GroupDelegationConfiguration(restGroupDelegationConfiguration.enabled,
				restGroupDelegationConfiguration.enableSubprojects, restGroupDelegationConfiguration.logoUrl,
				restGroupDelegationConfiguration.registrationForm, restGroupDelegationConfiguration.signupEnquiryForm,
				restGroupDelegationConfiguration.membershipUpdateEnquiryForm,
				restGroupDelegationConfiguration.attributes);

	}
}
