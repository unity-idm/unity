/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.form;

import java.util.function.Function;

import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.types.registration.RegistrationFormNotifications;

public class RegistrationFormNotificationsMapperTest
		extends MapperTestBase<RegistrationFormNotifications, DBRegistrationFormNotifications>
{

	@Override
	protected RegistrationFormNotifications getFullAPIObject()
	{
		RegistrationFormNotifications registrationFormNotifications = new RegistrationFormNotifications();
		registrationFormNotifications.setAcceptedTemplate("acceptedTemplate");
		registrationFormNotifications.setAdminsNotificationGroup("/group");
		registrationFormNotifications.setInvitationProcessedTemplate("invitationProcessedTemplate");
		registrationFormNotifications.setInvitationTemplate("invitationTemplate");
		registrationFormNotifications.setRejectedTemplate("rejectTemplate");
		registrationFormNotifications.setSendUserNotificationCopyToAdmin(true);
		registrationFormNotifications.setSubmittedTemplate("submittedTemplate");
		registrationFormNotifications.setUpdatedTemplate("updatedTemplate");
		return registrationFormNotifications;
	}

	@Override
	protected DBRegistrationFormNotifications getFullDBObject()
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

	@Override
	protected Pair<Function<RegistrationFormNotifications, DBRegistrationFormNotifications>, Function<DBRegistrationFormNotifications, RegistrationFormNotifications>> getMapper()
	{
		return Pair.of(RegistrationFormNotificationsMapper::map, RegistrationFormNotificationsMapper::map);
	}

}
