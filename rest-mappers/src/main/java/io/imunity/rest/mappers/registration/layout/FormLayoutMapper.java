/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.registration.layout;

import java.util.Optional;
import java.util.stream.Collectors;

import io.imunity.rest.api.types.registration.layout.RestFormLayout;
import pl.edu.icm.unity.base.registration.layout.FormLayout;

public class FormLayoutMapper
{
	public static RestFormLayout map(FormLayout formLayout)
	{
		return RestFormLayout.builder()
				.withElements(Optional.ofNullable(formLayout.getElements())
						.map(e -> e.stream()
								.map(FormLayoutElementMapper::map)
								.collect(Collectors.toList()))
						.orElse(null))
				.build();
	}

	public static FormLayout map(RestFormLayout restFormLayout)
	{
		return new FormLayout(Optional.ofNullable(restFormLayout.elements)
						.map(e -> e.stream()
								.map(FormLayoutElementMapper::map)
								.collect(Collectors.toList()))
						.orElse(null));
	}
}
