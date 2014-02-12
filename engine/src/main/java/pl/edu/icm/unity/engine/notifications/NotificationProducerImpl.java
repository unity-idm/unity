/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.notifications;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBGroups;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.notify.NotificationChannelDB;
import pl.edu.icm.unity.db.generic.notify.NotificationChannelHandler;
import pl.edu.icm.unity.engine.internal.AttributesHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.notifications.MessageTemplate;
import pl.edu.icm.unity.notifications.MessageTemplate.Message;
import pl.edu.icm.unity.notifications.NotificationChannelInstance;
import pl.edu.icm.unity.notifications.NotificationFacility;
import pl.edu.icm.unity.notifications.NotificationProducer;
import pl.edu.icm.unity.notifications.NotificationStatus;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.registries.NotificationFacilitiesRegistry;
import pl.edu.icm.unity.server.utils.CacheProvider;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.NotificationChannel;

/**
 * Internal (shouldn't be exposed directly to end-users) subsystem for sending notifications.
 * @author K. Benedyczak
 */
@Component
public class NotificationProducerImpl implements NotificationProducer
{
	private AttributesHelper attributesHelper;
	private DBSessionManager db;
	private Ehcache channelsCache;
	private NotificationFacilitiesRegistry facilitiesRegistry;
	private NotificationChannelDB channelDB;
	private MessageTemplateManagement msgTemplMan;
	private DBGroups dbGroups;
	
	@Autowired
	public NotificationProducerImpl(AttributesHelper attributesHelper, DBSessionManager db,
			CacheProvider cacheProvider, UnityServerConfiguration serverConfig,
			NotificationFacilitiesRegistry facilitiesRegistry, NotificationChannelDB channelDB,
			DBGroups dbGroups, MessageTemplateManagement msgTemplMan)
	{
		this.attributesHelper = attributesHelper;
		this.db = db;
		this.dbGroups = dbGroups;
		initCache(cacheProvider.getManager());
		this.facilitiesRegistry = facilitiesRegistry;
		this.channelDB = channelDB;
		this.msgTemplMan = msgTemplMan;
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
	
	private Future<NotificationStatus> sendNotification(EntityParam recipient, String channelName, 
			String msgSubject, String message) throws EngineException
	{
		recipient.validateInitialization();
		NotificationChannelInstance channel;
		String recipientAddress;
		SqlSession sql = db.getSqlSession(true);
		try
		{
			channel = loadChannel(channelName, sql);
			NotificationFacility facility = facilitiesRegistry.getByName(channel.getFacilityId());
			recipientAddress = getAddressForEntity(recipient, facility.getRecipientAddressMetadataKey(), sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}

		return channel.sendNotification(recipientAddress, msgSubject, message);
	}
	
	private NotificationChannelInstance loadChannel(String channelName, SqlSession sql) throws EngineException
	{
		Element cachedChannel = channelsCache.get(channelName);
		NotificationChannelInstance channel;
		if (cachedChannel == null)
		{
			channel = loadFromDb(channelName, sql);
		} else
			channel = (NotificationChannelInstance) cachedChannel.getObjectValue();
		
		if (channel == null)
			throw new WrongArgumentException("Channel " + channelName + " is not known");
		return channel;
	}
	
	private NotificationChannelInstance loadFromDb(String channelName, SqlSession sql) throws EngineException
	{
		NotificationChannel channelDesc = channelDB.get(channelName, sql);
		NotificationFacility facility = facilitiesRegistry.getByName(channelDesc.getFacilityId());
		return facility.getChannel(channelDesc.getConfiguration());
	}
	
	private String getAddressForEntity(EntityParam recipient, String metadataId, SqlSession sql) throws EngineException
	{
		AttributeExt<?> attr = attributesHelper.getAttributeByMetadata(recipient, "/", metadataId, sql);
		if (attr == null)
			throw new IllegalIdentityValueException("The entity does not have the email address specified");
		return (String) attr.getValues().get(0);
	}

	@Override
	public Future<NotificationStatus> sendNotification(EntityParam recipient,
			String channelName, String templateId, Map<String, String> params)
			throws EngineException
	{
		MessageTemplate template = msgTemplMan.getTemplate(templateId);
		Message templateMsg = template.getMessage(params);
		return sendNotification(recipient, channelName, templateMsg.getSubject(), templateMsg.getBody());
	}

	@Override
	public void sendNotificationToGroup(String group, String channelName,
			String templateId, Map<String, String> params) throws EngineException
	{
		if (templateId == null)
			return;
		MessageTemplate template = msgTemplMan.getTemplate(templateId);
		Message templateMsg = template.getMessage(params);
		String subject = templateMsg.getSubject();
		String body = templateMsg.getBody();
		GroupContents contents;
		SqlSession sql = db.getSqlSession(true);
		try
		{
			contents = dbGroups.getContents(group, GroupContents.MEMBERS, sql);

			List<Long> entities = contents.getMembers();
			NotificationChannelInstance channel = loadChannel(channelName, sql);
			NotificationFacility facility = facilitiesRegistry.getByName(channel.getFacilityId());

			for (Long entity: entities)
			{
				try
				{
					String recipientAddress = getAddressForEntity(new EntityParam(entity), 
							facility.getRecipientAddressMetadataKey(), sql);
					channel.sendNotification(recipientAddress, subject, body);
				} catch (IllegalIdentityValueException e)
				{
					//OK - ignored
				}
			}
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public Future<NotificationStatus> sendNotification(String recipientAddress,
			String channelName, String templateId, Map<String, String> params)
			throws EngineException
	{
		NotificationChannelInstance channel;
		SqlSession sql = db.getSqlSession(true);
		try
		{
			channel = loadChannel(channelName, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
		MessageTemplate template = msgTemplMan.getTemplate(templateId);
		Message templateMsg = template.getMessage(params);
		return channel.sendNotification(recipientAddress, templateMsg.getSubject(), templateMsg.getBody());
	}

	
	public AttributeType getChannelAddressAttribute(String channelName, SqlSession sql) throws EngineException
	{
		NotificationChannelInstance channel = loadChannel(channelName, sql);
		NotificationFacility facility = facilitiesRegistry.getByName(channel.getFacilityId());
		return attributesHelper.getAttributeTypeWithSingeltonMetadata(
				facility.getRecipientAddressMetadataKey(), sql);
	}
}
