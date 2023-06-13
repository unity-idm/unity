/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.function.Function;

import pl.edu.icm.unity.base.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.base.registration.ConfirmationMode;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.base.registration.URLQueryPrefillConfig;
import pl.edu.icm.unity.base.registration.invite.PrefilledEntryMode;
import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;

public class AttributeRegistrationParamMapperTest
		extends MapperTestBase<AttributeRegistrationParam, DBAttributeRegistrationParam>
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
	protected DBAttributeRegistrationParam getFullDBObject()
	{
		return DBAttributeRegistrationParam.builder()
				.withAttributeType("type")
				.withGroup("/")
				.withShowGroups(true)
				.withUseDescription(true)
				.withConfirmationMode("CONFIRMED")
				.withDescription("desc")
				.withLabel("label")
				.withOptional(false)
				.withRetrievalSettings("automatic")
				.withUrlQueryPrefill(DBurlQueryPrefillConfig.builder()
						.withMode("DEFAULT")
						.withParamName("param")
						.build())
				.build();
	}

	@Override
	protected Pair<Function<AttributeRegistrationParam, DBAttributeRegistrationParam>, Function<DBAttributeRegistrationParam, AttributeRegistrationParam>> getMapper()
	{
		return Pair.of(AttributeRegistrationParamMapper::map, AttributeRegistrationParamMapper::map);
	}

}
