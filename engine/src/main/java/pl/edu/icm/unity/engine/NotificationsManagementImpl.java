/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.notify.NotificationChannelDB;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.internal.NotificationsManagementCore;
import pl.edu.icm.unity.engine.notifications.NotificationFacility;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.NotificationsManagement;
import pl.edu.icm.unity.types.basic.NotificationChannel;


/**
 * Notifications management implementation.
 * @author K. Benedyczak
 */
@Component
@InvocationEventProducer
public class NotificationsManagementImpl implements NotificationsManagement
{
	private DBSessionManager db;
	private NotificationChannelDB notificationDB;
	private NotificationsManagementCore notificationsCore;
	private AuthorizationManager authz;
	
	
	@Autowired
	public NotificationsManagementImpl(DBSessionManager db,
			NotificationChannelDB notificationDB,
			NotificationsManagementCore notificationsCore, AuthorizationManager authz)
	{
		this.db = db;
		this.notificationDB = notificationDB;
		this.notificationsCore = notificationsCore;
		this.authz = authz;
	}


	@Override
	public Set<String> getNotificationFacilities() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return notificationsCore.getNotificationFacilities();
	}

	@Override
	public void addNotificationChannel(NotificationChannel channel) throws EngineException
	{
		if (channel.getFacilityId() == null || channel.getName() == null || channel.getConfiguration() == null)
			throw new WrongArgumentException("All channel fields must be set up");
		NotificationFacility facility = notificationsCore.getNotificationFacility(channel.getFacilityId());
		if (facility == null)
			throw new WrongArgumentException("Notification facility name is unknown: " + 
					channel.getFacilityId());
		facility.validateConfiguration(channel.getConfiguration());
		
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			notificationDB.insert(channel.getName(), channel, sql);
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
			notificationDB.remove(channelName, sql);
			sql.commit();
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
			NotificationChannel channel = notificationDB.get(channelName, sql);
			NotificationFacility facility = notificationsCore.getNotificationFacility(
					channel.getFacilityId());
			facility.validateConfiguration(newConfiguration);
			channel.setConfiguration(newConfiguration);
			notificationDB.update(channelName, channel, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public Map<String, NotificationChannel> getNotificationChannels() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			Map<String, NotificationChannel> ret = notificationDB.getAllAsMap(sql);
			sql.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

}
