/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.elements;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Div;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

public class ExtraLayoutPanel extends Div
{
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private boolean empty;

	public ExtraLayoutPanel(String id, File panelFile) {
		setId(id);
		if (panelFile != null)
		{
			try
			{
				if (isPanelFileReadable(panelFile))
				{
					final Html html = new Html(new FileInputStream(panelFile));
					getElement().appendChild(html.getElement());
					empty = false;
				} else
				{
					LOG.error("Configured Panel File: {}, couldn't be read, file is unreachable",
							panelFile.getParent());
					empty = true;
				}
			} catch (IOException | IllegalArgumentException exception)
			{
				LOG.error("Could not load panel: " + id, exception);
				empty = true;
			}
		}
	}


	public boolean isEmpty()
	{
		return empty;
	}

	private boolean isPanelFileReadable(File panelFile)
	{
		return !panelFile.getPath().isBlank()
				&& panelFile.exists()
				&& panelFile.isFile();
	}

}
