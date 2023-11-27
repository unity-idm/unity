/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.mappers.registration;

import java.util.function.Function;

import io.imunity.rest.api.types.registration.RestAttributeRegistrationParam;
import io.imunity.rest.api.types.registration.RestURLQueryPrefillConfig;
import pl.edu.icm.unity.rest.mappers.MapperTestBase;
import pl.edu.icm.unity.rest.mappers.Pair;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.ConfirmationMode;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.URLQueryPrefillConfig;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;

public class AttributeRegistrationParamMapperTest
		extends MapperTestBase<AttributeRegistrationParam, RestAttributeRegistrationParam>
{

	@SuppressWarnings("deprecation")
	@Override
	protected AttributeRegistrationParam getFullAPIObject()
	{
		AttributeRegistrationParam attributeRegistrationParam = new AttributeRegistrationParam();
		attributeRegistrationParam.setConfirmationMode(ConfirmationMode.CONFIRMED);
		attributeRegistrationParam.setDescription("desc");
		attributeRegistrationParam.setAttributeType("type");
		attributeRegistrationParam.setLabel("label");
		attributeRegistrationParam.setOptional(false);
		attributeRegistrationParam.setRetrievalSettings(ParameterRetrievalSettings.automatic);
		attributeRegistrationParam.setUrlQueryPrefill(new URLQueryPrefillConfig("param", PrefilledEntryMode.DEFAULT));
		attributeRegistrationParam.setShowGroups(true);
		attributeRegistrationParam.setUseDescription(true);
		attributeRegistrationParam.setGroup("/");
		return attributeRegistrationParam;
	}

	@Override
	protected RestAttributeRegistrationParam getFullRestObject()
	{
		return RestAttributeRegistrationParam.builder()
				.withAttributeType("type")
				.withGroup("/")
				.withShowGroups(true)
				.withUseDescription(true)
				.withConfirmationMode("CONFIRMED")
				.withDescription("desc")
				.withLabel("label")
				.withOptional(false)
				.withRetrievalSettings("automatic")
				.withUrlQueryPrefill(RestURLQueryPrefillConfig.builder()
						.withMode("DEFAULT")
						.withParamName("param")
						.build())
				.build();
	}

	@Override
	protected Pair<Function<AttributeRegistrationParam, RestAttributeRegistrationParam>, Function<RestAttributeRegistrationParam, AttributeRegistrationParam>> getMapper()
	{
		return Pair.of(AttributeRegistrationParamMapper::map, AttributeRegistrationParamMapper::map);
	}

}
