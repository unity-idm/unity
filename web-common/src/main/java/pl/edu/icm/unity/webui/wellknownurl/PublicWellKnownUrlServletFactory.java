/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.wellknownurl;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.eclipse.jetty.servlet.FilterHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.wellknown.PublicWellKnownURLServletProvider;
import pl.edu.icm.unity.webui.SimpleVaadinServletFactory;
import pl.edu.icm.unity.webui.authn.InvocationContextSetupFilter;
import pl.edu.icm.unity.webui.authn.remote.RemoteAuthnResponseProcessingFilter;

/**
 * Shared Vaadin servlet providing access to various views, which are publicly available under well known links.
 */
@Component
public class PublicWellKnownUrlServletFactory extends SimpleVaadinServletFactory implements PublicWellKnownURLServletProvider
{
	private final RemoteAuthnResponseProcessingFilter remoteAuthnFilter;

	@Autowired
	public PublicWellKnownUrlServletFactory(ApplicationContext applicationContext, MessageSource msg, 
			UnityServerConfiguration config,
			RemoteAuthnResponseProcessingFilter remoteAuthnFilter)
	{
		super(applicationContext, config, msg, PublicNavigationUI.class.getSimpleName(), new Properties(),
				UnityServerConfiguration.WELL_KNOWN_URL_THEME, 
				UnityServerConfiguration.WELL_KNOWN_URL_TEMPLATE,
				PublicWellKnownURLServletProvider.SERVLET_PATH);
		this.remoteAuthnFilter = remoteAuthnFilter;
	}
	
	@Override
	public List<FilterHolder> getServiceFilters()
	{
		return Lists.newArrayList(
				new FilterHolder(remoteAuthnFilter),
				new FilterHolder(new InvocationContextSetupFilter(config, null, null, Collections.emptyList())));
	}
}
