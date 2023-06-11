/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.form;

import pl.edu.icm.unity.base.registration.RegistrationFormNotifications;

class RegistrationFormNotificationsMapper
{
	static DBRegistrationFormNotifications map(RegistrationFormNotifications registrationFormNotifications)
	{
		return DBRegistrationFormNotifications.builder()
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

	static RegistrationFormNotifications map(DBRegistrationFormNotifications restRegistrationFormNotifications)
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
