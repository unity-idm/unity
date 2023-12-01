/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.registration;

import java.util.Optional;

import io.imunity.rest.api.types.registration.RestAttributeRegistrationParam;
import pl.edu.icm.unity.base.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.base.registration.ConfirmationMode;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;

public class AttributeRegistrationParamMapper
{
	@SuppressWarnings("deprecation")
	public static RestAttributeRegistrationParam map(AttributeRegistrationParam attribueRegistrationParam)
	{
		return RestAttributeRegistrationParam.builder()
				.withAttributeType(attribueRegistrationParam.getAttributeType())
				.withGroup(attribueRegistrationParam.getGroup())
				.withShowGroups(attribueRegistrationParam.isShowGroups())
				.withUseDescription(attribueRegistrationParam.isUseDescription())
				.withConfirmationMode(attribueRegistrationParam.getConfirmationMode()
						.name())
				.withDescription(attribueRegistrationParam.getDescription())
				.withLabel(attribueRegistrationParam.getLabel())
				.withOptional(attribueRegistrationParam.isOptional())
				.withRetrievalSettings(attribueRegistrationParam.getRetrievalSettings()
						.name())
				.withUrlQueryPrefill(Optional.ofNullable(attribueRegistrationParam.getUrlQueryPrefill())
						.map(URLQueryPrefillConfigMapper::map)
						.orElse(null))
				.build();
	}

	@SuppressWarnings("deprecation")
	public static AttributeRegistrationParam map(RestAttributeRegistrationParam registrationParam)
	{
		AttributeRegistrationParam attributeRegistrationParam = new AttributeRegistrationParam();
		attributeRegistrationParam.setAttributeType(registrationParam.attributeType);
		attributeRegistrationParam.setUseDescription(registrationParam.useDescription);
		attributeRegistrationParam.setShowGroups(registrationParam.showGroups);
		attributeRegistrationParam.setGroup(registrationParam.group);
		attributeRegistrationParam.setConfirmationMode(ConfirmationMode.valueOf(registrationParam.confirmationMode));
		attributeRegistrationParam.setDescription(registrationParam.description);
		attributeRegistrationParam.setLabel(registrationParam.label);
		attributeRegistrationParam.setOptional(registrationParam.optional);
		attributeRegistrationParam
				.setRetrievalSettings(ParameterRetrievalSettings.valueOf(registrationParam.retrievalSettings));
		attributeRegistrationParam.setUrlQueryPrefill(Optional.ofNullable(registrationParam.urlQueryPrefill)
				.map(URLQueryPrefillConfigMapper::map)
				.orElse(null));
		return attributeRegistrationParam;
	}
}
