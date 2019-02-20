/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.msgtemplate;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.msgtemplates.GenericMessageTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.NotificationsManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.msgtemplate.MessageTemplateConsumersRegistry;
import pl.edu.icm.unity.engine.api.msgtemplate.MessageTemplateValidator;
import pl.edu.icm.unity.engine.api.msgtemplate.MessageTemplateValidator.IllegalVariablesException;
import pl.edu.icm.unity.engine.api.msgtemplate.MessageTemplateValidator.MandatoryVariablesException;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.notifications.InternalFacilitiesManagement;
import pl.edu.icm.unity.engine.notifications.NotificationFacility;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.store.api.generic.MessageTemplateDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.basic.MessageTemplate;

/**
 * Implementation of {@link MessageTemplateManagement}
 * 
 * @author P. Piernik
 */
@Component
@Primary
@InvocationEventProducer
public class MessageTemplateManagementImpl implements MessageTemplateManagement
{
	private InternalAuthorizationManager authz;
	private MessageTemplateDB mtDB;
	private MessageTemplateConsumersRegistry registry;
	private InternalFacilitiesManagement facilityMan;
	private MessageTemplateProcessor messageTemplateProcessor = new MessageTemplateProcessor();
	private MessageTemplateLoader loader;
	private File configFile;

	@Autowired
	public MessageTemplateManagementImpl(InternalAuthorizationManager authz, MessageTemplateDB mtDB,
			MessageTemplateConsumersRegistry registry,
			InternalFacilitiesManagement facilityMan,
			NotificationsManagement notificationMan,
			UnityServerConfiguration config)
	{
		this.authz = authz;
		this.mtDB = mtDB;
		this.registry = registry;
		this.facilityMan = facilityMan;
		this.loader = new MessageTemplateLoader(this, notificationMan, true);
		configFile = config.getFileValue(UnityServerConfiguration.TEMPLATES_CONF, false);
	}
	
	@Transactional
	@Override
	public void addTemplate(MessageTemplate toAdd) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		validateMessageTemplate(toAdd);
		mtDB.create(toAdd);
	}

	@Transactional
	@Override
	public void removeTemplate(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		mtDB.delete(name);
	}

	@Transactional
	@Override
	public void updateTemplate(MessageTemplate updated) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		validateMessageTemplate(updated);
		mtDB.update(updated);
	}

	@Transactional
	@Override
	public Map<String, MessageTemplate> listTemplates() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		return mtDB.getAllAsMap();
	}

	@Transactional
	@Override
	public MessageTemplate getTemplate(String name) throws EngineException
	{	
		authz.checkAuthorization(AuthzCapability.maintenance);
		return mtDB.get(name);
	}

	@Transactional
	@Override
	public MessageTemplate getPreprocessedTemplate(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		Map<String, MessageTemplate> allAsMap = mtDB.getAllAsMap();
		MessageTemplate requested = allAsMap.get(name);
		if (requested == null)
			throw new IllegalArgumentException("There is no message template " + name);
		return preprocessTemplate(allAsMap, requested);
	}

	@Transactional
	@Override
	public MessageTemplate getPreprocessedTemplate(MessageTemplate toProcess)
			throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		Map<String, MessageTemplate> allAsMap = mtDB.getAllAsMap();
		return preprocessTemplate(allAsMap, toProcess);
	}
	
	private MessageTemplate preprocessTemplate(Map<String, MessageTemplate> allAsMap,
			MessageTemplate requested)
	{
		Map<String, MessageTemplate> genericTemplates = allAsMap.entrySet().stream()
				.filter(e -> e.getValue().getConsumer().equals(GenericMessageTemplateDef.NAME))
				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
		return messageTemplateProcessor.preprocessMessage(requested, genericTemplates);
	}
	
	@Transactional
	@Override
	public Map<String, MessageTemplate> getCompatibleTemplates(String templateConsumer) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		Map<String, MessageTemplate> all = mtDB.getAllAsMap();
		Map<String, MessageTemplate> ret = new HashMap<String, MessageTemplate>(all);
		for (MessageTemplate m : all.values())
		{
			if (!m.getConsumer().equals(templateConsumer))
			{
				ret.remove(m.getName());
			}
		}
		return ret;
	}
	
	@Transactional
	@Override
	public void reloadFromConfiguration(Set<String> toReload)
	{
		authz.checkAuthorizationRT("/", AuthzCapability.maintenance);
		loader.initializeMsgTemplates(configFile, s -> toReload.contains(s));
	}
	
	private void validateMessageTemplate(MessageTemplate toValidate)
			throws EngineException
	{
		MessageTemplateDefinition con = registry.getByName(toValidate.getConsumer());
		try
		{
			MessageTemplateValidator.validateMessage(con, toValidate.getMessage());
		} catch (IllegalVariablesException e)
		{
			throw new WrongArgumentException("The following variables are unknown: " + e.getUnknown());
		} catch (MandatoryVariablesException e)
		{
			throw new WrongArgumentException("The following variables must be used: " + e.getMandatory());
		}

		if (toValidate.getConsumer().equals(GenericMessageTemplateDef.NAME))
			return; 
		

		String channel = toValidate.getNotificationChannel();
		if (channel == null || channel.isEmpty())
		{
			throw new WrongArgumentException(
					"Notification channel must be set in message template");
		}

		NotificationFacility facility;
		try
		{
			facility = facilityMan.getNotificationFacilityForChannel(channel);
		} catch (Exception e)
		{
			throw new WrongArgumentException(
					"Cannot get facility for channel: " + channel, e);
		}

		if (!con.getCompatibleFacilities().contains(facility.getName()))
		{
			throw new WrongArgumentException("Notification channel "
					+ toValidate.getNotificationChannel()
					+ " is not compatible with used message consumer "
					+ toValidate.getConsumer());
		}
	}
}
