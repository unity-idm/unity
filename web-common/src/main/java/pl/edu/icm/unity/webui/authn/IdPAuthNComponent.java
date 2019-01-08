/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import com.vaadin.server.Resource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CustomComponent;

import pl.edu.icm.unity.types.authn.AuthenticationOptionKeyUtils;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Small widget showing a clickable component presenting a remote IdP. Implemented as Button.
 * Suitable as an authenticate button.
 * Logo + Sing in with ...
 *  
 * @author K. Benedyczak
 */
public class IdPAuthNComponent extends CustomComponent
{
	private Button providerB;

	public IdPAuthNComponent(String id, Resource logo, String name)
	{
		providerB = new Button();
		providerB.setStyleName(Styles.vButtonLink.toString());
		providerB.addStyleName(Styles.externalSignInButton.toString());
		providerB.addStyleName("u-idpAuthentication-" + AuthenticationOptionKeyUtils.encodeToCSS(id));
		providerB.setCaption(name);
		if (logo != null)
			providerB.setIcon(logo);
		setCompositionRoot(providerB);
	}
	
	public void addClickListener(ClickListener listener)
	{
		providerB.addClickListener(listener);
	}
}
