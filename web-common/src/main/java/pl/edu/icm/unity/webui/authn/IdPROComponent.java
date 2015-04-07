/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import pl.edu.icm.unity.webui.VaadinEndpointProperties.ScaleMode;
import pl.edu.icm.unity.webui.common.ImageUtils;
import pl.edu.icm.unity.webui.common.Styles;

import com.google.common.html.HtmlEscapers;
import com.vaadin.server.Resource;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;

/**
 * Small widget showing a component presenting a remote IdP. Not interactive.
 * @author K. Benedyczak
 */
public class IdPROComponent extends CustomComponent
{
	public IdPROComponent(String logoUrl, String name, ScaleMode scaleMode)
	{
		Resource logo = IdPComponent.getLogo(logoUrl, name);

		if (logo != null)
		{
			Image provider = new Image();
			provider.addStyleName(Styles.smallMargins.toString());
			provider.setSource(logo);
			ImageUtils.setScaleStyling(scaleMode, provider);
			provider.setDescription(HtmlEscapers.htmlEscaper().escape(name));
			setCompositionRoot(provider);
		} else
		{
			Label provider = new Label(name);
			setCompositionRoot(provider);
		}
	}
}
