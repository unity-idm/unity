/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.base.policy_agreement;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import pl.edu.icm.unity.base.i18n.I18nString;

public class PolicyAgreementConfiguration
{
	public final List<Long> documentsIdsToAccept;
	public final PolicyAgreementPresentationType presentationType;
	public final I18nString text;

	@JsonCreator
	public PolicyAgreementConfiguration(@JsonProperty("documentsIdsToAccept") List<Long> documentsIdsToAccept,
			@JsonProperty("presentationType") PolicyAgreementPresentationType presentationType,
			@JsonProperty("text") I18nString text)
	{
		this.documentsIdsToAccept = Collections.unmodifiableList(
				documentsIdsToAccept == null ? Collections.emptyList() : documentsIdsToAccept);

		this.presentationType = presentationType;
		this.text = text;
	}

	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof PolicyAgreementConfiguration))
			return false;
		PolicyAgreementConfiguration castOther = (PolicyAgreementConfiguration) other;
		return Objects.equals(documentsIdsToAccept, castOther.documentsIdsToAccept)
				&& Objects.equals(presentationType, castOther.presentationType)
				&& Objects.equals(text, castOther.text);

	}

	@Override
	public int hashCode()
	{
		return Objects.hash(documentsIdsToAccept, presentationType, text);
	}

}
