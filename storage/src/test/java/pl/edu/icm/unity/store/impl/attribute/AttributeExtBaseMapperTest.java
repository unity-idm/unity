/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.attribute;

import java.util.Date;
import java.util.List;
import java.util.function.Function;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.store.MapperWithMinimalTestBase;
import pl.edu.icm.unity.store.Pair;

public class AttributeExtBaseMapperTest extends MapperWithMinimalTestBase<AttributeExt, DBAttributeExt>
{

	@Override
	protected AttributeExt getFullAPIObject()
	{
		AttributeExt attributeExt = new AttributeExt(
				new Attribute("attr", "syntax", "/A", List.of("v1", "v2"), "remoteIdp", "translationProfile"), false);
		attributeExt.setCreationTs(new Date(100L));
		attributeExt.setUpdateTs(new Date(1000L));
		return attributeExt;
	}

	@Override
	protected DBAttributeExt getFullDBObject()
	{
		return DBAttributeExt.builder()
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
	protected AttributeExt getMinAPIObject()
	{
		return new AttributeExt(new Attribute("attr", "syntax", "/A", List.of("v1", "v2")), false, null, null);
	}

	@Override
	protected DBAttributeExt getMinDBObject()
	{
		return DBAttributeExt.builder()
				.withGroupPath("/A")
				.withValueSyntax("syntax")
				.withValues(List.of("v1", "v2"))
				.withName("attr")
				.withDirect(false)
				.build();

	}

	@Override
	protected Pair<Function<AttributeExt, DBAttributeExt>, Function<DBAttributeExt, AttributeExt>> getMapper()
	{
		return Pair.of(AttributeExtMapper::map, AttributeExtMapper::map);
	}

}
