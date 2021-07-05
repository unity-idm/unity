/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.annotations.Theme;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import io.imunity.upman.common.UpManErrorView;
import io.imunity.upman.common.ViewHeader;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.layout.SidebarLayout;
import io.imunity.webelements.menu.MenuButton;
import io.imunity.webelements.menu.MenuComoboBox;
import io.imunity.webelements.menu.left.LeftMenu;
import io.imunity.webelements.menu.left.LeftMenuLabel;
import io.imunity.webelements.menu.top.TopRightMenu;
import io.imunity.webelements.navigation.AppContextViewProvider;
import io.imunity.webelements.navigation.NavigationHierarchyManager;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityEndpointUIBase;
import pl.edu.icm.unity.webui.authn.StandardWebLogoutHandler;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.exceptions.ControllerException;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiresDialogLauncher;

/**
 * The main entry point of the group management UI.
 * 
 * @author P.Piernik
 */
@Component("UpManUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
public class UpManUI extends UnityEndpointUIBase
{
	private StandardWebLogoutHandler authnProcessor;
	private SidebarLayout upManLayout;
	private NavigationHierarchyManager navigationMan;

	private MenuComoboBox projectCombo;
	private Map<String, String> projects;
	private ProjectController projectController;
	private HomeServiceLinkController homeServiceController;
	private UpmanEndpointProperties config;

	@Autowired
	public UpManUI(MessageSource msg, EnquiresDialogLauncher enquiryDialogLauncher,
			StandardWebLogoutHandler authnProcessor,
			Collection<UpManNavigationInfoProvider> providers, ProjectController controller,
			HomeServiceLinkController homeServiceController)
	{
		super(msg, enquiryDialogLauncher);
		this.authnProcessor = authnProcessor;
		this.navigationMan = new NavigationHierarchyManager(providers);
		this.projectController = controller;
		this.homeServiceController = homeServiceController;
	}

	@Override
	public void configure(ResolvedEndpoint description, List<AuthenticationFlow> authenticators,
			EndpointRegistrationConfiguration regCfg, Properties endpointProperties)
	{
		super.configure(description, authenticators, regCfg, endpointProperties);
		this.config = new UpmanEndpointProperties(endpointProperties);
	}

	private void buildTopMenu()
	{
		TopRightMenu topMenu = upManLayout.getTopRightMenu();

		Optional<String> homeLinkIfAvailable = homeServiceController.getHomeLinkIfAvailable(config);
		if (homeLinkIfAvailable.isPresent())
		{
			topMenu.addMenuElement(MenuButton.get("logout").withIcon(Images.home.getResource())
					.withDescription(msg.getMessage("UpManMenu.toHomeService")).withClickListener(
							e -> Page.getCurrent().open(homeLinkIfAvailable.get(), null)));
		}

		topMenu.addMenuElement(MenuButton.get("logout").withIcon(Images.exit.getResource())
				.withDescription(msg.getMessage("UpManMenu.logout")).withClickListener(e -> logout()));

	}

	private void logout()
	{
		authnProcessor.logout();
	}

	private void buildLeftMenu()
	{
		LeftMenu leftMenu = upManLayout.getLeftMenu();
		leftMenu.setToggleVisible(false);

		LeftMenuLabel logo = LeftMenuLabel.get();
		leftMenu.addMenuElement(logo);

		LeftMenuLabel space1 = LeftMenuLabel.get();
		leftMenu.addMenuElement(space1);

		projectCombo = MenuComoboBox.get().withCaption(msg.getMessage("UpManMenu.projectNameCaption"));
		projectCombo.setItemCaptionGenerator(i -> projects.get(i));
		projectCombo.setEmptySelectionAllowed(false);
		reloadProjectsCombo();
		logo.setIcon(projectController.getProjectLogoOrNull(projectCombo.getValue()));
		projectCombo.addValueChangeListener(e -> {
			if (e.getValue() == null || e.getValue().isEmpty())
				return;

			View view = UI.getCurrent().getNavigator().getCurrentView();
			if (view instanceof UnityView)
			{
				NavigationHelper.goToView(((UnityView) view).getViewName());
			}
			logo.setIcon(projectController.getProjectLogoOrNull(e.getValue()));
		});

		if (!(projects.size() == 1))
		{
			leftMenu.addMenuElement(projectCombo);
		} else
		{
			LeftMenuLabel projectLabel = LeftMenuLabel.get()
					.withCaption(msg.getMessage("UpManMenu.projectNameCaption") + " "
							+ projects.values().iterator().next());
			leftMenu.addMenuElement(projectLabel);
		}

		LeftMenuLabel space2 = LeftMenuLabel.get();
		leftMenu.addMenuElement(space2);

		leftMenu.addNavigationElements(UpManRootNavigationInfoProvider.ID);
	}

	@Override
	protected void enter(VaadinRequest request)
	{
		if (!reloadProjectInternal())
			return;
		VerticalLayout naviContent = new VerticalLayout();
		naviContent.setSizeFull();
		naviContent.setStyleName(Styles.contentBox.toString());
		Navigator navigator = new Navigator(this, naviContent);

		navigator.setErrorView((UnityView) navigationMan.getNavigationInfoMap()
				.get(UpManErrorView.VIEW_NAME).objectFactory.getObject());
		navigator.addProvider(new AppContextViewProvider(navigationMan));

		ViewHeader viewHedear = new ViewHeader();
		navigator.addViewChangeListener(viewHedear);

		upManLayout = new SidebarLayout(navigationMan, naviContent, viewHedear);
		buildTopMenu();
		buildLeftMenu();
		setContent(upManLayout);
	}

	@Override
	public String getUiRootPath()
	{
		return endpointDescription.getEndpoint().getContextAddress();
	}

	private DelegatedGroup getProjectGroupInternal() throws ControllerException
	{
		return projectController.getProjectGroup(projectCombo.getValue());
	}

	private boolean reloadProjectInternal()
	{
		try
		{
			projects = projectController.getProjectForUser(
					InvocationContext.getCurrent().getLoginSession().getEntityId());
		} catch (ControllerException e)
		{
			Notification notification = NotificationPopup.getErrorNotification(e.getCaption(),
					e.getDetails());
			notification.addCloseListener(l -> logout());
			notification.show(Page.getCurrent());
			setContent(new VerticalLayout());
			return false;
		}
		return true;
	}

	private void reloadProjectsCombo()
	{
		projectCombo.clear();
		if (projects != null)
		{
			projectCombo.setItems(projects.keySet());
			projectCombo.setValue(projects.keySet().iterator().next());
		}
	}

	public static DelegatedGroup getProjectGroup() throws ControllerException
	{
		UpManUI ui = (UpManUI) UI.getCurrent();
		return ui.getProjectGroupInternal();
	}

	public static void reloadProjects()
	{
		UpManUI ui = (UpManUI) UI.getCurrent();
		if (ui.reloadProjectInternal())
			ui.reloadProjectsCombo();
	}
}
