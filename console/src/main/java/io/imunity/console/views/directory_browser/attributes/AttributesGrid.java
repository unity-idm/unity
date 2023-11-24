/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_browser.attributes;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.function.SerializablePredicate;
import io.imunity.console.attribute.AttributeEditDialog;
import io.imunity.console.attribute.AttributeEditor;
import io.imunity.console.views.directory_browser.EntityWithLabel;
import io.imunity.vaadin.elements.ColumnToggleMenu;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.ActionMenuWithHandlerSupport;
import io.imunity.vaadin.endpoint.common.ComponentWithToolbar;
import io.imunity.vaadin.endpoint.common.Toolbar;
import io.imunity.vaadin.endpoint.common.WebSession;
import io.imunity.vaadin.endpoint.common.bus.EventsBus;
import io.imunity.vaadin.endpoint.common.grid.GridWithActionColumn;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.SingleActionHandler;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeClassHelper;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.imunity.console.tprofile.Constants.FORM_PROFILE;
import static io.imunity.vaadin.elements.CSSVars.SMALL_MARGIN;
import static io.imunity.vaadin.elements.CssClassNames.*;
import static pl.edu.icm.unity.engine.api.utils.TimeUtil.formatStandardInstant;


@PrototypeComponent
public class AttributesGrid extends VerticalLayout
{
	private final MessageSource msg;
	private final AttributesController controller;
	private final AttributeHandlerRegistry registry;
	private final NotificationPresenter notificationPresenter;
	private final GridWithActionColumn<AttributeExt> attributesGrid;
	private final SerializablePredicate<AttributeExt> internalAttrsFilter;
	private final SerializablePredicate<AttributeExt> effectiveAttrsFilter;
	private final EventsBus bus;

	private AttributeClassHelper acHelper;
	private Map<String, AttributeType> attributeTypes;
	private EntityWithLabel owner;
	private String groupPath;

