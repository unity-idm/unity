/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.eresp;

import java.util.Optional;
import java.util.stream.Collectors;

import pl.edu.icm.unity.store.objstore.reg.common.AdminCommentMapper;
import pl.edu.icm.unity.store.objstore.reg.common.RegistrationContextMapper;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;


public class EnquiryResponseStateMapper
{
	public static DBEnquiryResponseState map(EnquiryResponseState registrationRequestState)
	{
		return DBEnquiryResponseState.builder()
				.withAdminComments(Optional.ofNullable(registrationRequestState.getAdminComments())
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(AdminCommentMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withRegistrationContext(
						RegistrationContextMapper.map(registrationRequestState.getRegistrationContext()))
				.withRequest(EnquiryResponseMapper.map(registrationRequestState.getRequest()))
				.withRequestId(registrationRequestState.getRequestId())
				.withStatus(Optional.ofNullable(registrationRequestState.getStatus())
						.map(s -> s.name())
						.orElse(null))
				.withTimestamp(registrationRequestState.getTimestamp())
				.withEntityId(registrationRequestState.getEntityId())
				.build();
	}

	public static EnquiryResponseState map(DBEnquiryResponseState restRegistrationRequestState)
	{
		EnquiryResponseState registrationRequestState = new EnquiryResponseState();
		registrationRequestState.setAdminComments(Optional.ofNullable(restRegistrationRequestState.adminComments)
				.map(p -> p.stream()
						.map(a -> Optional.ofNullable(a)
								.map(AdminCommentMapper::map)
								.orElse(null))
						.collect(Collectors.toList()))
				.orElse(null));
		registrationRequestState.setEntityId(restRegistrationRequestState.entityId);
		registrationRequestState.setRegistrationContext(
				RegistrationContextMapper.map(restRegistrationRequestState.registrationContext));
		registrationRequestState.setRequest(EnquiryResponseMapper.map(restRegistrationRequestState.request));
		registrationRequestState.setRequestId(restRegistrationRequestState.requestId);
		registrationRequestState.setStatus(Optional.ofNullable(restRegistrationRequestState.status)
				.map(RegistrationRequestStatus::valueOf)
				.orElse(null));
		registrationRequestState.setTimestamp(restRegistrationRequestState.timestamp);
		return registrationRequestState;
	}

}
