/*
 * Copyright (c) 2022 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.mvel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import pl.edu.icm.unity.types.basic.Group;

public class CachingMVELGroupProviderTest
{
	@Test
	public void shouldReturnWithParent()
	{
		Map<String, Group> groups = ImmutableMap.of("/", new Group("/"),
				"/parent", new Group("/parent"),
				"/parent/child", new Group("/parent/child"));
		
		CachingMVELGroupProvider provider = new CachingMVELGroupProvider(groups);
		
		MVELGroup mvelGroup = provider.get("/parent/child");
		
		assertThat(mvelGroup.getName()).isEqualTo("/parent/child");
	}
}
