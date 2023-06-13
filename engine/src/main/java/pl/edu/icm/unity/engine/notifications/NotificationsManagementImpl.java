/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.notifications;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.notifications.CommunicationTechnology;
import pl.edu.icm.unity.base.notifications.NotificationChannel;
import pl.edu.icm.unity.base.notifications.NotificationChannelInfo;
import pl.edu.icm.unity.engine.api.NotificationsManagement;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.store.api.generic.NotificationChannelDB;
import pl.edu.icm.unity.store.api.tx.Transactional;


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
	private ChannelInstanceFactory channelFactory;
	
	
	@Autowired
	public NotificationsManagementImpl(NotificationChannelDB notificationDB,
			NotificationsManagementCore notificationsCore, InternalAuthorizationManager authz,
			ChannelInstanceFactory channelFactory)
	{
		this.notificationDB = notificationDB;
		this.notificationsCore = notificationsCore;
		this.authz = authz;
		this.channelFactory = channelFactory;
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
	public Map<String, NotificationChannelInfo> getNotificationChannels() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		return getNotificationChannelsMap();
	}

	private Map<String, NotificationChannelInfo> getNotificationChannelsMap()
	{
		return notificationDB.getAll().stream()
				.map(this::enrichChannelWithInfo)
				.collect(Collectors.toMap(channel -> channel.getName(), channel -> channel));
	}
	
	private NotificationChannelInfo enrichChannelWithInfo(NotificationChannel channel)
	{
		NotificationChannelInstance loadedChannel = channelFactory.loadChannel(channel.getName());
		return new NotificationChannelInfo(channel, loadedChannel.providesMessageTemplatingFunctionality());
	}
	
	@Transactional
	@Override
	public Map<String, NotificationChannelInfo> getNotificationChannelsForTechnologies(
			EnumSet<CommunicationTechnology> technologies) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);

		if (technologies == null)
			return new HashMap<>();
		
		Map<String, NotificationChannelInfo> all = getNotificationChannelsMap();
		Map<String, NotificationChannelInfo> ret = new HashMap<>(all);
		for (NotificationChannel ch : all.values())
		{
			NotificationFacility facility = notificationsCore.getNotificationFacility(ch.getFacilityId());
			if (!technologies.contains(facility.getTechnology()))
				ret.remove(ch.getName());
		}
		return ret;
	}
}
