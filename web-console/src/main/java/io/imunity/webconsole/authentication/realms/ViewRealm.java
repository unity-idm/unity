/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.realms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

import io.imunity.webconsole.common.ControllerException;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Show realm view
 * 
 * @author P.Piernik
 *
 */
@Component
public class ViewRealm extends FormLayout implements View
{
	private RealmController controller;
	private UnityMessageSource msg;

	@Autowired
	public ViewRealm(UnityMessageSource msg, RealmController controller)
	{
		this.msg = msg;
		this.controller = controller;
		setMargin(true);
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		String realmName = event.getParameterMap().isEmpty() ? ""
				: event.getParameterMap().keySet().iterator().next();

		AuthenticationRealm realm;
		try
		{
			realm = controller.getRealm(realmName);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e.getErrorCaption(), e.getErrorDetails());
			UI.getCurrent().getNavigator().navigateTo(Realms.class.getSimpleName());
			return;
		}

		Label name = new Label(realm.getName());
		name.setCaption(msg.getMessage("AuthenticationRealm.name"));
		addComponent(name);

		Label desc = new Label(realm.getDescription());
		desc.setCaption(msg.getMessage("AuthenticationRealm.description"));
		addComponent(desc);

		Label blockFor = new Label(String.valueOf(realm.getBlockFor()));
		blockFor.setCaption(msg.getMessage("AuthenticationRealm.blockFor"));
		addComponent(blockFor);

		Label blockAfterUnsuccessfulLogins = new Label(
				String.valueOf(realm.getBlockAfterUnsuccessfulLogins()));
		blockAfterUnsuccessfulLogins.setCaption(
				msg.getMessage("AuthenticationRealm.blockAfterUnsuccessfulLogins"));
		addComponent(blockAfterUnsuccessfulLogins);

		Label maxInactivity = new Label(String.valueOf(realm.getMaxInactivity()));
		maxInactivity.setCaption(msg.getMessage("AuthenticationRealm.maxInactivity"));
		addComponent(maxInactivity);

		Label allowForRememberMeDays = new Label(
				String.valueOf(realm.getAllowForRememberMeDays()));
		allowForRememberMeDays.setCaption(
				msg.getMessage("AuthenticationRealm.allowForRememberMeDays"));
		addComponent(allowForRememberMeDays);

		Label rememberMePolicy = new Label(realm.getRememberMePolicy().toString());
		rememberMePolicy.setCaption(msg.getMessage("AuthenticationRealm.rememberMePolicy"));
		addComponent(rememberMePolicy);
	}
}
