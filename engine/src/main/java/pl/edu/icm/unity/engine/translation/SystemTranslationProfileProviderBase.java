/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.utils.ClasspathResourceReader;
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
{

	public static final String TRANSLATION_PROFILE_CLASSPATH = "profiles";
	
	private ApplicationContext applicationContext;
	protected Map<String, TranslationProfile> profiles;
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_TRANSLATION,
			SystemTranslationProfileProviderBase.class);

	public SystemTranslationProfileProviderBase(ApplicationContext applicationContext)
	{
		this.applicationContext = applicationContext;
		this.profiles = new HashMap<>();
		loadProfiles();

	}

	private void loadProfiles()
	{
		String type = getType().toString().toLowerCase();
		ClasspathResourceReader classPathReader = new ClasspathResourceReader(
				applicationContext);
		try
		{

			Collection<ObjectNode> jsons = classPathReader
					.readJsons(TRANSLATION_PROFILE_CLASSPATH + "/" + type);

			if (jsons.isEmpty())
			{
				LOG.debug("Directory with system {} translation profiles is empty",
						type);
				return;
			}

			for (ObjectNode json : jsons)
			{
				TranslationProfile tp = new TranslationProfile(json);
				tp.setProfileMode(ProfileMode.READ_ONLY);
				checkProfile(tp);
				LOG.debug("Add system {} translation profile '{}'", type, tp);
				profiles.put(tp.getName(), tp);
			}

		} catch (Exception e)
		{
			throw new InternalException(
					"Can't load system " + type + " translation profiles", e);
		}
	}

	
	public Map<String, TranslationProfile> getSystemProfiles()
	{
		Map<String, TranslationProfile> copy = new HashMap<>();
		for (TranslationProfile tp : profiles.values())
			copy.put(tp.getName(), tp.clone());
		return copy;
	}
	
	protected void checkProfile(TranslationProfile profile) throws EngineException
	{
		if (profile.getProfileType() != getType())
			throw new IllegalArgumentException(
					"Unsupported profile type: " + profile.getProfileType());
	
		if (profiles.containsKey(profile.getName()))
		{
			throw new InternalException("Duplicate definition of system profile " + profile);
		}
	}
	
	protected abstract ProfileType getType();
}
