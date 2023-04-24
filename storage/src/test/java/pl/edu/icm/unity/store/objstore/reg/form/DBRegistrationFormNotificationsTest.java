/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.form;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBRegistrationFormNotificationsTest extends DBTypeTestBase<DBRegistrationFormNotifications>
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
	protected DBRegistrationFormNotifications getObject()
	{
		return DBRegistrationFormNotifications.builder()
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
