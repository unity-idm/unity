/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman;

import java.util.Collection;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.vaadin.annotations.Theme;
import com.vaadin.navigator.PushStateNavigation;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webelements.layout.SidebarLayout;
import io.imunity.webelements.menu.MenuButton;
import io.imunity.webelements.menu.MenuComoboBox;
import io.imunity.webelements.menu.left.LeftMenu;
import io.imunity.webelements.menu.left.LeftMenuLabel;
import io.imunity.webelements.menu.top.TopMenu;
import io.imunity.webelements.navigation.AppContextViewProvider;
import io.imunity.webelements.navigation.NavigationHierarchyManager;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.UnityEndpointUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.authn.StandardWebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiresDialogLauncher;

/**
 * The main entry point of the group management UI.
 * 
 * @author P.Piernik
 *
 */
@PushStateNavigation
@Component("UpManUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
public class UpManUI extends UnityEndpointUIBase implements UnityWebUI
{
	private StandardWebAuthenticationProcessor authnProcessor;
	private SidebarLayout upManLayout;
	private AppContextViewProvider appContextViewProvider;
	private NavigationHierarchyManager navigationMan;

	@Autowired
	public UpManUI(UnityMessageSource msg, EnquiresDialogLauncher enquiryDialogLauncher,
			StandardWebAuthenticationProcessor authnProcessor,
			Collection<UpManNavigationInfoProvider> providers)
	{
		super(msg, enquiryDialogLauncher);
		this.authnProcessor = authnProcessor;

		this.navigationMan = new NavigationHierarchyManager(providers);
		this.appContextViewProvider = new AppContextViewProvider(navigationMan);

	}

	private void buildTopMenu()
	{
		TopMenu topMenu = upManLayout.getTopMenu();

		topMenu.addMenuElement(MenuButton.get("logout").withIcon(Images.exit.getResource())
				.withDescription(msg.getMessage("UpManMenu.logout"))
				.withClickListener(e -> logout()));

	}

	private void logout()
	{
		authnProcessor.logout();
	}

	private void buildLeftMenu()
	{
		LeftMenu leftMenu = upManLayout.getLeftMenu();
		LeftMenuLabel label = LeftMenuLabel.get().withIcon(Images.logoSmall.getResource());
				//TODO - disabled until minimalized menu CSS is fixed.
				//.withClickListener(e -> webConsoleLayout.getLeftMenu().toggleSize());

		leftMenu.addMenuElement(label);

		LeftMenuLabel space1 = LeftMenuLabel.get();
		
		leftMenu.addMenuElement(space1);
		
		MenuComoboBox projectCombo = MenuComoboBox.get().withCaption(msg.getMessage("UpManMenu.projectNameCaption"));
		
		//TODO Fill project names
		//LoginSession entity = InvocationContext.getCurrent().getLoginSession();
		projectCombo.setItems(getProjectNames(null));
		projectCombo.setValue(getProjectNames(null).iterator().next());
		projectCombo.setEmptySelectionAllowed(false);
	
		leftMenu.addMenuElement(projectCombo);

		LeftMenuLabel space = LeftMenuLabel.get();
		
		leftMenu.addMenuElement(space);
		
		
		leftMenu.addNavigationElements(UpManRootNavigationInfoProvider.ID);
	}

	@Override
	protected void enter(VaadinRequest request)
	{
		upManLayout = SidebarLayout.get(navigationMan)
				.withNaviContent(new VerticalLayout())
				.withViewProvider(appContextViewProvider)
				.withErrorView((UnityView) navigationMan.getNavigationInfoMap()
						.get(UpManErrorView.VIEW_NAME).objectFactory
								.getObject())
				.build();
		buildTopMenu();
		buildLeftMenu();
		setContent(upManLayout);
	}
	
	@Override
	public String getUiRootPath()
	{
		return endpointDescription.getEndpoint().getContextAddress();
	}
	
	//TODO 
	private Set<String> getProjectNames(String loggedUserName)
	{
		return Sets.newHashSet("Project 1" , "Project 2" , "Project 3");
		
	}


}
