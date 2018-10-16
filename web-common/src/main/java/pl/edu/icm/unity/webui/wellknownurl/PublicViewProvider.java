/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.wellknownurl;

import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.server.VaadinRequest;

/**
 * Implementations provide {@link View}s for registration in {@link PublicNavigationUI}.
 * Marker interface.
 * 
 * @author K. Benedyczak
 */
public interface PublicViewProvider extends ViewProvider
{
	public void refresh(VaadinRequest request, Navigator navigator); 
}
