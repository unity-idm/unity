/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute;

import java.util.Collections;
import java.util.List;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.wellknown.AttributesContentPublicServletProvider;

@Component
class AttributesContentPublicServletFactory implements AttributesContentPublicServletProvider
{
	private final AttributeSupport attributesSupport;
	private final AttributeTypeSupport attributeTypeSupport;

	@Autowired
	AttributesContentPublicServletFactory(AttributeSupport attributesSupport, AttributeTypeSupport attributeTypeSupport)
	{
		this.attributesSupport = attributesSupport;
		this.attributeTypeSupport = attributeTypeSupport;
	}

	@Override
	public ServletHolder getServiceServlet()
	{
		AttributesContentPublicServlet servlet = new AttributesContentPublicServlet(attributesSupport,
				attributeTypeSupport);
		return new ServletHolder(servlet);
	}

	@Override
	public List<FilterHolder> getServiceFilters()
	{
		return Collections.emptyList();
	}
}
