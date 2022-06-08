/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.annotations.Theme;
import com.vaadin.navigator.Navigator;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Notification;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.spi.WebConsoleExtNavigationInfoProvider;
import io.imunity.webelements.layout.SidebarLayout;
import io.imunity.webelements.menu.MenuButton;
import io.imunity.webelements.menu.left.LeftMenu;
import io.imunity.webelements.menu.left.LeftMenuLabel;
import io.imunity.webelements.menu.top.TopRightMenu;
import io.imunity.webelements.navigation.NavigationHierarchyManager;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.webui.UnityEndpointUIBase;
import pl.edu.icm.unity.webui.authn.StandardWebLogoutHandler;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.exceptions.ControllerException;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiresDialogLauncher;

/**
 * The main entry point of the web console UI.
 * 
 * @author P.Piernik
 *
 */
@Component("WebConsoleUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
public class WebConsoleUI extends UnityEndpointUIBase
{
	private StandardWebLogoutHandler authnProcessor;
	private SidebarLayout webConsoleLayout;
	private NavigationHierarchyManager navigationMan;
	private AuthorizationController authzController;

	@Autowired
	public WebConsoleUI(MessageSource msg, EnquiresDialogLauncher enquiryDialogLauncher,
			StandardWebLogoutHandler authnProcessor, AuthorizationController authzController,
			Collection<WebConsoleNavigationInfoProvider> providers,
			Collection<WebConsoleExtNavigationInfoProvider> extProviders)
	{
		super(msg, enquiryDialogLauncher);
		this.authnProcessor = authnProcessor;
		this.authzController = authzController;
		this.navigationMan = new NavigationHierarchyManager(
				Stream.concat(providers.stream(), extProviders.stream()).collect(Collectors.toList()));
	}

	private void buildTopRightMenu()
	{
		TopRightMenu topMenu = webConsoleLayout.getTopRightMenu();

		topMenu.addMenuElement(MenuButton.get("logout").withIcon(Images.exit.getResource())
				.withDescription(msg.getMessage("WebConsoleMenu.logout"))
				.withClickListener(e -> logout()));

	}

	private void logout()
	{
		authnProcessor.logout();
	}

	private void buildLeftMenu()
	{
		LeftMenu leftMenu = webConsoleLayout.getLeftMenu();
		LeftMenuLabel logo = LeftMenuLabel.get().withIcon(Images.logoSmall.getResource());

		leftMenu.addMenuElement(logo);
		LeftMenuLabel space1 = LeftMenuLabel.get();
		leftMenu.addMenuElement(space1);
		leftMenu.addNavigationElements(WebConsoleRootNavigationInfoProvider.ID);
	}

	@Override
	protected void enter(VaadinRequest request)
	{	
		try
		{
			authzController.hasAdminAccess();
		} catch (ControllerException e)
		{
			Notification notification = NotificationPopup
					.getErrorNotification(e.getCaption(), e.getDetails());
			notification.addCloseListener(l -> logout());
			notification.show(Page.getCurrent());
			setContent(new VerticalLayout());
			return;
		}
	
		VerticalLayout naviContent = new VerticalLayout();
		naviContent.setSizeFull();
		naviContent.setStyleName(Styles.contentBox.toString());
		Navigator navigator = new Navigator(this, naviContent);
		
		navigator.setErrorView((UnityView) navigationMan.getNavigationInfoMap()
				.get(WebConsoleErrorView.VIEW_NAME).objectFactory
				.getObject());
		navigator.addProvider(new WebConsoleAppContextViewProvider(navigationMan, sandboxRouter));
		WebConsoleBreadCrumbs breadCrumbs = new WebConsoleBreadCrumbs(navigationMan);
		navigator.addViewChangeListener(breadCrumbs);
		WebConsoleWarnViewComponent webConsoleWarnComponent = new WebConsoleWarnViewComponent();
		navigator.addViewChangeListener(webConsoleWarnComponent);
		
		webConsoleLayout =  new SidebarLayout(navigationMan, naviContent, breadCrumbs, webConsoleWarnComponent);
	
		buildTopRightMenu();
		buildLeftMenu();
	
		setContent(webConsoleLayout);
	}
	
	@Override
	public String getUiRootPath()
	{
		return endpointDescription.getEndpoint().getContextAddress();
	}
}
