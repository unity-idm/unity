/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_setup.attribute_types;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.StreamResource;

import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.SearchField;
import io.imunity.vaadin.endpoint.common.ActionMenuWithHandlerSupport;
import io.imunity.vaadin.endpoint.common.ComponentWithToolbar;
import io.imunity.vaadin.endpoint.common.Toolbar;
import io.imunity.vaadin.endpoint.common.grid.FilterableGridHelper;
import io.imunity.vaadin.endpoint.common.grid.GridWithActionColumn;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.SingleActionHandler;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.directorySetup.attributeTypes")
@Route(value = "/attribute-types", layout = ConsoleMenu.class)
public class AttributeTypesView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final AttributeTypeController controller;
	private final NotificationPresenter notificationPresenter;
	private GridWithActionColumn<AttributeTypeEntry> attrTypesGrid;

	@Autowired
	AttributeTypesView(MessageSource msg, AttributeTypeController controller,
			NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.controller = controller;
		this.notificationPresenter = notificationPresenter;
		init();
	}

	private void init()
	{
		attrTypesGrid = new GridWithActionColumn<AttributeTypeEntry>(msg, getActionsHandlers());
		attrTypesGrid.addShowDetailsColumn(new ComponentRenderer<>(this::getDetailsComponent));
		attrTypesGrid.setMultiSelect(true);

		Column<AttributeTypeEntry> name = attrTypesGrid.addComponentColumn(this::createNameWithDetailsArrow)
				.setHeader(msg.getMessage("AttributeTypesView.nameCaption"))
				.setAutoWidth(true)
				.setSortable(true)
				.setComparator(Comparator.comparing(r -> r.attributeType.getName()));

		attrTypesGrid.addComponentColumn(at -> new NativeLabel(at.getDisplayedName()))
				.setHeader(msg.getMessage("AttributeTypesView.displayedNameCaption"))
				.setSortable(true);
		attrTypesGrid.addComponentColumn(at -> new NativeLabel(at.attributeType.getValueSyntax()))
				.setHeader(msg.getMessage("AttributeTypesView.typeCaption"))
				.setSortable(true);
		attrTypesGrid.addComponentColumn(at -> generateCheckBox(at.attributeType.isSelfModificable()))
				.setHeader(msg.getMessage("AttributeTypesView.selfModifiableCaption"))
				.setSortable(true);
		attrTypesGrid.addComponentColumn(at -> new NativeLabel(at.getBoundsDesc()))
				.setHeader(msg.getMessage("AttributeTypesView.cardinalityCaption"))
				.setSortable(true);
		attrTypesGrid.addComponentColumn(at -> generateCheckBox(at.attributeType.isUniqueValues()))
				.setHeader(msg.getMessage("AttributeTypesView.uniqueValuesCaption"))
				.setSortable(true);


		attrTypesGrid.addHamburgerActions(getHamburgerActionsHandlers());

		ActionMenuWithHandlerSupport<AttributeTypeEntry> hamburgerMenu = new ActionMenuWithHandlerSupport<>();
		hamburgerMenu.addActionHandlers(getHamburgerCommonHandlers());
		attrTypesGrid.addSelectionListener(hamburgerMenu.getSelectionListener());

		SearchField search = FilterableGridHelper.generateSearchField(attrTypesGrid, msg);

		Toolbar<AttributeTypeEntry> toolbar = new Toolbar<>();
		toolbar.addHamburger(hamburgerMenu);
		toolbar.addSearch(search);
		ComponentWithToolbar attrTypeGridWithToolbar = new ComponentWithToolbar(attrTypesGrid, toolbar);
		attrTypeGridWithToolbar.setSpacing(false);
		attrTypeGridWithToolbar.setSizeFull();

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		main.setPadding(true);
		main.setSizeFull();
		main.add(createHeaderActionLayout());
		main.add(attrTypeGridWithToolbar);

		getContent().add(main);
		getContent().setHeightFull();
	
		refresh();
		attrTypesGrid.sort(GridSortOrder.asc(name)
				.build());
	}

	private List<SingleActionHandler<AttributeTypeEntry>> getHamburgerCommonHandlers()
	{

		SingleActionHandler<AttributeTypeEntry> remove = SingleActionHandler
				.builder4Delete(msg, AttributeTypeEntry.class)
				.withDisabledPredicate(at -> at.attributeType.isTypeImmutable())
				.withHandler(this::tryRemove)
				.build();

		SingleActionHandler<AttributeTypeEntry> export = SingleActionHandler.builder(AttributeTypeEntry.class)
				.multiTarget()
				.withCaption(msg.getMessage("AttributeTypesView.export"))
				.withDisabledPredicate(at -> at.attributeType.isTypeImmutable())
				.withIcon(VaadinIcon.EXCHANGE)
				.withHandler(this::export)
				.build();

		return Arrays.asList(export, remove);

	}

	private List<SingleActionHandler<AttributeTypeEntry>> getHamburgerActionsHandlers()
	{
		SingleActionHandler<AttributeTypeEntry> copy = SingleActionHandler.builder4Copy(msg, AttributeTypeEntry.class)
				.withDisabledPredicate(at -> at.attributeType.isTypeImmutable())
				.withHandler(at -> goToCopy(at.iterator()
						.next()))
				.build();
		List<SingleActionHandler<AttributeTypeEntry>> hamburgerHandlers = new ArrayList<>();
		hamburgerHandlers.add(copy);
		hamburgerHandlers.addAll(getHamburgerCommonHandlers());
		return hamburgerHandlers;

	}

	private List<SingleActionHandler<AttributeTypeEntry>> getActionsHandlers()
	{
		SingleActionHandler<AttributeTypeEntry> edit = SingleActionHandler.builder4Edit(msg, AttributeTypeEntry.class)
				.withDisabledPredicate(at -> !at.isEditable())
				.withHandler(r -> gotoEdit(r.iterator()
						.next()))
				.build();

		return Arrays.asList(edit);
	}

	private void gotoEdit(AttributeTypeEntry next)
	{
		UI.getCurrent()
				.navigate(EditAttributeTypeView.class, next.attributeType.getName());
	}

	private void goToCopy(AttributeTypeEntry next)
	{
		UI.getCurrent()
				.navigate(NewAttributeTypeView.class, next.attributeType.getName());
	}

	private void export(Set<AttributeTypeEntry> selectedItems)
	{
		Anchor download = new Anchor(getStreamResource(selectedItems), "");
		download.getElement()
				.setAttribute("download", true);
		getContent().add(download);
		download.getElement()
				.executeJs("return new Promise(resolve =>{this.click(); setTimeout(() => resolve(true), 150)})",
						download.getElement())
				.then(j -> getContent().remove(download));
	}

	private StreamResource getStreamResource(Set<AttributeTypeEntry> selectedItems)
	{
		return new StreamResource(getNewFilename(selectedItems), () ->
		{

			try
			{
				byte[] content = Constants.MAPPER.writeValueAsBytes(selectedItems.stream()
						.map(at -> at.attributeType)
						.collect(Collectors.toSet()));
				return new ByteArrayInputStream(content);
			} catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		})
		{
			@Override
			public Map<String, String> getHeaders()
			{
				Map<String, String> headers = new HashMap<>(super.getHeaders());
				headers.put("Content-Disposition", "attachment; filename=\"" + getNewFilename(selectedItems) + "\"");
				return headers;
			}
		};
	}

	private String getNewFilename(Set<AttributeTypeEntry> selectedItems)
	{
		return selectedItems.size() > 1 ? "attributeTypes.json"
				: (selectedItems.iterator()
						.next().attributeType.getName() + ".json");
	}

	private void refresh()
	{
		attrTypesGrid.setItems(getAttributeTypes());
	}

	private void tryRemove(Set<AttributeTypeEntry> items)
	{
		String confirmText = MessageUtils.createConfirmFromNames(msg, items.stream()
				.map(a -> a.attributeType)
				.collect(Collectors.toSet()));
		Checkbox checkbox = new Checkbox(msg.getMessage("AttributeTypesView.withInstances"));
		ConfirmDialog confirmDialog = new ConfirmDialog(msg.getMessage("ConfirmDialog.confirm"), "",
				msg.getMessage("ok"), e -> remove(items, checkbox.getValue()), msg.getMessage("cancel"), e ->
				{
				});
		confirmDialog.add(new NativeLabel(msg.getMessage("AttributeTypesView.confirmDelete", confirmText)), checkbox);
		confirmDialog.open();

	}

	private void remove(Set<AttributeTypeEntry> items, boolean withInstances)
	{
		try
		{
			controller.removeAttributeTypes(items.stream()
					.map(a -> a.attributeType)
					.collect(Collectors.toSet()), withInstances);
			items.forEach(a -> attrTypesGrid.removeElement(a));
		} catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getMessage());
		}
	}

	public HorizontalLayout createHeaderActionLayout()
	{
		HorizontalLayout headerLayout = new HorizontalLayout();
		headerLayout.setPadding(false);
		headerLayout.setMargin(false);
		headerLayout.setSpacing(true);
		headerLayout.setWidthFull();
		Button addButton = new Button(msg.getMessage("addNew"), e -> UI.getCurrent()
				.navigate(NewAttributeTypeView.class));
		addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		addButton.setIcon(VaadinIcon.PLUS_CIRCLE_O.create());
		Button importButton = new Button(msg.getMessage("AttributeTypesView.import"), e -> UI.getCurrent()
				.navigate(ImportAttributeTypesView.class));
		importButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		importButton.setIcon(VaadinIcon.DOWNLOAD.create());
		headerLayout.add(importButton);
		headerLayout.add(addButton);
		headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		headerLayout.setAlignSelf(Alignment.END, importButton, addButton);
		return headerLayout;
	}

	private Checkbox generateCheckBox(boolean initialValue)
	{
		Checkbox checkbox = new Checkbox(initialValue);
		checkbox.setReadOnly(true);
		return checkbox;
	}

	private Collection<AttributeTypeEntry> getAttributeTypes()
	{
		try
		{
			return controller.getAttributeTypes();
		} catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getCause()
					.getMessage());
		}
		return Collections.emptyList();
	}

	private HorizontalLayout createNameWithDetailsArrow(AttributeTypeEntry entry)
	{
		Component label = entry.isEditable()
				? new RouterLink(entry.attributeType.getName(), EditAttributeTypeView.class,
						entry.attributeType.getName())
				: new NativeLabel(entry.attributeType.getName());

		return new HorizontalLayout(label);
	}

	private FormLayout getDetailsComponent(AttributeTypeEntry i)
	{
		FormLayout wrapper = new FormLayout();
		NativeLabel label = new NativeLabel(i.getDescription());
		label.setWidthFull();
		wrapper.addFormItem(label, msg.getMessage("AttributeTypesView.descriptionLabelCaption"));
		return wrapper;
	}
}
