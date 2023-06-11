/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.groups;

import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;

class GroupDelegationConfigurationMapper
{
	static DBGroupDelegationConfiguration map(GroupDelegationConfiguration groupDelegationConfiguration)
	{
		return DBGroupDelegationConfiguration.builder()
				.withAttributes(groupDelegationConfiguration.attributes)
				.withEnabled(groupDelegationConfiguration.enabled)
				.withEnableSubprojects(groupDelegationConfiguration.enableSubprojects)
				.withLogoUrl(groupDelegationConfiguration.logoUrl)
				.withMembershipUpdateEnquiryForm(groupDelegationConfiguration.membershipUpdateEnquiryForm)
				.withRegistrationForm(groupDelegationConfiguration.registrationForm)
				.withSignupEnquiryForm(groupDelegationConfiguration.signupEnquiryForm)
				.build();
	}

	static GroupDelegationConfiguration map(DBGroupDelegationConfiguration dbGroupDelegationConfiguration)
	{
		return new GroupDelegationConfiguration(dbGroupDelegationConfiguration.enabled,
				dbGroupDelegationConfiguration.enableSubprojects, dbGroupDelegationConfiguration.logoUrl,
				dbGroupDelegationConfiguration.registrationForm, dbGroupDelegationConfiguration.signupEnquiryForm,
				dbGroupDelegationConfiguration.membershipUpdateEnquiryForm, dbGroupDelegationConfiguration.attributes);

	}
}
