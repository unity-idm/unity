/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.registration.RestRegistrationFormNotifications;
import pl.edu.icm.unity.restadm.mappers.MapperTestBase;
import pl.edu.icm.unity.types.registration.RegistrationFormNotifications;

public class RegistrationFormNotificationsMapperTest
		extends MapperTestBase<RegistrationFormNotifications, RestRegistrationFormNotifications>
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
	protected RestRegistrationFormNotifications getFullRestObject()
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

	@Override
	protected Pair<Function<RegistrationFormNotifications, RestRegistrationFormNotifications>, Function<RestRegistrationFormNotifications, RegistrationFormNotifications>> getMapper()
	{
		return Pair.of(RegistrationFormNotificationsMapper::map, RegistrationFormNotificationsMapper::map);
	}

}
