/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.utils;

import java.util.Set;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;

import io.imunity.vaadin.endpoint.common.file.DownloadHandlers;
import pl.edu.icm.unity.base.Constants;

public class ObjectToJsonFileExporterHelper
{
	public static void export(Div view, Set<Object> selectedItems, String fileName)
	{
		Anchor download = new Anchor(DownloadHandlers.forJson(() ->
		{
			try
			{
				return Constants.MAPPER.writeValueAsBytes(selectedItems);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}, fileName), "");
		download.getElement()
				.setAttribute("download", true);
		view.add(download);
		download.getElement()
				.executeJs("return new Promise(resolve => {this.click(); setTimeout(() => resolve(true), 150)})")
				.then(j -> view.remove(download));
	}
}
