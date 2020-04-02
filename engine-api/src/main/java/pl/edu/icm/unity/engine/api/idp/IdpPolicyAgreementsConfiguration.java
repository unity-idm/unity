/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.idp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.types.I18nString;

public class IdpPolicyAgreementsConfiguration
{
	public final I18nString title;
	public final I18nString information;
	public final List<PolicyAgreementConfiguration> agreements;

	public IdpPolicyAgreementsConfiguration()
	{
		this(null, null, new ArrayList<>());
	}

	public IdpPolicyAgreementsConfiguration(I18nString title, I18nString information,
			List<PolicyAgreementConfiguration> agreements)
	{
		this.title = title;
		this.information = information;
		this.agreements = Collections
				.unmodifiableList(agreements == null ? Collections.emptyList() : agreements);
	}

	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof IdpPolicyAgreementsConfiguration))
			return false;
		IdpPolicyAgreementsConfiguration castOther = (IdpPolicyAgreementsConfiguration) other;
		return Objects.equals(title, castOther.title) && Objects.equals(information, castOther.information)
				&& Objects.equals(agreements, castOther.agreements);

	}

	@Override
	public int hashCode()
	{
		return Objects.hash(title, information, agreements);
	}

}
