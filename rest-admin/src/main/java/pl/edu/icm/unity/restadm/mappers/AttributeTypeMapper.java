/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import java.util.HashMap;
import java.util.Optional;

import io.imunity.rest.api.types.basic.RestAttributeType;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeType;

public class AttributeTypeMapper
{
	public static RestAttributeType map(AttributeType attributeType)
	{
		return RestAttributeType.builder()
				.withName(attributeType.getName())
				.withSyntaxId(attributeType.getValueSyntax())
				.withSyntaxState(attributeType.getValueSyntaxConfiguration())
				.withFlags(attributeType.getFlags())
				.withI18nDescription(Optional.ofNullable(attributeType.getDescription())
						.map(I18nStringMapper::map)
						.orElse(null))
				.withDisplayedName(Optional.ofNullable(attributeType.getDisplayedName())
						.map(I18nStringMapper::map)
						.orElse(null))
				.withMetadata(attributeType.getMetadata())
				.withGlobal(attributeType.isGlobal())
				.withSelfModificable(attributeType.isSelfModificable())
				.withUniqueValues(attributeType.isUniqueValues())
				.withMaxElements(attributeType.getMaxElements())
				.withMinElements(attributeType.getMinElements())
				.build();
	}

	public static AttributeType map(RestAttributeType restAttributeType)
	{
		AttributeType type = new AttributeType(restAttributeType.name, restAttributeType.syntaxId,
				Optional.ofNullable(restAttributeType.displayedName)
						.map(I18nStringMapper::map)
						.orElse(new I18nString(restAttributeType.name)),
				Optional.ofNullable(restAttributeType.i18nDescription)
						.map(I18nStringMapper::map)
						.orElse(new I18nString(restAttributeType.description)));
		type.setFlags(restAttributeType.flags);
		type.setGlobal(restAttributeType.global);
		type.setMaxElements(restAttributeType.maxElements);
		type.setMinElements(restAttributeType.minElements);
		type.setUniqueValues(restAttributeType.uniqueValues);
		type.setValueSyntaxConfiguration(restAttributeType.syntaxState);
		type.setSelfModificable(restAttributeType.selfModificable);
		type.setMetadata(Optional.ofNullable(restAttributeType.metadata).orElse(new HashMap<>()));
		return type;

	}
}
