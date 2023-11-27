/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.mappers;

import java.util.stream.Collectors;

import io.imunity.rest.api.types.basic.RestEntity;
import pl.edu.icm.unity.rest.mappers.authn.CredentialInfoMapper;
import pl.edu.icm.unity.types.basic.Entity;

public class EntityMapper
{
	public static RestEntity map(Entity entity)
	{
		return RestEntity.builder()
				.withIdentities(entity.getIdentities()
						.stream()
						.map(i -> IdentityMapper.map(i))
						.collect(Collectors.toList()))
				.withCredentialInfo(CredentialInfoMapper.map(entity.getCredentialInfo()))
				.withEntityInformation(EntityInformationMapper.map(entity.getEntityInformation()))
				.build();
	}

	static Entity map(RestEntity restEntity)
	{
		return new Entity(restEntity.identities.stream()
				.map(IdentityMapper::map)
				.collect(Collectors.toList()), EntityInformationMapper.map(restEntity.entityInformation),
				CredentialInfoMapper.map(restEntity.credentialInfo));
	}
}
