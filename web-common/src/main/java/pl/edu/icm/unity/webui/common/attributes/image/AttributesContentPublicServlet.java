/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.image;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.google.common.base.CharMatcher;

import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.attributes.SharedAttributeContent;
import pl.edu.icm.unity.engine.api.attributes.SharedAttributeInfo;
import pl.edu.icm.unity.engine.api.attributes.SharedAttributeSpec;
import pl.edu.icm.unity.engine.api.attributes.SharedAttributeSpec.SharedAttributeContentProvider;
import pl.edu.icm.unity.types.basic.Attribute;

/**
 * Exposes attribute's value associated with extenral id.
 */
class AttributesContentPublicServlet extends HttpServlet
{
	private final AttributesManagement attributesManagement;
	private final AttributeTypeSupport attributeTypeSupport;

	AttributesContentPublicServlet(AttributesManagement attributesManagement,
			AttributeTypeSupport attributeTypeSupport)
	{
		this.attributesManagement = attributesManagement;
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

		SharedAttributeContent sharedContent = getAttributeContentToShare(externalId);
		if (sharedContent == null)
		{
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		resp.setContentType(sharedContent.type.getMimeType());
		try (ByteArrayInputStream in = new ByteArrayInputStream(sharedContent.content);
				ServletOutputStream out = resp.getOutputStream())
		{
			IOUtils.copy(in, out);
		}
	}

	private SharedAttributeContent getAttributeContentToShare(String externalId)
	{
		for (Attribute attribute : attributesManagement.getAllAttributesByKeyword(externalId))
		{
			AttributeValueSyntax<?> syntax = attributeTypeSupport.getSyntax(attribute);
			SharedAttributeSpec spec = syntax.shareSpec().orElse(null);
			if (spec != null)
			{
				for (String stringRepresentation : attribute.getValues())
				{
					SharedAttributeInfo info = spec.getInfo(stringRepresentation);
					if (externalId.equals(info.externalId))
					{
						SharedAttributeContentProvider contentProvider = spec.getContentProvider();
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
