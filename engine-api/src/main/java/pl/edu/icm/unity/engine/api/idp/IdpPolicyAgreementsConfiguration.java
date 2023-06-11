/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.idp;

import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.policyAgreement.PolicyAgreementConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class IdpPolicyAgreementsConfiguration
{
	public final I18nString title;
	public final I18nString information;
	public final int width;
	public final String widthUnit;
	public final List<PolicyAgreementConfiguration> agreements;

	public IdpPolicyAgreementsConfiguration(MessageSource msg)
	{
		this(msg.getI18nMessage("PolicyAgreementsConfiguration.defaultTitle"), null, 40, "em",
				new ArrayList<>());
	}

	public IdpPolicyAgreementsConfiguration(I18nString title, I18nString information, int width, String widthUnit,
			List<PolicyAgreementConfiguration> agreements)
	{
		this.title = title;
		this.information = information;
		this.width = width;
		this.widthUnit = widthUnit;
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
				&& Objects.equals(width, castOther.width)
				&& Objects.equals(widthUnit, castOther.widthUnit)
				&& Objects.equals(agreements, castOther.agreements);

	}

	@Override
	public int hashCode()
	{
		return Objects.hash(title, information, width, widthUnit, agreements);
	}

	@Override
	public String toString()
	{
		return "IdpPolicyAgreementsConfiguration{" +
				"title=" + title +
				", information=" + information +
				", width=" + width +
				", widthUnit='" + widthUnit + '\'' +
				", agreements=" + agreements +
				'}';
	}
}
