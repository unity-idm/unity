/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.attribute;

import java.util.Date;
import java.util.List;
import java.util.function.Function;

import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;

public class AttributeExtMapperTest extends MapperTestBase<AttributeExt, DBAttributeExtBase>
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
	protected DBAttributeExtBase getFullDBObject()
	{
		return DBAttributeExtBase.builder()
				.withRemoteIdp("remoteIdp")
				.withTranslationProfile("translationProfile")
				.withValues(List.of("v1", "v2"))
				.withCreationTs(new Date(100L))
				.withUpdateTs(new Date(1000L))
				.withDirect(false)
				.build();
	}

	@Override
	protected Pair<Function<AttributeExt, DBAttributeExtBase>, Function<DBAttributeExtBase, AttributeExt>> getMapper()
	{
		return Pair.of(AttributeExtBaseMapper::map, a -> AttributeExtBaseMapper.map(a, "attr", "syntax", "/A"));
	}

}
