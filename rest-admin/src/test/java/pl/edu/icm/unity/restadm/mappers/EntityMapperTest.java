/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.authn.RestCredentialInfo;
import io.imunity.rest.api.types.authn.RestCredentialPublicInformation;
import io.imunity.rest.api.types.basic.RestEntity;
import io.imunity.rest.api.types.basic.RestEntityInformation;
import io.imunity.rest.api.types.basic.RestIdentity;
import io.imunity.rest.api.types.confirmation.RestConfirmationInfo;
import pl.edu.icm.unity.types.authn.CredentialInfo;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.Identity;

public class EntityMapperTest extends MapperTestBase<Entity, RestEntity>
{

	@Override
	protected Entity getFullAPIObject()
	{
		Identity id = new Identity("email", "email@email.com", 0, "email@email.com");
		id.setCreationTs(new Date(1));
		id.setUpdateTs(new Date(2));

		return new Entity(List.of(id), new EntityInformation(0), new CredentialInfo("cred",
				Map.of("cpi1", new CredentialPublicInformation(LocalCredentialState.correct, "detail", "extraInfo"))));
	}

	@Override
	protected RestEntity getFullRestObject()
	{
		return RestEntity.builder()
				.withIdentities(List.of(RestIdentity.builder()
						.withCreationTs(new Date(1))
						.withUpdateTs(new Date(2))
						.withComparableValue("email@email.com")
						.withEntityId(0)
						.withTypeId("email")
						.withValue("email@email.com")
						.withConfirmationInfo(RestConfirmationInfo.builder()
								.withConfirmed(false)
								.build())
						.build()))
				.withCredentialInfo(RestCredentialInfo.builder()
						.withCredentialRequirementId("cred")
						.withCredentialsState(Map.of("cpi1", RestCredentialPublicInformation.builder()
								.withExtraInformation("extraInfo")
								.withState("correct")
								.withStateDetail("detail")
								.build()))
						.build())
				.withEntityInformation(RestEntityInformation.builder()
						.withEntityId(0L)
						.withState("valid")
						.build())
				.build();
	}

	@Override
	protected Pair<Function<Entity, RestEntity>, Function<RestEntity, Entity>> getMapper()
	{
		return Pair.of(EntityMapper::map, EntityMapper::map);
	}

}
