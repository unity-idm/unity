/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.Optional;

import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.ConfirmationMode;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;

class AttributeRegistrationParamMapper
{
	@SuppressWarnings("deprecation")
	static DBAttributeRegistrationParam map(AttributeRegistrationParam attribueRegistrationParam)
	{
		return DBAttributeRegistrationParam.builder()
				.withAttributeType(attribueRegistrationParam.getAttributeType())
				.withGroup(attribueRegistrationParam.getGroup())
				.withShowGroups(attribueRegistrationParam.isShowGroups())
				.withUseDescription(attribueRegistrationParam.isUseDescription())
				.withConfirmationMode(Optional.ofNullable(attribueRegistrationParam.getConfirmationMode()).map(Enum::name).orElse(null))
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
	static AttributeRegistrationParam map(DBAttributeRegistrationParam registrationParam)
	{
		AttributeRegistrationParam attributeRegistrationParam = new AttributeRegistrationParam();
		attributeRegistrationParam.setAttributeType(registrationParam.attributeType);
		attributeRegistrationParam.setUseDescription(registrationParam.useDescription);
		attributeRegistrationParam.setShowGroups(registrationParam.showGroups);
		attributeRegistrationParam.setGroup(registrationParam.group);
		attributeRegistrationParam.setConfirmationMode(Optional.ofNullable(registrationParam.confirmationMode).map(ConfirmationMode::valueOf).orElse(null));
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
