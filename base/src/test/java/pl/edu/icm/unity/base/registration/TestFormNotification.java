/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.registration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;

public class TestFormNotification
{
	@Test
	public void shouldDeserializeWithMissingJsonElements()
	{
		RegistrationFormNotifications not = new RegistrationFormNotifications();
		not.setAcceptedTemplate("accepted");
		not.setSendUserNotificationCopyToAdmin(false);
		ObjectNode parsed = Constants.MAPPER.convertValue(not, ObjectNode.class);
		parsed.remove("sendUserNotificationCopyToAdmin");

		RegistrationFormNotifications notParsed = Constants.MAPPER.convertValue(
				parsed, RegistrationFormNotifications.class);
		
		assertThat(notParsed).isEqualTo(not);
	}
}
