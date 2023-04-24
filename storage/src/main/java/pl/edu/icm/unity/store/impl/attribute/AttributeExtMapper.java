/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.attribute;

import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;

public class AttributeExtMapper
{
	public static DBAttributeExt map(AttributeExt attributeExt)
	{
		return DBAttributeExt.builder()
				.withDirect(attributeExt.isDirect())
				.withCreationTs(attributeExt.getCreationTs())
				.withUpdateTs(attributeExt.getUpdateTs())
				.withName(attributeExt.getName())
				.withValueSyntax(attributeExt.getValueSyntax())
				.withGroupPath(attributeExt.getGroupPath())
				.withValues(attributeExt.getValues())
				.withTranslationProfile(attributeExt.getTranslationProfile())
				.withRemoteIdp(attributeExt.getRemoteIdp())
				.build();
	}

	public static AttributeExt map(DBAttributeExt restAttributeExt)
	{

		return new AttributeExt(
				new Attribute(restAttributeExt.name, restAttributeExt.valueSyntax, restAttributeExt.groupPath,
						restAttributeExt.values, restAttributeExt.remoteIdp, restAttributeExt.translationProfile),
				restAttributeExt.direct, restAttributeExt.creationTs, restAttributeExt.updateTs);

	}
}
