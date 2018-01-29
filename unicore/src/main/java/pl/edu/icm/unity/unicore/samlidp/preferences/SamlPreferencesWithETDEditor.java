/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.preferences;

import java.util.Set;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferencesEditor;
import pl.edu.icm.unity.unicore.samlidp.preferences.SamlPreferencesWithETD.SPETDSettings;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

/**
 * Viewing and editing UI of {@link SamlPreferencesWithETD}.
 * @author K. Benedyczak
 */
public class SamlPreferencesWithETDEditor extends SamlPreferencesEditor 
{
	private SamlPreferencesWithETD preferences;
	
	public SamlPreferencesWithETDEditor(UnityMessageSource msg, SamlPreferencesWithETD preferences, 
			EntityManagement idsMan,
			AttributeTypeManagement atsMan, AttributeHandlerRegistry attributeHandlerRegistries,
			IdentityTypeSupport idTpeSupport)
	{
		super(msg, preferences, idsMan, atsMan, attributeHandlerRegistries, idTpeSupport);
		this.preferences = preferences;
	}

	@Override
	protected SamlSPSettingsWithETDViewer configureViewer()
	{
		final SamlSPSettingsWithETDViewer viewer = new SamlSPSettingsWithETDViewer(msg, 
				attributeHandlerRegistry);
		table.addSelectionListener(event -> 
		{
			Set<String> items = event.getAllSelectedItems();
			if (!items.isEmpty())
			{
				String item = items.iterator().next();
				SPSettings sp = preferences.getSPSettings(item);
				SPETDSettings spEtd = preferences.getSPETDSettings(item);
				viewer.setInput(sp, spEtd);
			} else
				viewer.setInput(null);
		});
		return viewer;
	}
	
	@Override
	public String getValue() throws FormValidationException
	{
		return JsonUtil.serialize(preferences.getSerializedConfiguration());
	}
	
	@Override
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
		SPSettingsWithETDEditor editor = new SPSettingsWithETDEditor(msg, attributeHandlerRegistry,
				idTpeSupport, identities, atTypes, preferences.getKeys());
		new SPSettingsWithETDDialog(msg, editor, (spSettings, etdSettings, sp) ->
		{
			preferences.setSPSettings(sp, spSettings);
			preferences.setSPETDSettings(sp, etdSettings);
			table.setInput(preferences.getKeys());
			listener.preferencesModified();
		}).show();
	}

	@Override
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
		String sp = items.iterator().next();
		SPSettingsWithETDEditor editor = new SPSettingsWithETDEditor(msg, attributeHandlerRegistry, 
				idTpeSupport, identities, atTypes, sp, preferences.getSPSettings(sp),
				preferences.getSPETDSettings(sp));
		new SPSettingsWithETDDialog(msg, editor, (spSettings, etdSettings, sp2) -> 
		{
			preferences.setSPSettings(sp2, spSettings);
			preferences.setSPETDSettings(sp2, etdSettings);
			table.setInput(preferences.getKeys());
			listener.preferencesModified();
		}).show();
	}
}
