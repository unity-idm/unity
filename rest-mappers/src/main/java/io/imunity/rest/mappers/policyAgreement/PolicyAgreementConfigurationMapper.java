/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.policyAgreement;

import java.util.Optional;

import io.imunity.rest.api.types.policyAgreement.RestPolicyAgreementConfiguration;
import io.imunity.rest.mappers.I18nStringMapper;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementPresentationType;

public class PolicyAgreementConfigurationMapper
{
	public static RestPolicyAgreementConfiguration map(PolicyAgreementConfiguration policyAgreementConfiguration)
	{
		return RestPolicyAgreementConfiguration.builder()
				.withDocumentsIdsToAccept(Optional.ofNullable(policyAgreementConfiguration.documentsIdsToAccept)
						.orElse(null))
				.withPresentationType(policyAgreementConfiguration.presentationType.name())
				.withText(Optional.ofNullable(policyAgreementConfiguration.text)
						.map(I18nStringMapper::map)
						.orElse(null))
				.build();
	}

	public static PolicyAgreementConfiguration map(RestPolicyAgreementConfiguration restPolicyAgreementConfiguration)
	{
		return new PolicyAgreementConfiguration(restPolicyAgreementConfiguration.documentsIdsToAccept,
				PolicyAgreementPresentationType.valueOf(restPolicyAgreementConfiguration.presentationType),
				Optional.ofNullable(restPolicyAgreementConfiguration.text)
						.map(I18nStringMapper::map)
						.orElse(null));
	}
}
