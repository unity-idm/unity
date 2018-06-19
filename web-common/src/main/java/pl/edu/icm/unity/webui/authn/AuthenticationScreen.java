/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Component;

/**
 * Component providing a complete authentication screen. This component will be used to 
 * fill the {@link AuthenticationUI} 
 * 
 * @author K. Benedyczak
 */
public interface AuthenticationScreen extends Component 
{
	public void refresh(VaadinRequest request); 
}
