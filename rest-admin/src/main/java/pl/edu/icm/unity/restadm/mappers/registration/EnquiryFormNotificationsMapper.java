/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration;

import io.imunity.rest.api.types.registration.RestEnquiryFormNotifications;
import pl.edu.icm.unity.types.registration.EnquiryFormNotifications;

public class EnquiryFormNotificationsMapper
{
	public static RestEnquiryFormNotifications map(EnquiryFormNotifications enquiryFormNotifications)
	{
		return RestEnquiryFormNotifications.builder()
				.withAcceptedTemplate(enquiryFormNotifications.getAcceptedTemplate())
				.withAdminsNotificationGroup(enquiryFormNotifications.getAdminsNotificationGroup())
				.withInvitationProcessedTemplate(enquiryFormNotifications.getInvitationProcessedTemplate())
				.withInvitationTemplate(enquiryFormNotifications.getInvitationTemplate())
				.withRejectedTemplate(enquiryFormNotifications.getRejectedTemplate())
				.withSendUserNotificationCopyToAdmin(enquiryFormNotifications.isSendUserNotificationCopyToAdmin())
				.withSubmittedTemplate(enquiryFormNotifications.getSubmittedTemplate())
				.withUpdatedTemplate(enquiryFormNotifications.getUpdatedTemplate())
				.withEnquiryToFillTemplate(enquiryFormNotifications.getEnquiryToFillTemplate())
				.build();

	}

	public static EnquiryFormNotifications map(RestEnquiryFormNotifications restEnquiryFormNotifications)
	{
		EnquiryFormNotifications enquiryFormNotifications = new EnquiryFormNotifications();
		enquiryFormNotifications.setAcceptedTemplate(restEnquiryFormNotifications.acceptedTemplate);
		enquiryFormNotifications.setAdminsNotificationGroup(restEnquiryFormNotifications.adminsNotificationGroup);
		enquiryFormNotifications
				.setInvitationProcessedTemplate(restEnquiryFormNotifications.invitationProcessedTemplate);
		enquiryFormNotifications.setInvitationTemplate(restEnquiryFormNotifications.invitationTemplate);
		enquiryFormNotifications.setRejectedTemplate(restEnquiryFormNotifications.rejectedTemplate);
		enquiryFormNotifications.setSubmittedTemplate(restEnquiryFormNotifications.submittedTemplate);
		enquiryFormNotifications.setUpdatedTemplate(restEnquiryFormNotifications.updatedTemplate);
		enquiryFormNotifications
				.setSendUserNotificationCopyToAdmin(restEnquiryFormNotifications.sendUserNotificationCopyToAdmin);
		enquiryFormNotifications.setEnquiryToFillTemplate(restEnquiryFormNotifications.enquiryToFillTemplate);
		return enquiryFormNotifications;
	}
}
