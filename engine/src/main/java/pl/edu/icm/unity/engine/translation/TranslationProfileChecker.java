/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.AttributeValueConverter;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationActionsRegistry;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationActionsRegistry;
import pl.edu.icm.unity.engine.translation.in.InputTranslationProfile;
import pl.edu.icm.unity.engine.translation.in.InputTranslationProfileRepository;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationProfile;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationProfileRepository;

/**
 * Set of helper method for translation profiles
 * No operation in this class performs any authorization.
 * @author P.Piernik
 *
 */
@Component
public class TranslationProfileChecker
{
	private InputTranslationProfileRepository inputRepo;
	private InputTranslationActionsRegistry inputActionReg;
	private OutputTranslationProfileRepository outputRepo;
	private AttributeValueConverter attrConverter;
	private OutputTranslationActionsRegistry outputActionReg;
	private GroupsManagement groupsMan;
	
	@Autowired
	public TranslationProfileChecker(InputTranslationProfileRepository inputRepo,
			InputTranslationActionsRegistry inputActionReg,
			OutputTranslationProfileRepository outputRepo,
			AttributeValueConverter attrConverter,
			OutputTranslationActionsRegistry outputActionReg,
			GroupsManagement groupsMan)
	{
		this.inputRepo = inputRepo;
		this.inputActionReg = inputActionReg;
		this.outputRepo = outputRepo;
		this.attrConverter = attrConverter;
		this.outputActionReg = outputActionReg;
		this.groupsMan = groupsMan;
	}

	public void checkBaseProfileContent(TranslationProfile profile)
	{
		if (profile.getProfileType() != ProfileType.INPUT && profile.getProfileType() != ProfileType.OUTPUT ) 	
			throw new IllegalArgumentException(
					"Unsupported profile type: " + profile.getProfileType());

	}

	public void checkProfileContent(TranslationProfile profile)
	{
		
		TranslationProfileInstance<?, ?> instance;
		if (profile.getProfileType() == ProfileType.INPUT)
			instance = new InputTranslationProfile(profile, inputRepo, inputActionReg);
		else if (profile.getProfileType() == ProfileType.OUTPUT)
			instance = new OutputTranslationProfile(profile, outputRepo,
					outputActionReg, attrConverter, groupsMan);
		else
			throw new IllegalArgumentException(
					"Unsupported profile type: " + profile.getProfileType());
		if (instance.hasInvalidActions())
			throw new IllegalArgumentException("Profile definition is invalid");
	}	
}
