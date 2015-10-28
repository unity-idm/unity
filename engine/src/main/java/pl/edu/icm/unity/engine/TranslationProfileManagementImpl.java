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

import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.tprofile.TranslationProfileDB;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.translation.ProfileType;
import pl.edu.icm.unity.server.translation.TranslationCondition;
import pl.edu.icm.unity.server.translation.TranslationProfile;
import pl.edu.icm.unity.server.translation.in.InputTranslationProfile;
import pl.edu.icm.unity.server.translation.out.OutputTranslationAction;
import pl.edu.icm.unity.server.translation.out.OutputTranslationProfile;
import pl.edu.icm.unity.server.translation.out.OutputTranslationRule;
import pl.edu.icm.unity.stdext.tactions.out.CreateAttributeActionFactory;

/**
 * Implementation of {@link TranslationProfileManagement}
 * 
 * @author K. Benedyczak
 */
@Component
@InvocationEventProducer
public class TranslationProfileManagementImpl implements TranslationProfileManagement
{
	private DBSessionManager db;
	private AuthorizationManager authz;
	private TranslationProfileDB tpDB;
	private TranslationActionsRegistry tactionReg;
	private OutputTranslationProfile defaultProfile;
	
	@Autowired
	public TranslationProfileManagementImpl(DBSessionManager db, AuthorizationManager authz,
			TranslationProfileDB tpDB, TranslationActionsRegistry tactionReg) 
					throws IllegalTypeException, EngineException
	{
		this.db = db;
		this.authz = authz;
		this.tpDB = tpDB;
		this.tactionReg = tactionReg;
		
		this.defaultProfile = createDefaultOutputProfile();
	}

	@Override
	public void addProfile(TranslationProfile toAdd) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			tpDB.insert(toAdd.getName(), toAdd, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void removeProfile(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			tpDB.remove(name, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void updateProfile(TranslationProfile updated) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			tpDB.update(updated.getName(), updated, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public Map<String, InputTranslationProfile> listInputProfiles() throws EngineException
	{
		return listProfiles(InputTranslationProfile.class, ProfileType.INPUT);
	}

	@Override
	public Map<String, OutputTranslationProfile> listOutputProfiles() throws EngineException
	{
		return listProfiles(OutputTranslationProfile.class, ProfileType.OUTPUT);
	}
	

	@SuppressWarnings("unchecked")
	private <T extends TranslationProfile> Map<String, T> listProfiles(Class<T> clazz,
			ProfileType type) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(false);
		try
		{
			Map<String, TranslationProfile> all = tpDB.getAllAsMap(sql);
			sql.commit();
			Map<String, T> ret = new HashMap<String, T>();
			for (Map.Entry<String, TranslationProfile> e: all.entrySet())
				if (e.getValue().getProfileType().equals(type))
					ret.put(e.getKey(), (T) e.getValue());
			return ret;
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public OutputTranslationProfile getDefaultOutputProfile() throws EngineException
	{
		return defaultProfile;
	}

	private OutputTranslationProfile createDefaultOutputProfile() throws IllegalTypeException, EngineException
	{
		List<OutputTranslationRule> rules = new ArrayList<>();
		OutputTranslationAction action1 = (OutputTranslationAction) tactionReg.getByName(
				CreateAttributeActionFactory.NAME).getInstance(
				"memberOf", 
				"groups");
		rules.add(new OutputTranslationRule(action1, new TranslationCondition()));
		return new OutputTranslationProfile("DEFAULT OUTPUT PROFILE", rules);
	}
}
