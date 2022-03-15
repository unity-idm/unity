/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user.mapping.evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;

@Component("MappingEvaluatorRegistry")
@Primary
public class MappingEvaluatorRegistry extends TypesRegistryBase<MappingEvaluator>
{
	@Autowired
	public MappingEvaluatorRegistry(Optional<List<MappingEvaluator>> typeElements)
	{
		super(typeElements.orElseGet(ArrayList::new));
	}

	@Override
	protected String getId(MappingEvaluator from)
	{
		return from.getId();
	}
}
