/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.WebSession;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.UI;
import com.vaadin.ui.Button.ClickEvent;


/**
 * Vaadin UI of the authentication application. Displays configured authenticators and 
 * coordinates authentication.
 * @author K. Benedyczak
 */
@Component("AuthenticationUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AuthenticationUI extends UI implements UnityWebUI
{
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("serial")
	@Override
	protected void init(final VaadinRequest request)
	{
		Button dummyAuthn = new Button("Authenticate");
		dummyAuthn.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				VaadinSession.getCurrent().getSession().setAttribute(
						WebSession.USER_SESSION_KEY, "dummy");
				logged();
			}
		});
		setContent(dummyAuthn);
	}
	
	private void logged()
	{
		VaadinSession vss = VaadinSession.getCurrent();
		if (vss == null)
			throw new RuntimeException("BUG Can't get VaadinSession to store authenticated user's data.");
		WrappedSession session = vss.getSession();
		UI ui = UI.getCurrent();
		if (ui == null)
			throw new RuntimeException("BUG Can't get UI to redirect the authenticated user.");
		String origURL = (String) session.getAttribute(AuthenticationFilter.ORIGINAL_ADDRESS);
		//String origFragment = (String) session.getAttribute(AuthenticationApp.ORIGINAL_FRAGMENT);
		if (origURL == null)
			return;
			//origURL = DEFAULT_PORTAL_PATH;
		//if (origFragment == null)
		//	origFragment = "";
		//else
		//	origFragment = "#" + origFragment;
		
		//origURL = origURL+origFragment;
		ui.getPage().open(origURL, "");
	}

	@Override
	public void configure(EndpointDescription description,
			List<Map<String, BindingAuthn>> authenticators)
	{
		// TODO Auto-generated method stub
		
	}
}
