/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.basic;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.I18nString;

@RunWith(MockitoJUnitRunner.class)
public class TestMVELGroup
{
	@Mock
	private MessageSource msg;

	@Test
	public void shouldReturnEncodedPath()
	{
		Group root = new Group("/");
		root.setDisplayedName(new I18nString("Root"));
		Group a = new Group("/a");
		a.setDisplayedName(new I18nString("GrA"));
		Group ab = new Group("/a/b");
		ab.setDisplayedName(new I18nString("GrB"));

		Map<String, Group> groupProvider = new HashMap<>();
		groupProvider.put("/", root);
		groupProvider.put("/a", a);
		groupProvider.put("/a/b", ab);
		MVELGroup mvelGroup = new MVELGroup(ab, s -> groupProvider.get(s));
		
		assertThat(mvelGroup.getEncodedGroupPath(":", g -> g.getDisplayedName().getDefaultValue()), is("Root:GrA:GrB"));

	}
}
