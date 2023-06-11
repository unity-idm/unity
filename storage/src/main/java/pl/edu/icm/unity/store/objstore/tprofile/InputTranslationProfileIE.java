/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.tprofile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.store.api.generic.InputTranslationProfileDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase;

/**
 * Handles import/export of {@link TranslationProfile}.
 * @author K. Benedyczak
 */
@Component
public class InputTranslationProfileIE extends GenericObjectIEBase<TranslationProfile>
{
	@Autowired
	public InputTranslationProfileIE(InputTranslationProfileDB dao, ObjectMapper jsonMapper)
	{
		super(dao, jsonMapper, 108, 
				InputTranslationProfileHandler.TRANSLATION_PROFILE_OBJECT_TYPE);
	}
	
	@Override
	protected TranslationProfile convert(ObjectNode src)
	{
		return TranslationProfileMapper.map(jsonMapper.convertValue(src, DBTranslationProfile.class));
	}

	@Override
	protected ObjectNode convert(TranslationProfile src)
	{
		return jsonMapper.convertValue(TranslationProfileMapper.map(src), ObjectNode.class);
	}
}



