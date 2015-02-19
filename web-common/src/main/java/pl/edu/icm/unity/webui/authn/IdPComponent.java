/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.net.MalformedURLException;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.webui.VaadinEndpointProperties.ScaleMode;
import pl.edu.icm.unity.webui.common.ImageUtils;
import pl.edu.icm.unity.webui.common.Styles;

import com.google.common.html.HtmlEscapers;
import com.vaadin.server.Resource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CustomComponent;

/**
 * Small widget showing a clickable component presenting a remote IdP. Implemented as Button.
 *  
 * @author K. Benedyczak
 */
public class IdPComponent extends CustomComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, IdPComponent.class);
	
	private Button providerB;

	public IdPComponent(String id, Resource logo, String name, ScaleMode scaleMode)
	{
		providerB = new Button();
		providerB.setImmediate(true);
		providerB.setStyleName(Styles.vButtonLinkV.toString());
		providerB.addStyleName(Styles.verticalMargins6.toString());
		providerB.addStyleName(Styles.horizontalMargins6.toString());
		providerB.setId("IdpSelector." + id);
		
		if (logo != null)
		{
			providerB.setIcon(logo);
			ImageUtils.setScaleStyling(scaleMode, providerB);
			providerB.setDescription(HtmlEscapers.htmlEscaper().escape(name));
		} else
		{
			providerB.setCaption(name);
		}
		providerB.setData(id);
		setCompositionRoot(providerB);
	}
	
	public static Resource getLogo(String logoUrl, String name)
	{
		Resource logo = null;
		try
		{
			if (logoUrl != null)
				logo = ImageUtils.getLogoResource(logoUrl);
		} catch (MalformedURLException e)
		{
			log.warn("Can not load logo image of the IdP with url " + logoUrl, e);
		}

		if (logo == null && name == null)
		{
			throw new IllegalArgumentException("Neither logo nor name of IdP was given");
		}
		return logo;
	}
	
	public void addClickListener(ClickListener listener)
	{
		providerB.addClickListener(listener);
	}
}
