/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.SystemTranslationProfileProvider;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.translation.ProfileMode;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 * Base for all system translation profile providers
 * 
 * @author P.Piernik
 *
 */
public abstract class SystemTranslationProfileProviderBase
		implements SystemTranslationProfileProvider
{
	private ApplicationContext applicationContext;

	protected Map<String, TranslationProfile> profiles;

	private static final Logger LOG = Log.getLogger(Log.U_SERVER_TRANSLATION,
			SystemTranslationProfileProviderBase.class);

	public SystemTranslationProfileProviderBase(ApplicationContext applicationContext)
	{
		this.applicationContext = applicationContext;
		this.profiles = new HashMap<String, TranslationProfile>();
		loadProfiles();

	}

	private void loadProfiles()
	{

		String type = getSupportedType().toString().toLowerCase();
		try
		{
			Resource[] resources = applicationContext
					.getResources("classpath:profiles/" + type + "/*.json");

			if (resources == null || resources.length == 0)
			{
				LOG.debug("Directory with system " + type
						+ " translation profiles is empty");
				return;

			}
			for (Resource r : resources)
			{
				ObjectNode json;
				String source = FileUtils.readFileToString(r.getFile());
				json = JsonUtil.parse(source);
				TranslationProfile tp = new TranslationProfile(json);
				tp.setProfileMode(ProfileMode.READ_ONLY);
				checkProfile(tp);
				LOG.debug("Add system " + type + " translation profile '" + tp
						+ "'");
				profiles.put(tp.getName(), tp);
			}
		} catch (Exception e)
		{
			throw new InternalException(
					"Can't load system " + type + " translation profiles", e);
		}
	}

	@Override
	public Map<String, TranslationProfile> getSystemProfiles()
	{
		return profiles;
	}

	@Override
	public ProfileType getSupportedType()
	{
		return getType();
	}

	protected void checkProfile(TranslationProfile profile) throws EngineException
	{
		if (profile.getProfileType() != getType())
			throw new IllegalArgumentException(
					"Unsupported profile type: " + profile.getProfileType());
	}
	
	protected abstract ProfileType getType();
}
