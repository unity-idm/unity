/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.tprofile;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.translation.ProfileType;
import pl.edu.icm.unity.server.translation.TranslationProfile;
import pl.edu.icm.unity.server.translation.in.InputTranslationProfile;
import pl.edu.icm.unity.server.translation.in.InputTranslationRule;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * Component to edit or add input translation profile
 * 
 * @author P. Piernik
 * 
 */
public class InputTranslationProfileEditor extends TranslationProfileEditor
{
	public InputTranslationProfileEditor(UnityMessageSource msg,
			TranslationActionsRegistry registry, TranslationProfile toEdit,
			AttributesManagement attrsMan, IdentitiesManagement idMan, AuthenticationManagement authnMan,
			GroupsManagement groupsMan) throws EngineException
	{
		super(msg, registry, toEdit, attrsMan, idMan, authnMan, groupsMan);
	}

	@Override
	public InputTranslationProfile getProfile() throws Exception
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
			throw new FormValidationException();
		String n = name.getValue();
		String desc = description.getValue();
		List<InputTranslationRule> trules = new ArrayList<InputTranslationRule>();
		for (RuleComponent cr : rules)
		{
			InputTranslationRule r = (InputTranslationRule) cr.getRule();
			if (r != null)
			{
				trules.add(r);
			}

		}
		InputTranslationProfile profile = new InputTranslationProfile(n, trules);
		profile.setDescription(desc);
		return profile;
	}

	@Override
	protected ProfileType getProfileType()
	{
		return ProfileType.INPUT;
	}
}
