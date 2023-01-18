/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.policyAgreement;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.basic.RestI18nString;
import io.imunity.rest.api.types.policyAgreement.RestPolicyAgreementConfiguration;
import pl.edu.icm.unity.restadm.mappers.MapperTestBase;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementPresentationType;

public class PolicyAgreementConfigurationMapperTest
		extends MapperTestBase<PolicyAgreementConfiguration, RestPolicyAgreementConfiguration>
{

	@Override
	protected PolicyAgreementConfiguration getAPIObject()
	{
		return new PolicyAgreementConfiguration(List.of(1l, 2l), PolicyAgreementPresentationType.CHECKBOX_SELECTED,
				new I18nString("text"));
	}

	@Override
	protected RestPolicyAgreementConfiguration getRestObject()
	{
		return RestPolicyAgreementConfiguration.builder()
				.withDocumentsIdsToAccept(List.of(1l, 2l))
				.withPresentationType("CHECKBOX_SELECTED")
				.withText(RestI18nString.builder()
						.withDefaultValue("text")
						.build())
				.build();
	}

	@Override
	protected Pair<Function<PolicyAgreementConfiguration, RestPolicyAgreementConfiguration>, Function<RestPolicyAgreementConfiguration, PolicyAgreementConfiguration>> getMapper()
	{
		return Pair.of(PolicyAgreementConfigurationMapper::map, PolicyAgreementConfigurationMapper::map);
	}

}
