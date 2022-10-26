/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import io.imunity.rest.api.types.basic.RestAttributeExt;
import pl.edu.icm.unity.types.basic.AttributeExt;

public class AttributeExtMapper
{
	static RestAttributeExt map(AttributeExt attributeExt) {
		return RestAttributeExt.builder()
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
}
