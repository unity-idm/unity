/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.imunity.rest.api.types.basic.RestAttributeStatement;
import io.imunity.rest.api.types.basic.RestGroup;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.Group;

public class GroupMapper
{
	public static RestGroup map(Group group)
	{
		return RestGroup.builder()
				.withPublicGroup(group.isPublic())
				.withI18nDescription(Optional.ofNullable(group.getDescription())
						.map(I18nStringMapper::map)
						.orElse(null))
				.withDisplayedName(Optional.ofNullable(group.getDisplayedName())
						.map(I18nStringMapper::map)
						.orElse(null))
				.withAttributeStatements(Arrays.stream(group.getAttributeStatements())
						.map(as -> Optional.ofNullable(as)
								.map(AttributeStatementMapper::map)
								.orElse(null))
						.toArray(RestAttributeStatement[]::new))
				.withDelegationConfiguration(Optional.ofNullable(group.getDelegationConfiguration())
						.map(GroupDelegationConfigurationMapper::map)
						.orElse(null))
				.withAttributesClasses(group.getAttributesClasses())
				.withPath(group.getPathEncoded())
				.withProperties(group.getProperties()
						.values()
						.stream()
						.map(GroupPropertyMapper::map)
						.collect(Collectors.toList()))
				.build();
	}

	public static Group map(RestGroup rgroup)
	{
		Group group = new Group(rgroup.path);
		group.setAttributesClasses(Optional.ofNullable(rgroup.attributesClasses)
				.orElse(null));
		group.setAttributeStatements(Optional.ofNullable(rgroup.attributeStatements)
				.map(as -> Stream.of(as)
						.map(a -> Optional.ofNullable(a)
								.map(AttributeStatementMapper::map)
								.orElse(null))
						.collect(Collectors.toList())
						.toArray(new AttributeStatement[rgroup.attributeStatements.length]))
				.orElse(null));
		group.setDelegationConfiguration(Optional.ofNullable(rgroup.delegationConfiguration)
				.map(GroupDelegationConfigurationMapper::map)
				.orElse(null));
		group.setDescription(Optional.ofNullable(rgroup.i18nDescription)
				.map(I18nStringMapper::map)
				.orElse(null));
		group.setDisplayedName(Optional.ofNullable(rgroup.displayedName)
				.map(I18nStringMapper::map)
				.orElse(null));
		group.setPublic(rgroup.publicGroup);
		group.setProperties(Optional.ofNullable(rgroup.properties)
				.map(gp -> gp.stream()
						.map(GroupPropertyMapper::map)
						.collect(Collectors.toList()))
				.orElse(null));
		return group;
	}
}
