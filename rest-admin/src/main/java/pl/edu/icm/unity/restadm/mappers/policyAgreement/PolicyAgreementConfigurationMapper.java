/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.policyAgreement;

import java.util.Optional;

import io.imunity.rest.api.types.policyAgreement.RestPolicyAgreementConfiguration;
import pl.edu.icm.unity.base.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.base.policyAgreement.PolicyAgreementPresentationType;
import pl.edu.icm.unity.restadm.mappers.I18nStringMapper;

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
