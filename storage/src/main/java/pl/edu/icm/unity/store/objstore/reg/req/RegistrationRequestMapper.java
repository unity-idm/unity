/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.req;

import java.util.Optional;
import java.util.stream.Collectors;

import pl.edu.icm.unity.store.impl.attribute.AttributeMapper;
import pl.edu.icm.unity.store.objstore.reg.common.CredentialParamValueMapper;
import pl.edu.icm.unity.store.objstore.reg.common.GroupSelectionMapper;
import pl.edu.icm.unity.store.objstore.reg.common.IdentityParamMapper;
import pl.edu.icm.unity.store.objstore.reg.common.PolicyAgreementDecisionMapper;
import pl.edu.icm.unity.store.objstore.reg.common.SelectionMapper;
import pl.edu.icm.unity.types.registration.RegistrationRequest;

public class RegistrationRequestMapper
{
	public static DBRegistrationRequest map(RegistrationRequest registrationRequest)
	{
		return DBRegistrationRequest.builder()
				.withAgreements(Optional.ofNullable(registrationRequest.getAgreements())
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(SelectionMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withAttributes(Optional.ofNullable(registrationRequest.getAttributes())
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(AttributeMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withComments(registrationRequest.getComments())
				.withCredentials(Optional.ofNullable(registrationRequest.getCredentials())
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(CredentialParamValueMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withFormId(registrationRequest.getFormId())
				.withGroupSelections(Optional.ofNullable(registrationRequest.getGroupSelections())
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(GroupSelectionMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withIdentities(Optional.ofNullable(registrationRequest.getIdentities())
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(IdentityParamMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withPolicyAgreements(Optional.ofNullable(registrationRequest.getPolicyAgreements())
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(PolicyAgreementDecisionMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withRegistrationCode(registrationRequest.getRegistrationCode())
				.withUserLocale(registrationRequest.getUserLocale())
				.build();
	}

	public static RegistrationRequest map(DBRegistrationRequest restRegistrationRequest)
	{
		RegistrationRequest registrationRequest = new RegistrationRequest();
		registrationRequest.setAgreements(Optional.ofNullable(restRegistrationRequest.agreements)
				.map(p -> p.stream()
						.map(a -> Optional.ofNullable(a)
								.map(SelectionMapper::map)
								.orElse(null))
						.collect(Collectors.toList()))
				.orElse(null));
		registrationRequest.setAttributes(Optional.ofNullable(restRegistrationRequest.attributes)
				.map(p -> p.stream()
						.map(a -> Optional.ofNullable(a)
								.map(AttributeMapper::map)
								.orElse(null))
						.collect(Collectors.toList()))
				.orElse(null));

		registrationRequest.setComments(restRegistrationRequest.comments);
		registrationRequest.setCredentials(Optional.ofNullable(restRegistrationRequest.credentials)
				.map(p -> p.stream()
						.map(a -> Optional.ofNullable(a)
								.map(CredentialParamValueMapper::map)
								.orElse(null))
						.collect(Collectors.toList()))
				.orElse(null));
		registrationRequest.setFormId(restRegistrationRequest.formId);
		registrationRequest.setGroupSelections(Optional.ofNullable(restRegistrationRequest.groupSelections)
				.map(p -> p.stream()
						.map(a -> Optional.ofNullable(a)
								.map(GroupSelectionMapper::map)
								.orElse(null))
						.collect(Collectors.toList()))
				.orElse(null));
		registrationRequest.setIdentities(Optional.ofNullable(restRegistrationRequest.identities)
				.map(p -> p.stream()
						.map(a -> Optional.ofNullable(a)
								.map(IdentityParamMapper::map)
								.orElse(null))
						.collect(Collectors.toList()))
				.orElse(null));
		registrationRequest.setPolicyAgreements(Optional.ofNullable(restRegistrationRequest.policyAgreements)
				.map(p -> p.stream()
						.map(a -> Optional.ofNullable(a)
								.map(PolicyAgreementDecisionMapper::map)
								.orElse(null))
						.collect(Collectors.toList()))
				.orElse(null));
		registrationRequest.setRegistrationCode(restRegistrationRequest.registrationCode);
		registrationRequest.setUserLocale(restRegistrationRequest.userLocale);

		return registrationRequest;

	}

}
