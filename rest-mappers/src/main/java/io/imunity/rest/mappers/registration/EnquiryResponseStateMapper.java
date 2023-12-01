/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.registration;

import java.util.Optional;
import java.util.stream.Collectors;

import io.imunity.rest.api.types.registration.RestEnquiryResponseState;
import pl.edu.icm.unity.base.registration.EnquiryResponseState;
import pl.edu.icm.unity.base.registration.RegistrationRequestStatus;

public class EnquiryResponseStateMapper
{
	public static RestEnquiryResponseState map(EnquiryResponseState enquiryResponseState)
	{
		return RestEnquiryResponseState.builder()
				.withAdminComments(Optional.ofNullable(enquiryResponseState.getAdminComments())
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(AdminCommentMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withRegistrationContext(
						RegistrationContextMapper.map(enquiryResponseState.getRegistrationContext()))
				.withRequest(EnquiryResponseMapper.map(enquiryResponseState.getRequest()))
				.withRequestId(enquiryResponseState.getRequestId())
				.withStatus(Optional.ofNullable(enquiryResponseState.getStatus())
						.map(s -> s.name())
						.orElse(null))
				.withTimestamp(enquiryResponseState.getTimestamp())
				.withEntityId(enquiryResponseState.getEntityId())
				.build();
	}

	public static EnquiryResponseState map(RestEnquiryResponseState restEnquiryResponseState)
	{
		EnquiryResponseState enquiryResponseState = new EnquiryResponseState();
		enquiryResponseState.setAdminComments(Optional.ofNullable(restEnquiryResponseState.adminComments)
				.map(p -> p.stream()
						.map(a -> Optional.ofNullable(a)
								.map(AdminCommentMapper::map)
								.orElse(null))
						.collect(Collectors.toList()))
				.orElse(null));
		enquiryResponseState.setEntityId(restEnquiryResponseState.entityId);
		enquiryResponseState.setRegistrationContext(
				RegistrationContextMapper.map(restEnquiryResponseState.registrationContext));
		enquiryResponseState.setRequest(EnquiryResponseMapper.map(restEnquiryResponseState.request));
		enquiryResponseState.setRequestId(restEnquiryResponseState.requestId);
		enquiryResponseState.setStatus(Optional.ofNullable(restEnquiryResponseState.status)
				.map(RegistrationRequestStatus::valueOf)
				.orElse(null));
		enquiryResponseState.setTimestamp(restEnquiryResponseState.timestamp);
		return enquiryResponseState;
	}

}
