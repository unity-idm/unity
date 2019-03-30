/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.idpcommon;

import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.Label100;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

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
		main.setSpacing(false);
		main.setMargin(false);
		
		if (logo != null)
		{
			Image logoI = new Image();
			logoI.setAlternateText(msg.getMessage("SPInfoComponent.requesterName", name));
			logoI.setSource(logo);
			main.addComponent(logoI);
			main.setComponentAlignment(logoI, Alignment.TOP_CENTER);

			Label spacer = HtmlTag.br();
			spacer.addStyleName(Styles.vLabelSmall.toString());
			main.addComponent(spacer);
		}
		
		Label info1Id = new Label100(msg.getMessage("SPInfoComponent.requesterName", name));
		info1Id.addStyleName(Styles.vLabelLarge.toString());
		main.addComponent(info1Id);
		
		if (url != null)
		{
			Label info1Addr = new Label100(msg.getMessage("SPInfoComponent.requesterAddress", url));
			info1Addr.addStyleName(Styles.vLabelSmall.toString());
			main.addComponent(info1Addr);
		}

		setCompositionRoot(main);
	}
}
