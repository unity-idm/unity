/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.tprofile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.translation.ProfileType;
import pl.edu.icm.unity.server.translation.TranslationAction;
import pl.edu.icm.unity.server.translation.TranslationActionFactory;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webadmin.tprofile.ActionParameterComponentFactory.Provider;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.RequiredComboBox;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.UserError;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

/**
 * Responsible for editing of a single {@link TranslationAction}
 * 
 */
public class ActionEditor implements Iterable<Component>
{
	private UnityMessageSource msg;
	private ProfileType profileType;
	private TranslationActionsRegistry tc;
	
	private List<Component> contents = new ArrayList<>();
	private ComboBox actions;
	private FormLayout paramsList;
	private Label actionParams;
	private Provider actionComponentProvider;

	public ActionEditor(ProfileType profileType, UnityMessageSource msg, TranslationActionsRegistry tc,
			TranslationAction toEdit, ActionParameterComponentFactory.Provider actionComponentProvider)
	{
		this.profileType = profileType;
		this.msg = msg;
		this.tc = tc;
		this.actionComponentProvider = actionComponentProvider;
		initUI(toEdit);
	}

	private void initUI(TranslationAction toEdit)
	{
		paramsList = new CompactFormLayout();
		paramsList.setSpacing(true);

		actions = new RequiredComboBox(msg.getMessage("ActionEditor.ruleAction"), msg);
		for (TranslationActionFactory a : tc.getAll())
		{
			if (a.getSupportedProfileType() == profileType)
				actions.addItem(a.getName());
		}
		actions.setValidationVisible(false);
		actions.setNullSelectionAllowed(false);
		actions.addValueChangeListener(new ValueChangeListener()
		{

			@Override
			public void valueChange(ValueChangeEvent event)
			{
				String action = (String) actions.getValue();
				setParams(action, null);

			}
		});

		actionParams = new Label();
		actionParams.setCaption(msg.getMessage("ActionEditor.actionParameters"));
		
		contents.add(actions);
		contents.add(actionParams);
		contents.add(paramsList);

		if (toEdit != null)
		{
			actions.setValue(toEdit.getActionDescription().getName());
			setParams(actions.getValue().toString(), toEdit.getParameters());
		} else
		{
			actionParams.setVisible(false);
			if (!actions.isEmpty())
				actions.setValue(actions.getItemIds().iterator().next());
		}
	}

	private void setParams(String action, String[] values)
	{
		paramsList.removeAllComponents();
		
		TranslationActionFactory factory = getActionFactory(action);
		if (factory == null)
		{	
			return;
		}
		
		actions.setDescription(msg.getMessage(factory.getDescriptionKey()));
		ActionParameterDesc[] params = factory.getParameters();	
		for (int i = 0; i < params.length; i++)
		{
			ActionParameterComponent p = actionComponentProvider.getParameterComponent(params[i]);
			p.setValidationVisible(false);
			if (values != null && values.length > i)
			{
				p.setActionValue(values[i]);
			}		
			paramsList.addComponent(p);
		}
		actionParams.setVisible(paramsList.getComponentCount() != 0);
	}

	private String[] getActionParams()
	{
		ArrayList<String> params = new ArrayList<String>();
		for (int i = 0; i < paramsList.getComponentCount(); i++)
		{
			ActionParameterComponent tc = (ActionParameterComponent) paramsList.getComponent(i);
			((AbstractComponent)tc).setComponentError(null);
			String val = tc.getActionValue();
			params.add(val);
		}
		String[] wrapper = new String[params.size()];
		return params.toArray(wrapper);
	}
	
	private TranslationActionFactory getActionFactory(String action)
	{
		TranslationActionFactory factory = null;
		try
		{
			factory = tc.getByName(action);

		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("ActionEditor.errorGetActions"), e);
		}
		return factory;
	}
	

	public TranslationAction getAction() throws FormValidationException
	{
		String ac = (String) actions.getValue();
		TranslationActionFactory factory = getActionFactory(ac);
		try
		{
			return factory.getInstance(getActionParams());
		} catch (Exception e)
		{
			String error = msg.getMessage("ActionEditor.parametersError", e.getMessage());
			UserError ue = new UserError(error);
			for (int i = 0; i < paramsList.getComponentCount(); i++)
				((AbstractComponent)paramsList.getComponent(i)).setComponentError(ue);
			throw new FormValidationException(error);
		}
	}

	@Override
	public Iterator<Component> iterator()
	{
		return contents.iterator();
	}
}
