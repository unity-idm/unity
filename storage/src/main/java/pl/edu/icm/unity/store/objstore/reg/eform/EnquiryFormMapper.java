/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.eform;

import java.util.List;
import java.util.Optional;

import pl.edu.icm.unity.store.objstore.reg.common.BaseFormMapper;
import pl.edu.icm.unity.store.objstore.reg.layout.FormLayoutMapper;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.types.registration.EnquiryFormNotifications;

class EnquiryFormMapper
{
	static DBEnquiryForm map(EnquiryForm enquiryForm)
	{
		return BaseFormMapper.map(DBEnquiryForm.builder(), enquiryForm)
				.withType(enquiryForm.getType()
						.name())
				.withTargetCondition(enquiryForm.getTargetCondition())
				.withTargetGroups(List.of(enquiryForm.getTargetGroups()))
				.withNotificationsConfiguration(Optional.ofNullable(enquiryForm.getNotificationsConfiguration())
						.map(EnquiryFormNotificationsMapper::map)
						.orElse(null))
				.withLayout(Optional.ofNullable(enquiryForm.getLayout())
						.map(FormLayoutMapper::map)
						.orElse(null))
				.build();

	}

	static EnquiryForm map(DBEnquiryForm dbEnquiryForm)
	{
		return BaseFormMapper.map(new EnquiryFormBuilder(), dbEnquiryForm)
				.withType(EnquiryType.valueOf(dbEnquiryForm.type))
				.withTargetGroups(dbEnquiryForm.targetGroups.toArray(String[]::new))
				.withTargetCondition(dbEnquiryForm.targetCondition)
				.withNotificationsConfiguration(Optional.ofNullable(dbEnquiryForm.notificationsConfiguration)
						.map(EnquiryFormNotificationsMapper::map)
						.orElse(new EnquiryFormNotifications()))
				.withLayout(Optional.ofNullable(dbEnquiryForm.layout)
						.map(FormLayoutMapper::map)
						.orElse(null))
				.build();

	}

}
