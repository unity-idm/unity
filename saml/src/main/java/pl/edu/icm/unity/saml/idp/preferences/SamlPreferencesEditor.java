/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.preferences;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.preferences.PreferencesEditor;

/**
 * Viewing and editing UI of {@link SamlPreferences}.
 * @author K. Benedyczak
 */
public class SamlPreferencesEditor implements PreferencesEditor
{
	protected UnityMessageSource msg;
	protected SamlPreferences preferences;
	protected EntityManagement idsMan;
	protected AttributeTypeManagement atsMan;
	protected ModificationListener listener;
	
	protected HorizontalLayout main;
	protected GenericElementsTable<String> table;
	protected SamlSPSettingsViewer viewer;
	
	protected List<Identity> identities;
	protected Collection<AttributeType> atTypes;
	protected AttributeHandlerRegistry attributeHandlerRegistry;
	protected IdentityTypeSupport idTpeSupport;

	public SamlPreferencesEditor(UnityMessageSource msg, SamlPreferences preferences, EntityManagement idsMan,
			AttributeTypeManagement atsMan, AttributeHandlerRegistry attributeHandlerRegistry,
			IdentityTypeSupport idTpeSupport)
	{
		this.msg = msg;
		this.preferences = preferences;
		this.idsMan = idsMan;
		this.atsMan = atsMan;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.idTpeSupport = idTpeSupport;
		
		init();
	}

	protected void initStateData() throws EngineException
	{
		LoginSession auth = InvocationContext.getCurrent().getLoginSession();
		EntityParam entParam = new EntityParam(auth.getEntityId());
		identities = idsMan.getEntity(entParam).getIdentities();
		atTypes = atsMan.getAttributeTypes();
	}
	
	private void init()
	{
		main = new HorizontalLayout();
		
		table = new GenericElementsTable<>(msg.getMessage("SAMLPreferences.spSettings"), 
				this::getDisplayedName);
		table.setWidth(90, Unit.PERCENTAGE);
		table.setHeight(300, Unit.PIXELS);
		main.addComponent(table);
		viewer = configureViewer();
		main.addComponent(viewer);
		table.addActionHandler(getAddAction());
		table.addActionHandler(getEditAction());
		table.addActionHandler(getDeleteAction());
		main.setSizeFull();
		main.setMargin(false);

		table.setInput(preferences.getKeys());
		viewer.setInput(null);
	}
	
	private String getDisplayedName(String element)
	{
		return element.equals("") ? msg.getMessage("SAMLPreferences.defaultSP") : element;
	}

	protected SamlSPSettingsViewer configureViewer()
	{
		final SamlSPSettingsViewer viewer = new SamlSPSettingsViewer(msg, attributeHandlerRegistry);
		table.addSelectionListener(event ->
		{
			Set<String> items = event.getAllSelectedItems();
			if (!items.isEmpty())
			{
				SPSettings sp = preferences.getSPSettings(items.iterator().next());
				viewer.setInput(sp);
			} else
				viewer.setInput(null);
		});
		return viewer;
	}
	
	@Override
	public Component getComponent()
	{
		return main;
	}

	@Override
	public String getValue() throws FormValidationException
	{
		return JsonUtil.serialize(preferences.getSerializedConfiguration());
	}
	
	protected SingleActionHandler<String> getAddAction()
	{
		return SingleActionHandler.builder4Add(msg, String.class)
				.withHandler(this::showAddDialog)
				.build();
	}

	private void showAddDialog(Set<String> items)
	{
		try
		{
			initStateData();
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("SAMLPreferences.errorLoadindSystemInfo"), e);
			return;
		}
		SPSettingsEditor editor = new SPSettingsEditor(msg, attributeHandlerRegistry, 
				idTpeSupport, identities, 
				atTypes, preferences.getKeys());
		new SPSettingsDialog(msg, editor, (spSettings, sp) -> 
		{
			preferences.setSPSettings(sp, spSettings);
			table.setInput(preferences.getKeys());
			listener.preferencesModified();
		}).show();
	}
	
	protected SingleActionHandler<String> getEditAction()
	{
		return SingleActionHandler.builder4Edit(msg, String.class)
				.withHandler(this::showEditDialog)
				.build();
	}

	private void showEditDialog(Set<String> items)
	{
		try
		{
			initStateData();
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("SAMLPreferences.errorLoadindSystemInfo"), e);
			return;
		}
		String item = items.iterator().next();
		SPSettingsEditor editor = new SPSettingsEditor(msg, attributeHandlerRegistry, 
				idTpeSupport, identities, 
				atTypes, item, preferences.getSPSettings(item));
		new SPSettingsDialog(msg, editor, (spSettings, sp) -> 
		{
			preferences.setSPSettings(sp, spSettings);
			table.setInput(preferences.getKeys());
			listener.preferencesModified();
		}).show();
	}
	
	private SingleActionHandler<String> getDeleteAction()
	{
		return SingleActionHandler.builder4Delete(msg, String.class)
				.withHandler(this::deleteHandler)
				.build();
	}

	private void deleteHandler(Set<String> items)
	{
		String item = items.iterator().next();
		preferences.removeSPSettings(item);
		table.setInput(preferences.getKeys());
		listener.preferencesModified();
	}

	@Override
	public void setChangeListener(ModificationListener listener)
	{
		this.listener = listener;
	}
}
