/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.attributetype;

import java.util.HashMap;
import java.util.Optional;

import pl.edu.icm.unity.store.types.I18nStringMapper;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeType;

class AttributeTypeBaseMapper
{
	static DBAttributeTypeBase map(AttributeType attributeType)
	{
		return DBAttributeTypeBase.builder()
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

	static AttributeType map(DBAttributeTypeBase dbAttributeTypeBase, String name, String syntaxId)
	{
		AttributeType type = new AttributeType(name, syntaxId, Optional.ofNullable(dbAttributeTypeBase.displayedName)
				.map(I18nStringMapper::map)
				.orElse(new I18nString(name)),
				Optional.ofNullable(dbAttributeTypeBase.i18nDescription)
						.map(I18nStringMapper::map)
						.orElse(new I18nString(dbAttributeTypeBase.description)));
		type.setFlags(dbAttributeTypeBase.flags);
		type.setGlobal(dbAttributeTypeBase.global);
		type.setMaxElements(dbAttributeTypeBase.maxElements);
		type.setMinElements(dbAttributeTypeBase.minElements);
		type.setUniqueValues(dbAttributeTypeBase.uniqueValues);
		type.setValueSyntaxConfiguration(dbAttributeTypeBase.syntaxState);
		type.setSelfModificable(dbAttributeTypeBase.selfModificable);
		type.setMetadata(Optional.ofNullable(dbAttributeTypeBase.metadata)
				.orElse(new HashMap<>()));
		return type;

	}
}
