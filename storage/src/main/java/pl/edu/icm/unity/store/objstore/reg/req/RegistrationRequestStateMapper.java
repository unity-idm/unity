/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.req;

import java.util.Optional;
import java.util.stream.Collectors;

import pl.edu.icm.unity.base.registration.RegistrationRequestState;
import pl.edu.icm.unity.base.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.store.objstore.reg.common.AdminCommentMapper;
import pl.edu.icm.unity.store.objstore.reg.common.RegistrationContextMapper;


public class RegistrationRequestStateMapper
{
	public static DBRegistrationRequestState map(RegistrationRequestState registrationRequestState)
	{
		return DBRegistrationRequestState.builder()
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

	public static RegistrationRequestState map(DBRegistrationRequestState restRegistrationRequestState)
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
