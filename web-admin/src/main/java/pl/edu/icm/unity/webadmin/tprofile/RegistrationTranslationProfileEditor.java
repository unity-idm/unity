/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.tprofile;

import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.translation.AbstractTranslationProfile;
import pl.edu.icm.unity.server.translation.ProfileType;
import pl.edu.icm.unity.server.translation.TranslationProfile;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationProfile;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationRule;
import pl.edu.icm.unity.server.utils.UnityMessageSource;

/**
 * Component to edit or add a registration form translation profile
 * 
 * @author K. Benedyczak
 * 
 */
public class RegistrationTranslationProfileEditor extends TranslationProfileEditor<RegistrationTranslationRule>
{
	public RegistrationTranslationProfileEditor(UnityMessageSource msg,
			TranslationActionsRegistry registry, TranslationProfile toEdit,
			AttributesManagement attrsMan, IdentitiesManagement idMan, AuthenticationManagement authnMan,
			GroupsManagement groupsMan) throws EngineException
	{
		super(msg, registry, toEdit, attrsMan, idMan, authnMan, groupsMan);
	}

	@Override
	protected ProfileType getProfileType()
	{
		return ProfileType.REGISTRATION;
	}

	@Override
	public AbstractTranslationProfile<RegistrationTranslationRule> createProfile(String name,
			List<RegistrationTranslationRule> trules)
	{
		return new RegistrationTranslationProfile(name, trules);
	}
	
	protected void initUI(TranslationProfile toEdit)
	{
		super.initUI(toEdit);
		name.setVisible(false);
		description.setVisible(false);
	}
}
