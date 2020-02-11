/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.image;

import java.util.Collections;

import javax.servlet.Filter;

import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSyntaxFactoriesRegistry;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.wellknown.AttributesContentServletProvider;
import pl.edu.icm.unity.webui.authn.InvocationContextSetupFilter;

@Component
class AttributesContentServletFactory implements AttributesContentServletProvider
{
	private final UnityServerConfiguration config;
	private final AttributesManagement attributesManagement;
	private final AttributeSyntaxFactoriesRegistry atSyntaxRegistry;

	@Autowired
	AttributesContentServletFactory(UnityServerConfiguration config,
			AttributesManagement attributesManagement,
			AttributeSyntaxFactoriesRegistry atSyntaxRegistry)
	{
		this.config = config;
		this.attributesManagement = attributesManagement;
		this.atSyntaxRegistry = atSyntaxRegistry;
	}

	@Override
	public ServletHolder getServiceServlet()
	{
		AttributesContentServlet servlet = new AttributesContentServlet(attributesManagement, atSyntaxRegistry);
		return new ServletHolder(servlet);
	}

	@Override
	public Filter getServiceFilter()
	{
		return new InvocationContextSetupFilter(config, null, null, Collections.emptyList());
	}
}
