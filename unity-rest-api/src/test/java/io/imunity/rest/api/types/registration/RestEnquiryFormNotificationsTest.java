/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestEnquiryFormNotificationsTest extends RestTypeBase<RestEnquiryFormNotifications>
{
	@Override
	protected String getJson()
	{
		return "{\"rejectedTemplate\":\"rejectTemplate\",\"acceptedTemplate\":\"acceptedTemplate\","
				+ "\"updatedTemplate\":\"updatedTemplate\",\"invitationTemplate\":\"invitationTemplate\","
				+ "\"invitationProcessedTemplate\":\"invitationProcessedTemplate\","
				+ "\"adminsNotificationGroup\":\"/group\",\"sendUserNotificationCopyToAdmin\":true,"
				+ "\"enquiryToFillTemplate\":\"enquiryToFillTemplate\",\"submittedTemplate\":\"submittedTemplate\"}\n";
	}

	@Override
	protected RestEnquiryFormNotifications getObject()
	{
		return RestEnquiryFormNotifications.builder()
				.withAcceptedTemplate("acceptedTemplate")
				.withAdminsNotificationGroup("/group")
				.withInvitationProcessedTemplate("invitationProcessedTemplate")
				.withRejectedTemplate("rejectTemplate")
				.withSendUserNotificationCopyToAdmin(true)
				.withSubmittedTemplate("submittedTemplate")
				.withUpdatedTemplate("updatedTemplate")
				.withInvitationTemplate("invitationTemplate")
				.withEnquiryToFillTemplate("enquiryToFillTemplate")
				.build();
	}
}
