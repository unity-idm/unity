/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.preferences;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ConfirmDialog.Callback;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.preferences.PreferencesEditor;
import pl.edu.icm.unity.webui.common.preferences.PreferencesHandler;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;

/**
 * Management of a single preference.
 * @author K. Benedyczak
 */
public class PreferenceViewTab extends VerticalLayout
{
	private PreferencesHandler preferenceHandler;
	private PreferencesManagement prefMan;
	private UnityMessageSource msg;
	private EntityParam entityParam;
	
	private VerticalLayout viewerPanel;
	private PreferencesEditor editor;
	private String currentValue;
	
	public PreferenceViewTab(UnityMessageSource msg, PreferencesHandler preferenceHandler, 
			PreferencesManagement prefMan)
	{
		this.msg = msg;
		this.preferenceHandler = preferenceHandler;
		this.prefMan = prefMan;
		
		AuthenticatedEntity entity = InvocationContext.getCurrent().getAuthenticatedEntity();
		entityParam = new EntityParam(entity.getEntityId());
		init();
	}
	
	private void reset()
	{
		try
		{
			prefMan.removePreference(entityParam, preferenceHandler.getPreferenceId());
			refresh();
		} catch (EngineException e)
		{
			ErrorPopup.showError(msg.getMessage("Preferences.errorReset"), e);
		}
	}
	
	private void update()
	{
		String value;
		try
		{
			value = editor.getValue();
		} catch (FormValidationException e)
		{
			ErrorPopup.showFormError(msg);
			return;
		}
		try
		{
			prefMan.setPreference(entityParam, preferenceHandler.getPreferenceId(), value);
			refresh();
		} catch (EngineException e)
		{
			ErrorPopup.showError(msg.getMessage("Preferences.errorUpdate"), e);
		}
	}
	
	private void refresh() throws EngineException
	{
		currentValue = prefMan.getPreference(entityParam, preferenceHandler.getPreferenceId());
		editor = preferenceHandler.getPreferencesEditor(currentValue);
		viewerPanel.removeAllComponents();
		viewerPanel.addComponent(editor.getComponent());
	}
	
	private void init()
	{
		
		HorizontalLayout toolbar = new HorizontalLayout();
		toolbar.setMargin(true);
		toolbar.setSpacing(true);
		Button edit = new Button(msg.getMessage("Preferences.save"));
		edit.setIcon(Images.hAccept.getResource());
		edit.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				update();
			}
		});
		Button reset = new Button(msg.getMessage("Preferences.reset"));
		reset.setIcon(Images.hRemove.getResource());

		reset.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				new ConfirmDialog(msg, msg.getMessage("Preferences.confirmReset", 
								preferenceHandler.getPreferenceLabel()),
						new Callback()
						{
							@Override
							public void onConfirm()
							{
								reset();
							}
						}).show();
			}
		});
		toolbar.addComponents(edit, reset);
		viewerPanel = new VerticalLayout();
		viewerPanel.setMargin(true);
		viewerPanel.setSizeFull();
		setCaption(preferenceHandler.getPreferenceLabel());
		
		try
		{
			refresh();
			addComponents(toolbar, viewerPanel);
		} catch (EngineException e)
		{
			ErrorComponent ec = new ErrorComponent();
			ec.setError(msg.getMessage("Preferences.errorGet"), e);
			addComponent(ec);
		}
	}
}
