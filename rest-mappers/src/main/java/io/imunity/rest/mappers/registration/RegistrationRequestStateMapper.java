/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.registration;

import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;

import java.util.Optional;
import java.util.stream.Collectors;

import io.imunity.rest.api.types.registration.RestRegistrationRequestState;

public class RegistrationRequestStateMapper
{
	public static RestRegistrationRequestState map(RegistrationRequestState registrationRequestState)
	{
		return RestRegistrationRequestState.builder()
				.withAdminComments(Optional.ofNullable(registrationRequestState.getAdminComments())
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(AdminCommentMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withRegistrationContext(
						RegistrationContextMapper.map(registrationRequestState.getRegistrationContext()))
				.withRequest(RegistrationRequestMapper.map(registrationRequestState.getRequest()))
				.withRequestId(registrationRequestState.getRequestId())
				.withStatus(Optional.ofNullable(registrationRequestState.getStatus())
						.map(s -> s.name())
						.orElse(null))
				.withTimestamp(registrationRequestState.getTimestamp())
				.withCreatedEntityId(registrationRequestState.getCreatedEntityId())
				.build();
	}

	public static RegistrationRequestState map(RestRegistrationRequestState restRegistrationRequestState)
	{
		RegistrationRequestState registrationRequestState = new RegistrationRequestState();
		registrationRequestState.setAdminComments(Optional.ofNullable(restRegistrationRequestState.adminComments)
				.map(p -> p.stream()
						.map(a -> Optional.ofNullable(a)
								.map(AdminCommentMapper::map)
								.orElse(null))
						.collect(Collectors.toList()))
				.orElse(null));
		registrationRequestState.setCreatedEntityId(restRegistrationRequestState.createdEntityId);
		registrationRequestState.setRegistrationContext(
				RegistrationContextMapper.map(restRegistrationRequestState.registrationContext));
		registrationRequestState.setRequest(RegistrationRequestMapper.map(restRegistrationRequestState.request));
		registrationRequestState.setRequestId(restRegistrationRequestState.requestId);
		registrationRequestState.setStatus(Optional.ofNullable(restRegistrationRequestState.status)
				.map(RegistrationRequestStatus::valueOf)
				.orElse(null));
		registrationRequestState.setTimestamp(restRegistrationRequestState.timestamp);
		return registrationRequestState;
	}

}
