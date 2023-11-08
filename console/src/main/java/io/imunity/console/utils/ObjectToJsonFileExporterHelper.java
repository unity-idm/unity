/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.utils;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.server.StreamResource;

import pl.edu.icm.unity.base.Constants;

public class ObjectToJsonFileExporterHelper
{
	public static void export(Div view, Set<Object> selectedItems, String fileName)
	{
		Anchor download = new Anchor(getStreamResource(selectedItems, fileName), "");
		download.getElement()
				.setAttribute("download", true);
		view.add(download);
		download.getElement()
				.executeJs("return new Promise(resolve =>{this.click(); setTimeout(() => resolve(true), 150)})",
						download.getElement())
				.then(j -> view.remove(download));
	}

	private static StreamResource getStreamResource(Set<Object> selectedItems, String fileName)
	{
		return new StreamResource(fileName, () ->
		{

			try
			{
				byte[] content = Constants.MAPPER.writeValueAsBytes(selectedItems);
				return new ByteArrayInputStream(content);
			} catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		})
		{
			@Override
			public Map<String, String> getHeaders()
			{
				Map<String, String> headers = new HashMap<>(super.getHeaders());
				headers.put("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
				return headers;
			}
		};
	}
}
