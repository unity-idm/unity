/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.realms;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;

import io.imunity.webconsole.WebConsoleNavigationInfoProvider;
import io.imunity.webconsole.authentication.realms.Realms.RealmsNavigationInfoProvider;
import io.imunity.webelements.common.AbstractConfirmView;
import io.imunity.webelements.exception.ControllerException;
import io.imunity.webelements.navigation.NameParamViewNameProvider;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * View for add realm
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class NewRealm extends AbstractConfirmView
{

	public static String VIEW_NAME = "NewRealm";

	private RealmController controller;
	private AuthenticationRealmEditor editor;

	@Autowired
	public NewRealm(UnityMessageSource msg, RealmController controller)
	{
		super(msg, msg.getMessage("ok", msg.getMessage("cancel")));
		this.controller = controller;
	}

	@Override
	protected void onConfirm()
	{
		if (editor.hasErrors())
		{
			return;
		}

		try
		{
			if (!controller.addRealm(editor.getAuthenticationRealm()))
				return;
		} catch (ControllerException e)
		{

			NotificationPopup.showError(e.getErrorCaption(), e.getErrorDetails());
			return;
		}

		UI.getCurrent().getNavigator().navigateTo(Realms.class.getSimpleName());

	}

	@Override
	protected void onCancel()
	{
		UI.getCurrent().getNavigator().navigateTo(Realms.class.getSimpleName());

	}

	private AuthenticationRealm getDefaultAuthenticationRealm()
	{
		AuthenticationRealm bean = new AuthenticationRealm();
		bean.setName(msg.getMessage("AuthenticationRealm.defaultName"));
		bean.setRememberMePolicy(RememberMePolicy.allowFor2ndFactor);
		bean.setAllowForRememberMeDays(14);
		bean.setBlockFor(60);
		bean.setMaxInactivity(1800);
		bean.setBlockAfterUnsuccessfulLogins(5);
		return bean;
	}

	@Override
	protected Component getContents(ViewChangeEvent event) throws Exception
	{

		editor = new AuthenticationRealmEditor(msg, getDefaultAuthenticationRealm());
		return editor;
	}

	@org.springframework.stereotype.Component
	public static class NewRealmNavigationInfoProvider
			implements WebConsoleNavigationInfoProvider
	{

		private RealmsNavigationInfoProvider parent;
		private ObjectFactory<?> factory;

		@Autowired
		public NewRealmNavigationInfoProvider(RealmsNavigationInfoProvider parent,
				ObjectFactory<NewRealm> factory)
		{
			this.parent = parent;
			this.factory = factory;

		}

		@Override
		public NavigationInfo getNavigationInfo()
		{

			return new NavigationInfo.NavigationInfoBuilder(VIEW_NAME,
					Type.ParameterizedView)
							.withParent(parent.getNavigationInfo())
							.withObjectFactory(factory)
							.withDisplayNameProvider(
									new NameParamViewNameProvider())
							.build();
		}
	}
}
