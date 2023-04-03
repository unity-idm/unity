/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.attributetype;

import java.util.HashMap;
import java.util.Optional;

import pl.edu.icm.unity.store.types.common.I18nStringMapper;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeType;

class AttributeTypeMapper
{
	static DBAttributeType map(AttributeType attributeType)
	{
		return DBAttributeType.builder()
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

	static AttributeType map(DBAttributeType dbAttributeType)
	{
		AttributeType type = new AttributeType(dbAttributeType.name, dbAttributeType.syntaxId,
				Optional.ofNullable(dbAttributeType.displayedName)
						.map(I18nStringMapper::map)
						.orElse(new I18nString(dbAttributeType.name)),
				Optional.ofNullable(dbAttributeType.i18nDescription)
						.map(I18nStringMapper::map)
						.orElse(new I18nString(dbAttributeType.description)));
		type.setFlags(dbAttributeType.flags);
		type.setGlobal(dbAttributeType.global);
		type.setMaxElements(dbAttributeType.maxElements);
		type.setMinElements(dbAttributeType.minElements);
		type.setUniqueValues(dbAttributeType.uniqueValues);
		type.setValueSyntaxConfiguration(dbAttributeType.syntaxState);
		type.setSelfModificable(dbAttributeType.selfModificable);
		type.setMetadata(Optional.ofNullable(dbAttributeType.metadata)
				.orElse(new HashMap<>()));
		return type;

	}
}
