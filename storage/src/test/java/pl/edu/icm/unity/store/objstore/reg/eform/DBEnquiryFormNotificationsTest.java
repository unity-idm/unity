/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.eform;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBEnquiryFormNotificationsTest extends DBTypeTestBase<DBEnquiryFormNotifications>
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
	protected DBEnquiryFormNotifications getObject()
	{
		return DBEnquiryFormNotifications.builder()
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
