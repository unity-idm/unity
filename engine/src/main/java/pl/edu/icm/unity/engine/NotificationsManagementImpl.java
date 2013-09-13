/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.internal.NotificationsManagementCore;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.notifications.NotificationFacility;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.NotificationsManagement;
import pl.edu.icm.unity.server.utils.CacheProvider;


/**
 * Notifications management implementation.
 * @author K. Benedyczak
 */
@Component
public class NotificationsManagementImpl implements NotificationsManagement
{
	public static final String NOTIFICATION_CHANNEL_ID = "notificationChannel";

	private DBSessionManager db;
	private DBGeneric dbGeneric;
	private NotificationsManagementCore notificationsCore;
	private AuthorizationManager authz;
	

	
	@Autowired
	public NotificationsManagementImpl(NotificationsManagementCore notificationsCore,
			DBSessionManager db, DBGeneric dbGeneric, ObjectMapper mapper, 
			CacheProvider cacheManager, AttributesManagement attrMan, AuthorizationManager authz)
	{
		this.notificationsCore = notificationsCore;
		this.db = db;
		this.dbGeneric = dbGeneric;
		this.authz = authz;
	}

	
	@Override
	public Set<String> getNotificationFacilities() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return notificationsCore.getNotificationFacilities();
	}

	@Override
	public void addNotificationChannel(String facilityId, String channelName,
			String configuration) throws EngineException
	{
		if (facilityId == null || channelName == null || configuration == null)
			throw new WrongArgumentException("None of the arguments can be null");
		NotificationFacility facility = notificationsCore.getNotificationFacility(facilityId);
		if (facility == null)
			throw new WrongArgumentException("Notification facility name is unknown: " + facilityId);
		facility.validateConfiguration(configuration);
		
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			byte[] contents = notificationsCore.serializeChannel(configuration);
			dbGeneric.addObject(channelName, NOTIFICATION_CHANNEL_ID, 
					facilityId, contents, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void removeNotificationChannel(String channelName) throws EngineException
	{
		if (channelName == null)
			throw new WrongArgumentException("None of the arguments can be null");
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			dbGeneric.removeObject(channelName, NOTIFICATION_CHANNEL_ID, sql);
			sql.commit();
		} catch(IllegalArgumentException e)
		{
			throw new WrongArgumentException("Notification channel with name " + channelName + 
					" is not known", e);
		}finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void updateNotificationChannel(String channelName, String newConfiguration)
			throws EngineException
	{
		if (channelName == null || newConfiguration == null)
			throw new WrongArgumentException("None of the arguments can be null");
		
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			GenericObjectBean raw = dbGeneric.getObjectByNameType(channelName, 
					NOTIFICATION_CHANNEL_ID, sql);
			if (raw == null)
				throw new WrongArgumentException("Channel " + channelName + " is not known");
			NotificationFacility facility = notificationsCore.getNotificationFacility(raw.getSubType());
			facility.validateConfiguration(newConfiguration);
			byte[] contents = notificationsCore.serializeChannel(newConfiguration);
			dbGeneric.updateObject(channelName, NOTIFICATION_CHANNEL_ID, contents, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public Map<String, String> getNotificationChannels() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			List<GenericObjectBean> raw = dbGeneric.getObjectsOfType(NOTIFICATION_CHANNEL_ID, sql);
			Map<String, String> ret = new HashMap<>(raw.size());
			for (GenericObjectBean gb: raw)
			{
				ret.put(gb.getName(), notificationsCore.deserializeChannel(gb.getContents()));
			}
			sql.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

}
