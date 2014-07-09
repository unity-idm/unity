/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.tprofile.TranslationProfileDB;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.translation.ProfileType;
import pl.edu.icm.unity.server.translation.TranslationProfile;
import pl.edu.icm.unity.server.translation.in.InputTranslationProfile;
import pl.edu.icm.unity.server.translation.out.OutputTranslationProfile;

/**
 * Implementation of {@link TranslationProfileManagement}
 * 
 * @author K. Benedyczak
 */
public class TranslationProfileManagementImpl implements TranslationProfileManagement
{
	private DBSessionManager db;
	private AuthorizationManager authz;
	private TranslationProfileDB tpDB;
	
	@Autowired
	public TranslationProfileManagementImpl(DBSessionManager db, AuthorizationManager authz,
			TranslationProfileDB tpDB)
	{
		this.db = db;
		this.authz = authz;
		this.tpDB = tpDB;
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

}
