/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.preferences;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.RadioButtonGroup;

import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

/**
 * Allows to edit settings for a single SAML Service Provider.
 * 
 * @author K. Benedyczak
 */
public class SPSettingsEditor extends FormLayout
{
	protected UnityMessageSource msg;
	protected List<Identity> identities;
	protected Collection<AttributeType> attributeTypes;
	
	protected ComboBox<String> sp;
	protected Label spLabel;
	protected RadioButtonGroup<Decision> decision;
	protected RadioButtonGroup<Identity> identity;
	protected GenericElementsTable<TableEntry> hidden;
	private AttributeHandlerRegistry handlerReg;
	private IdentityTypeSupport idTypeSupport;
	
	public SPSettingsEditor(UnityMessageSource msg, AttributeHandlerRegistry handlerReg, 
			IdentityTypeSupport idTypeSupport, List<Identity> identities, 
			Collection<AttributeType> atTypes, String sp, SPSettings initial)
	{
		this(msg, handlerReg, idTypeSupport, identities, atTypes, sp, initial, null);
	}

	public SPSettingsEditor(UnityMessageSource msg, AttributeHandlerRegistry handlerReg, 
			IdentityTypeSupport idTypeSupport, List<Identity> identities, 
			Collection<AttributeType> atTypes,
			Set<String> allSps)
	{
		this(msg, handlerReg, idTypeSupport, identities, atTypes, null, null, allSps);
	}

	private SPSettingsEditor(UnityMessageSource msg, AttributeHandlerRegistry handlerReg, 
			IdentityTypeSupport idTypeSupport, List<Identity> identities, 
			Collection<AttributeType> atTypes, String sp, SPSettings initial, Set<String> allSps)
	{
		this.msg = msg;
		this.handlerReg = handlerReg;
		this.idTypeSupport = idTypeSupport;
		this.identities = new ArrayList<>(identities);
		this.attributeTypes = atTypes;
		initUI(initial, sp, allSps);
	}
	
	public SPSettings getSPSettings()
	{
		SPSettings ret = new SPSettings();
		Decision selDecision = decision.getSelectedItem().get();
		if (selDecision == Decision.AUTO_ACCEPT)
		{
			ret.setDefaultAccept(true);
			ret.setDoNotAsk(true);
		} else if (selDecision == Decision.AUTO_DENY)
		{
			ret.setDefaultAccept(false);
			ret.setDoNotAsk(true);			
		} else
		{
			ret.setDefaultAccept(false);
			ret.setDoNotAsk(false);
		}

		Identity id = identity.getValue();
		if (id != null)
		{
			IdentityTypeDefinition idType = idTypeSupport.getTypeDefinition(id.getTypeId());
			if (!idType.isDynamic() && !idType.isTargeted())
				ret.setSelectedIdentity(id.getComparableValue());
		}

		Map<String, Attribute> hidden = getHidden();
		ret.setHiddenAttribtues(hidden);
		return ret;
	}
	
	public String getSP()
	{
		return sp == null ? spLabel.getValue() : (String) sp.getValue();
	}
	
	private Map<String, Attribute> getHidden()
	{
		List<TableEntry> itemIds = hidden.getElements();
		Map<String, Attribute> hiddenAttrs = new HashMap<>();
		for (TableEntry item: itemIds)
			hiddenAttrs.put(item.name, item.hiddenValues);
		return hiddenAttrs;
	}
	
	private void initUI(SPSettings initial, String initialSp, Set<String> allSps)
	{
		if (initial == null)
		{
			sp = new ComboBox<>(msg.getMessage("SAMLPreferences.SP"));
			sp.setDescription(msg.getMessage("SAMLPreferences.SPdesc"));
			sp.setWidth(100, Unit.PERCENTAGE);
			sp.setTextInputAllowed(true);
			sp.setNewItemProvider(s ->
			{
				List<String> items = Lists.newArrayList(s);
				items.addAll(allSps);
				sp.setItems(items);
				return Optional.of(s);
			});
			sp.setEmptySelectionAllowed(true);
			sp.setItems(allSps);
			addComponent(sp);
		} else
		{
			spLabel = new Label(initialSp);
			spLabel.setCaption(msg.getMessage("SAMLPreferences.SP"));
			addComponent(spLabel);
		}
		
		decision = new RadioButtonGroup<>(msg.getMessage("SAMLPreferences.decision"));
		decision.setItemCaptionGenerator(this::getDecisionCaption);
		decision.setItems(Decision.AUTO_ACCEPT, Decision.AUTO_DENY, Decision.NO_AUTO);

		identity = new RadioButtonGroup<>(msg.getMessage("SAMLPreferences.identity"));
		identity.setItems(identities);
		identity.setItemCaptionGenerator(id -> 
			idTypeSupport.getTypeDefinition(id.getTypeId()).toPrettyString(id));
		
		hidden = new GenericElementsTable<>(msg.getMessage("SAMLPreferences.hidden"));
		hidden.setHeight(200, Unit.PIXELS);
		hidden.addActionHandler(getAddAction());
		hidden.addActionHandler(getDeleteAction());
		
		addComponents(decision, identity, hidden);
		
		if (initial != null)
			setValues(initial);
		else
			setDefaults();
	}
	
