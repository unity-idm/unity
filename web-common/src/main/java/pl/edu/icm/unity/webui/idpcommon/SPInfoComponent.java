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

import pl.edu.icm.unity.base.message.MessageSource;
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
	private MessageSource msg;
	private Resource logo;
	private String name;
	private String url;
	
	
	/**
	 * @param logo can be null
	 * @param url can be null
	 */
	public SPInfoComponent(MessageSource msg, Resource logo, String name, String url)
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

			main.addComponent(getAddressInfoLabel(getRequesterInfoWithLogo()));
		} else
		{
			Label requesterNameInfo = new Label100(msg.getMessage("SPInfoComponent.requesterName", name));
			requesterNameInfo.addStyleName("u-authn-title");
			requesterNameInfo.addStyleName(Styles.textCenter.toString());
			main.addComponent(requesterNameInfo);
			
			if (url != null)
			{
				String presentationAddr = msg.getMessage("SPInfoComponent.requesterAddress", 
						URIPresentationHelper.getHumanReadableDomain(url));
				main.addComponent(getAddressInfoLabel(presentationAddr));
			}
		}
		
		Label spacer = HtmlTag.br();
		spacer.addStyleName(Styles.vLabelSmall.toString());
		main.addComponent(spacer);
		Label requestedAccessInfo = new Label100(msg.getMessage("SPInfoComponent.requestedAccess"));
		requestedAccessInfo.addStyleName("u-requestedAccessInfo");
		requestedAccessInfo.addStyleName(Styles.textCenter.toString());
		main.addComponent(requestedAccessInfo);
		
		setCompositionRoot(main);
	}
	
	private Label getAddressInfoLabel(String message)
	{
		Label infoAddr = new Label100(message);
		infoAddr.addStyleName("u-requesterAddressInfo");
		infoAddr.addStyleName(Styles.textCenter.toString());
		return infoAddr;
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
