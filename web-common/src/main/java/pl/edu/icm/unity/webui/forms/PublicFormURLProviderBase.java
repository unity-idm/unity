/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.forms;

import java.util.Objects;

import org.apache.logging.log4j.Logger;

import com.vaadin.navigator.Navigator;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.webui.forms.reg.PublicRegistrationURLProvider;
import pl.edu.icm.unity.webui.wellknownurl.PublicViewProvider;

/**
 * Base for public form view providers.
 * @author P.Piernik
 *
 */
public abstract class PublicFormURLProviderBase implements PublicViewProvider
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, PublicRegistrationURLProvider.class);

	@Override
	public void refresh(VaadinRequest request, Navigator navigator)
	{
		StandalonePublicView view = getViewFromSession();
		if (view != null)
		{
			String viewName = getCurrentViewName();
			String requestedFormName = getFormName(getCurrentViewName());
			String cachedFormName = view.getFormName();
			if (requestedFormName != null && Objects.equals(requestedFormName, cachedFormName))
			{
				view.refresh(request);
			}

			else
			{
				navigator.navigateTo(viewName);
			}
			LOG.debug("Form " + view.getFormName() + " refreshed");
		}
	}

	private String getCurrentViewName()
	{
		String viewName = Page.getCurrent().getUriFragment();
		if (viewName.startsWith("!"))
			viewName = viewName.substring(1);
		return viewName;
	}
	
	protected abstract StandalonePublicView getViewFromSession();
	protected abstract String getFormName(String viewAndParameters);
}
