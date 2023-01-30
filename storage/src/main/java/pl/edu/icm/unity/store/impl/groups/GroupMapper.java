/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.groups;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import pl.edu.icm.unity.store.types.I18nStringMapper;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;

class GroupMapper
{
	static DBGroupBase mapBaseGroup(Group group)
	{
		return DBGroupBase.builder()
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
						.toArray(DBAttributeStatement[]::new))
				.withDelegationConfiguration(Optional.ofNullable(group.getDelegationConfiguration())
						.map(GroupDelegationConfigurationMapper::map)
						.orElse(null))
				.withAttributesClasses(group.getAttributesClasses())
				.withProperties(group.getProperties()
						.values()
						.stream()
						.map(GroupPropertyMapper::map)
						.collect(Collectors.toList()))
				.build();
	}

	static Group mapFromBaseGroup(DBGroupBase rgroup, String path)
	{
		Group group = new Group(path);
		group.setAttributesClasses(Optional.ofNullable(rgroup.attributesClasses)
				.orElse(new HashSet<>()));
		group.setAttributeStatements(Optional.ofNullable(rgroup.attributeStatements)
				.map(as -> Stream.of(as)
						.map(a -> Optional.ofNullable(a)
								.map(AttributeStatementMapper::map)
								.orElse(null))
						.collect(Collectors.toList())
						.toArray(new AttributeStatement[rgroup.attributeStatements.length]))
				.orElse(new AttributeStatement[0]));
		group.setDelegationConfiguration(Optional.ofNullable(rgroup.delegationConfiguration)
				.map(GroupDelegationConfigurationMapper::map)
				.orElse(new GroupDelegationConfiguration(false)));
		group.setDescription(Optional.ofNullable(rgroup.i18nDescription)
				.map(I18nStringMapper::map)
				.orElse(new I18nString(rgroup.description)));
		group.setDisplayedName(Optional.ofNullable(rgroup.displayedName)
				.map(I18nStringMapper::map)
				.orElse(new I18nString(group.toString())));
		group.setPublic(rgroup.publicGroup);
		group.setProperties(Optional.ofNullable(rgroup.properties)
				.map(gp -> gp.stream()
						.map(GroupPropertyMapper::map)
						.collect(Collectors.toList()))
				.orElse(Collections.emptyList()));
		return group;
	}
	
	static DBGroup map(Group group)
	{
		return DBGroup.builder().withPath(group.getPathEncoded())
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
						.toArray(DBAttributeStatement[]::new))
				.withDelegationConfiguration(Optional.ofNullable(group.getDelegationConfiguration())
						.map(GroupDelegationConfigurationMapper::map)
						.orElse(null))
				.withAttributesClasses(group.getAttributesClasses())
				.withProperties(group.getProperties()
						.values()
						.stream()
						.map(GroupPropertyMapper::map)
						.collect(Collectors.toList()))
				.build();
	}
	
	static Group map(DBGroup rgroup)
	{
		return mapFromBaseGroup(rgroup, rgroup.path);
	}
	
	
	
}
