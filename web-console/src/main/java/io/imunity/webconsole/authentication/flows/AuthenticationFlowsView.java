/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.flows;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.authentication.AuthenticationNavigationInfoProvider;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.StandardButtonsHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions.ActionColumn;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions.ActionColumn.Position;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions.Column;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Lists all flows
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class AuthenticationFlowsView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "Flows";

	private AuthenticationFlowsController flowsMan;
	private UnityMessageSource msg;
	private ListOfElementsWithActions<AuthenticationFlowEntry> flowsList;

	@Autowired
	public AuthenticationFlowsView(UnityMessageSource msg, AuthenticationFlowsController flowsMan)
	{
		this.msg = msg;
		this.flowsMan = flowsMan;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		Button newCert = StandardButtonsHelper.build4AddAction(msg,
				e -> NavigationHelper.goToView(NewAuthenticationFlowView.VIEW_NAME));
		HorizontalLayout buttonsBar = StandardButtonsHelper.buildButtonsBar(Alignment.MIDDLE_RIGHT, newCert);

		SingleActionHandler<AuthenticationFlowEntry> edit = SingleActionHandler
				.builder4Edit(msg, AuthenticationFlowEntry.class)
				.withHandler(r -> gotoEdit(r.iterator().next())).build();

		SingleActionHandler<AuthenticationFlowEntry> remove = SingleActionHandler
				.builder4Delete(msg, AuthenticationFlowEntry.class)
				.withHandler(r -> tryRemove(r.iterator().next())
				).build();

		flowsList = new ListOfElementsWithActions<>(
				Arrays.asList(new Column<>(msg.getMessage("AuthenticationFlow.nameCaption"),
						f -> StandardButtonsHelper.buildLinkButton(f.flow.getName(),
								e -> gotoEdit(f)),
						1),
						new Column<>(msg.getMessage("AuthenticationFlow.endpointsCaption"),
								r -> new Label(String.join(", ", r.endpoints)), 4)),
				new ActionColumn<>(msg.getMessage("actions"), Arrays.asList(edit, remove), 0,
						Position.Right));

		flowsList.setAddSeparatorLine(true);

		for (AuthenticationFlowEntry flow : getFlows())
		{
			flowsList.addEntry(flow);
		}

		VerticalLayout main = new VerticalLayout();
		main.addComponent(buttonsBar);
		main.addComponent(flowsList);
		main.setWidth(100, Unit.PERCENTAGE);
		main.setMargin(false);
		setCompositionRoot(main);
	}

	private void gotoEdit(AuthenticationFlowEntry e)
	{
		NavigationHelper.goToView(EditAuthenticationFlowView.VIEW_NAME + "/" + CommonViewParam.name.toString()
				+ "=" + e.flow.getName());
	}

	private Collection<AuthenticationFlowEntry> getFlows()
	{
		try
		{
			return flowsMan.getFlows();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
		return Collections.emptyList();
	}

	private void remove(AuthenticationFlowEntry flow)
	{
		try
		{
			flowsMan.removeFlow(flow.flow);
			flowsList.removeEntry(flow);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
	}

	private void tryRemove(AuthenticationFlowEntry flow)
	{

		String confirmText = MessageUtils.createConfirmFromStrings(msg, Sets.newHashSet(flow.flow.getName()));
		new ConfirmDialog(msg, msg.getMessage("AuthenticationFlowsView.confirmDelete", confirmText),
				() -> remove(flow)).show();
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("WebConsoleMenu.authentication.flows");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class FlowsNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public FlowsNavigationInfoProvider(UnityMessageSource msg, AuthenticationNavigationInfoProvider parent,
				ObjectFactory<AuthenticationFlowsView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo()).withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.authentication.flows")).build());

		}
	}

}
