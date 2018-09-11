/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import org.apache.logging.log4j.util.Strings;

import com.vaadin.server.Page;
import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RedirectConfig;
import pl.edu.icm.unity.webui.common.ImageUtils;
import pl.edu.icm.unity.webui.common.Styles;


/**
 * Registration completed view. Can be used both in case of finishing up with an error or success.
 * Optionally a link button can be shown to redirect a person to a target page. 
 */
class RegistrationCompletedComponent extends CustomComponent
{
	RegistrationCompletedComponent(UnityMessageSource msg, String information, boolean error, String logoURL, 
			RedirectConfig redirectConfig)
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(true);
		
		if (logoURL != null && !logoURL.isEmpty())
		{
			Resource logoResource = ImageUtils.getConfiguredImageResource(logoURL);
			Image image = new Image(null, logoResource);
			image.addStyleName("u-signup-logo");
			main.addComponent(image);
			main.setComponentAlignment(image, Alignment.MIDDLE_CENTER);
		}
		
		HorizontalLayout headerWrapper = new HorizontalLayout();
		headerWrapper.setSpacing(false);
		headerWrapper.setMargin(false);
		
		Label infoL = new Label(information);
		infoL.addStyleName(error ? "u-reg-final-info" : "u-reg-final-error");
		main.addComponent(infoL);
		main.setComponentAlignment(infoL, Alignment.MIDDLE_CENTER);
		
		if (redirectConfig != null && !Strings.isEmpty(redirectConfig.getRedirectURL()))
		{
			Button redirectB = new Button(redirectConfig.getRedirectCaption().getValue(msg));
			redirectB.setStyleName(Styles.vButtonPrimary.toString());
			redirectB.addStyleName("u-reg-final-redirect");
			redirectB.addClickListener(e -> 
				Page.getCurrent().open(redirectConfig.getRedirectURL(), null));
			main.addComponent(redirectB);
			main.setComponentAlignment(redirectB, Alignment.MIDDLE_CENTER);
		}
		
		setCompositionRoot(main);
	}
}
