/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.idp;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.types.I18nString;

public class IdpPolicyAgreementsConfiguration
{
	public I18nString title;
	public I18nString information;
	public List<PolicyAgreementConfiguration> agreements;

	public IdpPolicyAgreementsConfiguration()
	{

	}

	public I18nString getTitle()
	{
		return title;
	}

	public void setTitle(I18nString title)
	{
		this.title = title;
	}

	public I18nString getInformation()
	{
		return information;
	}

	public void setInformation(I18nString information)
	{
		this.information = information;
	}

	public List<PolicyAgreementConfiguration> getAgreements()
	{
		return agreements;
	}

	public void setAgreements(List<PolicyAgreementConfiguration> agreements)
	{
		this.agreements = agreements;
	}

	public Properties toProperties(UnityMessageSource msg)
	{
		Properties ret = new Properties();
		if (title != null && !title.isEmpty())
		{
			title.toProperties(ret, CommonIdPProperties.POLICY_AGREEMENTS_TITLE, msg);
		}
		if (information != null && !information.isEmpty())
		{
			information.toProperties(ret, CommonIdPProperties.POLICY_AGREEMENTS_INFO, msg);
		}
		for (PolicyAgreementConfiguration agreement : agreements)
		{
			ret.putAll(agreement.toProperties(msg, CommonIdPProperties.POLICY_AGREEMENTS_PFX
					+ (agreements.indexOf(agreement) + 1) + "."));
		}
		return ret;
	}

	public void fromPropoerties(UnityMessageSource msg, UnityPropertiesHelper properties)
	{
		title = properties.getLocalizedStringWithoutFallbackToDefault(msg,
				CommonIdPProperties.POLICY_AGREEMENTS_TITLE);
		information = properties.getLocalizedStringWithoutFallbackToDefault(msg,
				CommonIdPProperties.POLICY_AGREEMENTS_INFO);
		agreements = new ArrayList<>();

		for (String key : properties.getStructuredListKeys(CommonIdPProperties.POLICY_AGREEMENTS_PFX))
		{
			PolicyAgreementConfiguration config = PolicyAgreementConfiguration.fromProperties(msg,
					properties, key);
			agreements.add(config);
		}
	}

}
