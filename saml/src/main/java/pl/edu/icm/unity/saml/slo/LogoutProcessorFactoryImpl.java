/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.slo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.LogoutProcessor;
import pl.edu.icm.unity.engine.api.authn.LogoutProcessorFactory;
import pl.edu.icm.unity.engine.api.session.SessionParticipantTypesRegistry;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;

@Component
class LogoutProcessorFactoryImpl implements LogoutProcessorFactory
{
	private LogoutContextsStore contextsStore;
	private PKIManagement pkiManagement;
	private FreemarkerAppHandler freemarker;
	private String consumerUri;
	private SessionParticipantTypesRegistry registry;
	
	@Autowired
	LogoutProcessorFactoryImpl(LogoutContextsStore contextsStore,
			@Qualifier("insecure") PKIManagement pkiManagement, FreemarkerAppHandler freemarker,
			SLOReplyInstaller sloReplyInstaller, SessionParticipantTypesRegistry registry)
	{
		this.contextsStore = contextsStore;
		this.pkiManagement = pkiManagement;
		this.freemarker = freemarker;
		this.registry = registry;
		
		try
		{
			sloReplyInstaller.enable();
			this.consumerUri = sloReplyInstaller.getServletURL();
		} catch (EngineException e)
		{
			throw new InternalException("Can't install SLO reply handler", e);
		}
	}

	@Override
	public LogoutProcessor getInstance()
	{
		SLOAsyncMessageHandler responseHandler = new SLOAsyncMessageHandler(freemarker);
		InternalLogoutProcessor internalProcessor = new InternalLogoutProcessor(pkiManagement, contextsStore, 
				responseHandler, consumerUri);
		return new LogoutProcessorImpl(contextsStore, internalProcessor, registry);
	}
}