	AttributesGrid(MessageSource msg, AttributesController controller, AttributeHandlerRegistry registry,
			NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.controller = controller;
		this.registry = registry;
		this.notificationPresenter = notificationPresenter;
		this.bus = WebSession.getCurrent().getEventBus();

		attributesGrid = new GridWithActionColumn<>(msg, Collections.emptyList());

		attributesGrid.addShowDetailsColumn(new ComponentRenderer<>(controller::getDetailsComponent));

		Grid.Column<AttributeExt> nameColumn = attributesGrid.addColumn(Attribute::getName)
				.setHeader(msg.getMessage("AttributesGrid.nameCaption"))
				.setSortable(true)
				.setAutoWidth(true)
				.setResizable(true);

		attributesGrid.addComponentColumn(controller::getShortAttrValuesRepresentation)
				.setHeader(msg.getMessage("AttributesGrid.valueCaption"))
				.setSortable(true)
				.setAutoWidth(true)
				.setResizable(true);

		ColumnToggleMenu columnToggleMenu = new ColumnToggleMenu();

		Grid.Column<AttributeExt> createdOnColumn = attributesGrid.addColumn(a -> a.getCreationTs() != null ?
						formatStandardInstant(a.getCreationTs().toInstant()) : "")
				.setHeader(msg.getMessage("AttributesGrid.createCaption"))
				.setResizable(true)
				.setSortable(true)
				.setAutoWidth(true);
		createdOnColumn.setVisible(false);
		columnToggleMenu.addColumn(msg.getMessage("AttributesGrid.createCaption"), createdOnColumn);

		Grid.Column<AttributeExt> updatedOnColumn = attributesGrid.addColumn(a -> a.getUpdateTs() != null ?
						formatStandardInstant(a.getUpdateTs().toInstant()) : "")
				.setHeader(msg.getMessage("AttributesGrid.updateCaption"))
				.setResizable(true)
				.setSortable(true)
				.setAutoWidth(true);
		updatedOnColumn.setVisible(false);
		columnToggleMenu.addColumn(msg.getMessage("AttributesGrid.updateCaption"), updatedOnColumn);

		Grid.Column<AttributeExt> sourceColumn = attributesGrid.addColumn(a -> getSourceName(msg, a))
				.setHeader(msg.getMessage("AttributesGrid.sourceCaption"))
				.setResizable(true)
				.setSortable(true)
				.setAutoWidth(true);
		sourceColumn.setVisible(false);
		columnToggleMenu.addColumn(msg.getMessage("AttributesGrid.sourceCaption"), sourceColumn);

		attributesGrid.setClassNameGenerator(a ->
		{
			StringBuilder style = new StringBuilder();
			if (checkAttributeImmutable(a) || !a.isDirect())
				style.append(" ").append(IMMUTABLE_ATTRIBUTE.getName());
			if (acHelper.isMandatory(a.getName()))
				style.append(" ").append(BOLD.getName());
			return style.toString().trim();
		});
		attributesGrid.sort(GridSortOrder.asc(nameColumn).build());

		attributesGrid.addHamburgerActions(getRowActionsHandlers());
		attributesGrid.setActionColumnHeader(getActions(columnToggleMenu));

		attributesGrid.setMultiSelect(true);
		attributesGrid.setSizeFull();

		ActionMenuWithHandlerSupport<AttributeExt> hamburgerMenu = new ActionMenuWithHandlerSupport<>();
		hamburgerMenu.addActionHandlers(getGlobalHamburgerHandlers());

		attributesGrid.addSelectionListener(hamburgerMenu.getSelectionListener());

		Toolbar<AttributeExt> toolbar = new Toolbar<>();
		toolbar.setWidthFull();
		toolbar.addHamburger(hamburgerMenu, Alignment.END);
		Button button = new Button(msg.getMessage("add"), VaadinIcon.PLUS_CIRCLE_O.create());
		button.addClickListener(e -> showAddDialog());
		button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		toolbar.add(button);
		ComponentWithToolbar componentWithToolbar = new ComponentWithToolbar(attributesGrid, toolbar);
		componentWithToolbar.setSizeFull();
		componentWithToolbar.setClassName(SMALL_GAP.getName());
		add(componentWithToolbar);
		setSizeFull();
		setPadding(false);

		effectiveAttrsFilter = AttributeExt::isDirect;
		internalAttrsFilter = a -> !checkAttributeImmutable(a);

		hamburgerMenu.add(new Hr());

		MenuItem showEffective = hamburgerMenu.addItem(msg.getMessage("Attribute.showEffective"),
				c -> updateAttributesFilter(!c.getSource().isChecked(), effectiveAttrsFilter));
		showEffective.setCheckable(true);
		showEffective.setChecked(true);

		MenuItem showInternal = hamburgerMenu.addItem(msg.getMessage("Attribute.showInternal"),
				c -> updateAttributesFilter(!c.getSource().isChecked(), internalAttrsFilter));
		showInternal.setCheckable(true);
		showInternal.setChecked(false);

		updateAttributesFilter(false, effectiveAttrsFilter);
		updateAttributesFilter(true, internalAttrsFilter);
	}

	private String getSourceName(MessageSource msg, AttributeExt a)
	{
		if (a.getTranslationProfile() != null)
		{
			if(a.getTranslationProfile().equals(FORM_PROFILE))
				return msg.getMessage("Attribute.formAutomation");
			return a.getTranslationProfile() + (a.getRemoteIdp() != null ? " " + a.getRemoteIdp() : "");
		}
		else if (a.isDirect())
			return msg.getMessage("Attribute.direct");
		 else
			return msg.getMessage("Attribute.fromStatement");
	}

	private HorizontalLayout getActions(ColumnToggleMenu columnToggleMenu)
	{
		Span actions = new Span(msg.getMessage("actions"));
		HorizontalLayout horizontalLayout = new HorizontalLayout(actions, columnToggleMenu.getTarget());
		horizontalLayout.setSpacing(false);
		horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		actions.getStyle().set("margin-right", SMALL_MARGIN.value());
		return horizontalLayout;
	}

	private void updateAttributesFilter(boolean add, SerializablePredicate<AttributeExt> filter)
	{
		if (add)
			attributesGrid.addFilter(filter);
		else
			attributesGrid.removeFilter(filter);
	}

	private boolean checkAttributeImmutable(AttributeExt attribute)
	{
		AttributeType attributeType = attributeTypes.get(attribute.getName());
		if (attributeType == null)
			return false;
		return attributeType.isInstanceImmutable();
	}

