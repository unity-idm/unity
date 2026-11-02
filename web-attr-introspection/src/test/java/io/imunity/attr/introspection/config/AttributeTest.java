/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import pl.edu.icm.unity.base.Constants;

class AttributeTest
{
	@Test
	void shouldDeserializeAttributeWithoutDescription() throws JsonProcessingException
	{
		// when
		Attribute attribute = Constants.MAPPER.readValue("{\"name\":\"name\",\"mandatory\":true}", Attribute.class);

		// then
		assertThat(attribute.name).isEqualTo("name");
		assertThat(attribute.description).isNull();
		assertThat(attribute.mandatory).isTrue();
	}

	@Test
	void shouldTreatOmittedMandatoryFlagAsOptional() throws JsonProcessingException
	{
		// when
		Attribute attribute = Constants.MAPPER.readValue("{\"name\":\"name\"}", Attribute.class);

		// then
		assertThat(attribute.mandatory).isFalse();
	}
}
