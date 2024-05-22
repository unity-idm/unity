/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.layout;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.logging.log4j.Logger;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Div;

import pl.edu.icm.unity.base.utils.Log;

class ExtraLayoutPanel extends Div
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, ExtraLayoutPanel.class);
	private boolean empty;

	ExtraLayoutPanel(String id, File panelFile) {
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
					log.error("Configured Panel File: {}, couldn't be read, file is unreachable",
							panelFile.getParent());
					empty = true;
				}
			} catch (IOException | IllegalArgumentException exception)
			{
				log.error("Could not load panel: " + id, exception);
				empty = true;
			}
		}
	}


	boolean isEmpty()
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
