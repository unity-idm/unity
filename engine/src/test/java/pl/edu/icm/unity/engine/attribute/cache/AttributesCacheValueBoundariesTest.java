/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.attribute.image.ImageType;
import pl.edu.icm.unity.base.attribute.image.UnityImage;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.engine.api.attributes.AttributeSearchService;
import pl.edu.icm.unity.stdext.attr.ImageAttribute;
import pl.edu.icm.unity.stdext.attr.ImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;

/**
 * Real attribute syntaxes through the whole refresh: maximum length values are cached and searchable, binary
 * (image) values are cached but never searchable.
 */
public class AttributesCacheValueBoundariesTest extends AttributesCacheIntegrationTestBase
{
	private static final int STRING_MAX_LENGTH = 10240;

	@Autowired
	private AttributeSearchService attributeSearchService;

	private long entityId;
	private EntityParam entity;

	@BeforeEach
	public void setup() throws Exception
	{
		AttributeType longText = new AttributeType("longText", StringAttributeSyntax.ID);
		longText.setMaxElements(3);
		aTypeMan.addAttributeType(longText);
		aTypeMan.addAttributeType(new AttributeType("picture", ImageAttributeSyntax.ID));
		groupsMan.addGroup(new Group("/A"));
		entityId = addEntity("e1", "/A");
		entity = new EntityParam(entityId);
	}

	@Test
	public void maximumLengthMultivaluedStringIsRefreshedAndSearchableNearItsEnd() throws Exception
	{
		List<String> values = List.of(
				"a".repeat(2001),
				"b".repeat(STRING_MAX_LENGTH),
				"c".repeat(STRING_MAX_LENGTH - "TAIL".length()) + "TAIL");
		attrsMan.createAttribute(entity, StringAttribute.of("longText", "/A", values));

		startBackgroundRefresh();

		awaitFreshCachedValues(entityId, "/A", "longText", values);
		assertThat(attributeSearchService.searchEntities("/A", "ctail")).containsExactly(entityId);
		assertThat(attributeSearchService.searchEntities("/A", "longText", "ctail")).containsExactly(entityId);
	}

	@Test
	public void imageValueIsCachedButNotSearchable() throws Exception
	{
		UnityImage image = new UnityImage(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), ImageType.PNG);
		attrsMan.createAttribute(entity, ImageAttribute.of("picture", "/A", image));

		startBackgroundRefresh();

		awaitFreshCachedValues(entityId, "/A", "picture", List.of(image.serialize()));
		String storedValue = image.serialize();
		String fragment = storedValue.substring(storedValue.length() / 2, storedValue.length() / 2 + 8);
		assertThat(attributeSearchService.searchEntities("/A", fragment)).isEmpty();
		assertThat(attributeSearchService.searchEntities("/A", "picture", fragment)).isEmpty();
	}
}
