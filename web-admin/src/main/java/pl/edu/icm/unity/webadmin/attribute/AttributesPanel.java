/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.data.Container;
import com.vaadin.data.Container.Filterable;
import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.Action;
import com.vaadin.server.Resource;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.confirmations.ConfirmationManager;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.attributes.AttributeClassHelper;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.confirmation.VerifiableElement;
import pl.edu.icm.unity.webadmin.utils.MessageUtils;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ConfirmDialog.Callback;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.SmallTable;
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
	private ConfirmationManager confirmationMan;
	
	private VerticalLayout left;
	private CheckBox showEffective;
	private CheckBox showInternal;
	private InternalAttributesFilter internalAttrsFilter;
	private EffectiveAttributesFilter effectiveAttrsFilter;
	private HorizontalLayout filtersBar;
	private List<AttributeExt<?>> attributes;
	private ValuesRendererPanel attributeValues;
	private Table attributesTable;
	private EntityParam owner;
	private String groupPath;
	private AttributeClassHelper acHelper;
	private Map<String, AttributeType> attributeTypes;
	private EventsBus bus;
	
	@Autowired
	public AttributesPanel(UnityMessageSource msg, AttributeHandlerRegistry registry, 
			AttributesManagement attributesManagement, GroupsManagement groupsManagement, ConfirmationManager confirmationMan)
	{
		this.msg = msg;
		this.registry = registry;
		this.attributesManagement = attributesManagement;
		this.groupsManagement = groupsManagement;
		this.confirmationMan = confirmationMan;
		this.bus = WebSession.getCurrent().getEventBus();
		attributesTable = new SmallTable();
		attributesTable.setNullSelectionAllowed(false);
		attributesTable.setImmediate(true);
		BeanItemContainer<AttributeItem> tableContainer = new BeanItemContainer<AttributeItem>(AttributeItem.class);
		attributesTable.setSelectable(true);
		attributesTable.setMultiSelect(true);
		attributesTable.setContainerDataSource(tableContainer);
		attributesTable.setColumnHeaders(new String[] {msg.getMessage("Attribute.attributes")});
		
		attributesTable.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				Collection<AttributeItem> items = getItems(attributesTable.getValue()); 
				if (items.size() > 1 || items.isEmpty())
				{
					updateValues(null);
					return;
				}
				AttributeItem selected = items.iterator().next();
				if (selected != null)
					updateValues(selected.getAttribute());
				else
					updateValues(null);
			}
		});

		Toolbar toolbar = new Toolbar(attributesTable, Orientation.VERTICAL);
		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(attributesTable, toolbar);
		tableWithToolbar.setSizeFull();
		SingleActionHandler[] handlers = new SingleActionHandler[] {new AddAttributeActionHandler(), 
				new EditAttributeActionHandler(), new RemoveAttributeActionHandler(), new ResendConfirmationAttributeActionHandler()};
		for (SingleActionHandler handler: handlers)
			attributesTable.addActionHandler(handler);
		toolbar.addActionHandlers(handlers);
		
		internalAttrsFilter = new InternalAttributesFilter();
		effectiveAttrsFilter = new EffectiveAttributesFilter();
		showEffective = new CheckBox(msg.getMessage("Attribute.showEffective"), true);
		showEffective.setImmediate(true);
		showEffective.addStyleName(Styles.emphasized.toString());
		showEffective.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				updateAttributesFilter(!showEffective.getValue(), effectiveAttrsFilter);
			}
		});
		showInternal = new CheckBox(msg.getMessage("Attribute.showInternal"), false);
		showInternal.setImmediate(true);
		showInternal.addStyleName(Styles.immutableAttribute.toString());
		showInternal.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				updateAttributesFilter(!showInternal.getValue(), internalAttrsFilter);
			}
		});
		Label required = new Label(msg.getMessage("Attribute.requiredBold"));
		required.setStyleName(Styles.bold.toString());
		filtersBar = new HorizontalLayout(showEffective, showInternal, required);
		filtersBar.setComponentAlignment(showEffective, Alignment.MIDDLE_LEFT);
		filtersBar.setComponentAlignment(showInternal, Alignment.MIDDLE_LEFT);
		filtersBar.setComponentAlignment(required, Alignment.MIDDLE_RIGHT);
		filtersBar.setSpacing(true);
		filtersBar.setSizeUndefined();
		
		attributeValues = new ValuesRendererPanel(msg);
		attributeValues.setSizeFull();

		left = new VerticalLayout();
		left.setMargin(new MarginInfo(false, true, false, false));
		left.setSizeFull();
		left.setSpacing(true);
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
		attributeTypes = attributesManagement.getAttributeTypesAsMap();
	}
	
	public void setInput(EntityParam owner, String groupPath, Collection<AttributeExt<?>> attributesCol) 
			throws EngineException
	{
		this.owner = owner;
		this.attributes = new ArrayList<AttributeExt<?>>(attributesCol.size());
		this.attributes.addAll(attributesCol);
		this.groupPath = groupPath;
		updateACHelper(owner, groupPath);
		updateAttributes();
	}
	
	private void updateACHelper(EntityParam owner, String groupPath) throws EngineException
	{
		Group group = groupsManagement.getContents(groupPath, GroupContents.METADATA).getGroup();
		Collection<AttributesClass> acs = attributesManagement.getEntityAttributeClasses(owner, groupPath);
		Map<String, AttributesClass> knownClasses = attributesManagement.getAttributeClasses();
		Set<String> assignedClasses = new HashSet<String>(acs.size());
		for (AttributesClass ac: acs)
			assignedClasses.add(ac.getName());
		assignedClasses.addAll(group.getAttributesClasses());
		
		acHelper = new AttributeClassHelper(knownClasses, assignedClasses);
	}
	
	private void reloadAttributes() throws EngineException
	{
		Collection<AttributeExt<?>> attributesCol = attributesManagement.getAllAttributes(
				owner, true, groupPath, null, true);
		this.attributes = new ArrayList<AttributeExt<?>>(attributesCol.size());
		this.attributes.addAll(attributesCol);
	}
	
	private void updateAttributes() throws EngineException
	{
		refreshAttributeTypes();
		attributesTable.removeAllItems();
		attributeValues.removeValues();
		if (attributes.size() == 0)
			return;
		for (AttributeExt<?> attribute: attributes)
			attributesTable.addItem(new AttributeItem(attribute));

		attributesTable.select(attributes.get(0));
	}
	
	private void updateValues(AttributeExt<?> attribute)
	{
		if (attribute == null)
		{
			attributeValues.removeValues();
			return;
		}
		AttributeValueSyntax<?> syntax = attribute.getAttributeSyntax();
		WebAttributeHandler<?> handler = registry.getHandler(syntax.getValueSyntaxId());
		attributeValues.setValues(handler, attribute);
	}
	
	private void updateAttributesFilter(boolean add, Container.Filter filter)
	{
		Container.Filterable filterable = (Filterable) attributesTable.getContainerDataSource();
		if (!add)
			filterable.removeContainerFilter(filter);
		else
			filterable.addContainerFilter(filter);
	}
	
	public class AttributeItem
	{
		private AttributeExt<?> attribute;

		public AttributeItem(AttributeExt<?> value)
		{
			this.attribute = value;
		}
		
		public Label getName()
		{
			Label l = new Label(attribute.getName());
			AttributeType attributeType = attributeTypes.get(attribute.getName());
			StringBuilder style = new StringBuilder();
			if (!attribute.isDirect())
				style.append(Styles.emphasized.toString());
			if (attributeType.isInstanceImmutable())
				style.append(" " + Styles.immutableAttribute.toString());
			if (acHelper.isMandatory(attribute.getName()))
				style.append(" " + Styles.bold.toString());
			String styleS = style.toString().trim(); 
			if (styleS.length() > 0)
				l.setStyleName(styleS);
			return l;
		}
		
		private AttributeExt<?> getAttribute()
		{
			return attribute;
		}
		
		public String toString()
		{
			return attribute.getName();
		}
	}
	
	private void removeAttribute(AttributeItem attributeItem)
	{
		Attribute<?> toRemove = attributeItem.getAttribute();
		try
		{
			attributesManagement.removeAttribute(owner, toRemove.getGroupPath(), toRemove.getName());
			reloadAttributes();
			updateAttributes();
			bus.fireEvent(new AttributeChangedEvent(toRemove.getGroupPath(), toRemove.getName()));
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("Attribute.removeAttributeError", toRemove.getName()), e);
		}
	}
	
	private boolean addAttribute(Attribute<?> attribute)
	{
		try
		{
			attributesManagement.setAttribute(owner, attribute, false);
			reloadAttributes();
			updateAttributes();
			bus.fireEvent(new AttributeChangedEvent(attribute.getGroupPath(), attribute.getName()));
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("Attribute.addAttributeError", attribute.getName()), e);
			return false;
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private boolean updateAttribute(Attribute<?> attribute)
	{
		try
		{
			attributesManagement.setAttribute(owner, attribute, true);
			for (int i=0; i<attributes.size(); i++)
			{
				if (attributes.get(i).getName().equals(attribute.getName()))
				{
					attributes.set(i, new AttributeExt(attribute, true));
				}
					
			}
			bus.fireEvent(new AttributeChangedEvent(attribute.getGroupPath(), attribute.getName()));
			reloadAttributes();
			updateAttributes();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("Attribute.addAttributeError", attribute.getName()), e);
			return false;
		}
	}
	
	private boolean checkAttributeImm(AttributeItem item)
	{
		AttributeExt<?> attribute = ((AttributeItem) item)
				.getAttribute();
		AttributeType attributeType = attributeTypes.get(attribute
				.getName());
		return attributeType.isInstanceImmutable()
				|| !attribute.isDirect();
	}
	
	private boolean checkAttributeMandatory(AttributeItem item)
	{
		AttributeExt<?> attribute = (item)
				.getAttribute();
		return acHelper.isMandatory(attribute.getName());
			
	}
	
	private boolean checkAttributeIsVerifiable(AttributeItem item)
	{
		return item.getAttribute().getAttributeSyntax().isVerifiable();
	}
	
	private boolean checkAttributeIsConfirmed(AttributeItem item)
	{	
		for (Object valA : item.getAttribute().getValues())
		{
			VerifiableElement val = (VerifiableElement) valA;
			ConfirmationInfo ci = val.getConfirmationInfo();
			if (!ci.isConfirmed())
				return false;
		}
		return true;	
	}
		
	private Collection<AttributeItem> getItems(Object target)
	{
		Collection<?> c = (Collection<?>) target;
		Collection<AttributeItem> items = new ArrayList<AttributeItem>();
		for (Object o: c)
		{
			AttributeItem at = (AttributeItem) o;
			items.add(at);	
		}
		return items;
	}
	
	/**
	 * Extends {@link SingleActionHandler}. Returns action only for selections on an attribute. 
	 * @author K. Benedyczak
	 */
	private abstract class AbstractAttributeActionHandler extends SingleActionHandler
	{

		public AbstractAttributeActionHandler(String caption, Resource icon)
		{
			super(caption, icon);
		}
		
		@Override
		public Action[] getActions(Object target, Object sender)
		{
			if (target == null)
				return EMPTY;
			
			if (target instanceof Collection<?>)
			{		
				for (AttributeItem item : getItems(target))
				{
					if (checkAttributeImm((AttributeItem) item))
						return EMPTY;
				}
			} else
			{
				if (checkAttributeImm((AttributeItem) target))
					return EMPTY;
			}
			return super.getActions(target, sender);
		}
	}
		
	private class RemoveAttributeActionHandler extends AbstractAttributeActionHandler
	{
		public RemoveAttributeActionHandler()
		{
			super(msg.getMessage("Attribute.removeAttribute"), 
					Images.delete.getResource());
			setMultiTarget(true);
		}
		
		@Override
		public Action[] getActions(Object target, Object sender)
		{
			Action[] ret = super.getActions(target, sender);
			if (ret.length > 0)
			{
				if (target instanceof Collection<?>)
				{
					for (AttributeItem item : getItems(target))
					{
						if (checkAttributeMandatory((AttributeItem) item))
							return EMPTY;
					}
				} else
				{
					if (checkAttributeMandatory((AttributeItem) target))
						return EMPTY;
				}
			}
			return ret;
		}
		
		@Override
		public void handleAction(Object sender, final Object target)
		{
			final Collection<AttributeItem> items = getItems(target);	
			String confirmText = MessageUtils.createConfirmFromStrings(msg, items);
			
			ConfirmDialog confirm = new ConfirmDialog(msg, msg.getMessage(
					"Attribute.removeConfirm", confirmText), new Callback()
			{
				@Override
				public void onConfirm()
				{
					for (AttributeItem item : items)
					{
						removeAttribute(item);
					}
				}
			});
			confirm.show();
		}
	}

	private class AddAttributeActionHandler extends SingleActionHandler
	{
		public AddAttributeActionHandler()
		{
			super(msg.getMessage("Attribute.addAttribute"), 
					Images.add.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			List<AttributeType> allowed = new ArrayList<>(attributeTypes.size());
			for (AttributeType at: attributeTypes.values())
			{
				if (at.isInstanceImmutable())
					continue;
				if (acHelper.isAllowed(at.getName()))
				{
					boolean used = false;
					for (AttributeExt<?> a: attributes)
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
				NotificationPopup.showNotice(msg, msg.getMessage("notice"),
						msg.getMessage("Attribute.noAvailableAttributes"));
				return;
			}
			
			AttributeEditor attributeEditor = new AttributeEditor(msg, allowed, 
					groupPath, registry, true);
			AttributeEditDialog dialog = new AttributeEditDialog(msg, 
					msg.getMessage("Attribute.addAttribute"), 
					new AttributeEditDialog.Callback()
					{
						@Override
						public boolean newAttribute(Attribute<?> newAttribute)
						{
							return addAttribute(newAttribute);							
						}
					}, attributeEditor);
			dialog.show();
		}
	}

	private class EditAttributeActionHandler extends AbstractAttributeActionHandler
	{
		public EditAttributeActionHandler()
		{
			super(msg.getMessage("Attribute.editAttribute"), 
					Images.edit.getResource());
		}
		
		@Override
		public void handleAction(Object sender, final Object target)
		{
			final Attribute<?> attribute = ((AttributeItem) target).getAttribute();
			AttributeType attributeType = attributeTypes.get(attribute.getName());
			AttributeEditor attributeEditor = new AttributeEditor(msg, attributeType, attribute, 
					registry);
			AttributeEditDialog dialog = new AttributeEditDialog(msg, 
					msg.getMessage("Attribute.editAttribute"), 
					new AttributeEditDialog.Callback()
					{
						@Override
						public boolean newAttribute(Attribute<?> newAttribute)
						{
							return updateAttribute(newAttribute);
						}
					}, attributeEditor);
			dialog.show();
		}
	}
	
	private class ResendConfirmationAttributeActionHandler extends AbstractAttributeActionHandler
	{
		public ResendConfirmationAttributeActionHandler()
		{
			super(msg.getMessage("Attribute.resendConfirmation"), 
					Images.confirm.getResource());
			setMultiTarget(true);
			setHideIfNotNeeded(true);
		}
		
		@Override
		public Action[] getActions(Object target, Object sender)
		{
			Action[] ret = super.getActions(target, sender);
			if (ret.length > 0)
			{
				if (target instanceof Collection<?>)
				{
					for (AttributeItem item : getItems(target))
					{
						if (!checkAttributeIsVerifiable(item))
							return EMPTY;			
						if (checkAttributeIsConfirmed(item))
							return EMPTY;
					}
				} else
				{
					AttributeItem item = (AttributeItem) target;
					if (!checkAttributeIsVerifiable(item))
						return EMPTY;			
					if (checkAttributeIsConfirmed(item))
						return EMPTY;
				}
			}
			return ret;
		}	

		@Override
		public void handleAction(Object sender, final Object target)
		{
			final Collection<AttributeItem> items = getItems(target);
			String confirmText = MessageUtils.createConfirmFromStrings(msg, items);
			
			ConfirmDialog confirm = new ConfirmDialog(msg, msg.getMessage(
					"Attribute.confirmResendConfirmation", confirmText), new Callback()
			{
				@Override
				public void onConfirm()
				{
					for (AttributeItem item : items)
					{
						confirmationMan.sendVerificationsQuiet(owner, Collections.singletonList(item.getAttribute()), true);
					}
				}
			});
			confirm.show();
		}
	}
	
	
	private static class EffectiveAttributesFilter implements Container.Filter
	{
		@Override
		public boolean passesFilter(Object itemId, Item item)
				throws UnsupportedOperationException
		{
			AttributeItem ai = (AttributeItem) itemId;
			return ai.getAttribute().isDirect();
		}

		@Override
		public boolean appliesToProperty(Object propertyId)
		{
			return true;
		}
	}

	private class InternalAttributesFilter implements Container.Filter
	{		
		@Override
		public boolean passesFilter(Object itemId, Item item)
				throws UnsupportedOperationException
		{
			AttributeItem ai = (AttributeItem) itemId;
			AttributeType attributeType = attributeTypes.get(ai.getAttribute().getName());
			if (attributeType == null)
			{
				log.error("Attribute type is not in the map: " + ai.getAttribute().getName());
				return false;
			}
			return !attributeType.isInstanceImmutable();
		}

		@Override
		public boolean appliesToProperty(Object propertyId)
		{
			return true;
		}
	}
}
