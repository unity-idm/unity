/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.policyAgreement;

import java.util.Collections;
import java.util.List;

import pl.edu.icm.unity.types.I18nString;

public class PolicyAgreementConfiguration
{
	public final List<Long> documentsIdsToAccept;
	public final PolicyAgreementPresentationType presentationType;
	public final I18nString text;

	public PolicyAgreementConfiguration(List<Long> documentsIdsToAccept,
			PolicyAgreementPresentationType presentationType, I18nString text)
	{
		this.documentsIdsToAccept = documentsIdsToAccept == null ? Collections.unmodifiableList(Collections.emptyList())
				: Collections.unmodifiableList(documentsIdsToAccept);;
		this.presentationType = presentationType;
		this.text = text;
	}
}
