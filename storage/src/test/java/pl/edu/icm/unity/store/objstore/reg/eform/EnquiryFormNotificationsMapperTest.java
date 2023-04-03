/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.eform;

import java.util.function.Function;

import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.types.registration.EnquiryFormNotifications;

public class EnquiryFormNotificationsMapperTest
		extends MapperTestBase<EnquiryFormNotifications, DBEnquiryFormNotifications>
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
	protected DBEnquiryFormNotifications getFullDBObject()
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

	@Override
	protected Pair<Function<EnquiryFormNotifications, DBEnquiryFormNotifications>, Function<DBEnquiryFormNotifications, EnquiryFormNotifications>> getMapper()
	{
		return Pair.of(EnquiryFormNotificationsMapper::map, EnquiryFormNotificationsMapper::map);
	}

}
