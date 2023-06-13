/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.registration.RestEnquiryFormNotifications;
import pl.edu.icm.unity.base.registration.EnquiryFormNotifications;
import pl.edu.icm.unity.restadm.mappers.MapperTestBase;

public class EnquiryFormNotificationsMapperTest
		extends MapperTestBase<EnquiryFormNotifications, RestEnquiryFormNotifications>
{

	@Override
	protected EnquiryFormNotifications getFullAPIObject()
	{
		EnquiryFormNotifications enquiryFormNotifications = new EnquiryFormNotifications();
		enquiryFormNotifications.setAcceptedTemplate("acceptedTemplate");
		enquiryFormNotifications.setAdminsNotificationGroup("/group");
		enquiryFormNotifications.setInvitationProcessedTemplate("invitationProcessedTemplate");
		enquiryFormNotifications.setInvitationTemplate("invitationTemplate");
		enquiryFormNotifications.setRejectedTemplate("rejectTemplate");
		enquiryFormNotifications.setSendUserNotificationCopyToAdmin(true);
		enquiryFormNotifications.setSubmittedTemplate("submittedTemplate");
		enquiryFormNotifications.setUpdatedTemplate("updatedTemplate");
		enquiryFormNotifications.setEnquiryToFillTemplate("enquiryToFillTemplate");
		return enquiryFormNotifications;
	}

	@Override
	protected RestEnquiryFormNotifications getFullRestObject()
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

	@Override
	protected Pair<Function<EnquiryFormNotifications, RestEnquiryFormNotifications>, Function<RestEnquiryFormNotifications, EnquiryFormNotifications>> getMapper()
	{
		return Pair.of(EnquiryFormNotificationsMapper::map, EnquiryFormNotificationsMapper::map);
	}

}
