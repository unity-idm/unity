/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.preferences;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.Action.Handler;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferencesEditor;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.unicore.samlidp.preferences.SamlPreferencesWithETD.SPETDSettings;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GenericElementsTable.GenericItem;

/**
 * Viewing and editing UI of {@link SamlPreferencesWithETD}.
 * @author K. Benedyczak
 */
public class SamlPreferencesWithETDEditor extends SamlPreferencesEditor 
{
	private SamlPreferencesWithETD preferences;
	
	public SamlPreferencesWithETDEditor(UnityMessageSource msg, SamlPreferencesWithETD preferences, IdentitiesManagement idsMan,
			AttributesManagement atsMan)
	{
		super(msg, preferences, idsMan, atsMan);
		this.preferences = preferences;
	}

	@Override
	protected Handler[] getHandlers()
	{
		return new Handler[] {new AddActionHandler(),
					new EditActionHandler(),
					new DeleteActionHandler()};
	}
	
	@Override
	protected SamlSPSettingsWithETDViewer configureViewer()
	{
		final SamlSPSettingsWithETDViewer viewer = new SamlSPSettingsWithETDViewer(msg);
		table.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				@SuppressWarnings("unchecked")
				GenericItem<String> item = (GenericItem<String>)table.getValue();
				if (item != null)
				{
					SPSettings sp = preferences.getSPSettings(item.getElement());
					SPETDSettings spEtd = preferences.getSPETDSettings(item.getElement());
					viewer.setInput(sp, spEtd);
				} else
					viewer.setInput(null);
			}
		});
		return viewer;
	}
	
	@Override
	public String getValue() throws FormValidationException
	{
		return preferences.getSerializedConfiguration();
	}
	
	private class AddActionHandler extends SamlPreferencesEditor.AddActionHandler
	{
		@Override
		public void handleAction(Object sender, final Object target)
		{
			try
			{
				initStateData();
			} catch (EngineException e)
			{
				ErrorPopup.showError(msg, msg.getMessage("SAMLPreferences.errorLoadindSystemInfo"), e);
				return;
			}
			SPSettingsWithETDEditor editor = new SPSettingsWithETDEditor(msg, identities, 
					atTypes, preferences.getKeys());
			new SPSettingsWithETDDialog(msg, editor, new SPSettingsWithETDDialog.Callback()
			{
				@Override
				public void updatedSP(SPSettings spSettings,
						SPETDSettings etdSettings, String sp)
				{
					preferences.setSPSettings(sp, spSettings);
					preferences.setSPETDSettings(sp, etdSettings);
					table.setInput(preferences.getKeys());
					listener.preferencesModified();
				}
			}).show();
		}
	}
	
	private class EditActionHandler extends SamlPreferencesEditor.EditActionHandler
	{
		@Override
		public void handleAction(Object sender, final Object target)
		{
			try
			{
				initStateData();
			} catch (EngineException e)
			{
				ErrorPopup.showError(msg, msg.getMessage("SAMLPreferences.errorLoadindSystemInfo"), e);
				return;
			}
			@SuppressWarnings("unchecked")
			GenericItem<String> item = (GenericItem<String>)target;
			String sp = item.getElement();
			SPSettingsWithETDEditor editor = new SPSettingsWithETDEditor(msg, identities, 
					atTypes, sp, preferences.getSPSettings(sp),
					preferences.getSPETDSettings(sp));
			new SPSettingsWithETDDialog(msg, editor, new SPSettingsWithETDDialog.Callback()
			{
				@Override
				public void updatedSP(SPSettings spSettings,
						SPETDSettings etdSettings, String sp)
				{
					preferences.setSPSettings(sp, spSettings);
					preferences.setSPETDSettings(sp, etdSettings);
					table.setInput(preferences.getKeys());
					listener.preferencesModified();
				}
			}).show();
		}
	}
}
