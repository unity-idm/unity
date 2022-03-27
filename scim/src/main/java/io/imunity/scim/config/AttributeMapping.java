/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.config;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.imunity.scim.console.AttributeMappingBean;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "mappingType", defaultImpl = UndefinedMapping.class, visible=true)
@JsonSubTypes(
{ @Type(value = ComplexAttributeMapping.class, name = "Complex"),
		@Type(value = SimpleAttributeMapping.class, name = "Simple"),
		@Type(value = ReferenceAttributeMapping.class, name = "Reference") })
public interface AttributeMapping
{
	Optional<DataArray> getDataArray();
	String getEvaluatorId();

	AttributeMappingBean toBean();
}
