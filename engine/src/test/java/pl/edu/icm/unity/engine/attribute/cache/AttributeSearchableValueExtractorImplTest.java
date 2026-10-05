/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.store.api.AttributeSearchableValueExtractor;

@ExtendWith(MockitoExtension.class)
public class AttributeSearchableValueExtractorImplTest
{
	@Mock
	private AttributeTypeHelper attributeTypeHelper;
	@Mock
	private AttributeValueSyntax<?> syntax;

	@Test
	public void shouldJoinValuesForSearchableSyntax()
	{
		doReturn(syntax).when(attributeTypeHelper).getUnconfiguredSyntax("string");
		when(syntax.isSearchable()).thenReturn(true);
		AttributeSearchableValueExtractor extractor = new AttributeSearchableValueExtractorImpl(attributeTypeHelper);

		String result = extractor.getSearchableValue("string", List.of("foo", "bar"));

		assertThat(result).isEqualTo("foo bar");
	}

	@Test
	public void shouldReturnNullForNonSearchableSyntax()
	{
		doReturn(syntax).when(attributeTypeHelper).getUnconfiguredSyntax("jpegImage");
		when(syntax.isSearchable()).thenReturn(false);
		AttributeSearchableValueExtractor extractor = new AttributeSearchableValueExtractorImpl(attributeTypeHelper);

		assertThat(extractor.getSearchableValue("jpegImage", List.of("base64data"))).isNull();
	}

	@Test
	public void shouldReturnNullForEmptyValuesWithoutLookingUpSyntax()
	{
		AttributeSearchableValueExtractor extractor = new AttributeSearchableValueExtractorImpl(attributeTypeHelper);

		assertThat(extractor.getSearchableValue("string", List.of())).isNull();
	}
}
