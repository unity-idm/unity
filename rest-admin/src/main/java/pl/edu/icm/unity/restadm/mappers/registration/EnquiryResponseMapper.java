/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration;

import java.util.Optional;
import java.util.stream.Collectors;

import io.imunity.rest.api.types.registration.RestEnquiryResponse;
import pl.edu.icm.unity.base.registration.EnquiryResponse;
import pl.edu.icm.unity.restadm.mappers.AttributeMapper;
import pl.edu.icm.unity.restadm.mappers.IdentityParamMapper;
import pl.edu.icm.unity.restadm.mappers.policyAgreement.PolicyAgreementDecisionMapper;

public class EnquiryResponseMapper
{
	public static RestEnquiryResponse map(EnquiryResponse enquiryResponse)
	{
		return RestEnquiryResponse.builder()
				.withAgreements(Optional.ofNullable(enquiryResponse.getAgreements())
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(SelectionMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withAttributes(Optional.ofNullable(enquiryResponse.getAttributes())
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(AttributeMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withComments(enquiryResponse.getComments())
				.withCredentials(Optional.ofNullable(enquiryResponse.getCredentials())
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(CredentialParamValueMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withFormId(enquiryResponse.getFormId())
				.withGroupSelections(Optional.ofNullable(enquiryResponse.getGroupSelections())
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(GroupSelectionMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withIdentities(Optional.ofNullable(enquiryResponse.getIdentities())
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(IdentityParamMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withPolicyAgreements(Optional.ofNullable(enquiryResponse.getPolicyAgreements())
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(PolicyAgreementDecisionMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withRegistrationCode(enquiryResponse.getRegistrationCode())
				.withUserLocale(enquiryResponse.getUserLocale())
				.build();
	}

	public static EnquiryResponse map(RestEnquiryResponse restEnquiryResponse)
	{
		EnquiryResponse enquiryResponse = new EnquiryResponse();
		enquiryResponse.setAgreements(Optional.ofNullable(restEnquiryResponse.agreements)
				.map(p -> p.stream()
						.map(a -> Optional.ofNullable(a)
								.map(SelectionMapper::map)
								.orElse(null))
						.collect(Collectors.toList()))
				.orElse(null));
		enquiryResponse.setAttributes(Optional.ofNullable(restEnquiryResponse.attributes)
				.map(p -> p.stream()
						.map(a -> Optional.ofNullable(a)
								.map(AttributeMapper::map)
								.orElse(null))
						.collect(Collectors.toList()))
				.orElse(null));

		enquiryResponse.setComments(restEnquiryResponse.comments);
		enquiryResponse.setCredentials(Optional.ofNullable(restEnquiryResponse.credentials)
				.map(p -> p.stream()
						.map(a -> Optional.ofNullable(a)
								.map(CredentialParamValueMapper::map)
								.orElse(null))
						.collect(Collectors.toList()))
				.orElse(null));
		enquiryResponse.setFormId(restEnquiryResponse.formId);
		enquiryResponse.setGroupSelections(Optional.ofNullable(restEnquiryResponse.groupSelections)
				.map(p -> p.stream()
						.map(a -> Optional.ofNullable(a)
								.map(GroupSelectionMapper::map)
								.orElse(null))
						.collect(Collectors.toList()))
				.orElse(null));
		enquiryResponse.setIdentities(Optional.ofNullable(restEnquiryResponse.identities)
				.map(p -> p.stream()
						.map(a -> Optional.ofNullable(a)
								.map(IdentityParamMapper::map)
								.orElse(null))
						.collect(Collectors.toList()))
				.orElse(null));
		enquiryResponse.setPolicyAgreements(Optional.ofNullable(restEnquiryResponse.policyAgreements)
				.map(p -> p.stream()
						.map(a -> Optional.ofNullable(a)
								.map(PolicyAgreementDecisionMapper::map)
								.orElse(null))
						.collect(Collectors.toList()))
				.orElse(null));
		enquiryResponse.setRegistrationCode(restEnquiryResponse.registrationCode);
		enquiryResponse.setUserLocale(restEnquiryResponse.userLocale);

		return enquiryResponse;

	}

}
