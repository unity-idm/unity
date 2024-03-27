/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_setup.attribute_classes;

import static io.imunity.console.views.ViewHeaderActionLayoutFactory.createHeaderActionLayout;
import static io.imunity.vaadin.elements.CssClassNames.GRID_DETAILS_FORM;
import static io.imunity.vaadin.elements.CssClassNames.GRID_DETAILS_FORM_ITEM;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.grid.GridWithActionColumn;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.attribute.AttributesClass;
import pl.edu.icm.unity.base.describedObject.DescribedObjectROImpl;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.directorySetup.attributeClasses", parent = "WebConsoleMenu.directorySetup")
@Route(value = "/attribute-classes", layout = ConsoleMenu.class)
public class AttributeClassesView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final AttributeClassController controller;
	private GridWithActionColumn<AttributesClass> attributesClassGrid;

	AttributeClassesView(MessageSource msg, AttributeClassController controller)
	{
		this.msg = msg;
		this.controller = controller;
		initUI();
	}

	public void initUI()
	{

		attributesClassGrid = new GridWithActionColumn<AttributesClass>(msg::getMessage, getActionsHandlers());
		attributesClassGrid.addShowDetailsColumn(new ComponentRenderer<>(this::getDetailsComponent));

		Grid.Column<AttributesClass> nameColumn = attributesClassGrid
				.addComponentColumn(this::createNameWithDetailsArrow)
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

		attributesClassGrid.sort(GridSortOrder.asc(nameColumn)
				.build());
		attributesClassGrid.setItems(controller.getAttributeClasses());

		VerticalLayout main = new VerticalLayout(createHeaderActionLayout(msg, AttributeClassesEditView.class),
				attributesClassGrid);
		main.setSpacing(false);
		getContent().add(main);
	}

	private List<SingleActionHandler<AttributesClass>> getActionsHandlers()
	{
		SingleActionHandler<AttributesClass> edit = SingleActionHandler
				.builder4Edit(msg::getMessage, AttributesClass.class)
				.withHandler(r -> gotoEdit(r.iterator()
						.next()))
				.build();

		SingleActionHandler<AttributesClass> remove = SingleActionHandler
				.builder4Delete(msg::getMessage, AttributesClass.class)
				.withHandler(this::tryRemove)
				.build();

		return Arrays.asList(edit, remove);
	}

	private void gotoEdit(AttributesClass next)
	{
		UI.getCurrent()
				.navigate(AttributeClassesEditView.class, next.getName());
	}

	private void tryRemove(Set<AttributesClass> items)
	{
		AttributesClass item = items.iterator()
				.next();
		String confirmText = MessageUtils.createConfirmFromStrings(msg, Sets.newHashSet(item.getName()));
		new ConfirmDialog(msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("AttributeClassesView.confirmDelete", confirmText), msg.getMessage("ok"),
				e -> remove(item), msg.getMessage("cancel"), e ->
				{
				}).open();
	}

	private void remove(AttributesClass attrClass)
	{
		controller.removeAttributeClass(attrClass);
		attributesClassGrid.setItems(controller.getAttributeClasses());
	}

	private FormLayout getDetailsComponent(AttributesClass attributesClass)
	{
		FormLayout wrapper = new FormLayout();
		FormLayout.FormItem formItem = wrapper.addFormItem(new Span(attributesClass.getDescription()),
				msg.getMessage("AttributeClassesView.descriptionLabelCaption"));
		formItem.addClassName(GRID_DETAILS_FORM_ITEM.getName());
		wrapper.addClassName(GRID_DETAILS_FORM.getName());
		return wrapper;
	}

	private RouterLink createNameWithDetailsArrow(AttributesClass attributesClass)
	{
		return new RouterLink(attributesClass.getName(), AttributeClassesEditView.class, attributesClass.getName());
	}

}
