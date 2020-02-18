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

import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.wellknown.AttributesContentPublicServletProvider;
import pl.edu.icm.unity.webui.authn.InvocationContextSetupFilter;

@Component
class AttributesContentPublicServletFactory implements AttributesContentPublicServletProvider
{
	private final UnityServerConfiguration config;
	private final AttributeSupport attributesSupport;
	private final AttributeTypeSupport attributeTypeSupport;

	@Autowired
	AttributesContentPublicServletFactory(UnityServerConfiguration config,
			AttributeSupport attributesSupport,
			AttributeTypeSupport attributeTypeSupport)
	{
		this.config = config;
		this.attributesSupport = attributesSupport;
		this.attributeTypeSupport = attributeTypeSupport;
	}

	@Override
	public ServletHolder getServiceServlet()
	{
		AttributesContentPublicServlet servlet = new AttributesContentPublicServlet(attributesSupport, attributeTypeSupport);
		return new ServletHolder(servlet);
	}

	@Override
	public Filter getServiceFilter()
	{
		return new InvocationContextSetupFilter(config, null, null, Collections.emptyList());
	}
}
