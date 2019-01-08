/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.server.SerializablePredicate;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeClassHelper;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;

/**
 * Displays attributes and their values. 
 * Allows for adding/removing attributes.
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AttributesPanel extends HorizontalSplitPanel
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AttributesPanel.class);

	private UnityMessageSource msg;
	private AttributeHandlerRegistry registry;
	private AttributesManagement attributesManagement;
	private GroupsManagement groupsManagement;
	private AttributeTypeSupport atSupport;
	
	private VerticalLayout left;
	private CheckBox showEffective;
	private CheckBox showInternal;
	private SerializablePredicate<AttributeExt> internalAttrsFilter;
	private SerializablePredicate<AttributeExt> effectiveAttrsFilter;
	private HorizontalLayout filtersBar;
	private List<AttributeExt> attributes;
	private ValuesRendererPanel attributeValues;
	private GenericElementsTable<AttributeExt> attributesTable;
	private EntityParam owner;
	private String groupPath;
	private AttributeClassHelper acHelper;
	private Map<String, AttributeType> attributeTypes;
	private EventsBus bus;
	private AttributeTypeManagement aTypeManagement;
	private AttributeClassManagement acMan;

	@Autowired
	public AttributesPanel(UnityMessageSource msg, AttributeHandlerRegistry registry,
			AttributesManagement attributesManagement,
			GroupsManagement groupsManagement, AttributeTypeManagement atManagement,
			AttributeClassManagement acMan, AttributeTypeSupport atSupport)
	{
		this.msg = msg;
		this.registry = registry;
		this.attributesManagement = attributesManagement;
		this.groupsManagement = groupsManagement;
		this.aTypeManagement = atManagement;
		this.acMan = acMan;
		this.atSupport = atSupport;
		this.bus = WebSession.getCurrent().getEventBus();

		attributesTable = new GenericElementsTable<>(
				msg.getMessage("Attribute.attributes"),
				element -> element.getName(), true);
		attributesTable.setMultiSelect(true);
		attributesTable.setWidth(100, Unit.PERCENTAGE);
		attributesTable.setStyleGenerator(a -> {
			StringBuilder style = new StringBuilder();
			if (!a.isDirect())
				style.append(Styles.emphasized.toString());
			if (checkAttributeImmutable(a))
				style.append(" " + Styles.immutableAttribute.toString());
			if (acHelper.isMandatory(a.getName()))
				style.append(" " + Styles.bold.toString());
			return style.toString().trim();
		});
		attributesTable.addSelectionListener(event -> {
			Collection<AttributeExt> items = event.getAllSelectedItems();
			if (items.size() > 1 || items.isEmpty())
			{
				updateValues(null);
				return;
			}
			AttributeExt selected = items.iterator().next();
			if (selected != null)
				updateValues(selected);
			else
				updateValues(null);
		});

		attributesTable.addActionHandler(getAddAction());
		attributesTable.addActionHandler(getEditAction());
		attributesTable.addActionHandler(getDeleteAction());

		Toolbar<AttributeExt> toolbar = new Toolbar<>(Orientation.VERTICAL);
		attributesTable.addSelectionListener(toolbar.getSelectionListener());
		toolbar.addActionHandlers(attributesTable.getActionHandlers());
		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(attributesTable,
				toolbar);
		tableWithToolbar.setWidth(100, Unit.PERCENTAGE);
		tableWithToolbar.setHeight(100, Unit.PERCENTAGE);

		effectiveAttrsFilter = a -> a.isDirect();
		internalAttrsFilter = a -> !checkAttributeImmutable(a);

		showEffective = new CheckBox(msg.getMessage("Attribute.showEffective"), true);
		showEffective.addStyleName(Styles.emphasized.toString());
		showEffective.addValueChangeListener(event -> updateAttributesFilter(
				!showEffective.getValue(), effectiveAttrsFilter));

		showInternal = new CheckBox(msg.getMessage("Attribute.showInternal"), false);
		showInternal.addStyleName(Styles.immutableAttribute.toString());
		showInternal.addValueChangeListener(event -> updateAttributesFilter(
				!showInternal.getValue(), internalAttrsFilter));

		Label required = new Label(msg.getMessage("Attribute.requiredBold"));
		required.setStyleName(Styles.bold.toString());
		filtersBar = new HorizontalLayout(showEffective, showInternal, required);
		filtersBar.setComponentAlignment(showEffective, Alignment.MIDDLE_LEFT);
		filtersBar.setComponentAlignment(showInternal, Alignment.MIDDLE_LEFT);
		filtersBar.setComponentAlignment(required, Alignment.MIDDLE_RIGHT);
		filtersBar.setSizeUndefined();
		filtersBar.setMargin(false);

		attributeValues = new ValuesRendererPanel(msg, atSupport);
		attributeValues.setSizeFull();

		left = new VerticalLayout();
		left.setMargin(new MarginInfo(false, true, false, false));
		left.setSizeFull();
		left.addComponents(filtersBar, tableWithToolbar);
		left.setExpandRatio(tableWithToolbar, 1.0f);

		setFirstComponent(left);
		setSecondComponent(attributeValues);
		setSplitPosition(40, Unit.PERCENTAGE);

		updateAttributesFilter(!showEffective.getValue(), effectiveAttrsFilter);
		updateAttributesFilter(!showInternal.getValue(), internalAttrsFilter);
	}

	private void refreshAttributeTypes() throws EngineException
	{
		attributeTypes = aTypeManagement.getAttributeTypesAsMap();
	}

	public void setInput(EntityParam owner, String groupPath,
			Collection<AttributeExt> attributesCol) throws EngineException
	{
		this.owner = owner;
		this.attributes = new ArrayList<AttributeExt>(attributesCol.size());
		this.attributes.addAll(attributesCol);
		this.groupPath = groupPath;
		updateACHelper(owner, groupPath);
		updateAttributes();
	}

	private void updateACHelper(EntityParam owner, String groupPath) throws EngineException
	{
		Group group = groupsManagement.getContents(groupPath, GroupContents.METADATA)
				.getGroup();
		Collection<AttributesClass> acs = acMan.getEntityAttributeClasses(owner, groupPath);
		Map<String, AttributesClass> knownClasses = acMan.getAttributeClasses();
		Set<String> assignedClasses = new HashSet<String>(acs.size());
		for (AttributesClass ac : acs)
			assignedClasses.add(ac.getName());
		assignedClasses.addAll(group.getAttributesClasses());

		acHelper = new AttributeClassHelper(knownClasses, assignedClasses);
	}

	private void reloadAttributes() throws EngineException
	{
		Collection<AttributeExt> attributesCol = attributesManagement
				.getAllAttributes(owner, true, groupPath, null, true);
		this.attributes = new ArrayList<AttributeExt>(attributesCol.size());
		this.attributes.addAll(attributesCol);
	}

	private void updateAttributes() throws EngineException
	{
		refreshAttributeTypes();
		attributeValues.removeValues();
		attributesTable.setInput(attributes);
		attributesTable.deselectAll();
		// attributesTable.selectFirst();
	}

	private void updateValues(AttributeExt attribute)
	{
		try
		{
			if (attribute == null)
			{
				attributeValues.removeValues();
				return;
			}
			AttributeValueSyntax<?> syntax = atSupport.getSyntax(attribute);
			WebAttributeHandler handler = registry.getHandler(syntax);
			attributeValues.setValues(handler, attribute);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"Attribute.addAttributeError", attribute.getName()), e);
		}
	}

	private void updateAttributesFilter(boolean add, SerializablePredicate<AttributeExt> filter)
	{
		if (add)
			attributesTable.addFilter(filter);
		else
			attributesTable.removeFilter(filter);
	}

	private void removeAttribute(AttributeExt toRemove)
	{
		try
		{
			attributesManagement.removeAttribute(owner, toRemove.getGroupPath(),
					toRemove.getName());
			reloadAttributes();
			updateAttributes();
			bus.fireEvent(new AttributeChangedEvent(toRemove.getGroupPath(),
					toRemove.getName()));
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"Attribute.removeAttributeError", toRemove.getName()), e);
		}
	}

	private boolean addAttribute(Attribute attribute)
	{
		try
		{
			attributesManagement.createAttributeSuppressingConfirmation(owner, attribute);
			reloadAttributes();
			updateAttributes();
			bus.fireEvent(new AttributeChangedEvent(attribute.getGroupPath(),
					attribute.getName()));
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"Attribute.addAttributeError", attribute.getName()), e);
			return false;
		}
	}

	private boolean updateAttribute(Attribute attribute)
	{
		try
		{
			attributesManagement.setAttributeSuppressingConfirmation(owner, attribute);
			for (int i = 0; i < attributes.size(); i++)
			{
				if (attributes.get(i).getName().equals(attribute.getName()))
				{
					attributes.set(i, new AttributeExt(attribute, true));
				}

			}
			bus.fireEvent(new AttributeChangedEvent(attribute.getGroupPath(),
					attribute.getName()));
			reloadAttributes();
			updateAttributes();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"Attribute.addAttributeError", attribute.getName()), e);
			return false;
		}
	}

	private boolean checkAttributeImmutable(AttributeExt attribute)
	{
		AttributeType attributeType = attributeTypes.get(attribute.getName());
		if (attributeType == null)
		{
			log.error("Attribute type is not in the map: " + attribute.getName());
			return false;
		}
		return attributeType.isInstanceImmutable();
	}
	
	private boolean isAttributeEditable(AttributeExt item)
	{
		return checkAttributeImmutable(item) || !item.isDirect();
	}

	private boolean checkAttributeMandatory(AttributeExt item)
	{
		return acHelper.isMandatory(item.getName());

	}

	private SingleActionHandler<AttributeExt> getDeleteAction()
	{
		return SingleActionHandler.builder4Delete(msg, AttributeExt.class)
				.withDisabledPredicate(a -> isAttributeEditable(a)
						|| checkAttributeMandatory(a))
				.withHandler(this::deleteHandler).build();
	}

	private void deleteHandler(Collection<AttributeExt> items)
	{
		String confirmText = MessageUtils.createConfirmFromNames(msg, items);

		ConfirmDialog confirm = new ConfirmDialog(msg,
				msg.getMessage("Attribute.removeConfirm", confirmText),
				() -> items.forEach(this::removeAttribute));
		confirm.show();
	}

	private SingleActionHandler<AttributeExt> getAddAction()
	{
		return SingleActionHandler.builder4Add(msg, AttributeExt.class)
				.withHandler(this::showAddDialog).build();
	}

	private void showAddDialog(Collection<AttributeExt> target)
	{
		List<AttributeType> allowed = new ArrayList<>(attributeTypes.size());
		for (AttributeType at : attributeTypes.values())
		{
			if (at.isInstanceImmutable())
				continue;
			if (acHelper.isAllowed(at.getName()))
			{
				boolean used = false;
				for (AttributeExt a : attributes)
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

		AttributeEditor attributeEditor = new AttributeEditor(msg, allowed, owner, groupPath,
				registry, true);
		AttributeEditDialog dialog = new AttributeEditDialog(msg,
				msg.getMessage("Attribute.addAttribute"), a -> addAttribute(a),
				attributeEditor);
		dialog.show();
	}

	private SingleActionHandler<AttributeExt> getEditAction()
	{
		return SingleActionHandler.builder4Edit(msg, AttributeExt.class)
				.withDisabledPredicate(a -> isAttributeEditable(a))
				.withHandler(this::showEditDialog).build();

	}

	private void showEditDialog(Collection<AttributeExt> target)
	{
		final Attribute attribute = target.iterator().next();
		AttributeType attributeType = attributeTypes.get(attribute.getName());
		AttributeEditor attributeEditor = new AttributeEditor(msg, attributeType, attribute, owner,
				registry);
		AttributeEditDialog dialog = new AttributeEditDialog(msg,
				msg.getMessage("Attribute.editAttribute"), a -> updateAttribute(a),
				attributeEditor);
		dialog.show();
	}
}
