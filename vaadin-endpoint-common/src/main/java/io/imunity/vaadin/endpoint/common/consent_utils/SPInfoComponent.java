/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.consent_utils;

import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import pl.edu.icm.unity.base.message.MessageSource;

/**
 * Presents an information about a service provider which is asking for a favor.
 * Can show its logo, name and URL.
 */
public class SPInfoComponent extends VerticalLayout
{
	private final MessageSource msg;
	private final Image logo;
	private final String name;
	private final String url;
	
	
	/**
	 * @param logo can be null
	 * @param url can be null
	 */
	public SPInfoComponent(MessageSource msg, Image logo, String name, String url)
	{
		this.msg = msg;
		this.logo = logo;
		this.name = name;
		this.url = url;
		
		if (name == null)
			throw new IllegalArgumentException("SP name must be provided");
		
		initUI();
	}

	private void initUI()
	{
		setPadding(false);
		setMargin(false);
		setAlignItems(Alignment.CENTER);

		if (logo != null)
		{
			Tooltip.forComponent(this).setText(msg.getMessage("SPInfoComponent.requesterName", name));
			add(logo);
			setAlignItems(Alignment.CENTER);

			add(new H2(getRequesterInfoWithLogo()));
		} else
		{
			H2 requesterNameInfo = new H2(msg.getMessage("SPInfoComponent.requesterName", name));
			add(requesterNameInfo);
			
			if (url != null)
			{
				String presentationAddr = msg.getMessage("SPInfoComponent.requesterAddress", 
						URIPresentationHelper.getHumanReadableDomain(url));
				add(new Span(presentationAddr));
			}
		}

		add(new HtmlComponent("br"));
		Span requestedAccessInfo = new Span(msg.getMessage("SPInfoComponent.requestedAccess"));
		add(requestedAccessInfo);
	}
	
	private String getRequesterInfoWithLogo()
	{
		if (url != null)
		{
			String presentationAddr = URIPresentationHelper.getHumanReadableDomain(url);
			return msg.getMessage("SPInfoComponent.requesterAddressAndName", name, presentationAddr);
		} else
		{
			return msg.getMessage("SPInfoComponent.requesterName", name);
		}
	}
}
