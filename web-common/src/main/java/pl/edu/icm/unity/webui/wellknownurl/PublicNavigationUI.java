/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.wellknownurl;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;

/**
 * The Vaadin UI providing a concrete view depending on URL fragment. Actual views are configured via DI.
 * This variant is for use with unprotected resources, i.e. those not requiring prior authentication.
 * 
 * @author K. Benedyczak
 */
@Component("PublicNavigationUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
@PreserveOnRefresh
public class PublicNavigationUI extends GenericNavigationUI<PublicViewProvider>
{
	@Autowired
	public PublicNavigationUI(UnityMessageSource msg, Collection<PublicViewProvider> viewProviders)
	{
		super(msg, viewProviders);
	}

	@Override
	protected void refresh(VaadinRequest request)
	{
		super.refresh(request);
		if (this.viewProviders != null)
			this.viewProviders.forEach(view -> view.refresh(request, navigator));
	}
}


