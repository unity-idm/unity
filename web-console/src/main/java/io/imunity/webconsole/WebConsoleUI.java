/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.PushStateNavigation;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.authentication.Flows;
import io.imunity.webconsole.authentication.Routes;
import io.imunity.webconsole.authentication.realms.EditRealm;
import io.imunity.webconsole.authentication.realms.NewRealm;
import io.imunity.webconsole.authentication.realms.Realms;
import io.imunity.webconsole.idprovider.OAuth;
import io.imunity.webconsole.idprovider.SAML;
import io.imunity.webconsole.layout.LeftMenu;
import io.imunity.webconsole.layout.TopMenu;
import io.imunity.webconsole.layout.WebConsoleLayout;
import io.imunity.webconsole.leftmenu.components.MenuButton;
import io.imunity.webconsole.leftmenu.components.MenuLabel;
import io.imunity.webconsole.leftmenu.components.SubMenu;
import io.imunity.webconsole.other.OtherServices;
import io.imunity.webconsole.topmenu.components.TopMenuTextField;
import io.imunity.webconsole.userprofile.UserProfile;
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
	private WebConsoleLayout webConsoleLayout;
	private AppContextViewProvider appContextViewProvider;

	@Autowired
	public WebConsoleUI(UnityMessageSource msg, EnquiresDialogLauncher enquiryDialogLauncher,
			StandardWebAuthenticationProcessor authnProcessor,
			AppContextViewProvider viewProvider)
	{
		super(msg, enquiryDialogLauncher);
		this.authnProcessor = authnProcessor;
		this.appContextViewProvider = viewProvider;
	}

	private void setDefaultPage()
	{
		UI.getCurrent().getNavigator().setErrorView(Dashboard.class);

	}

	private void buildTopOnlyMenu()
	{
		TopMenu topMenu = webConsoleLayout.getTopMenu();
		topMenu.add(TopMenuTextField.get(VaadinIcons.SEARCH, msg.getMessage("WebConsoleUIMenu.search")));
		topMenu.add(MenuButton.get().withIcon(VaadinIcons.HOME)
				.withDescription(msg.getMessage("WebConsoleUIMenu.dashboard"))
				.withNavigateTo(Dashboard.class));

		topMenu.add(MenuButton.get().withIcon(Images.exit.getResource())
				.withDescription(msg.getMessage("WebConsoleUIMenu.logout"))
				.withClickListener(e -> logout()));

	}

	private void logout()
	{
		authnProcessor.logout();
	}

	private void buildLeftMenu()
	{
		LeftMenu leftMenu = webConsoleLayout.getLeftMenu();
		MenuLabel label = MenuLabel.get()
				.withIcon(Images.logoSmall.getResource());

		label.addLayoutClickListener(e -> {
			webConsoleLayout.getLeftMenu().toggleSize();
		});
		leftMenu.add(label);

		MenuButton dashboard = leftMenu.add(MenuButton.get()
				.withCaption(msg.getMessage("WebConsoleUIMenu.dashboard"))
				.withIcon(VaadinIcons.HOME).withNavigateTo(Dashboard.class));

		webConsoleLayout.getBreadCrumbs().setRoot(dashboard);

		SubMenu idprovider = leftMenu.add(SubMenu.get()
				.withCaption(msg.getMessage("WebConsoleUIMenu.idprovider"))
				.withIcon(Images.ok.getResource()));

		idprovider.add(MenuButton.get()
				.withCaption(msg.getMessage("WebConsoleUIMenu.oauth"))
				.withIcon(Images.ok.getResource()).withNavigateTo(OAuth.class));

		idprovider.add(MenuButton.get().withCaption(msg.getMessage("WebConsoleUIMenu.saml"))
				.withIcon(Images.ok.getResource()).withNavigateTo(SAML.class));

		leftMenu.add(MenuButton.get()
				.withCaption(msg.getMessage("WebConsoleUIMenu.userProfile"))
				.withIcon(Images.ok.getResource())
				.withNavigateTo(UserProfile.class));

		leftMenu.add(MenuButton.get()
				.withCaption(msg.getMessage("WebConsoleUIMenu.otherServices"))
				.withIcon(Images.ok.getResource())
				.withNavigateTo(OtherServices.class));

		SubMenu authentication = leftMenu.add(SubMenu.get()
				.withCaption(msg.getMessage("WebConsoleUIMenu.authentication"))
				.withIcon(Images.ok.getResource()));

		MenuButton realms = MenuButton.get()
				.withCaption(msg.getMessage("WebConsoleUIMenu.realms"))
				.withIcon(Images.ok.getResource()).withNavigateTo(Realms.class);

		MenuButton newRealm = MenuButton.get()
				.withCaption(msg.getMessage("WebConsoleUIMenu.newRealm"))
				.withNavigateTo(NewRealm.class);
		
		MenuButton editRealm = MenuButton.get()
				.withCaption(msg.getMessage("WebConsoleUIMenu.editRealm"))
				.withNavigateTo(EditRealm.class).withBreadCrumbProvider(e -> e.getParameters());

		//FIXME - I think this is a good example of a problem with this design: is "new realm" a button? no it is not.
		//is realms button in left menu a parent of button "new realm"? it is not. 
		//I think what is mixed up here are views, their relationships and elements of menus. 
		//I'd suggest: create a structure of views. A new interface (e.g. UIView). UIView can have parent and subordinary UIViews.
		//UIView may provide a component to be inserted to left menu 
		//(note: LeftMenu has very trivial needs for its contents, basically just a Component)
		// menu element UI != menu element. One menu element may have different UIs etc.
		realms.add(newRealm);
		realms.add(editRealm);

		authentication.add(realms);

		authentication.add(MenuButton.get()
				.withCaption(msg.getMessage("WebConsoleUIMenu.flows"))
				.withIcon(Images.ok.getResource()).withNavigateTo(Flows.class));

		authentication.add(MenuButton.get()
				.withCaption(msg.getMessage("WebConsoleUIMenu.routes"))
				.withIcon(Images.ok.getResource()).withNavigateTo(Routes.class));

	}

	@Override
	protected void enter(VaadinRequest request)
	{

		webConsoleLayout = WebConsoleLayout.get().withNaviContent(new VerticalLayout())
				.withViewProvider(appContextViewProvider).build();
		buildTopOnlyMenu();
		buildLeftMenu();
		setDefaultPage();
		setContent(webConsoleLayout);
	}
}
