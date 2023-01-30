/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.attribute;

import java.util.List;
import java.util.function.Function;

import pl.edu.icm.unity.store.MapperWithMinimalTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.types.basic.Attribute;

public class AttributeMapperTest extends MapperWithMinimalTestBase<Attribute, DBAttribute>
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
	protected DBAttribute getFullDBObject()
	{
		return DBAttribute.builder()
				.withGroupPath("/A/B")
				.withName("name")
				.withRemoteIdp("remoteIdp")
				.withTranslationProfile("profile")
				.withValues(List.of("val1", "val2"))
				.withValueSyntax("string")
				.build();
	}

	@Override
	protected Pair<Function<Attribute, DBAttribute>, Function<DBAttribute, Attribute>> getMapper()
	{
		return Pair.of(AttributeMapper::map, AttributeMapper::map);
	}

	@Override
	protected Attribute getMinAPIObject()
	{

		return new Attribute("name", "string", "/A/B", List.of("val1", "val2"));
	}

	@Override
	protected DBAttribute getMinDBObject()
	{
		return DBAttribute.builder()
				.withGroupPath("/A/B")
				.withName("name")
				.withValues(List.of("val1", "val2"))
				.withValueSyntax("string")
				.build();
	}

}
