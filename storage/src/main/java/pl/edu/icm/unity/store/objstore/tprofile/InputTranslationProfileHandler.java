/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.tprofile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.objstore.DefaultEntityHandler;

/**
 * Handler for {@link TranslationProfile}.
 * 
 * @author K. Benedyczak
 */
@Component
public class InputTranslationProfileHandler extends DefaultEntityHandler<TranslationProfile>
{
	public static final String TRANSLATION_PROFILE_OBJECT_TYPE = "inputTranslationProfile";
	
	@Autowired
	public InputTranslationProfileHandler(ObjectMapper jsonMapper)
	{
		super(jsonMapper, TRANSLATION_PROFILE_OBJECT_TYPE, TranslationProfile.class);
	}
	
	@Override
	public GenericObjectBean toBlob(TranslationProfile value)
	{
		if (value.getProfileType() != ProfileType.INPUT)
			throw new IllegalArgumentException("Trying to save profile of " + 
					value.getProfileType() + " type as input profile");
		
		try
		{
			return new GenericObjectBean(value.getName(),
					jsonMapper.writeValueAsBytes(TranslationProfileMapper.map(value)), supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize input profile to JSON", e);
		}
	}

	@Override
	public TranslationProfile fromBlob(GenericObjectBean blob)
	{
		try
		{
			return TranslationProfileMapper.map(jsonMapper.readValue(blob.getContents(), DBTranslationProfile.class));
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize input profile from JSON", e);
		}
	}
}
