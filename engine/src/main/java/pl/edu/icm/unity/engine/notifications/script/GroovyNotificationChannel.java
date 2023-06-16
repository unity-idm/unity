/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.notifications.script;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.Future;

import org.apache.logging.log4j.Logger;

import com.google.common.base.Stopwatch;

import eu.unicore.util.configuration.ConfigurationException;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.notification.NotificationStatus;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.engine.notifications.MessageTemplateParams;
import pl.edu.icm.unity.engine.notifications.NotificationChannelInstance;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.basic.MessageTemplate.Message;


class GroovyNotificationChannel implements NotificationChannelInstance
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_NOTIFY, GroovyNotificationChannel.class);
	private final GroovyNotificationChannelConfig config;
	private final ExecutorsService executorsService;
	
	GroovyNotificationChannel(GroovyNotificationChannelConfig config, ExecutorsService executorsService)
	{
		this.config = config;
		this.executorsService = executorsService;
	}

	@Override
	public String getFacilityId()
	{
		return GroovyEmailNotificationFacility.NAME;
	}

	@Override
	public boolean providesMessageTemplatingFunctionality()
	{
		return config.supportsTemplates;
	}

	@Override
	public Future<NotificationStatus> sendNotification(String recipientAddress, Message message)
	{
		Binding context = getMessageSendingContext(recipientAddress, message);
		return asyncInvokeScript(context);
	}
	
	@Override
	public Future<NotificationStatus> sendExternalTemplateMessage(String recipientAddress,
			MessageTemplateParams templateParams)
	{
		Binding context = getTemplateSendingContext(recipientAddress, templateParams);
		return asyncInvokeScript(context);
	}

	private Future<NotificationStatus> asyncInvokeScript(Binding context)
	{
		NotificationStatus retStatus = new NotificationStatus();
		return executorsService.getExecutionService().submit(() -> 
		{
			try
			{
				invokeScript(context);
			} catch (Exception e)
			{
				log.error("E-mail notification failed", e);
				retStatus.setProblem(e);
			}
		}, retStatus);
	}

	
	private void invokeScript(Binding binding)
	{
		GroovyShell shell = new GroovyShell(binding);
		log.info("Sending message to {} via Groovy script {}",  
				binding.getProperty("recipientAddress"), config.scriptPath);
		Reader scriptReader = getFileReader(config.scriptPath);
		Stopwatch timer = Stopwatch.createStarted();
		try
		{
			shell.evaluate(scriptReader);
		} catch (Exception e)
		{
			throw new InternalException("Failed to execute Groovy " 
					+ " script: " + config.scriptPath + ": reason: " + e.getMessage(), e);
		}
		log.debug("Groovy script {} finished in {}", config.scriptPath, timer);
	}
	
	private Reader getFileReader(String location)
	{
		try
		{
			return new FileReader(location);
		} catch (IOException e)
		{
			throw new ConfigurationException("Error loading script " + location, e);
		}
	}
	
	private Binding getTemplateSendingContext(String recipientAddress, MessageTemplateParams templateParams)
	{
		Binding binding = new Binding();
		binding.setVariable("recipientAddress", recipientAddress);
		binding.setVariable("templateId", templateParams.templateId);
		binding.setVariable("templateParams", templateParams.parameters);
		binding.setVariable("log", log);
		return binding;
	}

	private Binding getMessageSendingContext(String recipientAddress, Message message)
	{
		Binding binding = new Binding();
		binding.setVariable("recipientAddress", recipientAddress);
		binding.setVariable("subject", message.getSubject());
		binding.setVariable("body", message.getBody());
		binding.setVariable("type", message.getType().toString());
		binding.setVariable("log", log);
		return binding;
	}
}
