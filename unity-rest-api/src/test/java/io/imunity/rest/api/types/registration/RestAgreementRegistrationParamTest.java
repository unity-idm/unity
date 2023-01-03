/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import io.imunity.rest.api.types.basic.RestI18nString;
import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestAgreementRegistrationParamTest extends RestTypeBase<RestAgreementRegistrationParam>
{

	@Override
	protected String getJson()
	{
		return "{\"i18nText\":{\"DefaultValue\":\"testV\",\"Map\":{}},\"manatory\":true}";
	}

	@Override
	protected RestAgreementRegistrationParam getObject()
	{
		return RestAgreementRegistrationParam.builder()
				.withMandatory(true)
				.withText(RestI18nString.builder()
						.withDefaultValue("testV")
						.build())
				.build();
	}

}
