/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.config;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.imunity.scim.console.mapping.AttributeMappingBean;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UndefinedMapping implements AttributeMapping
{
	public static final String id = "Undefined";

	@Override
	public Optional<DataArray> getDataArray()
	{
		return Optional.empty();
	}

	@Override
	public String getEvaluatorId()
	{
		return id;
	}

	@Override
	public AttributeMappingBean toBean()
	{
		AttributeMappingBean bean = new AttributeMappingBean();
		return bean;
	}
}
