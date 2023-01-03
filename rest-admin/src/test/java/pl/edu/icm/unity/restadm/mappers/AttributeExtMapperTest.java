/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import java.util.Date;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.basic.RestAttributeExt;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;

public class AttributeExtMapperTest extends MapperTestBase<AttributeExt, RestAttributeExt>
{

	@Override
	protected AttributeExt getAPIObject()
	{
		AttributeExt attributeExt = new AttributeExt(
				new Attribute("attr", "syntax", "/A", List.of("v1", "v2"), "remoteIdp", "translationProfile"), false);
		attributeExt.setCreationTs(new Date(100L));
		attributeExt.setUpdateTs(new Date(1000L));
		return attributeExt;
	}

	@Override
	protected RestAttributeExt getRestObject()
	{
		return RestAttributeExt.builder()
				.withRemoteIdp("remoteIdp")
				.withTranslationProfile("translationProfile")
				.withGroupPath("/A")
				.withValueSyntax("syntax")
				.withValues(List.of("v1", "v2"))
				.withName("attr")
				.withCreationTs(new Date(100L))
				.withUpdateTs(new Date(1000L))
				.withDirect(false)
				.build();
	}

	@Override
	protected Pair<Function<AttributeExt, RestAttributeExt>, Function<RestAttributeExt, AttributeExt>> getMapper()
	{
		return Pair.of(AttributeExtMapper::map, AttributeExtMapper::map);
	}
}
