/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers;

import java.util.List;
import java.util.function.Function;

import io.imunity.rest.api.types.basic.RestAttribute;
import pl.edu.icm.unity.base.attribute.Attribute;

public class AttributeMapperTest extends MapperWithMinimalTestBase<Attribute, RestAttribute>
{

	@Override
	protected Attribute getFullAPIObject()
	{
		Attribute attribute = new Attribute("name", "string", "/A/B", List.of("val1", "val2"));
		attribute.setTranslationProfile("profile");
		attribute.setRemoteIdp("remoteIdp");
		return attribute;

	}

	@Override
	protected RestAttribute getFullRestObject()
	{
		return RestAttribute.builder()
				.withGroupPath("/A/B")
				.withName("name")
				.withRemoteIdp("remoteIdp")
				.withTranslationProfile("profile")
				.withValues(List.of("val1", "val2"))
				.withValueSyntax("string")
				.build();
	}

	@Override
	protected Pair<Function<Attribute, RestAttribute>, Function<RestAttribute, Attribute>> getMapper()
	{
		return Pair.of(AttributeMapper::map, AttributeMapper::map);
	}

	@Override
	protected Attribute getMinAPIObject()
	{

		return new Attribute("name", "string", "/A/B", List.of("val1", "val2"));
	}

	@Override
	protected RestAttribute getMinRestObject()
	{
		return RestAttribute.builder()
				.withGroupPath("/A/B")
				.withName("name")
				.withValues(List.of("val1", "val2"))
				.withValueSyntax("string")
				.build();
	}

}
