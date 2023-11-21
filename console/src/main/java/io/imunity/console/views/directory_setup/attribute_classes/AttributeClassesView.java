/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_setup.attribute_classes;

import com.google.common.collect.Sets;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.ActionIconBuilder;
import io.imunity.vaadin.elements.Breadcrumb;

import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.attribute.AttributesClass;
import pl.edu.icm.unity.base.describedObject.DescribedObjectROImpl;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;

import java.util.Comparator;

import static com.vaadin.flow.component.icon.VaadinIcon.EDIT;
import static com.vaadin.flow.component.icon.VaadinIcon.TRASH;
import static io.imunity.console.views.ViewHeaderActionLayoutFactory.createHeaderActionLayout;
import static io.imunity.vaadin.elements.CssClassNames.GRID_DETAILS_FORM;
import static io.imunity.vaadin.elements.CssClassNames.GRID_DETAILS_FORM_ITEM;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.directorySetup.attributeClasses", parent = "WebConsoleMenu.directorySetup")
@Route(value = "/attribute-classes", layout = ConsoleMenu.class)
public class AttributeClassesView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final AttributeClassController controller;
	private Grid<AttributesClass> attributesClassGrid;

	AttributeClassesView(MessageSource msg, AttributeClassController controller)
	{
		this.msg = msg;
		this.controller = controller;
		initUI();
	}

	public void initUI()
	{
		attributesClassGrid = new Grid<>();
		attributesClassGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
		attributesClassGrid.setItemDetailsRenderer(new ComponentRenderer<>(this::getDetailsComponent));

		Grid.Column<AttributesClass> nameColumn = attributesClassGrid.addComponentColumn(this::createNameWithDetailsArrow)
				.setHeader(msg.getMessage("AttributeClassesView.nameCaption"))
				.setAutoWidth(true)
				.setSortable(true)
				.setComparator(Comparator.comparing(DescribedObjectROImpl::getName));
		attributesClassGrid.addColumn(a -> String.join(", ", a.getAllowed()))
				.setHeader(msg.getMessage("AttributeClassesView.allowedCaption"))
				.setAutoWidth(true)
				.setSortable(true);
		attributesClassGrid.addColumn(a -> String.join(", ", a.getMandatory()))
				.setHeader(msg.getMessage("AttributeClassesView.mandatoryCaption"))
				.setAutoWidth(true)
				.setSortable(true);
		attributesClassGrid.addComponentColumn(this::createRowActionMenu)
				.setHeader(msg.getMessage("actions"))
				.setTextAlign(ColumnTextAlign.END);

		attributesClassGrid.sort(GridSortOrder.asc(nameColumn).build());
		attributesClassGrid.setItems(controller.getAttributeClasses());

		VerticalLayout main = new VerticalLayout(createHeaderActionLayout(msg, AttributeClassesEditView.class), attributesClassGrid);
		main.setSpacing(false);
		getContent().add(main);
	}

	private Component createRowActionMenu(AttributesClass attributesClass)
	{
		Icon generalSettings = new ActionIconBuilder()
				.icon(EDIT)
				.tooltipText(msg.getMessage("edit"))
				.navigation(AttributeClassesEditView.class, attributesClass.getName())
				.build();

		Icon remove = new ActionIconBuilder()
				.icon(TRASH)
				.tooltipText(msg.getMessage("remove"))
				.clickListener(() -> tryRemove(attributesClass))
				.build();

		HorizontalLayout horizontalLayout = new HorizontalLayout(generalSettings, remove);
		horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		return horizontalLayout;
	}

	private void tryRemove(AttributesClass attributesClass)
	{
		String confirmText = MessageUtils.createConfirmFromStrings(msg, Sets.newHashSet(attributesClass.getName()));
		new ConfirmDialog(
				msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("AttributeClassesView.confirmDelete", confirmText),
				msg.getMessage("ok"),
				e -> remove(attributesClass),
				msg.getMessage("cancel"),
				e -> {}
		).open();
	}

	private void remove(AttributesClass attrClass)
	{
		controller.removeAttributeClass(attrClass);
		attributesClassGrid.setItems(controller.getAttributeClasses());
	}

	private FormLayout getDetailsComponent(AttributesClass attributesClass)
	{
		FormLayout wrapper = new FormLayout();
		FormLayout.FormItem formItem = wrapper.addFormItem(
				new Span(attributesClass.getDescription()),
				msg.getMessage("AttributeClassesView.descriptionLabelCaption")
		);
		formItem.addClassName(GRID_DETAILS_FORM_ITEM.getName());
		wrapper.addClassName(GRID_DETAILS_FORM.getName());
		return wrapper;
	}

	private HorizontalLayout createNameWithDetailsArrow(AttributesClass attributesClass)
	{
		RouterLink label = new RouterLink(attributesClass.getName(), AttributeClassesEditView.class, attributesClass.getName());
		Icon openIcon = VaadinIcon.ANGLE_RIGHT.create();
		Icon closeIcon = VaadinIcon.ANGLE_DOWN.create();
		openIcon.setVisible(!attributesClassGrid.isDetailsVisible(attributesClass));
		closeIcon.setVisible(attributesClassGrid.isDetailsVisible(attributesClass));
		openIcon.addClickListener(e -> attributesClassGrid.setDetailsVisible(attributesClass, true));
		closeIcon.addClickListener(e -> attributesClassGrid.setDetailsVisible(attributesClass, false));
		return new HorizontalLayout(openIcon, closeIcon, label);
	}

}
