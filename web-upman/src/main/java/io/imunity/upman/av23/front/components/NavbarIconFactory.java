/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

public class NavbarIconFactory
{
	public static Icon logoutCreate(Runnable logout){
		Icon logoutIcon = new Icon(VaadinIcon.SIGN_OUT);
		logoutIcon.getStyle().set("cursor", "pointer");
		logoutIcon.addClickListener(
			event -> logout.run()
		);
		return logoutIcon;
	}

	public static Icon homeCreate(){
		Icon logout = new Icon(VaadinIcon.HOME);
		logout.getStyle().set("cursor", "pointer");
		logout.addClickListener(
				event -> UI.getCurrent().getPage().setLocation("/logout")
		);
		return logout;
	}
}
