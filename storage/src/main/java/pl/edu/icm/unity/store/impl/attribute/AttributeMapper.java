/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.attribute;

import pl.edu.icm.unity.types.basic.Attribute;

public class AttributeMapper
{
	public static DBAttribute map(Attribute attribute)
	{
		return DBAttribute.builder()
				.withName(attribute.getName())
				.withValueSyntax(attribute.getValueSyntax())
				.withGroupPath(attribute.getGroupPath())
				.withValues(attribute.getValues())
				.withTranslationProfile(attribute.getTranslationProfile())
				.withRemoteIdp(attribute.getRemoteIdp())
				.build();
	}

	public static Attribute map(DBAttribute dbAttribute)
	{
		return new Attribute(dbAttribute.name, dbAttribute.valueSyntax, dbAttribute.groupPath,
				dbAttribute.values, dbAttribute.remoteIdp, dbAttribute.translationProfile);

	}

}
