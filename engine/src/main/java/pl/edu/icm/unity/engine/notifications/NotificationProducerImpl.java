/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.notifications;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.notification.NotificationStatus;
import pl.edu.icm.unity.engine.api.utils.CacheProvider;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.api.generic.MessageTemplateDB;
import pl.edu.icm.unity.store.api.generic.NotificationChannelDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.api.tx.TxManager;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.basic.MessageTemplate.Message;
import pl.edu.icm.unity.types.basic.NotificationChannel;

/**
 * Internal (shouldn't be exposed directly to end-users) subsystem for sending notifications.
 * @author K. Benedyczak
 */
@Component
public class NotificationProducerImpl implements NotificationProducer, InternalFacilitiesManagement
{
	private static final String CACHE_ID = NotificationProducerImpl.class.getName() + "_cache";
	private Ehcache channelsCache;
	private NotificationFacilitiesRegistry facilitiesRegistry;
	private NotificationChannelDB channelDB;
	private MembershipDAO dbGroups;
	private MessageTemplateDB mtDB;
	private UnityMessageSource msg;
	private TxManager txManager;
	
	@Autowired
	public NotificationProducerImpl(
			CacheProvider cacheProvider, 
			NotificationFacilitiesRegistry facilitiesRegistry, NotificationChannelDB channelDB,
			MembershipDAO dbGroups, MessageTemplateDB mtDB, UnityMessageSource msg,
			TxManager txManager)
	{
		this.dbGroups = dbGroups;
		this.txManager = txManager;
		initCache(cacheProvider.getManager());
		this.facilitiesRegistry = facilitiesRegistry;
		this.channelDB = channelDB;
		this.mtDB = mtDB;
		this.msg = msg;
	}

	private void initCache(CacheManager cacheManager)
	{
		channelsCache = cacheManager.addCacheIfAbsent(CACHE_ID);
		CacheConfiguration config = channelsCache.getCacheConfiguration();
		config.setTimeToIdleSeconds(120);
		config.setTimeToLiveSeconds(120);
		PersistenceConfiguration persistCfg = new PersistenceConfiguration();
		persistCfg.setStrategy("none");
		config.persistence(persistCfg);
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
		NotificationChannel channelDesc = channelDB.get(channelName);
		NotificationFacility facility = facilitiesRegistry.getByName(channelDesc.getFacilityId());
		return facility.getChannel(channelDesc.getConfiguration());
	}

	@Transactional(autoCommit=false)
	@Override
	public Future<NotificationStatus> sendNotification(EntityParam recipient,
			String channelName, String templateId, Map<String, String> params, String locale, 
			String preferredAddress)
			throws EngineException
	{
		recipient.validateInitialization();

		MessageTemplate template;
		NotificationChannelInstance channel;
		String recipientAddress;
		template = mtDB.get(templateId);
		channel = loadChannel(channelName);
		NotificationFacility facility = facilitiesRegistry.getByName(channel.getFacilityId());
		recipientAddress = facility.getAddressForEntity(recipient, preferredAddress);
		txManager.commit();
		Message templateMsg = template.getMessage(locale, msg.getDefaultLocaleCode(), params);
		return channel.sendNotification(recipientAddress, templateMsg.getSubject(), templateMsg.getBody());
	}
	
	@Override
	@Transactional
	public void sendNotificationToGroup(String group, String channelName,
			String templateId, Map<String, String> params, String locale) throws EngineException
	{
		if (templateId == null)
			return;

		MessageTemplate template = mtDB.get(templateId);
		Message templateMsg = template.getMessage(locale, msg.getDefaultLocaleCode(), params);
		String subject = templateMsg.getSubject();
		String body = templateMsg.getBody();

		List<GroupMembership> memberships = dbGroups.getMembers(group);

		NotificationChannelInstance channel = loadChannel(channelName);
		NotificationFacility facility = facilitiesRegistry.getByName(channel.getFacilityId());

		for (GroupMembership membership: memberships)
		{
			try
			{
				String recipientAddress = facility.getAddressForEntity(
						new EntityParam(membership.getEntityId()), null);
				channel.sendNotification(recipientAddress, subject, body);
			} catch (IllegalIdentityValueException e)
			{
				//OK - ignored
			}
		}
	}

	@Override
	@Transactional(autoCommit=false)
	public Future<NotificationStatus> sendNotification(String recipientAddress,
			String channelName, String templateId, Map<String, String> params, String locale)
			throws EngineException
	{
		NotificationChannelInstance channel;
		MessageTemplate template;
		channel = loadChannel(channelName);
		template = mtDB.get(templateId);
		txManager.commit();
		Message templateMsg = template.getMessage(locale, msg.getDefaultLocaleCode(), params);
		return channel.sendNotification(recipientAddress, templateMsg.getSubject(), templateMsg.getBody());
	}

	
	@Override
	public NotificationFacility getNotificationFacilityForChannel(String channelName) 
			throws EngineException
	{
		NotificationChannelInstance channel = loadChannel(channelName);
		return facilitiesRegistry.getByName(channel.getFacilityId());
	}
}
