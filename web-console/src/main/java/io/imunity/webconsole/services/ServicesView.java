/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.services;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.WebConsoleRootNavigationInfoProvider;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.StandardButtonsHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.endpoint.Endpoint.EndpointState;
import pl.edu.icm.unity.webui.authn.endpoints.ServiceEditorComponent.ServiceEditorTab;
import pl.edu.icm.unity.webui.authn.endpoints.ServiceTypeInfoHelper;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class ServicesView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "Services";

	private UnityMessageSource msg;
	private ServicesController controller;
	private GridWithActionColumn<EndpointEntry> servicesGrid;

	@Autowired
	ServicesView(UnityMessageSource msg, ServicesController controller)
	{
		this.msg = msg;
		this.controller = controller;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{

		HorizontalLayout buttonsBar = StandardButtonsHelper.buildTopButtonsBar(StandardButtonsHelper
				.build4AddAction(msg, e -> NavigationHelper.goToView(NewServiceView.VIEW_NAME)));
		servicesGrid = new GridWithActionColumn<>(msg, getActionsHandlers(), false);
		servicesGrid.addComponentColumn(
				e -> StandardButtonsHelper.buildLinkButton(e.endpoint.getName(), ev -> gotoEdit(e)),
				msg.getMessage("ServicesView.nameCaption"), 10).setSortable(true)
				.setComparator((e1, e2) -> {
					return e2.endpoint.getName().compareTo(e1.endpoint.getName());
				}).setId("name");

		servicesGrid.addComponentColumn(e -> getStatusLabel(e.endpoint.getState()),
				msg.getMessage("ServicesView.statusCaption"), 10);

		servicesGrid.addColumn(e -> ServiceTypeInfoHelper.getType(msg, e.type.getName()), msg.getMessage("ServicesView.typeCaption"), 10);

		servicesGrid.addColumn(e -> ServiceTypeInfoHelper.getBinding(msg, e.type.getSupportedBinding()),
				msg.getMessage("ServicesView.bindingCaption"), 10);

		servicesGrid.addHamburgerActions(getHamburgerActionsHandlers());

		servicesGrid.setItems(getServices());
		servicesGrid.sort("name");

		VerticalLayout main = new VerticalLayout();
		main.addComponent(buttonsBar);
		main.addComponent(servicesGrid);
		main.setWidth(100, Unit.PERCENTAGE);
		main.setMargin(false);
		setCompositionRoot(main);

	}

	private Label getStatusLabel(EndpointState state)
	{
		Label l = new Label();
		l.setContentMode(ContentMode.HTML);
		l.setValue(state.equals(EndpointState.DEPLOYED)? Images.ok.getHtml() : Images.reject.getHtml());
		l.setDescription(state.toString());
		return l;
	}
	
	private Collection<EndpointEntry> getServices()
	{
		try
		{
			return controller.getServices();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			return Collections.emptyList();
		}
	}

	void refresh()
	{
		Collection<EndpointEntry> services = getServices();
		servicesGrid.setItems(services);
	}

	private List<SingleActionHandler<EndpointEntry>> getActionsHandlers()
	{
		SingleActionHandler<EndpointEntry> editGeneral = SingleActionHandler.builder(EndpointEntry.class)
				.withCaption(msg.getMessage("ServicesView.generalConfig"))
				.withIcon(Images.cogs.getResource())
				.withHandler(r -> gotoEdit(r.iterator().next(), ServiceEditorTab.GENERAL)).build();

		SingleActionHandler<EndpointEntry> editAuth = SingleActionHandler.builder(EndpointEntry.class)
				.withCaption(msg.getMessage("ServicesView.authenticationConfig"))
				.withIcon(Images.sign_in.getResource())
				.withHandler(r -> gotoEdit(r.iterator().next(), ServiceEditorTab.AUTHENTICATION))
				.build();

		return Arrays.asList(editGeneral, editAuth);

	}

	private List<SingleActionHandler<EndpointEntry>> getHamburgerActionsHandlers()
	{
		SingleActionHandler<EndpointEntry> remove = SingleActionHandler.builder4Delete(msg, EndpointEntry.class)
				.withHandler(r -> tryRemove(r.iterator().next())).build();
		SingleActionHandler<EndpointEntry> deploy = SingleActionHandler.builder(EndpointEntry.class)
				.withCaption(msg.getMessage("ServicesView.deploy")).withIcon(Images.play.getResource())
				.withDisabledPredicate(e -> e.endpoint.getState().equals(EndpointState.DEPLOYED))
				.withHandler(r -> deploy(r.iterator().next())).build();
		SingleActionHandler<EndpointEntry> undeploy = SingleActionHandler.builder(EndpointEntry.class)
				.withCaption(msg.getMessage("ServicesView.undeploy"))
				.withDisabledPredicate(e -> e.endpoint.getState().equals(EndpointState.UNDEPLOYED))
				.withIcon(Images.undeploy.getResource()).withHandler(r -> undeploy(r.iterator().next()))
				.build();
		return Arrays.asList(remove, deploy, undeploy);

	}

	private void undeploy(EndpointEntry endpoint)
	{
		try
		{
			controller.undeploy(endpoint.endpoint);
			refresh();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
	}

	private void deploy(EndpointEntry endpoint)
	{
		try
		{
			controller.deploy(endpoint.endpoint);
			refresh();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
	}

	private void tryRemove(EndpointEntry endpoint)
	{

		String confirmText = MessageUtils.createConfirmFromStrings(msg,
				Arrays.asList(endpoint.endpoint.getName()));

		new ConfirmDialog(msg, msg.getMessage("ServicesView.confirmDelete", confirmText),
				() -> remove(endpoint)).show();
	}

	private void remove(EndpointEntry endpoint)
	{
		try
		{
			controller.remove(endpoint.endpoint);
			refresh();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}

	}

	private void gotoEdit(EndpointEntry next)
	{
		gotoEdit(next, ServiceEditorTab.GENERAL);
	}

	private void gotoEdit(EndpointEntry next, ServiceEditorTab tab)
	{
		NavigationHelper.goToView(EditServiceView.VIEW_NAME + "/" + CommonViewParam.name.toString() + "="
				+ next.endpoint.getName() + "&&" + CommonViewParam.tab.toString() + "="
				+ tab.toString().toLowerCase());
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("WebConsoleMenu.services");
	}

	@Component
	public class ServicesNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		@Autowired
		public ServicesNavigationInfoProvider(UnityMessageSource msg,
				WebConsoleRootNavigationInfoProvider parent, ObjectFactory<ServicesView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo()).withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.services"))
					.withIcon(Images.server.getResource()).withPosition(50).build());

		}
	}
}
