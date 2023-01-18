/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestRegistrationFormNotificationsTest extends RestTypeBase<RestRegistrationFormNotifications>
{
	@Override
	protected String getJson()
	{
		return "{\"rejectedTemplate\":\"rejectTemplate\",\"acceptedTemplate\":\"acceptedTemplate\","
				+ "\"updatedTemplate\":\"updatedTemplate\",\"invitationTemplate\":\"invitationTemplate\","
				+ "\"invitationProcessedTemplate\":\"invitationProcessedTemplate\",\"adminsNotificationGroup\":\"/group\","
				+ "\"sendUserNotificationCopyToAdmin\":true," + "\"submittedTemplate\":\"submittedTemplate\"}\n";
	}

	@Override
	protected RestRegistrationFormNotifications getObject()
	{
		return RestRegistrationFormNotifications.builder()
				.withAcceptedTemplate("acceptedTemplate")
				.withAdminsNotificationGroup("/group")
				.withInvitationProcessedTemplate("invitationProcessedTemplate")
				.withRejectedTemplate("rejectTemplate")
				.withSendUserNotificationCopyToAdmin(true)
				.withSubmittedTemplate("submittedTemplate")
				.withUpdatedTemplate("updatedTemplate")
				.withInvitationTemplate("invitationTemplate")
				.build();
	}
}