	private void setDefaults()
	{
		decision.setSelectedItem(Decision.NO_AUTO);
		identity.setSelectedItem(identities.get(0));
	}
	
	private void setValues(SPSettings initial)
	{
		if (!initial.isDoNotAsk())
			decision.setSelectedItem(Decision.NO_AUTO);
		else if (initial.isDefaultAccept())
			decision.setSelectedItem(Decision.AUTO_ACCEPT);
		else
			decision.setSelectedItem(Decision.AUTO_DENY);
		
		String selId = initial.getSelectedIdentity();
		if (selId != null)
		{
			for (Identity i: identities)
			{
				if (i.getComparableValue().equals(selId))
				{
					identity.setSelectedItem(i);
					break;
				}
			}
		}

		Map<String, Attribute> hiddenAttribtues = initial.getHiddenAttribtues();
		Collection<TableEntry> converted = new ArrayList<>();
		for (Entry<String, Attribute> entry : hiddenAttribtues.entrySet())
			converted.add(new TableEntry(entry.getKey(), entry.getValue()));
		hidden.setInput(converted);
	}
	
	private SingleActionHandler<TableEntry> getDeleteAction()
	{
		return SingleActionHandler.builder4Delete(msg, TableEntry.class)
				.withHandler(target -> hidden.removeElement(target.iterator().next()))
				.build();
	}
	
	private SingleActionHandler<TableEntry> getAddAction()
	{
		return SingleActionHandler.builder4Add(msg, TableEntry.class)
				.withHandler(sel -> new SelectAttributeDialog(msg).show())
				.build();
	}

	private class SelectAttributeDialog extends AbstractDialog
	{
		private ComboBox<String> selection;
		
		public SelectAttributeDialog(UnityMessageSource msg)
		{
			super(msg, msg.getMessage("SAMLPreferences.selectAttribute"));
			setSizeMode(SizeMode.SMALL);
		}

		@Override
		protected Component getContents() throws Exception
		{
			selection = new ComboBox<>(msg.getMessage("SAMLPreferences.selectAttribute"));
			selection.setEmptySelectionAllowed(false);
			Set<String> alreadySelected = getHidden().keySet();
			List<String> available = attributeTypes.stream()
					.map(at -> at.getName())
					.filter(a -> !alreadySelected.contains(a))
					.filter(a -> !a.startsWith("sys:"))
					.sorted()
					.collect(Collectors.toList());
			selection.setItems(available);
			if (!available.isEmpty())
			{
				selection.setSelectedItem(available.get(0));
			} else
			{
				NotificationPopup.showNotice(msg.getMessage("notice"), msg.getMessage("SAMLPreferences.allSelected"));
				throw new FormValidationException();
			}
			return selection;
		}

		@Override
		protected void onConfirm()
		{
			String result = (String) selection.getValue();
			hidden.addElement(new TableEntry(result, null));
			close();
		}
	}
	
	private class TableEntry
	{
		private String name;
		private Attribute hiddenValues;
		
		public TableEntry(String name, Attribute hiddenValues)
		{
			this.name = name;
			this.hiddenValues = hiddenValues;
		}



		@Override
		public String toString()
		{
			return hiddenValues == null ? 
					name : 
					handlerReg.getSimplifiedAttributeRepresentation(hiddenValues);
		}
	}
	
	
	private String getDecisionCaption(Decision dec)
	{
		switch (dec)
		{
		case AUTO_ACCEPT:
			return msg.getMessage("SAMLPreferences.autoAccept");
		case AUTO_DENY:
			return 	msg.getMessage("SAMLPreferences.autoDeny");
		case NO_AUTO:
			return msg.getMessage("SAMLPreferences.noAuto");
		default:
			throw new IllegalArgumentException("Unknown decision");
		}
	}
	
	private enum Decision
	{
		AUTO_ACCEPT,
		AUTO_DENY,
		NO_AUTO
	}
}
