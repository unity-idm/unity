/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.idprovider;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.idprovider.IdpServicesView.IdpServicesNavigationInfoProvider;
import io.imunity.webconsole.spi.services.IdpServiceAdditionalAction;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.menu.MenuButton;
import io.imunity.webelements.navigation.BreadcrumbsComponent;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityViewWithSubViews;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.NotificationPopup;

@PrototypeComponent
public class AdditionalActionView extends CustomComponent implements UnityViewWithSubViews
{
	public static final String VIEW_NAME = "AdditionalIdpServiceView";

	private UnityMessageSource msg;
	private IdpServiceAdditionalActionsRegistry actionsRegistry;

	private String serviceName;
	private IdpServiceAdditionalAction action;;

	@Autowired
	public AdditionalActionView(UnityMessageSource msg, IdpServiceAdditionalActionsRegistry registry)
	{
		this.msg = msg;
		this.actionsRegistry = registry;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		serviceName = NavigationHelper.getParam(event, CommonViewParam.name.toString());
		String actionName = NavigationHelper.getParam(event, CommonViewParam.action.toString());

		try
		{
			action = actionsRegistry.getByName(actionName);
			setCompositionRoot(actionsRegistry.getByName(actionName).getActionContent(serviceName));
		} catch (Exception e)
		{
			NotificationPopup.showError(
					msg.getMessage("AdditionalActionView.unsupportedActionType", actionName), "");
		}

		setSizeFull();

	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Override
	public String getDisplayedName()
	{
		return serviceName;

	}

	@Override
	public BreadcrumbsComponent getBreadcrumbsComponent()
	{
		BreadcrumbsComponent component = new BreadcrumbsComponent();
		component.addButton(new MenuButton("", serviceName));
		component.addSeparator();
		component.addButton(new MenuButton("", action.getDisplayedName()));
		return component;
	}

	@Component
	public static class ExtraActionServiceNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public ExtraActionServiceNavigationInfoProvider(IdpServicesNavigationInfoProvider parent,
				ObjectFactory<AdditionalActionView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.ParameterizedViewWithSubviews)
					.withParent(parent.getNavigationInfo()).withObjectFactory(factory).build());

		}
	}

}
