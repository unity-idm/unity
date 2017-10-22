/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.engine.api.translation.SystemTranslationProfileProvider;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 * Allows read profiles from DB and @{SystemTranslationProfileProvider}s 
 * @author P.Piernik
 *
 */
public abstract class TranslationProfileRepositotory
{
	private NamedCRUDDAOWithTS<TranslationProfile> dao;
	private List<SystemTranslationProfileProvider> systemProfileproviders;
	
	
	
	public TranslationProfileRepositotory(NamedCRUDDAOWithTS<TranslationProfile> dao,
			List<SystemTranslationProfileProvider> systemProfileproviders)
	{
		this.dao = dao;
		this.systemProfileproviders = systemProfileproviders;
	}

	
	/**
	 * 
	 * @return all profiles both from DB and from @{SystemTranslationProfileProvider}s
	 * @throws EngineException
	 */
	@Transactional
	public Map<String, TranslationProfile> listAllProfiles() throws EngineException
	{
		Map<String, TranslationProfile> profiles =  dao.getAllAsMap();
		profiles.putAll(listSystemProfiles());
		return profiles;
	}
	
	/**
	 *   
	 * @return only system (READ_ONLY) profiles
	 */
	public Map<String, TranslationProfile> listSystemProfiles()
	{
		Map<String, TranslationProfile> profiles = new HashMap<String, TranslationProfile>();
		for (SystemTranslationProfileProvider p : systemProfileproviders)
			profiles.putAll(p.getSystemProfiles());
		return profiles;
	}
	
	/**
	 * 
	 * @param name
	 * @return return TranslationProfile with given name
	 * @throws EngineException
	 */
	@Transactional
	public TranslationProfile getProfile(String name) throws EngineException
	{	
		for (SystemTranslationProfileProvider p: systemProfileproviders)
		{
			TranslationProfile profile = p.getSystemProfiles().get(name);
			if (profile != null)
				return profile;			
		}
		return dao.get(name);
	}


}
