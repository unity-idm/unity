/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.tprofile;

import java.util.ArrayList;
import java.util.List;

import com.google.common.html.HtmlEscapers;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.translation.ProfileType;
import pl.edu.icm.unity.server.translation.TranslationProfile;
import pl.edu.icm.unity.server.translation.out.OutputTranslationProfile;
import pl.edu.icm.unity.server.translation.out.OutputTranslationRule;
import pl.edu.icm.unity.server.utils.UnityMessageSource;

/**
 * Component to edit or add input translation profile
 * 
 * @author P. Piernik
 * 
 */
public class OutputTranslationProfileEditor extends TranslationProfileEditor
{
	public OutputTranslationProfileEditor(UnityMessageSource msg,
			TranslationActionsRegistry registry, TranslationProfile toEdit,
			AttributesManagement attrsMan, IdentitiesManagement idMan, AuthenticationManagement authnMan,
			GroupsManagement groupsMan) throws EngineException
	{
		super(msg, registry, toEdit, attrsMan, idMan, authnMan, groupsMan);
	}

	public OutputTranslationProfile getProfile()
	{
		int nvalidr= 0;
		for (RuleComponent cr : rules)
		{
			if (!cr.validateRule())
			{
				nvalidr++;
			}
		}	
		name.setValidationVisible(true);
		if (!(name.isValid() && nvalidr == 0))
		{
			return null;
		}
		String n = name.getValue();
		String desc = description.getValue();
		List<OutputTranslationRule> trules = new ArrayList<OutputTranslationRule>();
		for (RuleComponent cr : rules)
		{
			OutputTranslationRule r = (OutputTranslationRule) cr.getRule();
			if (r != null)
			{
				trules.add(r);
			}

		}
		OutputTranslationProfile profile = new OutputTranslationProfile(n, trules);
		profile.setDescription(HtmlEscapers.htmlEscaper().escape(desc));
		return profile;
	}

	@Override
	protected ProfileType getProfileType()
	{
		return ProfileType.OUTPUT;
	}
}
