/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.policyAgreement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;

public class PolicyAgreementConfiguration
{
	public final List<Long> documentsIdsToAccept;
	public final PolicyAgreementPresentationType presentationType;
	public final I18nString text;

	public PolicyAgreementConfiguration(List<Long> documentsIdsToAccept,
			PolicyAgreementPresentationType presentationType, I18nString text)
	{
		this.documentsIdsToAccept = documentsIdsToAccept;
		this.presentationType = presentationType;
		this.text = text;
	}

	public Properties toProperties(UnityMessageSource msg, String prefix)
	{
		Properties p = new Properties();
		p.put(prefix + CommonIdPProperties.POLICY_AGREEMENT_DOCUMENTS, String.join(" ", documentsIdsToAccept
				.stream().map(id -> String.valueOf(id)).collect(Collectors.toList())));
		p.put(prefix + CommonIdPProperties.POLICY_AGREEMENT_PRESENTATION_TYPE, presentationType.toString());
		text.toProperties(p, prefix + CommonIdPProperties.POLICY_AGREEMENT_TEXT, msg);
		return p;
	}

	public static PolicyAgreementConfiguration fromProperties(UnityMessageSource msg,
			UnityPropertiesHelper properties, String prefix)
	{
		String docsP = properties.getValue(prefix + CommonIdPProperties.POLICY_AGREEMENT_DOCUMENTS);
		List<Long> docs = new ArrayList<>();
		if (docsP != null && !docsP.isEmpty())
		{
			docs.addAll(Arrays.asList(docsP.split(" ")).stream().map(s -> Long.valueOf(s))
					.collect(Collectors.toList()));
		}
		PolicyAgreementPresentationType presentationType = properties.getEnumValue(
				prefix + CommonIdPProperties.POLICY_AGREEMENT_PRESENTATION_TYPE,
				PolicyAgreementPresentationType.class);
		I18nString text = properties.getLocalizedStringWithoutFallbackToDefault(msg,
				prefix + CommonIdPProperties.POLICY_AGREEMENT_TEXT);
		return new PolicyAgreementConfiguration(docs, presentationType, text);

	}
}
