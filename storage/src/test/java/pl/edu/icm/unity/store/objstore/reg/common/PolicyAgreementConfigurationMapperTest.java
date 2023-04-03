/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.List;
import java.util.function.Function;

import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.store.types.common.DBI18nString;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementPresentationType;

public class PolicyAgreementConfigurationMapperTest
		extends MapperTestBase<PolicyAgreementConfiguration, DBPolicyAgreementConfiguration>
{

	@Override
	protected PolicyAgreementConfiguration getFullAPIObject()
	{
		return new PolicyAgreementConfiguration(List.of(1l, 2l), PolicyAgreementPresentationType.CHECKBOX_SELECTED,
				new I18nString("text"));
	}

	@Override
	protected DBPolicyAgreementConfiguration getFullDBObject()
	{
		return DBPolicyAgreementConfiguration.builder()
				.withDocumentsIdsToAccept(List.of(1l, 2l))
				.withPresentationType("CHECKBOX_SELECTED")
				.withText(DBI18nString.builder()
						.withDefaultValue("text")
						.build())
				.build();
	}

	@Override
	protected Pair<Function<PolicyAgreementConfiguration, DBPolicyAgreementConfiguration>, Function<DBPolicyAgreementConfiguration, PolicyAgreementConfiguration>> getMapper()
	{
		return Pair.of(PolicyAgreementConfigurationMapper::map, PolicyAgreementConfigurationMapper::map);
	}

}
