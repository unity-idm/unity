/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.translation.SystemTranslationProfileProvider;
import pl.edu.icm.unity.engine.attribute.AttributeValueConverter;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.translation.in.InputTranslationActionsRegistry;
import pl.edu.icm.unity.engine.translation.in.InputTranslationProfile;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationActionsRegistry;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationProfile;
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
	private AuthorizationManager authz;
	private InputTranslationProfileDB itpDB;
	private OutputTranslationProfileDB otpDB;
	private InputTranslationActionsRegistry inputActionReg;
	private OutputTranslationActionsRegistry outputActionReg;
	private AttributeValueConverter attrConverter;
	private List<SystemTranslationProfileProvider> systemProfileproviders;
	
	@Autowired
	public TranslationProfileManagementImpl(AuthorizationManager authz,
			InputTranslationProfileDB itpDB, OutputTranslationProfileDB otpDB,
			InputTranslationActionsRegistry inputActionReg,
			OutputTranslationActionsRegistry outputActionReg,
			AttributeValueConverter attrConverter, List<SystemTranslationProfileProvider> systemProfileproviders)
	{
		this.authz = authz;
		this.itpDB = itpDB;
		this.otpDB = otpDB;
		this.inputActionReg = inputActionReg;
		this.outputActionReg = outputActionReg;
		this.attrConverter = attrConverter;
		this.systemProfileproviders = systemProfileproviders;
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
	
	private SystemTranslationProfileProvider getSystemProvider(ProfileType type)
	{
		for (SystemTranslationProfileProvider provider : systemProfileproviders)
			if (provider.getSupportedType() == type)
				return provider;
		throw new IllegalArgumentException("Only input and output "
				+ "system profiles can be returned by this API");

	}
	
	@Override
	public void addProfile(TranslationProfile toAdd) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		isReadOnly(toAdd);	
		isSystemProfile(toAdd);
		checkProfileContent(toAdd);
		getDAO(toAdd).create(toAdd);
	}

	@Override
	public void removeProfile(ProfileType type, String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		isSystemProfile(type, name);
		getDAO(type).delete(name);
	}

	@Override
	public void updateProfile(TranslationProfile updated) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		isReadOnly(updated);
		isSystemProfile(updated);
		checkProfileContent(updated);
		getDAO(updated).update(updated);
	}

	@Override
	public Map<String, TranslationProfile> listInputProfiles() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		Map<String, TranslationProfile> profiles = getDAO(ProfileType.INPUT).getAllAsMap();
		profiles.putAll(getSystemProvider(ProfileType.INPUT).getSystemProfiles());
		return profiles;
		
	}

	@Override
	public Map<String, TranslationProfile> listOutputProfiles() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		Map<String, TranslationProfile> profiles =  getDAO(ProfileType.OUTPUT).getAllAsMap();
		profiles.putAll(getSystemProvider(ProfileType.OUTPUT).getSystemProfiles());
		return profiles;
	}

	@Override
	public TranslationProfile getInputProfile(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		TranslationProfile systemProfile = getSystemProvider(ProfileType.INPUT).getSystemProfiles().get(name);
		if (systemProfile != null)
			return systemProfile;
		return itpDB.get(name);
	}

	@Override
	public TranslationProfile getOutputProfile(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		TranslationProfile systemProfile = getSystemProvider(ProfileType.OUTPUT).getSystemProfiles().get(name);
		if (systemProfile != null)
			return systemProfile;
		return otpDB.get(name);
	}
	
	private void isReadOnly(TranslationProfile profile) throws EngineException
	{
		if (profile.getProfileMode() == ProfileMode.READ_ONLY)
			throw new IllegalArgumentException("Cannot create read only translation profile through this API");
	}
	
	private void isSystemProfile(TranslationProfile profile)
	{
		isSystemProfile(profile.getProfileType(), profile.getName());
	}
	
	private void isSystemProfile(ProfileType type, String name)
	{
		Set<String> systemProfiles = getSystemProvider(type).getSystemProfiles().keySet();
		if (systemProfiles.contains(name))
			throw new IllegalArgumentException("Translation profile '" + name + "' is the system profile and cannot be overwrite or remove");
	}
		
	private void checkProfileContent(TranslationProfile profile)
	{
		TranslationProfileInstance<?, ?> instance;
		if (profile.getProfileType() == ProfileType.INPUT)
			instance = new InputTranslationProfile(profile, this, inputActionReg);
		else if (profile.getProfileType() == ProfileType.OUTPUT)
			instance = new OutputTranslationProfile(profile, this, outputActionReg, attrConverter);
		else
			throw new IllegalArgumentException("Unsupported profile type: " + profile.getProfileType());
		if (instance.hasInvalidActions())
			throw new IllegalArgumentException("Profile definition is invalid");
	}
	
}
