/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.tprofile;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.db.generic.DefaultEntityHandler;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.translation.AbstractTranslationProfile;
import pl.edu.icm.unity.server.translation.ProfileType;
import pl.edu.icm.unity.server.translation.TranslationProfile;
import pl.edu.icm.unity.server.translation.in.InputTranslationProfile;
import pl.edu.icm.unity.server.translation.out.OutputTranslationProfile;

/**
 * Handler for {@link AbstractTranslationProfile}.
 * 
 * @author K. Benedyczak
 */
@Component
public class TranslationProfileHandler extends DefaultEntityHandler<TranslationProfile>
{
	public static final String TRANSLATION_PROFILE_OBJECT_TYPE = "translationProfile";
	private TranslationActionsRegistry actionsRegistry;
	
	@Autowired
	public TranslationProfileHandler(ObjectMapper jsonMapper, TranslationActionsRegistry actionsRegistry)
	{
		super(jsonMapper, TRANSLATION_PROFILE_OBJECT_TYPE, TranslationProfile.class);
		this.actionsRegistry = actionsRegistry;
	}

	@Override
	public GenericObjectBean toBlob(TranslationProfile value, SqlSession sql)
	{
		String json = value.toJson(jsonMapper);
		return new GenericObjectBean(value.getName(), json.getBytes(), supportedType, 
				value.getProfileType().toString());
	}

	@Override
	public TranslationProfile fromBlob(GenericObjectBean blob, SqlSession sql)
	{
		String subType = blob.getSubType();
		if (subType == null)
			subType = ProfileType.INPUT.toString();
		ProfileType pt = ProfileType.valueOf(subType);
		switch (pt)
		{
		case INPUT:
			return new InputTranslationProfile(new String(blob.getContents()), 
					jsonMapper, actionsRegistry);
		case OUTPUT:
			return new OutputTranslationProfile(new String(blob.getContents()), 
					jsonMapper, actionsRegistry);
		}
		throw new IllegalStateException("The stored translation profile with subtype id " + subType + 
				" has no implemented class representation");
	}
}
