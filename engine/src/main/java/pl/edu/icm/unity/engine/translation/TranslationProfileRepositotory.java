/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation;

import java.util.Map;

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
	private SystemTranslationProfileProviderBase systemProfileprovider;
	
	
	
	public TranslationProfileRepositotory(NamedCRUDDAOWithTS<TranslationProfile> dao,
			SystemTranslationProfileProviderBase systemProfileprovider)
	{
		this.dao = dao;
		this.systemProfileprovider = systemProfileprovider;
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
		return systemProfileprovider.getSystemProfiles();
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

		TranslationProfile profile = systemProfileprovider.getSystemProfiles().get(name);
		if (profile != null)
			return profile;

		return dao.get(name);
	}


}
