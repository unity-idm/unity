/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.identity_provider.endpoints;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;

import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.console.views.identity_provider.released_profile.endpoints.spi.IdpServiceAdditionalAction;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.NotificationPopup;

@PrototypeComponent
public class AdditionalIdpActionView extends ConsoleViewComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AdditionalIdpActionView.class);
	public static final String VIEW_NAME = "AdditionalIdpServiceView";

	private MessageSource msg;
	private IdpServiceAdditionalActionsRegistry actionsRegistry;

	private String serviceName;
	private IdpServiceAdditionalAction action;;

	@Autowired
	public AdditionalIdpActionView(MessageSource msg, IdpServiceAdditionalActionsRegistry registry)
	{
		this.msg = msg;
		this.actionsRegistry = registry;
	}

//	@Override
//	public void enter(ViewChangeEvent event)
//	{
//		serviceName = NavigationHelper.getParam(event, CommonViewParam.name.toString());
//		String actionName = NavigationHelper.getParam(event, CommonViewParam.action.toString());
//
//		try
//		{
//			action = actionsRegistry.getByName(actionName);
//			setCompositionRoot(actionsRegistry.getByName(actionName).getActionContent(serviceName));
//		} catch (Exception e)
//		{
//			log.error("Error entering additional action view", e);
//			NotificationPopup.showError(
//					msg.getMessage("AdditionalActionView.unsupportedActionType", actionName), "");
//		}
//
//		setSizeFull();
//
//	}
}
