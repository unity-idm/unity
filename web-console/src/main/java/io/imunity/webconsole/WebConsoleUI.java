/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.PushStateNavigation;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.dashboard.Dashboard;
import io.imunity.webelements.common.MenuButton;
import io.imunity.webelements.common.MenuElementContainer;
import io.imunity.webelements.layout.SidebarLayout;
import io.imunity.webelements.leftMenu.LeftMenu;
import io.imunity.webelements.leftMenu.MenuLabel;
import io.imunity.webelements.leftMenu.SubMenu;
import io.imunity.webelements.navigation.AppContextViewProvider;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.NavigationManager;
import io.imunity.webelements.topMenu.TopMenu;
import io.imunity.webelements.topMenu.TopMenuTextField;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.UnityEndpointUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.authn.StandardWebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiresDialogLauncher;

/**
 * The main entry point of the web console UI.
 * 
 * @author P.Piernik
 *
 */
@PushStateNavigation
@Component("WebConsoleUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
@PreserveOnRefresh
public class WebConsoleUI extends UnityEndpointUIBase implements UnityWebUI
{
	private StandardWebAuthenticationProcessor authnProcessor;
	private SidebarLayout webConsoleLayout;
	private AppContextViewProvider appContextViewProvider;
	private NavigationManager navigationMan;

	@Autowired
	public WebConsoleUI(UnityMessageSource msg, EnquiresDialogLauncher enquiryDialogLauncher,
			StandardWebAuthenticationProcessor authnProcessor,
			Collection<WebConsoleNavigationInfoProvider> providers)
	{
		super(msg, enquiryDialogLauncher);
		this.authnProcessor = authnProcessor;

		this.navigationMan = new NavigationManager(providers);
		this.appContextViewProvider = new AppContextViewProvider(navigationMan);

	}

	private void buildTopOnlyMenu()
	{
		TopMenu topMenu = webConsoleLayout.getTopMenu();
		topMenu.addMenuElement(TopMenuTextField.get(VaadinIcons.SEARCH,
				msg.getMessage("WebConsoleMenu.search")));
		topMenu.addMenuElement(MenuButton.get("home").withIcon(VaadinIcons.HOME)
				.withDescription(msg.getMessage("WebConsoleMenu.dashboard"))
				.withNavigateTo(Dashboard.class));

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
		MenuLabel label = MenuLabel.get().withIcon(Images.logoSmall.getResource())
				.withClickListener(e -> {
					webConsoleLayout.getLeftMenu().toggleSize();
				});

		leftMenu.addEntry(label);

		buildLeftMenu(navigationMan.getChildren(RootNavigationInfoProvider.ID), leftMenu);
	}

	private void buildLeftMenu(List<NavigationInfo> viewChildren, LeftMenu menuContainer)

	{
		for (NavigationInfo child : viewChildren)
		{
			if (child.type == Type.ViewGroup)
			{

				MenuElementContainer subMenu = SubMenu.get(child.id)
						.withCaption(child.id).withIcon(child.icon);
				buildSubMenu(navigationMan.getChildren(child.id), subMenu);
				menuContainer.addSubContainerEntry(subMenu);

			} else if (child.type == Type.View || child.type == Type.DefaultView)
			{
				menuContainer.addEntry(MenuButton.get(child.id)
						.withCaption(child.id).withNavigateTo(child.id)
						.withIcon(child.icon));
			}
		}
	}

	private void buildSubMenu(List<NavigationInfo> viewChildren,
			MenuElementContainer menuContainer)
	{
		for (NavigationInfo child : viewChildren)
		{
			menuContainer.addMenuElement(MenuButton.get(child.id).withCaption(child.id)
					.withNavigateTo(child.id).withIcon(child.icon));
		}
	}

	@Override
	protected void enter(VaadinRequest request)
	{

		webConsoleLayout = SidebarLayout.get(navigationMan)
				.withNaviContent(new VerticalLayout())
				.withViewProvider(appContextViewProvider).build();
		buildTopOnlyMenu();
		buildLeftMenu();
		setContent(webConsoleLayout);
	}
}
