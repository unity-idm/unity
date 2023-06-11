/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.layout;

import java.util.Optional;
import java.util.stream.Collectors;

import pl.edu.icm.unity.base.registration.layout.FormLayout;

public class FormLayoutMapper
{
	public static DBFormLayout map(FormLayout formLayout)
	{
		return DBFormLayout.builder()
				.withElements(Optional.ofNullable(formLayout.getElements())
						.map(e -> e.stream()
								.map(FormLayoutElementMapper::map)
								.collect(Collectors.toList()))
						.orElse(null))
				.build();
	}

	public static FormLayout map(DBFormLayout restFormLayout)
	{
		return new FormLayout(Optional.ofNullable(restFormLayout.elements)
						.map(e -> e.stream()
								.map(FormLayoutElementMapper::map)
								.collect(Collectors.toList()))
						.orElse(null));
	}
}
