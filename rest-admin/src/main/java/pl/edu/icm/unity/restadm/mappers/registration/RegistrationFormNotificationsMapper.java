/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration;

import io.imunity.rest.api.types.registration.RestRegistrationFormNotifications;
import pl.edu.icm.unity.base.registration.RegistrationFormNotifications;

public class RegistrationFormNotificationsMapper
{
	public static RestRegistrationFormNotifications map(RegistrationFormNotifications registrationFormNotifications)
	{
		return RestRegistrationFormNotifications.builder()
				.withAcceptedTemplate(registrationFormNotifications.getAcceptedTemplate())
				.withAdminsNotificationGroup(registrationFormNotifications.getAdminsNotificationGroup())
				.withInvitationProcessedTemplate(registrationFormNotifications.getInvitationProcessedTemplate())
				.withInvitationTemplate(registrationFormNotifications.getInvitationTemplate())
				.withRejectedTemplate(registrationFormNotifications.getRejectedTemplate())
				.withSendUserNotificationCopyToAdmin(registrationFormNotifications.isSendUserNotificationCopyToAdmin())
				.withSubmittedTemplate(registrationFormNotifications.getSubmittedTemplate())
				.withUpdatedTemplate(registrationFormNotifications.getUpdatedTemplate())
				.build();

	}

	public static RegistrationFormNotifications map(RestRegistrationFormNotifications restRegistrationFormNotifications)
	{
		RegistrationFormNotifications registrationFormNotifications = new RegistrationFormNotifications();
		registrationFormNotifications.setAcceptedTemplate(restRegistrationFormNotifications.acceptedTemplate);
		registrationFormNotifications
				.setAdminsNotificationGroup(restRegistrationFormNotifications.adminsNotificationGroup);
		registrationFormNotifications
				.setInvitationProcessedTemplate(restRegistrationFormNotifications.invitationProcessedTemplate);
		registrationFormNotifications.setInvitationTemplate(restRegistrationFormNotifications.invitationTemplate);
		registrationFormNotifications.setRejectedTemplate(restRegistrationFormNotifications.rejectedTemplate);
		registrationFormNotifications.setSubmittedTemplate(restRegistrationFormNotifications.submittedTemplate);
		registrationFormNotifications.setUpdatedTemplate(restRegistrationFormNotifications.updatedTemplate);
		registrationFormNotifications
				.setSendUserNotificationCopyToAdmin(restRegistrationFormNotifications.sendUserNotificationCopyToAdmin);
		return registrationFormNotifications;
	}
}
