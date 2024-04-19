/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.directoryBrowser;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.WebConsoleRootNavigationInfoProvider;
import io.imunity.webconsole.directoryBrowser.attributes.AttributesComponentPanel;
import io.imunity.webconsole.directoryBrowser.groupbrowser.GroupBrowserPanel;
import io.imunity.webconsole.directoryBrowser.groupdetails.GroupDetailsPanel;
import io.imunity.webconsole.directoryBrowser.identities.IdentitiesPanel;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.webui.common.CompositeSplitPanel;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.sessionscope.WebSessionComponent;

@WebSessionComponent
public class DirectoryBrowser extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "DirectoryBrowser";

	private MessageSource msg;
	private GroupBrowserPanel groupBrowserPanel;
	private AttributesComponentPanel attributesPanel;
	private IdentitiesPanel identitiesPanel;
	private GroupDetailsPanel groupDetailsPanel;

	@Autowired
	public DirectoryBrowser(MessageSource msg, GroupBrowserPanel groupBrowser,
			IdentitiesPanel identitiesTable, GroupDetailsPanel groupDetails,
			AttributesComponentPanel attributesComponent)
	{
		this.msg = msg;
		this.groupBrowserPanel = groupBrowser;
		this.identitiesPanel = identitiesTable;
		this.groupDetailsPanel = groupDetails;
		this.attributesPanel = attributesComponent;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{

		VerticalLayout mainL = new VerticalLayout();
		mainL.setMargin(false);
		mainL.setSpacing(false);
		mainL.setSizeFull();

		CompositeSplitPanel rightPanel = new CompositeSplitPanel(true, false, identitiesPanel,
				attributesPanel, 50);
		CompositeSplitPanel leftPanel = new CompositeSplitPanel(true, false, groupBrowserPanel, groupDetailsPanel, 50);

		CompositeSplitPanel main = new CompositeSplitPanel(false, false, leftPanel, rightPanel, 30);
		main.setMargin(new MarginInfo(true, false, false, false));

		mainL.addComponent(main);
		setSizeFull();

		setCompositionRoot(mainL);
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("WebConsoleMenu.directoryBrowser");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public class DirectoryBrowserNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		@Autowired
		@Lazy
		public DirectoryBrowserNavigationInfoProvider(MessageSource msg,
				ObjectFactory<DirectoryBrowser> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.DefaultView)
					.withParent(WebConsoleRootNavigationInfoProvider.ID).withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.directoryBrowser"))
					.withIcon(Images.group.getResource()).withPosition(10).build());

		}
	}
}
