/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_browser;

import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.console.views.directory_browser.attributes.AttributesComponentPanel;
import io.imunity.console.views.directory_browser.group_browser.GroupBrowserPanel;
import io.imunity.console.views.directory_browser.group_details.GroupDetailsPanel;
import io.imunity.console.views.directory_browser.identities.IdentitiesPanel;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.endpoint.common.WebSession;
import jakarta.annotation.security.PermitAll;

@PermitAll
@RouteAlias(value = "/", layout = ConsoleMenu.class)
@Breadcrumb(key = "WebConsoleMenu.directoryBrowser")
@Route(value = "/directory-browser", layout = ConsoleMenu.class)
public class DirectoryBrowserView extends ConsoleViewComponent
{
	private final GroupBrowserPanel groupBrowserPanel;
	private final AttributesComponentPanel attributesPanel;
	private final IdentitiesPanel identitiesPanel;
	private final GroupDetailsPanel groupDetailsPanel;

	DirectoryBrowserView(GroupBrowserPanel groupBrowserPanel, AttributesComponentPanel attributesPanel, IdentitiesPanel identitiesPanel, GroupDetailsPanel groupDetailsPanel)
	{
		this.groupBrowserPanel = groupBrowserPanel;
		this.attributesPanel = attributesPanel;
		this.identitiesPanel = identitiesPanel;
		this.groupDetailsPanel = groupDetailsPanel;
		init();
	}

	public void init()
	{
		SplitLayout leftLayout = new SplitLayout(groupBrowserPanel, groupDetailsPanel);
		leftLayout.setOrientation(SplitLayout.Orientation.VERTICAL);
		leftLayout.setSplitterPosition(60);
		SplitLayout rightLayout = new SplitLayout(identitiesPanel, attributesPanel);
		rightLayout.setOrientation(SplitLayout.Orientation.VERTICAL);
		rightLayout.setSplitterPosition(60);
		SplitLayout mainLayout = new SplitLayout(leftLayout, rightLayout);
		mainLayout.setSplitterPosition(27);
		mainLayout.setSizeFull();

		getContent().add(mainLayout);
		getContent().setHeightFull();
		WebSession.getCurrent().getEventBus().fireEvent(new RefreshAndSelectEvent());
	}
}