	private List<SingleActionHandler<AttributeExt>> getRowActionsHandlers()
	{
		SingleActionHandler<AttributeExt> edit = SingleActionHandler.builder4Edit(msg, AttributeExt.class)
				.withDisabledPredicate(this::isAttributeEditable).withHandler(this::showEditDialog)
				.build();
		return Arrays.asList(edit, getDeleteAction());
	}

	private void showEditDialog(Collection<AttributeExt> target)
	{
		Attribute attribute = target.iterator().next();
		AttributeType attributeType = attributeTypes.get(attribute.getName());
		AttributeEditor attributeEditor = new AttributeEditor(msg, attributeType, attribute, getEntityParam(),
				registry);
		AttributeEditDialog dialog = new AttributeEditDialog(msg, msg.getMessage("Attribute.editAttribute"),
				this::updateAttribute, attributeEditor, notificationPresenter);
		dialog.addConfirmListener(e -> refresh());
		dialog.open();
	}

	private List<SingleActionHandler<AttributeExt>> getGlobalHamburgerHandlers()
	{
		return Collections.singletonList(getDeleteAction());
	}

	private void showAddDialog()
	{
		List<AttributeType> allowed = new ArrayList<>(attributeTypes.size());
		for (AttributeType at : attributeTypes.values())
		{
			if (at.isInstanceImmutable())
				continue;
			if (acHelper.isAllowed(at.getName()))
			{
				boolean used = false;
				for (AttributeExt a : attributesGrid.getElements())
					if (a.isDirect() && a.getName().equals(at.getName()))
					{
						used = true;
						break;
					}
				if (!used)
					allowed.add(at);
			}
		}

		if (allowed.isEmpty())
		{
			notificationPresenter.showNotice(msg.getMessage("notice"),
					msg.getMessage("Attribute.noAvailableAttributes"));
			return;
		}

		AttributeEditor attributeEditor = new AttributeEditor(msg, allowed, getEntityParam(), groupPath,
				registry, true);
		AttributeEditDialog dialog = new AttributeEditDialog(msg, msg.getMessage("Attribute.addAttribute"),
				this::addAttribute, attributeEditor, notificationPresenter);
		dialog.open();
	}

	private boolean updateAttribute(Attribute attribute)
	{
		controller.updateAttribute(getEntityParam(), attribute, bus);

		attributesGrid.getElements().stream()
				.filter(a -> a.getName().equals(attribute.getName())).findFirst().ifPresent(
						toReplace -> attributesGrid.replaceElement(toReplace, new AttributeExt(attribute, true)));

		return true;
	}

	private boolean addAttribute(Attribute attribute)
	{
		controller.addAttribute(getEntityParam(), attribute, bus);

		refresh();
		return true;
	}

	private SingleActionHandler<AttributeExt> getDeleteAction()
	{
		return SingleActionHandler.builder4Delete(msg, AttributeExt.class)
				.withDisabledPredicate(a -> isAttributeEditable(a) || checkAttributeMandatory(a))
				.withHandler(this::deleteHandler).build();
	}

	private void deleteHandler(Collection<AttributeExt> items)
	{
		String confirmText = MessageUtils.createConfirmFromNames(msg, items);

		new ConfirmDialog(
				msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("Attribute.removeConfirm", confirmText),
				msg.getMessage("ok"),
				e -> removeAttributes(items),
				msg.getMessage("cancel"),
				e ->
				{
				}
		).open();
	}

	private void removeAttributes(Collection<AttributeExt> items)
	{
		controller.removeAttribute(getEntityParam(), items, bus);
		items.forEach(attributesGrid::removeElement);

		refresh();
	}

	private void refresh()
	{
		Collection<AttributeExt> attributesCol = controller.getAttributes(owner, groupPath);
		acHelper = controller.getAttributeClassHelper(getEntityParam(), groupPath);
		attributeTypes = controller.getAttributeTypes();
		attributesGrid.setItems(attributesCol);
	}

	EntityParam getEntityParam()
	{
		return new EntityParam(owner.getEntity().getId());
	}

	private boolean isAttributeEditable(AttributeExt item)
	{
		return checkAttributeImmutable(item) || !item.isDirect();
	}

	private boolean checkAttributeMandatory(AttributeExt item)
	{
		return acHelper.isMandatory(item.getName());

	}

	public void setInput(EntityWithLabel owner, String groupPath)
	{
		this.owner = owner;
		this.groupPath = groupPath;
		refresh();
	}

}
