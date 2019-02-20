/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.translation.in.InputTranslationProfileRepository;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationProfileRepository;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.generic.InputTranslationProfileDB;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.api.generic.OutputTranslationProfileDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.translation.ProfileMode;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 * Implementation of {@link TranslationProfileManagement}
 * 
 * @author K. Benedyczak
 */
@Component
@Primary
@InvocationEventProducer
@Transactional
public class TranslationProfileManagementImpl implements TranslationProfileManagement
{
	private InternalAuthorizationManager authz;
	private InputTranslationProfileDB itpDB;
	private OutputTranslationProfileDB otpDB;
	private InputTranslationProfileRepository inputRepo;
	private OutputTranslationProfileRepository outputRepo;
	private TranslationProfileChecker profileHelper;
	
	
	@Autowired
	public TranslationProfileManagementImpl(InternalAuthorizationManager authz,
			InputTranslationProfileDB itpDB, OutputTranslationProfileDB otpDB,
			InputTranslationProfileRepository inputRepo,
			OutputTranslationProfileRepository outputRepo,
			TranslationProfileChecker profileHelper)
	{
		this.authz = authz;
		this.itpDB = itpDB;
		this.otpDB = otpDB;
		this.inputRepo = inputRepo;
		this.outputRepo = outputRepo;
		this.profileHelper = profileHelper;
	}

	private NamedCRUDDAOWithTS<TranslationProfile> getDAO(TranslationProfile profile)
	{
		return getDAO(profile.getProfileType());
	}
	
	private NamedCRUDDAOWithTS<TranslationProfile> getDAO(ProfileType type)
	{
		if (type == ProfileType.INPUT)
			return itpDB;
		else if (type == ProfileType.OUTPUT)
			return otpDB;
		else
			throw new IllegalArgumentException("Only input and output "
					+ "profiles can be created with this API");
	}
	
	@Override
	public void addProfile(TranslationProfile toAdd) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		assertIsDefault(toAdd);	
		assertIsNotSystemProfile(toAdd);
		profileHelper.checkBaseProfileContent(toAdd);
		getDAO(toAdd).create(toAdd);
	}

	@Override
	public void removeProfile(ProfileType type, String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		assertIsNotSystemProfile(type, name);
		getDAO(type).delete(name);
	}

	@Override
	public void updateProfile(TranslationProfile updated) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		assertIsDefault(updated);
		assertIsNotSystemProfile(updated);
		profileHelper.checkBaseProfileContent(updated);
		getDAO(updated).update(updated);
	}

	@Override
	public Map<String, TranslationProfile> listInputProfiles() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		return inputRepo.listAllProfiles();
		
	}

	@Override
	public Map<String, TranslationProfile> listOutputProfiles() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		return outputRepo.listAllProfiles();
	}

	@Override
	public TranslationProfile getInputProfile(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		return inputRepo.getProfile(name);
	}

	@Override
	public TranslationProfile getOutputProfile(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		return outputRepo.getProfile(name);
	}
	
	private void assertIsDefault(TranslationProfile profile) throws EngineException
	{
		if (profile.getProfileMode() == ProfileMode.READ_ONLY)
			throw new IllegalArgumentException("Cannot create read only translation profile through this API");
	}
	
	private void assertIsNotSystemProfile(TranslationProfile profile)
	{
		assertIsNotSystemProfile(profile.getProfileType(), profile.getName());
	}
	
	private Map<String, TranslationProfile> getSystemProfiles(ProfileType type)
	{
		if (type == ProfileType.INPUT)
			return inputRepo.listSystemProfiles();

		else if (type == ProfileType.OUTPUT)
			return outputRepo.listSystemProfiles();
		else
			throw new IllegalArgumentException("Only input and output "
					+ "profiles can be created with this API");			
	}
	
	private void assertIsNotSystemProfile(ProfileType type, String name)
	{
		Set<String> systemProfiles = getSystemProfiles(type).keySet();
		if (systemProfiles.contains(name))
			throw new IllegalArgumentException("Translation profile '" + name + "' is the system profile and cannot be overwrite or remove");
	}	
}
