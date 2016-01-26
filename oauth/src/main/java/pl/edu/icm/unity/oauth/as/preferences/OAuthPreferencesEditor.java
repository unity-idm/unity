/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.preferences;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences.OAuthClientSettings;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.GenericElementsTable.GenericItem;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.preferences.PreferencesEditor;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.Action.Handler;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;

/**
 * Viewing and editing UI of {@link OAuthPreferences}.
 * @author K. Benedyczak
 */
public class OAuthPreferencesEditor implements PreferencesEditor
{
	protected UnityMessageSource msg;
	protected OAuthPreferences preferences;
	protected IdentitiesManagement idsMan;
	protected ModificationListener listener;
	
	protected HorizontalLayout main;
	protected GenericElementsTable<String> table;
	protected OAuthSPSettingsViewer viewer;
	
	protected Identity[] identities;

	public OAuthPreferencesEditor(UnityMessageSource msg, OAuthPreferences preferences, IdentitiesManagement idsMan)
	{
		this.msg = msg;
		this.preferences = preferences;
		this.idsMan = idsMan;
		
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
		
		table = new GenericElementsTable<String>(msg.getMessage("OAuthPreferences.spSettings"), 
				new GenericElementsTable.NameProvider<String>()
				{
					public Object toRepresentation(String element)
					{
						return element.equals("") ? 
								msg.getMessage("OAuthPreferences.defaultSP") : element;
					}
				});
		table.setWidth(90, Unit.PERCENTAGE);
		table.setHeight(300, Unit.PIXELS);
		main.addComponent(table);
		viewer = configureViewer();
		main.addComponent(viewer);
		for (Handler h: getHandlers())
			table.addActionHandler(h);
		main.setSizeFull();
		main.setSpacing(true);

		table.setInput(preferences.getKeys());
		viewer.setInput(null);
	}
	
	protected Handler[] getHandlers()
	{
		return new Handler[] {new AddActionHandler(),
					new EditActionHandler(),
					new DeleteActionHandler()};
	}
	
	protected OAuthSPSettingsViewer configureViewer()
	{
		final OAuthSPSettingsViewer viewer = new OAuthSPSettingsViewer(msg);
		table.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				@SuppressWarnings("unchecked")
				GenericItem<String> item = (GenericItem<String>)table.getValue();
				if (item != null)
				{
					OAuthClientSettings sp = preferences.getSPSettings(item.getElement());
					viewer.setInput(sp);
				} else
					viewer.setInput(null);
			}
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
		return preferences.getSerializedConfiguration();
	}
	
	protected class AddActionHandler extends SingleActionHandler
	{
		public AddActionHandler()
		{
			super(msg.getMessage("OAuthPreferences.addAction"), Images.add.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			try
			{
				initStateData();
			} catch (EngineException e)
			{
				NotificationPopup.showError(msg, msg.getMessage("OAuthPreferences.errorLoadindSystemInfo"), e);
				return;
			}
			OAuthSPSettingsEditor editor = new OAuthSPSettingsEditor(msg, identities, 
					preferences.getKeys());
			new OAuthSettingsDialog(msg, editor, new OAuthSettingsDialog.Callback()
			{
				@Override
				public void updatedClient(OAuthClientSettings spSettings, String sp)
				{
					preferences.setSPSettings(sp, spSettings);
					table.setInput(preferences.getKeys());
					listener.preferencesModified();
				}
			}).show();
		}
	}
	
	protected class EditActionHandler extends SingleActionHandler
	{
		public EditActionHandler()
		{
			super(msg.getMessage("OAuthPreferences.editAction"), Images.edit.getResource());
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			try
			{
				initStateData();
			} catch (EngineException e)
			{
				NotificationPopup.showError(msg, msg.getMessage("OAuthPreferences.errorLoadindSystemInfo"), e);
				return;
			}
			@SuppressWarnings("unchecked")
			GenericItem<String> item = (GenericItem<String>)target;
			OAuthSPSettingsEditor editor = new OAuthSPSettingsEditor(msg, identities, 
					item.getElement(), preferences.getSPSettings(item.getElement()));
			new OAuthSettingsDialog(msg, editor, new OAuthSettingsDialog.Callback()
			{
				@Override
				public void updatedClient(OAuthClientSettings spSettings, String sp)
				{
					preferences.setSPSettings(sp, spSettings);
					table.setInput(preferences.getKeys());
					listener.preferencesModified();
				}
			}).show();
		}
	}
	
	protected class DeleteActionHandler extends SingleActionHandler
	{
		public DeleteActionHandler()
		{
			super(msg.getMessage("OAuthPreferences.deleteAction"), 
					Images.delete.getResource());
		}
		
		@Override
		public void handleAction(Object sender, Object target)
		{
			
			GenericItem<?> item = (GenericItem<?>)target;
			preferences.removeSPSettings((String)item.getElement());
			table.setInput(preferences.getKeys());
			listener.preferencesModified();
		}
	}

	@Override
	public void setChangeListener(ModificationListener listener)
	{
		this.listener = listener;
	}
}
