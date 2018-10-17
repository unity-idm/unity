/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CustomComponent;

import pl.edu.icm.unity.webui.common.Styles;

/**
 * Simplified version of {@link IdPAuthNComponent} - no logo, other style, same otherwise
 *   
 * @author K. Benedyczak
 */
public class IdPAuthNGridComponent extends CustomComponent
{
	private Button providerB;

	public IdPAuthNGridComponent(String id, String name)
	{
		providerB = new Button();
		providerB.setStyleName(Styles.vButtonLink.toString());
		providerB.addStyleName(Styles.externalGridSignInButton.toString());
		providerB.addStyleName("u-idpAuthentication-" + id);
		providerB.setCaption(name);
		setCompositionRoot(providerB);
	}
	
	public void addClickListener(ClickListener listener)
	{
		providerB.addClickListener(listener);
	}
}
