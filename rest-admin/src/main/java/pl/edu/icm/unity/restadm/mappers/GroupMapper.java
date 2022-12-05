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
		if (group == null)
			return null;

		return RestGroup.builder()
				.withPublicGroup(group.isPublic())
				.withI18nDescription(I18nStringMapper.map(group.getDescription()))
				.withDisplayedName(I18nStringMapper.map(group.getDisplayedName()))
				.withAttributeStatements(Arrays.stream(group.getAttributeStatements())
						.map(s -> AttributeStatementMapper.map(s))
						.toArray(RestAttributeStatement[]::new))
				.withDelegationConfiguration(GroupDelegationConfigurationMapper.map(group.getDelegationConfiguration()))
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
		if (rgroup == null)
			return null;

		Group group = new Group(rgroup.path);
		group.setAttributesClasses(Optional.ofNullable(rgroup.attributesClasses)
				.orElse(null));
		group.setAttributeStatements(Optional.ofNullable(rgroup.attributeStatements)
				.map(as -> Stream.of(as)
						.map(s -> AttributeStatementMapper.map(s))
						.collect(Collectors.toList())
						.toArray(new AttributeStatement[rgroup.attributeStatements.length]))
				.orElse(null));
		group.setDelegationConfiguration(GroupDelegationConfigurationMapper.map(rgroup.delegationConfiguration));
		group.setDescription(I18nStringMapper.map(rgroup.i18nDescription));
		group.setDisplayedName(I18nStringMapper.map(rgroup.displayedName));
		group.setPublic(rgroup.publicGroup);
		group.setProperties(Optional.ofNullable(rgroup.properties)
				.map(gp -> gp.stream()
						.map(GroupPropertyMapper::map)
						.collect(Collectors.toList()))
				.orElse(null));
		return group;
	}
}
