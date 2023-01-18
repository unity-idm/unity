/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.basic.RestAttribute;
import pl.edu.icm.unity.types.basic.Attribute;

public class AttributeMapperTest extends MapperTestBase<Attribute, RestAttribute>
{

	@Override
	protected Attribute getAPIObject()
	{
		Attribute attribute = new Attribute("name", "string", "/A/B", List.of("val1", "val2"));
		attribute.setTranslationProfile("profile");
		attribute.setRemoteIdp("remoteIdp");
		return attribute;

	}

	@Override
	protected RestAttribute getRestObject()
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

}
