/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.attribute;

import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;

class AttributeExtBaseMapper
{
	static DBAttributeExtBase map(AttributeExt attributeExt)
	{
		return DBAttributeExtBase.builder()
				.withDirect(attributeExt.isDirect())
				.withCreationTs(attributeExt.getCreationTs())
				.withUpdateTs(attributeExt.getUpdateTs())
				.withValues(attributeExt.getValues())
				.withTranslationProfile(attributeExt.getTranslationProfile())
				.withRemoteIdp(attributeExt.getRemoteIdp())
				.build();
	}

	static AttributeExt map(DBAttributeExtBase dbAttributeExt, String name, String valueSyntax, String groupPath)
	{

		return new AttributeExt(
				new Attribute(name, valueSyntax, groupPath, dbAttributeExt.values, dbAttributeExt.remoteIdp,
						dbAttributeExt.translationProfile),
				dbAttributeExt.direct, dbAttributeExt.creationTs, dbAttributeExt.updateTs);

	}
}
