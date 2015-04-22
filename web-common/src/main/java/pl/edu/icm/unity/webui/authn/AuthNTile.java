/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.util.Map;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.server.authn.AuthenticationOption;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;

/**
 * Component showing a group of {@link VaadinAuthenticationUI}s. All of them are presented by means of small 
 * {@link IdPComponent} (logo/label).
 * 
 *   
 * @author K. Benedyczak
 */
public interface AuthNTile
{
	void setCaption(String caption);
	
	AuthenticationOption getAuthenticationOptionById(String id);
	
	VaadinAuthenticationUI getAuthenticatorById(String id);
	
	int size();

	Map<String, VaadinAuthenticationUI> getAuthenticators();

	String getFirstOptionId();

	void filter(String filter);
	
	Component getComponent();
	
	public interface SelectionChangedListener
	{
		void selectionChanged(VaadinAuthenticationUI selectedAuthnUI, 
				AuthenticationOption selectedOption, String optionKey);
	}
}
