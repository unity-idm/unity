/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;

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
		
		assertThat(notParsed, is(not));
	}
}
