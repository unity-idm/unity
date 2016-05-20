/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.tprofile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.store.api.generic.OutputTranslationProfileDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 * Handles import/export of {@link TranslationProfile}.
 * @author K. Benedyczak
 */
@Component
public class OutputTranslationProfileIE extends GenericObjectIEBase<TranslationProfile>
{
	@Autowired
	public OutputTranslationProfileIE(OutputTranslationProfileDB dao, ObjectMapper jsonMapper)
	{
		super(dao, jsonMapper, TranslationProfile.class, 109, 
				OutputTranslationProfileHandler.TRANSLATION_PROFILE_OBJECT_TYPE);
	}
}



