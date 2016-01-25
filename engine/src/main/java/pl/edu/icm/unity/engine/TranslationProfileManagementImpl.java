/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.generic.tprofile.TranslationProfileDB;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.transactions.SqlSessionTL;
import pl.edu.icm.unity.engine.transactions.Transactional;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.registries.OutputTranslationActionsRegistry;
import pl.edu.icm.unity.server.translation.ProfileType;
import pl.edu.icm.unity.server.translation.TranslationCondition;
import pl.edu.icm.unity.server.translation.TranslationProfile;
import pl.edu.icm.unity.server.translation.in.InputTranslationProfile;
import pl.edu.icm.unity.server.translation.out.OutputTranslationAction;
import pl.edu.icm.unity.server.translation.out.OutputTranslationProfile;
import pl.edu.icm.unity.server.translation.out.OutputTranslationRule;
import pl.edu.icm.unity.stdext.translation.out.CreateAttributeActionFactory;

/**
 * Implementation of {@link TranslationProfileManagement}
 * 
 * @author K. Benedyczak
 */
@Component
@InvocationEventProducer
public class TranslationProfileManagementImpl implements TranslationProfileManagement
{
	private AuthorizationManager authz;
	private TranslationProfileDB tpDB;
	private OutputTranslationActionsRegistry outtactionReg;
	private OutputTranslationProfile defaultProfile;
	
	@Autowired
	public TranslationProfileManagementImpl(AuthorizationManager authz,
			TranslationProfileDB tpDB, OutputTranslationActionsRegistry outtactionReg) 
					throws IllegalTypeException, EngineException
	{
		this.authz = authz;
		this.tpDB = tpDB;
		this.outtactionReg = outtactionReg;
		
		this.defaultProfile = createDefaultOutputProfile();
	}

	@Override
	@Transactional
	public void addProfile(TranslationProfile toAdd) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		tpDB.insert(toAdd.getName(), toAdd, sql);
	}

	@Override
	@Transactional
	public void removeProfile(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		tpDB.remove(name, sql);
	}

	@Override
	@Transactional
	public void updateProfile(TranslationProfile updated) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		tpDB.update(updated.getName(), updated, sql);
	}

	@Override
	@Transactional(noTransaction=true, autoCommit=false)
	public Map<String, InputTranslationProfile> listInputProfiles() throws EngineException
	{
		return listProfiles(InputTranslationProfile.class, ProfileType.INPUT);
	}

	@Override
	@Transactional(noTransaction=true, autoCommit=false)
	public Map<String, OutputTranslationProfile> listOutputProfiles() throws EngineException
	{
		return listProfiles(OutputTranslationProfile.class, ProfileType.OUTPUT);
	}
	

	@SuppressWarnings("unchecked")
	private <T extends TranslationProfile> Map<String, T> listProfiles(Class<T> clazz,
			ProfileType type) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		Map<String, TranslationProfile> all = tpDB.getAllAsMap(sql);
		sql.commit();
		Map<String, T> ret = new HashMap<String, T>();
		for (Map.Entry<String, TranslationProfile> e: all.entrySet())
			if (e.getValue().getProfileType().equals(type))
				ret.put(e.getKey(), (T) e.getValue());
		return ret;
	}

	@Override
	public OutputTranslationProfile getDefaultOutputProfile() throws EngineException
	{
		return defaultProfile;
	}

	private OutputTranslationProfile createDefaultOutputProfile() throws IllegalTypeException, EngineException
	{
		List<OutputTranslationRule> rules = new ArrayList<>();
		OutputTranslationAction action1 = outtactionReg.getByName(
				CreateAttributeActionFactory.NAME).getInstance(
				"memberOf", 
				"groups");
		rules.add(new OutputTranslationRule(action1, new TranslationCondition()));
		return new OutputTranslationProfile("DEFAULT OUTPUT PROFILE", rules);
	}
}
