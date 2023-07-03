/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.idp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementPresentationType;
import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;

/**
 * Maps {@link PolicyAgreementConfiguration} to properties and vice versa
 * 
 * @author P.Piernik
 *
 */
public class IdpPolicyAgreementsConfigurationParser
{
	public static Properties toProperties(MessageSource msg, IdpPolicyAgreementsConfiguration config,
			String prefix)
	{
		Properties ret = new Properties();
		if (config.title != null && !config.title.isEmpty())
		{
			config.title.toProperties(ret, prefix + CommonIdPProperties.POLICY_AGREEMENTS_TITLE, msg);
		}
		if (config.information != null && !config.information.isEmpty())
		{
			config.information.toProperties(ret, prefix + CommonIdPProperties.POLICY_AGREEMENTS_INFO, msg);
		}
	
		ret.put(prefix + CommonIdPProperties.POLICY_AGREEMENTS_WIDTH, String.valueOf(config.width));
		ret.put(prefix + CommonIdPProperties.POLICY_AGREEMENTS_WIDTH_UNIT, config.widthUnit);
		
		for (PolicyAgreementConfiguration agreement : config.agreements)
		{
			ret.putAll(policyAgreementConfigurationtoProperties(msg,
					prefix + CommonIdPProperties.POLICY_AGREEMENTS_PFX
							+ (config.agreements.indexOf(agreement) + 1) + ".",
					agreement));
		}
		return ret;
	}

	public static IdpPolicyAgreementsConfiguration fromPropoerties(MessageSource msg,
			UnityPropertiesHelper properties)
	{
		I18nString title = properties.getLocalizedStringWithoutFallbackToDefault(msg,
				CommonIdPProperties.POLICY_AGREEMENTS_TITLE);
		I18nString information = properties.getLocalizedStringWithoutFallbackToDefault(msg,
				CommonIdPProperties.POLICY_AGREEMENTS_INFO);
		int width = properties.getIntValue(CommonIdPProperties.POLICY_AGREEMENTS_WIDTH);
		String widthUnit = properties.getValue(CommonIdPProperties.POLICY_AGREEMENTS_WIDTH_UNIT);
		
		List<PolicyAgreementConfiguration> agreements = new ArrayList<>();

		for (String key : properties.getStructuredListKeys(CommonIdPProperties.POLICY_AGREEMENTS_PFX))
		{
			PolicyAgreementConfiguration config = policyAgreementConfigurationfromProperties(msg,
					properties, key);
			agreements.add(config);
		}

		return new IdpPolicyAgreementsConfiguration(title, information, width, widthUnit, agreements);
	}

	private static PolicyAgreementConfiguration policyAgreementConfigurationfromProperties(MessageSource msg,
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

	private static Properties policyAgreementConfigurationtoProperties(MessageSource msg, String prefix,
			PolicyAgreementConfiguration config)
	{
		Properties p = new Properties();
		p.put(prefix + CommonIdPProperties.POLICY_AGREEMENT_DOCUMENTS,
				String.join(" ", config.documentsIdsToAccept.stream().map(id -> String.valueOf(id))
						.collect(Collectors.toList())));
		p.put(prefix + CommonIdPProperties.POLICY_AGREEMENT_PRESENTATION_TYPE,
				config.presentationType.toString());
		config.text.toProperties(p, prefix + CommonIdPProperties.POLICY_AGREEMENT_TEXT, msg);
		return p;
	}
}
