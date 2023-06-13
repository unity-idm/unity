/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.identitytype;

import pl.edu.icm.unity.base.confirmation.EmailConfirmationConfiguration;

class EmailConfirmationConfigurationMapper
{
	static EmailConfirmationConfiguration map(DBEmailConfirmationConfiguration dbEmailConfirmationConfiguration)
	{
		EmailConfirmationConfiguration emailConfirmationConfiguration = new EmailConfirmationConfiguration(
				dbEmailConfirmationConfiguration.messageTemplate);
		emailConfirmationConfiguration.setValidityTime(dbEmailConfirmationConfiguration.validityTime);
		return emailConfirmationConfiguration;
	}

	static DBEmailConfirmationConfiguration map(EmailConfirmationConfiguration emailConfirmationConfiguration)
	{
		return DBEmailConfirmationConfiguration.builder()
				.withMessageTemplate(emailConfirmationConfiguration.getMessageTemplate())
				.withValidityTime(emailConfirmationConfiguration.getValidityTime())
				.build();
	}
}
