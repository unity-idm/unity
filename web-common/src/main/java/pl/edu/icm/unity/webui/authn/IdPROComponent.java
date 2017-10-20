/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import com.google.common.html.HtmlEscapers;
import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Image;
import com.vaadin.v7.ui.Label;
import com.vaadin.v7.ui.VerticalLayout;

import pl.edu.icm.unity.webui.VaadinEndpointProperties.ScaleMode;
import pl.edu.icm.unity.webui.common.ImageUtils;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Small widget showing a component presenting a remote IdP. Not interactive.
 * @author K. Benedyczak
 */
public class IdPROComponent extends CustomComponent
{
	public IdPROComponent(String logoUrl, String name, ScaleMode scaleMode)
	{
		Resource logo = IdPComponent.getLogo(logoUrl, name);
		VerticalLayout content = new VerticalLayout();

		if (logo != null)
		{
			Image providerLogo = new Image();
			providerLogo.addStyleName(Styles.smallMargins.toString());
			providerLogo.setSource(logo);
			ImageUtils.setScaleStyling(scaleMode, providerLogo);
			providerLogo.setDescription(HtmlEscapers.htmlEscaper().escape(name));
			content.addComponent(providerLogo);
			content.setComponentAlignment(providerLogo, Alignment.BOTTOM_CENTER);
		}
		Label providerName = new Label(name);
		providerName.addStyleName(Styles.textXLarge.toString());
		content.addComponent(providerName);
		content.setComponentAlignment(providerName, Alignment.TOP_CENTER);
		setCompositionRoot(content);
	}
}
