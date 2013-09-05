/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.notifications;

import java.util.concurrent.Future;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.engine.internal.NotificationsManagementCore;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.notifications.NotificationChannel;
import pl.edu.icm.unity.notifications.NotificationFacility;
import pl.edu.icm.unity.notifications.NotificationProducer;
import pl.edu.icm.unity.notifications.NotificationStatus;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.utils.CacheProvider;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Internal (shouldn't be exposed directly to end-users) subsystem for sending notifications.
 * @author K. Benedyczak
 */
@Component
public class NotificationProducerImpl implements NotificationProducer
{
	private static final String NOTIFICATION_CHANNEL_ID = "notificationChannel";
	private AttributesManagement attrMan;
	private DBSessionManager db;
	private DBGeneric dbGeneric;
	private Ehcache channelsCache;
	private NotificationsManagementCore notificationsCore;
	
	@Autowired
	public NotificationProducerImpl(@Qualifier("insecure") AttributesManagement attrMan, DBSessionManager db,
			DBGeneric dbGeneric, CacheProvider cacheProvider,
			NotificationsManagementCore notificationsCore)
	{
		this.attrMan = attrMan;
		this.db = db;
		this.dbGeneric = dbGeneric;
		initCache(cacheProvider.getManager());
		this.notificationsCore = notificationsCore;
	}

	private void initCache(CacheManager cacheManager)
	{
		channelsCache = cacheManager.addCacheIfAbsent(NOTIFICATION_CHANNEL_ID);
		CacheConfiguration config = channelsCache.getCacheConfiguration();
		config.setTimeToIdleSeconds(120);
		config.setTimeToLiveSeconds(120);
		PersistenceConfiguration persistCfg = new PersistenceConfiguration();
		persistCfg.setStrategy("none");
		config.persistence(persistCfg);
	}
	
	@Override
	public Future<NotificationStatus> sendNotification(EntityParam recipient, String channelName, 
			String msgSubject, String message) throws EngineException
	{
		recipient.validateInitialization();		
		NotificationChannel channel = loadChannel(channelName);
		NotificationFacility facility = notificationsCore.getNotificationFacility(channel.getFacilityId());
		String recipientAddress = getAddressForEntity(recipient, facility.getRecipientAddressMetadataKey());
		return channel.sendNotification(recipientAddress, msgSubject, message);
	}
	
	private NotificationChannel loadChannel(String channelName) throws EngineException
	{
		Element cachedChannel = channelsCache.get(channelName);
		NotificationChannel channel;
		if (cachedChannel == null)
		{
			channel = loadFromDb(channelName);
		} else
			channel = (NotificationChannel) cachedChannel.getObjectValue();
		
		if (channel == null)
			throw new WrongArgumentException("Channel " + channelName + " is not known");
		return channel;
	}
	
	private NotificationChannel loadFromDb(String channelName) throws EngineException
	{
		NotificationChannel channel;
		SqlSession sql = db.getSqlSession(true);
		try
		{
			GenericObjectBean raw = dbGeneric.getObjectByNameType(channelName, 
					NOTIFICATION_CHANNEL_ID, sql);
			String config = notificationsCore.deserializeChannel(raw.getContents());
			NotificationFacility facility = notificationsCore.getNotificationFacility(raw.getSubType());
			channel = facility.getChannel(config);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
		return channel;
	}
	
	private String getAddressForEntity(EntityParam recipient, String metadataId) throws EngineException
	{
		AttributeExt<?> attr = attrMan.getAttributeByMetadata(recipient, "/", metadataId);
		if (attr == null)
			throw new IllegalIdentityValueException("The entity does not have the email address specified");
		return (String) attr.getValues().get(0);
	}
}
