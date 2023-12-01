/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers;

import io.imunity.rest.api.types.basic.RestAttribute;
import pl.edu.icm.unity.types.basic.Attribute;

public class AttributeMapper
{
	public static RestAttribute map(Attribute attribute)
	{
		return RestAttribute.builder()
				.withName(attribute.getName())
				.withValueSyntax(attribute.getValueSyntax())
				.withGroupPath(attribute.getGroupPath())
				.withValues(attribute.getValues())
				.withTranslationProfile(attribute.getTranslationProfile())
				.withRemoteIdp(attribute.getRemoteIdp())
				.build();
	}

	public static Attribute map(RestAttribute restAttribute)
	{
		return new Attribute(restAttribute.name, restAttribute.valueSyntax, restAttribute.groupPath,
				restAttribute.values, restAttribute.remoteIdp, restAttribute.translationProfile);

	}

}
