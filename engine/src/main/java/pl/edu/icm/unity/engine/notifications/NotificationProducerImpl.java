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

import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.notify.NotificationChannelDB;
import pl.edu.icm.unity.db.generic.notify.NotificationChannelHandler;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.notifications.NotificationChannelInstance;
import pl.edu.icm.unity.notifications.NotificationFacility;
import pl.edu.icm.unity.notifications.NotificationProducer;
import pl.edu.icm.unity.notifications.NotificationStatus;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.registries.NotificationFacilitiesRegistry;
import pl.edu.icm.unity.server.utils.CacheProvider;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.NotificationChannel;

/**
 * Internal (shouldn't be exposed directly to end-users) subsystem for sending notifications.
 * @author K. Benedyczak
 */
@Component
public class NotificationProducerImpl implements NotificationProducer
{
	private AttributesManagement attrMan;
	private DBSessionManager db;
	private Ehcache channelsCache;
	private NotificationFacilitiesRegistry facilitiesRegistry;
	private NotificationChannelDB channelDB;
	
	@Autowired
	public NotificationProducerImpl(@Qualifier("insecure") AttributesManagement attrMan, DBSessionManager db,
			CacheProvider cacheProvider,
			NotificationFacilitiesRegistry facilitiesRegistry, NotificationChannelDB channelDB)
	{
		this.attrMan = attrMan;
		this.db = db;
		initCache(cacheProvider.getManager());
		this.facilitiesRegistry = facilitiesRegistry;
		this.channelDB = channelDB;
	}

	private void initCache(CacheManager cacheManager)
	{
		channelsCache = cacheManager.addCacheIfAbsent(NotificationChannelHandler.NOTIFICATION_CHANNEL_ID);
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
		NotificationChannelInstance channel = loadChannel(channelName);
		NotificationFacility facility = facilitiesRegistry.getByName(channel.getFacilityId());
		String recipientAddress = getAddressForEntity(recipient, facility.getRecipientAddressMetadataKey());
		return channel.sendNotification(recipientAddress, msgSubject, message);
	}
	
	private NotificationChannelInstance loadChannel(String channelName) throws EngineException
	{
		Element cachedChannel = channelsCache.get(channelName);
		NotificationChannelInstance channel;
		if (cachedChannel == null)
		{
			channel = loadFromDb(channelName);
		} else
			channel = (NotificationChannelInstance) cachedChannel.getObjectValue();
		
		if (channel == null)
			throw new WrongArgumentException("Channel " + channelName + " is not known");
		return channel;
	}
	
	private NotificationChannelInstance loadFromDb(String channelName) throws EngineException
	{
		NotificationChannelInstance channel;
		SqlSession sql = db.getSqlSession(true);
		try
		{
			NotificationChannel channelDesc = channelDB.get(channelName, sql);
			NotificationFacility facility = facilitiesRegistry.getByName(channelDesc.getFacilityId());
			channel = facility.getChannel(channelDesc.getConfiguration());
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
