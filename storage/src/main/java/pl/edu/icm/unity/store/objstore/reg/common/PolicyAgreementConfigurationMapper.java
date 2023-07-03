/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.Optional;

import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementPresentationType;
import pl.edu.icm.unity.store.types.common.I18nStringMapper;

class PolicyAgreementConfigurationMapper
{
	static DBPolicyAgreementConfiguration map(PolicyAgreementConfiguration policyAgreementConfiguration)
	{
		return DBPolicyAgreementConfiguration.builder()
				.withDocumentsIdsToAccept(Optional.ofNullable(policyAgreementConfiguration.documentsIdsToAccept)
						.orElse(null))
				.withPresentationType(policyAgreementConfiguration.presentationType.name())
				.withText(Optional.ofNullable(policyAgreementConfiguration.text)
						.map(I18nStringMapper::map)
						.orElse(null))
				.build();
	}

	static PolicyAgreementConfiguration map(DBPolicyAgreementConfiguration restPolicyAgreementConfiguration)
	{
		return new PolicyAgreementConfiguration(restPolicyAgreementConfiguration.documentsIdsToAccept,
				PolicyAgreementPresentationType.valueOf(restPolicyAgreementConfiguration.presentationType),
				Optional.ofNullable(restPolicyAgreementConfiguration.text)
						.map(I18nStringMapper::map)
						.orElse(null));
	}
}
