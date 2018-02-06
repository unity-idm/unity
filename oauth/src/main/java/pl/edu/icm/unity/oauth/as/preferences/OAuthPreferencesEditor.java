/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.preferences;

import java.util.List;
import java.util.Set;

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences.OAuthClientSettings;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.preferences.PreferencesEditor;

/**
 * Viewing and editing UI of {@link OAuthPreferences}.
 * @author K. Benedyczak
 */
public class OAuthPreferencesEditor implements PreferencesEditor
{
	protected UnityMessageSource msg;
	protected OAuthPreferences preferences;
	protected EntityManagement idsMan;
	private IdentityTypeSupport idTypeSupport;
	
	protected ModificationListener listener;
	
	protected HorizontalLayout main;
	protected GenericElementsTable<String> table;
	protected OAuthSPSettingsViewer viewer;
	
	protected List<Identity> identities;

	public OAuthPreferencesEditor(UnityMessageSource msg, OAuthPreferences preferences, EntityManagement idsMan,
			IdentityTypeSupport idTypeSupport)
	{
		this.msg = msg;
		this.preferences = preferences;
		this.idsMan = idsMan;
		this.idTypeSupport = idTypeSupport;
		
		init();
	}

	protected void initStateData() throws EngineException
	{
		LoginSession auth = InvocationContext.getCurrent().getLoginSession();
		EntityParam entParam = new EntityParam(auth.getEntityId());
		identities = idsMan.getEntity(entParam).getIdentities();
	}
	
	private void init()
	{
		main = new HorizontalLayout();
		
		table = new GenericElementsTable<>(msg.getMessage("OAuthPreferences.spSettings"), 
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
		return element.equals("") ? msg.getMessage("OAuthPreferences.defaultSP") : element;
	}
	
	protected OAuthSPSettingsViewer configureViewer()
	{
		final OAuthSPSettingsViewer viewer = new OAuthSPSettingsViewer(msg);
		table.addSelectionListener(event ->
		{
			Set<String> items = event.getAllSelectedItems();
			if (!items.isEmpty())
			{
				OAuthClientSettings sp = preferences.getSPSettings(
						items.iterator().next());
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
	
	private SingleActionHandler<String> getAddAction()
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
			NotificationPopup.showError(msg, msg.getMessage("OAuthPreferences.errorLoadindSystemInfo"), e);
			return;
		}
		OAuthSPSettingsEditor editor = new OAuthSPSettingsEditor(msg, idTypeSupport, identities, 
				preferences.getKeys());
		new OAuthSettingsDialog(msg, editor, (spSettings, sp) ->
		{
			preferences.setSPSettings(sp, spSettings);
			table.setInput(preferences.getKeys());
			listener.preferencesModified();
		}).show();
	}
	
	private SingleActionHandler<String> getEditAction()
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
			NotificationPopup.showError(msg, msg.getMessage("OAuthPreferences.errorLoadindSystemInfo"), e);
			return;
		}
		String item = items.iterator().next();
		OAuthSPSettingsEditor editor = new OAuthSPSettingsEditor(msg, idTypeSupport, identities, 
				item, preferences.getSPSettings(item));
		new OAuthSettingsDialog(msg, editor, (spSettings, sp) ->
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
