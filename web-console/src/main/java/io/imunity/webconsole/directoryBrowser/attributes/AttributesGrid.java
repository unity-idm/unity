/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.directoryBrowser.attributes;

import static pl.edu.icm.unity.engine.api.utils.TimeUtil.formatStandardInstant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.server.SerializablePredicate;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar.MenuItem;

import io.imunity.webconsole.attribute.AttributeEditDialog;
import io.imunity.webconsole.attribute.AttributeEditor;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeClassHelper;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistryV8;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Shows attributes grid
 * 
 * @author P.Piernik
 *
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AttributesGrid extends CustomComponent
{
	private MessageSource msg;
	private AttributesController controller;
	private AttributeHandlerRegistryV8 registry;

	private GridWithActionColumn<AttributeExt> attributesGrid;

	private SerializablePredicate<AttributeExt> internalAttrsFilter;
	private SerializablePredicate<AttributeExt> effectiveAttrsFilter;

	private AttributeClassHelper acHelper;
	private Map<String, AttributeType> attributeTypes;
	private EntityWithLabel owner;
	private String groupPath;
	private EventsBus bus;

	@Autowired
	AttributesGrid(MessageSource msg, AttributesController controller, AttributeHandlerRegistryV8 registry)
	{
		this.msg = msg;
		this.controller = controller;
		this.registry = registry;

		this.bus = WebSession.getCurrent().getEventBus();

		attributesGrid = new GridWithActionColumn<>(msg, Collections.emptyList(), false, false);

		attributesGrid.addShowDetailsColumn(a -> getDetailsComponent(a));

		attributesGrid.addSortableColumn(a -> a.getName(), msg.getMessage("AttributesGrid.nameCaption"), 10)
				.setResizable(true).setId("name");

		attributesGrid.addComponentColumn(a -> getShortValue(a), msg.getMessage("AttributesGrid.valueCaption"),
				80).setResizable(true);

		attributesGrid.addSortableColumn(a -> a.getCreationTs() != null ? 
					formatStandardInstant(a.getCreationTs().toInstant()) : "",
				msg.getMessage("AttributesGrid.createCaption"), 5).setResizable(true).setHidable(true)
				.setHidden(true);

		attributesGrid.addSortableColumn(a -> a.getUpdateTs() != null ? 
					formatStandardInstant(a.getUpdateTs().toInstant()) : "",
				msg.getMessage("AttributesGrid.updateCaption"), 5).setResizable(true).setHidable(true)
				.setHidden(true);

		attributesGrid.addSortableColumn(a -> 
		{
			if (a.getTranslationProfile() != null)
			{
				return a.getTranslationProfile() + (a.getRemoteIdp() != null ? " " + a.getRemoteIdp() : "");
			} else if (a.isDirect())
			{
				return msg.getMessage("Attribute.direct");
			} else
			{
				return msg.getMessage("Attribute.fromStatement");
			}
		}, msg.getMessage("AttributesGrid.sourceCaption"), 5).setResizable(true).setHidable(true);

		attributesGrid.setStyleGenerator(a -> {
			StringBuilder style = new StringBuilder();
			if (checkAttributeImmutable(a) || !a.isDirect())
				style.append(" " + Styles.immutableAttribute.toString());
			if (acHelper.isMandatory(a.getName()))
				style.append(" " + Styles.bold.toString());
			return style.toString().trim();
		});

		attributesGrid.sort("name");

		attributesGrid.addHamburgerActions(getRowActionsHandlers());
		attributesGrid.setMultiSelect(true);
		attributesGrid.setSizeFull();

		HamburgerMenu<AttributeExt> hamburgerMenu = new HamburgerMenu<>();
		hamburgerMenu.addActionHandlers(getGlobalHamburgerHandlers());

		attributesGrid.addSelectionListener(hamburgerMenu.getSelectionListener());

		Toolbar<AttributeExt> toolbar = new Toolbar<>(Orientation.HORIZONTAL);
		toolbar.setWidth(100, Unit.PERCENTAGE);
		toolbar.addHamburger(hamburgerMenu);
		toolbar.addActionButton(StandardButtonsHelper.buildActionButton(
						msg.getMessage("add"),
						Images.add,
						e -> showAddDialog()), 
				Alignment.MIDDLE_RIGHT);
		ComponentWithToolbar componentWithToolbar = new ComponentWithToolbar(attributesGrid, toolbar,
				Alignment.BOTTOM_LEFT);
		componentWithToolbar.setSizeFull();
		setCompositionRoot(componentWithToolbar);
		setSizeFull();

		effectiveAttrsFilter = a -> a.isDirect();
		internalAttrsFilter = a -> !checkAttributeImmutable(a);

		hamburgerMenu.addSeparator();

		MenuItem showEffective = hamburgerMenu.addItem(msg.getMessage("Attribute.showEffective"), null,
				c -> updateAttributesFilter(!c.isChecked(), effectiveAttrsFilter));
		showEffective.setCheckable(true);
		showEffective.setChecked(true);

		MenuItem showInternal = hamburgerMenu.addItem(msg.getMessage("Attribute.showInternal"), null,
				c -> updateAttributesFilter(!c.isChecked(), internalAttrsFilter));
		showInternal.setCheckable(true);
		showInternal.setChecked(false);

		updateAttributesFilter(false, effectiveAttrsFilter);
		updateAttributesFilter(true, internalAttrsFilter);
	}

	private com.vaadin.ui.Component getDetailsComponent(AttributeExt a)
	{
		try
		{
			return controller.getDetailsComponent(a);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			return new Label();
		}
	}

	private com.vaadin.ui.Component getShortValue(AttributeExt a)
	{
		try
		{
			return controller.getShortAttrValuesRepresentation(a);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			return new Label();
		}

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
				.withDisabledPredicate(a -> isAttributeEditable(a)).withHandler(this::showEditDialog)
				.build();
		return Arrays.asList(edit, getDeleteAction());
	}

	private void showEditDialog(Collection<AttributeExt> target)
	{
		final Attribute attribute = target.iterator().next();
		AttributeType attributeType = attributeTypes.get(attribute.getName());
		AttributeEditor attributeEditor = new AttributeEditor(msg, attributeType, attribute, getEntityParam(),
				registry);
		AttributeEditDialog dialog = new AttributeEditDialog(msg, msg.getMessage("Attribute.editAttribute"),
				a -> updateAttribute(a), attributeEditor);
		dialog.show();
	}

	private List<SingleActionHandler<AttributeExt>> getGlobalHamburgerHandlers()
	{
		return Arrays.asList(getDeleteAction());
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
			NotificationPopup.showNotice(msg.getMessage("notice"),
					msg.getMessage("Attribute.noAvailableAttributes"));
			return;
		}

		AttributeEditor attributeEditor = new AttributeEditor(msg, allowed, getEntityParam(), groupPath,
				registry, true);
		AttributeEditDialog dialog = new AttributeEditDialog(msg, msg.getMessage("Attribute.addAttribute"),
				a -> addAttribute(a), attributeEditor);
		dialog.show();
	}

	private boolean updateAttribute(Attribute attribute)
	{
		try
		{
			controller.updateAttribute(getEntityParam(), attribute, bus);

		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			return false;
		}

		AttributeExt toReplace = attributesGrid.getElements().stream()
				.filter(a -> a.getName().equals(attribute.getName())).findFirst().orElse(null);
		if (toReplace != null)
		{
			attributesGrid.replaceElement(toReplace, new AttributeExt(attribute, true));
		}

		return true;
	}

	private void refreshSave()
	{
		try
		{
			refresh();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
	}

	private boolean addAttribute(Attribute attribute)
	{
		try
		{
			controller.addAttribute(getEntityParam(), attribute, bus);

		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			return false;
		}

		refreshSave();
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

		ConfirmDialog confirm = new ConfirmDialog(msg, msg.getMessage("Attribute.removeConfirm", confirmText),
				() -> removeAttributes(items));
		confirm.show();
	}

	private void removeAttributes(Collection<AttributeExt> items)
	{
		try
		{
			controller.removeAttribute(getEntityParam(), items, bus);
			items.forEach(a -> attributesGrid.removeElement(a));
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}

		try
		{
			refresh();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}

	}

	private void refresh() throws ControllerException
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

	public void setInput(EntityWithLabel owner, String groupPath) throws ControllerException
	{
		this.owner = owner;
		this.groupPath = groupPath;
		refresh();
	}

}
