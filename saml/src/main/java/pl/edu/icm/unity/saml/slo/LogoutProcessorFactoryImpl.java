/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.slo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.LogoutProcessor;
import pl.edu.icm.unity.engine.api.authn.LogoutProcessorFactory;
import pl.edu.icm.unity.engine.api.session.SessionParticipantTypesRegistry;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;

/**
 * Factory of {@link LogoutProcessor}s.
 * @author K. Benedyczak
 */
@Component
public class LogoutProcessorFactoryImpl implements LogoutProcessorFactory
{
	private LogoutContextsStore contextsStore;
	private PKIManagement pkiManagement;
	private FreemarkerAppHandler freemarker;
	private String consumerUri;
	private SessionParticipantTypesRegistry registry;
	
	@Autowired
	public LogoutProcessorFactoryImpl(LogoutContextsStore contextsStore,
			PKIManagement pkiManagement, FreemarkerAppHandler freemarker,
			SLOReplyInstaller sloReplyInstaller, SessionParticipantTypesRegistry registry)
	{
		super();
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
		SLOAsyncResponseHandler responseHandler = new SLOAsyncResponseHandler(freemarker);
		InternalLogoutProcessor internalProcessor = new InternalLogoutProcessor(pkiManagement, contextsStore, 
				responseHandler, consumerUri);
		return new LogoutProcessorImpl(contextsStore, internalProcessor, registry);
	}
}
