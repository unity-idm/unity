/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.tprofile;

import java.util.ArrayList;

import org.mvel2.MVEL;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationAction;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationActionFactory;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationCondition;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationRule;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.RequiredComboBox;
import pl.edu.icm.unity.webui.common.RequiredTextField;
import pl.edu.icm.unity.webui.common.Styles;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.validator.AbstractStringValidator;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

/**
 * Responsible for edit TranslationRule
 * 
 * @author P. Piernik
 * 
 */
public class RuleComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private TranslationActionsRegistry tc;
	private ComboBox actions;
	private AbstractTextField condition;
	private FormLayout paramsList;
	private Callback callback;
	private Button up;
	private Button top;
	private Button down;
	private Button bottom;
	private Label actionParams;
	private boolean editMode;
	private Image helpAction;

	public RuleComponent(UnityMessageSource msg, TranslationActionsRegistry tc,
			TranslationRule toEdit, Callback callback)
	{
		this.callback = callback;
		this.msg = msg;
		this.tc = tc;
		editMode = toEdit != null;
		initUI(toEdit);
	}

	private void initUI(TranslationRule toEdit)
	{
		up = new Button();
		up.setDescription(msg.getMessage("TranslationProfileEditor.moveUp"));
		up.setIcon(Images.upArrow.getResource());
		up.addStyleName(Reindeer.BUTTON_LINK);
		up.addStyleName(Styles.toolbarButton.toString());
		up.addClickListener(new Button.ClickListener()
		{

			@Override
			public void buttonClick(ClickEvent event)
			{
				callback.moveUp(RuleComponent.this);

			}
		});
		
		top = new Button();
		top.setDescription(msg.getMessage("TranslationProfileEditor.moveTop"));
		top.setIcon(Images.topArrow.getResource());
		top.addStyleName(Reindeer.BUTTON_LINK);
		top.addStyleName(Styles.toolbarButton.toString());
		top.addClickListener(new Button.ClickListener()
		{

			@Override
			public void buttonClick(ClickEvent event)
			{
				callback.moveTop(RuleComponent.this);

			}
		});

		down = new Button();
		down.setDescription(msg.getMessage("TranslationProfileEditor.moveTop"));
		down.setIcon(Images.downArrow.getResource());
		down.addStyleName(Reindeer.BUTTON_LINK);
		down.addStyleName(Styles.toolbarButton.toString());
		down.addClickListener(new Button.ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				callback.moveDown(RuleComponent.this);

			}
		});
		bottom = new Button();
		bottom.setDescription(msg.getMessage("TranslationProfileEditor.moveBottom"));
		bottom.setIcon(Images.bottomArrow.getResource());
		bottom.addStyleName(Reindeer.BUTTON_LINK);
		bottom.addStyleName(Styles.toolbarButton.toString());
		bottom.addClickListener(new Button.ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				callback.moveBottom(RuleComponent.this);

			}
		});
		

		Button remove = new Button();
		remove.setDescription(msg.getMessage("TranslationProfileEditor.remove"));
		remove.setIcon(Images.delete.getResource());
		remove.addStyleName(Reindeer.BUTTON_LINK);
		remove.addStyleName(Styles.toolbarButton.toString());
		remove.addClickListener(new Button.ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				callback.remove(RuleComponent.this);

			}
		});

		HorizontalLayout toolbar = new HorizontalLayout();
		toolbar.setSpacing(false);
		toolbar.setMargin(false);
		HorizontalLayout buttonWrapper = new HorizontalLayout();
		buttonWrapper.setSpacing(false);
		buttonWrapper.setMargin(false);

		Label space = new Label();
		buttonWrapper.addComponents(space, up, top, down, bottom, remove);
		toolbar.addComponents(space, buttonWrapper);
		toolbar.setExpandRatio(space, 2);
		toolbar.setExpandRatio(buttonWrapper, 1);
		toolbar.setWidth(100, Unit.PERCENTAGE);

		paramsList = new FormLayout();
		paramsList.setSpacing(false);

		condition = new RequiredTextField(msg);
		condition.setCaption(msg.getMessage("TranslationProfileEditor.ruleCondition") + ":");
		condition.addValidator(new AbstractStringValidator(msg
				.getMessage("TranslationProfileEditor.conditionValidationFalse"))
		{
			@Override
			protected boolean isValidValue(String value)
			{
				try
				{
					MVEL.compileExpression(value);
				} catch (Exception e)
				{
					return false;
				}

				return true;

			}
		});
		condition.setValidationVisible(false);
		condition.setImmediate(true);

		actions = new RequiredComboBox(msg.getMessage("TranslationProfileEditor.ruleAction") + ":", msg);
		for (TranslationActionFactory a : tc.getAll())
		{
			actions.addItem(a.getName());
		}
		actions.setImmediate(true);
		actions.setValidationVisible(false);
		actions.addValueChangeListener(new ValueChangeListener()
		{

			@Override
			public void valueChange(ValueChangeEvent event)
			{
				String action = (String) actions.getValue();
				setParams(action, null);

			}
		});

		Label separator = new Label();
		separator.addStyleName(Styles.horizontalLine.toString());

		FormLayout help = new FormLayout();
		helpAction = new Image("", Images.help.getResource());
		helpAction.setDescription(msg
				.getMessage("TranslationProfileEditor.helpEmptyAction"));

		Image helpCondition = new Image("", Images.help.getResource());
		helpCondition.setDescription(msg
				.getMessage("TranslationProfileEditor.helpCondition"));

		help.addComponents(helpCondition, helpAction);
		help.setComponentAlignment(helpCondition, Alignment.TOP_LEFT);
		help.setComponentAlignment(helpAction, Alignment.BOTTOM_LEFT);
		help.setSpacing(true);

		HorizontalLayout wrapper = new HorizontalLayout();
		wrapper.setSpacing(false);
		wrapper.setMargin(false);
		actionParams = new Label();
		actionParams.setCaption(msg.getMessage("TranslationProfileEditor.actionParameters") + ":");
		FormLayout main = new FormLayout();
		main.setSpacing(true);
		main.setMargin(false);
		main.addComponents(condition, actions, actionParams);
		wrapper.addComponents(main, help);

		addComponents(separator, toolbar, wrapper, paramsList);
		setSpacing(false);
		setMargin(false);

		if (editMode)
		{
			condition.setValue(toEdit.getCondition().getCondition());
			actions.setValue(toEdit.getAction().getName());
			setParams(actions.getValue().toString(), toEdit.getAction().getParameters());
		} else
		{
			actionParams.setVisible(false);
		}

	}

	private void setParams(String action, String[] values)
	{

		paramsList.removeAllComponents();
		if (action == null)
		{
			actionParams.setVisible(false);
			helpAction.setDescription(msg
					.getMessage("TranslationProfileEditor.helpEmptyAction"));
			return;
		}
		
		TranslationActionFactory factory = getActionFactory(action);
		if (factory == null)
		{	
			return;
		}
		
		helpAction.setDescription(factory.getDescription());
		ActionParameterDesc[] params = factory.getParameters();	
		for (int i = 0; i < params.length; i++)
		{
			AbstractTextField p = null;
			if (params[i].isMandatory())
			{
				p = new RequiredTextField(params[i].getName() + ":", msg);
				p.setValidationVisible(false);

			} else
			{
				p = new TextField(params[i].getName() + ":");
			}
			p.setDescription(params[i].getDescription());
			if (values != null && values[i] != null)
			{
				p.setValue(values[i]);
			}		
			paramsList.addComponent(p);

		}
		actionParams.setVisible(paramsList.getComponentCount() != 0);
	}

	public TranslationRule getRule()
	{
		String ac = (String) actions.getValue();
		if (ac == null)
			return null;

		TranslationActionFactory factory = getActionFactory(ac);	
		ArrayList<String> params = new ArrayList<String>();
		for (int i = 0; i < paramsList.getComponentCount(); i++)
		{
			AbstractTextField tc = (AbstractTextField) paramsList.getComponent(i);
			String val = tc.getValue();
			if (tc.isRequired())
			{
				params.add(val);
			}
		}
		String[] wrapper = new String[params.size()];
		params.toArray(wrapper);
		TranslationAction action = null;
		try
		{
			
			action = factory.getInstance(wrapper);

		} catch (EngineException e)
		{
			ErrorPopup.showError(msg,
					msg.getMessage("TranslationProfileEditor.errorGetAction"),
					e);
		}
		TranslationCondition cnd = new TranslationCondition();
		cnd.setCondition(condition.getValue());
		TranslationRule rule = new TranslationRule(action, cnd);
		return rule;

	}
	
	private TranslationActionFactory getActionFactory(String action)
	{
		TranslationActionFactory factory = null;
		try
		{
			factory = tc.getByName(action);

		} catch (EngineException e)
		{
			ErrorPopup.showError(msg,
					msg.getMessage("TranslationProfileEditor.errorGetActions"),
					e);
		}
		return factory;
		
	}
	

	public void setUpVisible(boolean v)
	{
		up.setVisible(v);
	}

	public void setDownVisible(boolean v)
	{
		down.setVisible(v);
	}
	
	public void setTopVisible(boolean v)
	{
		top.setVisible(v);
	}

	public void setBottomVisible(boolean v)
	{
		bottom.setVisible(v);
	}
	
	public void setFocus()
	{
		condition.focus();
	}

	public boolean validateRule()
	{
		int nval = 0;
		for (int i = 0; i < paramsList.getComponentCount(); i++)
		{
			AbstractTextField tc = (AbstractTextField) paramsList.getComponent(i);
			tc.setValidationVisible(true);
			if (!tc.isValid())
			{
				nval++;
			}

		}

		condition.setValidationVisible(true);
		if (!condition.isValid())
		{
			nval++;
		}

		actions.setValidationVisible(true);
		if (!actions.isValid())
		{
			nval++;
		}

		return nval == 0;

	}

	public interface Callback
	{
		public boolean moveUp(RuleComponent rule);
		public boolean moveDown(RuleComponent rule);
		public boolean remove(RuleComponent rule);
		public boolean moveTop(RuleComponent rule);
		public boolean moveBottom(RuleComponent rule);
	}

}
