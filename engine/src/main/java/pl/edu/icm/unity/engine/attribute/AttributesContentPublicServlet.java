/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute;

import com.google.common.base.CharMatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.engine.api.attributes.*;
import pl.edu.icm.unity.engine.api.attributes.PublicAttributeSpec.PublicAttributeContentProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Exposes attribute's value associated with extenral id.
 */
class AttributesContentPublicServlet extends HttpServlet
{
	private final AttributeSupport attributesSupport;
	private final AttributeTypeSupport attributeTypeSupport;

	AttributesContentPublicServlet(AttributeSupport attributesSupport, AttributeTypeSupport attributeTypeSupport)
	{
		this.attributesSupport = attributesSupport;
		this.attributeTypeSupport = attributeTypeSupport;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		String externalId = getExternalId(req);
		if (externalId == null)
		{
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		PublicAttributeContent content = getAttributeContentToExpose(externalId);
		if (content == null)
		{
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		resp.setHeader("access-control-allow-origin", "*");
		resp.setContentType(content.mimeType);
		try (ByteArrayInputStream in = new ByteArrayInputStream(content.content);
				ServletOutputStream out = resp.getOutputStream())
		{
			IOUtils.copy(in, out);
		}
	}

	private PublicAttributeContent getAttributeContentToExpose(String externalId)
	{
		for (Attribute attribute : attributesSupport.getAttributesByKeyword(externalId))
		{
			AttributeValueSyntax<?> syntax = attributeTypeSupport.getSyntax(attribute);
			PublicAttributeSpec spec = syntax.publicExposureSpec().orElse(null);
			if (spec != null)
			{
				for (String stringRepresentation : attribute.getValues())
				{
					PublicAttributeInfo info = spec.getInfo(stringRepresentation);
					if (externalId.equals(info.externalId))
					{
						PublicAttributeContentProvider contentProvider = spec.getContentProvider();
						return contentProvider.getContent(stringRepresentation);
					}
				}
			}
		}
		return null;
	}

	private String getExternalId(HttpServletRequest req)
	{
		String pathInfo = req.getPathInfo();
		if (pathInfo == null || pathInfo.isEmpty())
			return null;
		String[] elements = CharMatcher.whitespace().trimFrom(pathInfo.substring(1)).split("/");
		if (elements.length != 1)
			return null;
		String externalId = elements[0];
		if (externalId == null || externalId.isEmpty())
			return null;
		return externalId;
	}
}
