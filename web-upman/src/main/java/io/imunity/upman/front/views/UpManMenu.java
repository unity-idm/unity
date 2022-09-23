/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NavigationTrigger;
import io.imunity.upman.front.UnityAppLayout;
import io.imunity.upman.front.UnityViewComponent;
import io.imunity.upman.front.model.ProjectGroup;
import io.imunity.upman.front.views.groups.GroupsView;
import io.imunity.upman.front.views.invitations.InvitationsView;
import io.imunity.upman.front.views.members.MembersView;
import io.imunity.upman.front.views.user_updates.UserUpdatesView;
import io.imunity.upman.utils.HomeServiceLinkService;
import io.imunity.upman.utils.ProjectService;
import io.imunity.vaadin23.elements.MenuComponent;
import io.imunity.vaadin23.endpoint.common.Vaddin23WebLogoutHandler;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.vaadin.flow.component.icon.VaadinIcon.*;
import static java.util.stream.Collectors.toList;

@CssImport(value = "./styles/vaadin-combo-box.css", themeFor = "vaadin-combo-box")
public class UpManMenu extends UnityAppLayout implements BeforeEnterObserver
{
	private final ProjectService projectService;
	private final ProjectsLayout projectsLayout;
	private Optional<UnityViewComponent> currentView = Optional.empty();

	@Autowired
	public UpManMenu(Vaddin23WebLogoutHandler standardWebLogoutHandler, ProjectService projectService, MessageSource msg,
	                 HomeServiceLinkService homeServiceLinkService)
	{
		super(Stream.of(
						MenuComponent.builder(MembersView.class).tabName(msg.getMessage("UpManMenu.members"))
								.icon(FAMILY).build(),
						MenuComponent.builder(GroupsView.class).tabName(msg.getMessage("UpManMenu.groups"))
								.icon(FILE_TREE).build(),
						MenuComponent.builder(InvitationsView.class).tabName(msg.getMessage("UpManMenu.invitations"))
								.icon(ENVELOPE_OPEN_O).build(),
						MenuComponent.builder(UserUpdatesView.class).tabName(msg.getMessage("UpManMenu.userUpdates"))
								.icon(USER_CHECK).build()
						)
						.collect(toList()), standardWebLogoutHandler, createHomeIcon(homeServiceLinkService)
		);
		this.projectService = projectService;

		HorizontalLayout imageLayout = new HorizontalLayout();
		imageLayout.getStyle().set("margin-top", "1.5em");
		imageLayout.getStyle().set("margin-bottom", "1.5em");


		List<ProjectGroup> projectGroups = projectService.getProjectForUser(InvocationContext.getCurrent().getLoginSession().getEntityId());

		super.initView();

		projectsLayout = new ProjectsLayout(msg, projectGroups, imageLayout);
		addToLeftContainerAsFirst(projectsLayout);
		addToLeftContainerAsFirst(imageLayout);
	}

	private static List<Component> createHomeIcon(HomeServiceLinkService homeServiceLinkService)
	{
		return homeServiceLinkService.getHomeLinkIfAvailable()
				.map(UpManMenu::createHomeIcon)
				.stream().collect(toList());
	}

	private static Component createHomeIcon(String url)
	{
		Icon home = VaadinIcon.HOME.create();
		home.getStyle().set("cursor", "pointer");
		home.addClickListener(event -> UI.getCurrent().getPage().setLocation(url));
		return home;
	}

	@Override
	public void showRouterLayoutContent(HasElement content)
	{
		super.showRouterLayoutContent(content);
		currentView = Optional.of((UnityViewComponent) content);
	}

	@Override
	public void beforeEnter(BeforeEnterEvent beforeEnterEvent)
	{
		if(beforeEnterEvent.getTrigger().equals(NavigationTrigger.PROGRAMMATIC))
		{
			projectsLayout.load(projectService.getProjectForUser(InvocationContext.getCurrent().getLoginSession().getEntityId()));
			ComponentUtil.setData(UI.getCurrent(), ProjectGroup.class, projectsLayout.selectedProject);
			currentView.ifPresent(UnityViewComponent::loadData);
		}
		if(ComponentUtil.getData(UI.getCurrent(), ProjectGroup.class) == null)
		{
			if (projectsLayout.selectedProject == null)
			{
				beforeEnterEvent.rerouteToError(IllegalAccessException.class);
				return;
			}
			ComponentUtil.setData(UI.getCurrent(), ProjectGroup.class, projectsLayout.selectedProject);
		}
	}

	class ProjectsLayout extends HorizontalLayout
	{
		private final MessageSource msg;
		private final HorizontalLayout imageLayout;

		private ProjectGroup selectedProject;

		ProjectsLayout(MessageSource msg, List<ProjectGroup> projectGroups, HorizontalLayout imageLayout)
		{
			this.msg = msg;
			this.imageLayout = imageLayout;

			setAlignItems(Alignment.CENTER);
			setJustifyContentMode(JustifyContentMode.CENTER);
			getStyle().set("margin-bottom", "1.5em");

			load(projectGroups);
		}

		private void load(List<ProjectGroup> projectGroups)
		{
			removeAll();
			if(projectGroups.size() == 1)
				add(createLabel(projectGroups));
			else
				add(createComboBox(projectGroups));
		}

		private ComboBox<ProjectGroup> createComboBox(List<ProjectGroup> projectGroups)
		{
			ComboBox<ProjectGroup> comboBox = new ComboBox<>();
			comboBox.setClassName("project-combo-box");
			comboBox.setLabel(msg.getMessage("UpManMenu.projectNameCaption"));

			comboBox.addValueChangeListener(event ->
			{
				if(event.getValue() == null)
				{
					comboBox.setValue(event.getOldValue());
					return;
				}
				selectedProject = event.getValue();
				ComponentUtil.setData(UI.getCurrent(), ProjectGroup.class, event.getValue());
				currentView.ifPresent(UnityViewComponent::loadData);
				setImage(event.getValue());
			});

			comboBox.setItemLabelGenerator(projectGroup -> projectGroup.displayedName);
			comboBox.setItems(projectGroups);
			comboBox.setClearButtonVisible(false);
			if(projectGroups.iterator().hasNext())
				comboBox.setValue(projectGroups.iterator().next());

			return comboBox;
		}

		private void setImage(ProjectGroup projectGroup)
		{
			Image image = new Image(projectService.getProjectLogo(projectGroup), "");
			image.setId("unity-logo-image");
			imageLayout.removeAll();
			imageLayout.add(image);
		}

		private Label createLabel(List<ProjectGroup> projectGroups)
		{
			ProjectGroup projectGroup = projectGroups.iterator().next();
			selectedProject = projectGroup;
			Label label = new Label(msg.getMessage("UpManMenu.projectNameCaption") + " " + projectGroup.displayedName);
			label.getStyle().set("color", "white");
			setImage(selectedProject);
			return label;
		}
	}
}
