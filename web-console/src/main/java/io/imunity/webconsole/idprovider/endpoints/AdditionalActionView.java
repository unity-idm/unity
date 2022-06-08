/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.idprovider.endpoints;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.idprovider.endpoints.IdpServicesView.IdpServicesNavigationInfoProvider;
import io.imunity.webconsole.spi.services.IdpServiceAdditionalAction;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.menu.MenuButton;
import io.imunity.webelements.navigation.BreadcrumbsComponent;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityViewWithSubViews;
import io.imunity.webelements.navigation.WarnComponent;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.NotificationPopup;

@PrototypeComponent
public class AdditionalActionView extends CustomComponent implements UnityViewWithSubViews
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AdditionalActionView.class);
	public static final String VIEW_NAME = "AdditionalIdpServiceView";

	private MessageSource msg;
	private IdpServiceAdditionalActionsRegistry actionsRegistry;

	private String serviceName;
	private IdpServiceAdditionalAction action;;

	@Autowired
	public AdditionalActionView(MessageSource msg, IdpServiceAdditionalActionsRegistry registry)
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
			log.error("Error entering additional action view", e);
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
	public WarnComponent getWarnComponent()
	{
		return new WarnComponent();
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
		public ExtraActionServiceNavigationInfoProvider(ObjectFactory<AdditionalActionView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.ParameterizedViewWithSubviews)
					.withParent(IdpServicesNavigationInfoProvider.ID).withObjectFactory(factory).build());

		}
	}

}
