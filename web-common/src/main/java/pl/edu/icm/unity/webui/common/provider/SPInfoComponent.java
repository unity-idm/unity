/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.provider;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

/**
 * Presents an information about a service provider which is asking for a favor.
 * Can show its logo, name and URL.
 * @author K. Benedyczak
 */
public class SPInfoComponent extends CustomComponent
{
	private UnityMessageSource msg;
	private Resource logo;
	private String name;
	private String url;
	
	
	/**
	 * @param msg
	 * @param logo can be null
	 * @param name
	 * @param url can be null
	 */
	public SPInfoComponent(UnityMessageSource msg, Resource logo, String name, String url)
	{
		super();
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
		VerticalLayout main = new VerticalLayout();
		
		if (logo != null)
		{
			Image logoI = new Image();
			logoI.setAlternateText(msg.getMessage("SPInfoComponent.requesterName", name));
			logoI.setSource(logo);
			main.addComponent(logoI);
			main.setComponentAlignment(logoI, Alignment.TOP_CENTER);

			Label spacer = HtmlTag.br();
			spacer.addStyleName(Reindeer.LABEL_SMALL);
			main.addComponent(spacer);
		}
		
		Label info1Id = new Label(msg.getMessage("SPInfoComponent.requesterName", name));
		info1Id.addStyleName(Reindeer.LABEL_H2);
		main.addComponent(info1Id);
		
		if (url != null)
		{
			Label info1Addr = new Label(msg.getMessage("SPInfoComponent.requesterAddress", url));
			info1Addr.addStyleName(Reindeer.LABEL_SMALL);
			main.addComponent(info1Addr);
		}

		setCompositionRoot(main);
	}
}
