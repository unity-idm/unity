/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.slo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.saml.idp.FreemarkerHandler;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.authn.LogoutProcessor;
import pl.edu.icm.unity.server.authn.LogoutProcessorFactory;

/**
 * Factory of {@link LogoutProcessor}s.
 * @author K. Benedyczak
 */
@Component
public class LogoutProcessorFactoryImpl implements LogoutProcessorFactory
{
	private LogoutContextsStore contextsStore;
	private PKIManagement pkiManagement;
	private FreemarkerHandler freemarker;
	private String consumerUri;
	
	@Autowired
	public LogoutProcessorFactoryImpl(LogoutContextsStore contextsStore,
			PKIManagement pkiManagement, FreemarkerHandler freemarker,
			SLOReplyInstaller sloReplyInstaller)
	{
		super();
		this.contextsStore = contextsStore;
		this.pkiManagement = pkiManagement;
		this.freemarker = freemarker;

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
		return new LogoutProcessorImpl(contextsStore, internalProcessor);
	}
}
