/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.views;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import io.imunity.upman.av23.components.ProjectService;
import io.imunity.upman.av23.components.Vaddin23WebLogoutHandler;
import io.imunity.upman.av23.front.UnityAppLayout;
import io.imunity.upman.av23.front.components.MenuComponent;
import io.imunity.upman.av23.front.components.UnityViewComponent;
import io.imunity.upman.av23.front.model.ProjectGroup;
import io.imunity.upman.av23.front.views.groups.GroupsView;
import io.imunity.upman.av23.front.views.members.MembersView;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.vaadin.flow.component.icon.VaadinIcon.*;
import static java.util.stream.Collectors.toList;

@CssImport(value = "./styles/vaadin-combo-box.css", themeFor = "vaadin-combo-box")
public class UpManMenu extends UnityAppLayout
{
	private Optional<UnityViewComponent> currentView = Optional.empty();

	@Autowired
	public UpManMenu(Vaddin23WebLogoutHandler standardWebLogoutHandler, ProjectService projectController, MessageSource msg) {
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
						.collect(toList()), standardWebLogoutHandler
		);
		HorizontalLayout imageLayout = new HorizontalLayout();
		imageLayout.getStyle().set("margin-top", "1.5em");
		imageLayout.getStyle().set("margin-bottom", "1.5em");


		List<ProjectGroup> projectGroups = projectController.getProjectForUser(InvocationContext.getCurrent().getLoginSession().getEntityId());

		super.initView();

		HorizontalLayout comboBoxLayout = createComboBoxLayout(projectController, msg, projectGroups, imageLayout);

		addToLeftContainerAsFirst(comboBoxLayout);
		addToLeftContainerAsFirst(imageLayout);
	}

	public HorizontalLayout createComboBoxLayout(ProjectService projectController, MessageSource msg, List<ProjectGroup> projectGroups, HorizontalLayout imageLayout)
	{
		ComboBox<ProjectGroup> comboBox = new ComboBox<>();
		comboBox.setClassName("project-combo-box");
		comboBox.setLabel(msg.getMessage("UpManMenu.projectNameCaption"));

		HorizontalLayout comboBoxLayout = new HorizontalLayout(comboBox);
		comboBoxLayout.setAlignItems(Alignment.CENTER);
		comboBoxLayout.setJustifyContentMode(JustifyContentMode.CENTER);
		comboBoxLayout.getStyle().set("margin-bottom", "1.5em");

		comboBox.addValueChangeListener(event ->
			{
				if(event.getValue() == null)
				{
					comboBox.setValue(event.getOldValue());
					return;
				}
				ComponentUtil.setData(UI.getCurrent(), ProjectGroup.class, event.getValue());
				currentView.ifPresent(UnityViewComponent::loadData);
				Image image = new Image(projectController.getProjectLogo(event.getValue()), "");
				image.setId("unity-logo-image");
				imageLayout.removeAll();
				imageLayout.add(image);
			});

		comboBox.setItemLabelGenerator(projectGroup -> projectGroup.displayedName);
		comboBox.setItems(projectGroups);
		comboBox.setClearButtonVisible(false);
		if(projectGroups.iterator().hasNext())
			comboBox.setValue(projectGroups.iterator().next());

		return comboBoxLayout;
	}

	@Override
	public void showRouterLayoutContent(HasElement content) {
		super.showRouterLayoutContent(content);
		currentView = Optional.of((UnityViewComponent) content);
	}

}
