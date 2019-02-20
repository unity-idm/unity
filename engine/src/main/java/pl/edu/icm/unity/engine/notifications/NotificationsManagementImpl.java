/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.notifications;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.NotificationsManagement;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.store.api.generic.NotificationChannelDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.basic.NotificationChannel;


/**
 * Notifications management implementation.
 * @author K. Benedyczak
 */
@Component
@Primary
@InvocationEventProducer
public class NotificationsManagementImpl implements NotificationsManagement
{
	private NotificationChannelDB notificationDB;
	private NotificationsManagementCore notificationsCore;
	private InternalAuthorizationManager authz;
	
	
	@Autowired
	public NotificationsManagementImpl(NotificationChannelDB notificationDB,
			NotificationsManagementCore notificationsCore, InternalAuthorizationManager authz)
	{
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

	@Transactional
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
		notificationDB.create(channel);
	}

	@Transactional
	@Override
	public void removeNotificationChannel(String channelName) throws EngineException
	{
		if (channelName == null)
			throw new WrongArgumentException("None of the arguments can be null");
		authz.checkAuthorization(AuthzCapability.maintenance);
		notificationDB.delete(channelName);
	}

	@Transactional
	@Override
	public void updateNotificationChannel(String channelName, String newConfiguration)
			throws EngineException
	{
		if (channelName == null || newConfiguration == null)
			throw new WrongArgumentException("None of the arguments can be null");
		
		authz.checkAuthorization(AuthzCapability.maintenance);
		NotificationChannel channel = notificationDB.get(channelName);
		NotificationFacility facility = notificationsCore.getNotificationFacility(
				channel.getFacilityId());
		facility.validateConfiguration(newConfiguration);
		channel.setConfiguration(newConfiguration);
		notificationDB.updateByName(channelName, channel);
	}

	@Transactional
	@Override
	public Map<String, NotificationChannel> getNotificationChannels() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		return notificationDB.getAllAsMap();
	}

	@Transactional
	@Override
	public Map<String, NotificationChannel> getNotificationChannelsForFacilities(
			Set<String> facilites) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);

		if (facilites == null)
			return new HashMap<>();

		Map<String, NotificationChannel> all = notificationDB.getAllAsMap();
		Map<String, NotificationChannel> ret = new HashMap<>(all);
		for (NotificationChannel ch : all.values())
			if (!facilites.contains(ch.getFacilityId()))
				ret.remove(ch.getName());
		return ret;
	}
}
