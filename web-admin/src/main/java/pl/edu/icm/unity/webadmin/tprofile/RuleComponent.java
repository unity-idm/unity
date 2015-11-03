/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.tprofile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.mvel2.MVEL;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.translation.AbstractTranslationRule;
import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.translation.ProfileType;
import pl.edu.icm.unity.server.translation.TranslationAction;
import pl.edu.icm.unity.server.translation.TranslationActionFactory;
import pl.edu.icm.unity.server.translation.TranslationCondition;
import pl.edu.icm.unity.server.translation.in.InputTranslationAction;
import pl.edu.icm.unity.server.translation.in.InputTranslationProfile;
import pl.edu.icm.unity.server.translation.in.InputTranslationRule;
import pl.edu.icm.unity.server.translation.in.MappingResult;
import pl.edu.icm.unity.server.translation.out.OutputTranslationAction;
import pl.edu.icm.unity.server.translation.out.OutputTranslationRule;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.RequiredComboBox;
import pl.edu.icm.unity.webui.common.RequiredTextField;
import pl.edu.icm.unity.webui.common.Styles;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.validator.AbstractStringValidator;
import com.vaadin.server.UserError;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Responsible for editing of a single TranslationRule
 * 
 * @author P. Piernik
 * @contributor Roman Krysinski
 * 
 */
public class RuleComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private ProfileType profileType;
	private TranslationActionsRegistry tc;
	private Collection<AttributeType> attributeTypes;
	private Collection<String> groups;
	private Collection<String> credReqs;
	private Collection<String> idTypes;
	private ComboBox actions;
	private AbstractTextField condition;
	private FormLayout paramsList;
	private MappingResultComponent mappingResultComponent;
	private Callback callback;
	private Button up;
	private Button top;
	private Button down;
	private Button bottom;
	private Label actionParams;
	private boolean editMode;
	private Image helpAction;

	public RuleComponent(ProfileType profileType, UnityMessageSource msg, TranslationActionsRegistry tc,
			AbstractTranslationRule<?> toEdit, Collection<AttributeType> attributeTypes, Collection<String> groups,
			Collection<String> credReqs, Collection<String> idTypes, Callback callback)
	{
		this.profileType = profileType;
		this.callback = callback;
		this.msg = msg;
		this.tc = tc;
		editMode = toEdit != null;
		this.attributeTypes = attributeTypes;
		this.groups = groups;
		this.credReqs = credReqs;
		this.idTypes = idTypes;
		initUI(toEdit);
	}

	private void initUI(AbstractTranslationRule<?> toEdit)
	{
		up = new Button();
		up.setDescription(msg.getMessage("TranslationProfileEditor.moveUp"));
		up.setIcon(Images.upArrow.getResource());
		up.addStyleName(Styles.vButtonLink.toString());
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
		top.addStyleName(Styles.vButtonLink.toString());
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
		down.setDescription(msg.getMessage("TranslationProfileEditor.moveDown"));
		down.setIcon(Images.downArrow.getResource());
		down.addStyleName(Styles.vButtonLink.toString());
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
		bottom.addStyleName(Styles.vButtonLink.toString());
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
		remove.addStyleName(Styles.vButtonLink.toString());
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

		paramsList = new CompactFormLayout();
		paramsList.setSpacing(true);

		condition = new RequiredTextField(msg);
		condition.setCaption(msg.getMessage("TranslationProfileEditor.ruleCondition"));
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

		actions = new RequiredComboBox(msg.getMessage("TranslationProfileEditor.ruleAction"), msg);
		for (TranslationActionFactory a : tc.getAll())
		{
			if (a.getSupportedProfileType() == profileType)
				actions.addItem(a.getName());
		}
		actions.setImmediate(true);
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

		Label separator = new Label();
		separator.addStyleName(Styles.horizontalLine.toString());

		FormLayout help = new CompactFormLayout();
		helpAction = new Image("", Images.help.getResource());
		helpAction.setDescription(msg.getMessage("TranslationProfileEditor.helpEmptyAction"));
		Image helpCondition = new Image("", Images.help.getResource());
		helpCondition.setDescription(msg.getMessage("TranslationProfileEditor.helpCondition"));
		help.addComponents(helpCondition, helpAction);
		help.setComponentAlignment(helpCondition, Alignment.TOP_LEFT);
		help.setComponentAlignment(helpAction, Alignment.BOTTOM_LEFT);
		help.setSpacing(true);

		HorizontalLayout wrapper = new HorizontalLayout();
		wrapper.setSpacing(false);
		wrapper.setMargin(false);
		actionParams = new Label();
		actionParams.setCaption(msg.getMessage("TranslationProfileEditor.actionParameters"));
		FormLayout main = new CompactFormLayout();
		main.setSpacing(true);
		main.setMargin(false);
		main.addComponents(condition, actions, actionParams);
		wrapper.addComponents(main, help);
		
		mappingResultComponent = new MappingResultComponent(msg);

		addComponents(separator, toolbar, wrapper, paramsList, mappingResultComponent);
		setSpacing(false);
		setMargin(false);

		if (editMode)
		{
			condition.setValue(toEdit.getCondition().getCondition());
			actions.setValue(toEdit.getAction().getActionDescription().getName());
			setParams(actions.getValue().toString(), toEdit.getAction().getParameters());
		} else
		{
			condition.setValue("true");
			actionParams.setVisible(false);
			if (actions.size() > 0)
			{
				actions.setValue(actions.getItemIds().toArray()[0]);
			}
		}

	}

	private void setParams(String action, String[] values)
	{
		paramsList.removeAllComponents();
		if (action == null)
		{
			actionParams.setVisible(false);
			helpAction.setDescription(msg.getMessage("TranslationProfileEditor.helpEmptyAction"));
			return;
		}
		
		TranslationActionFactory factory = getActionFactory(action);
		if (factory == null)
		{	
			return;
		}
		
		helpAction.setDescription(msg.getMessage(factory.getDescriptionKey()));
		ActionParameterDesc[] params = factory.getParameters();	
		for (int i = 0; i < params.length; i++)
		{
			ActionParameterComponent p = getParameterComponent(params[i]);
			p.setValidationVisible(false);
			if (values != null && values.length > i)
			{
				p.setActionValue(values[i]);
			}		
			paramsList.addComponent(p);
		}
		actionParams.setVisible(paramsList.getComponentCount() != 0);
	}

	private ActionParameterComponent getParameterComponent(ActionParameterDesc param)
	{
		switch (param.getType())
		{
		case ENUM:
			return new EnumActionParameterComponent(param, msg);
		case UNITY_ATTRIBUTE:
			return new AttributeActionParameterComponent(param, msg, attributeTypes);
		case UNITY_GROUP:
			return new BaseEnumActionParameterComponent(param, msg, groups);
		case UNITY_CRED_REQ:
			return new BaseEnumActionParameterComponent(param, msg, credReqs);
		case UNITY_ID_TYPE:
			return new BaseEnumActionParameterComponent(param, msg, idTypes);
		case EXPRESSION:
			return new ExtensionActionParameterComponent(param, msg);
		case DAYS:
			return new DaysActionParameterComponent(param, msg);
		case LARGE_TEXT:
			return new TextAreaActionParameterComponent(param, msg);
		default: 
			return new DefaultActionParameterComponent(param, msg);
		}
	}
	
	public AbstractTranslationRule<?> getRule() throws Exception
	{
		String ac = (String) actions.getValue();
		if (ac == null)
			return null;

		TranslationActionFactory factory = getActionFactory(ac);	
		TranslationAction action = factory.getInstance(getActionParams());
		TranslationCondition cnd = new TranslationCondition();
		cnd.setCondition(condition.getValue());			
		
		switch (profileType)
		{
		case INPUT:
			return new InputTranslationRule((InputTranslationAction) action, cnd);
		case OUTPUT:
			return new OutputTranslationRule((OutputTranslationAction) action, cnd);
		
		}
		throw new IllegalStateException("Not implemented");
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
			NotificationPopup.showError(msg,
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
		String ac = (String) actions.getValue();
		if (ac == null)
			return false;
		boolean ok = true;
		
		TranslationActionFactory factory = getActionFactory(ac);	
		try
		{
			factory.getInstance(getActionParams());
		} catch (Exception e)
		{
			UserError ue = new UserError(
					msg.getMessage("TranslationProfileEditor.parametersError", e.getMessage()));
			for (int i = 0; i < paramsList.getComponentCount(); i++)
				((AbstractComponent)paramsList.getComponent(i)).setComponentError(ue);
			ok = false;
		}
		
		condition.setComponentError(null);
		try 
		{
			TranslationCondition cnd = new TranslationCondition();
			cnd.setCondition(condition.getValue());			
		} catch (Exception e)
		{
			condition.setComponentError(new UserError(msg.getMessage(
					"TranslationProfileEditor.conditionError", e.getMessage())));
			ok = false;
		}
		
		return ok;
	}

	public void test(RemotelyAuthenticatedInput remoteAuthnInput) 
	{
		setReadOnlyStyle(true);
		InputTranslationRule rule = null;
		try 
		{
			rule = (InputTranslationRule) getRule();
		} catch (Exception e)
		{
			indicateExtensionError(e);
			return;
		}
		
		Map<String, Object> mvelCtx = InputTranslationProfile.createMvelContext(remoteAuthnInput);
		TranslationCondition conditionRule = rule.getCondition();
		try 
		{
			boolean result = conditionRule.evaluate(mvelCtx);
			setLayoutForEvaludatedCondition(result);
		} catch (EngineException e) 
		{
			indicateConditionError(e);
		}
		
		InputTranslationAction action = rule.getAction();
		try 
		{
			MappingResult mappingResult = action.invoke(remoteAuthnInput, mvelCtx, null);
			displayMappingResult(mappingResult);
		} catch (EngineException e) 
		{
			indicateExtensionError(e);
		}
	}
	
	private void setLayoutForEvaludatedCondition(boolean conditionResult) 
	{
		removeRuleComponentEvaluationStyle();
		if (conditionResult)
		{
			setColorForInputComponents(Styles.trueConditionBackground.toString());
		} else
		{
			setColorForInputComponents(Styles.falseConditionBackground.toString());
		}
	}
	
	private void displayMappingResult(MappingResult mappingResult) 
	{
		mappingResultComponent.displayMappingResult(mappingResult);
		mappingResultComponent.setVisible(true);
	}
	
	private void indicateConditionError(Exception e) 
	{
		condition.setStyleName(Styles.errorBackground.toString());
		condition.setComponentError(new UserError(NotificationPopup.getHumanMessage(e)));
		condition.setValidationVisible(true);
	}
	
	private void indicateExtensionError(Exception e) 
	{
		Iterator<Component> iter = paramsList.iterator();
		while (iter.hasNext())
		{
			Component c = iter.next();
			if (c instanceof ExtensionActionParameterComponent)
			{
				ExtensionActionParameterComponent extension = (ExtensionActionParameterComponent) c;
				extension.setStyleName(Styles.errorBackground.toString());
				extension.setComponentError(new UserError(NotificationPopup.getHumanMessage(e)));
				extension.setValidationVisible(true);
				break;
			}
		}	
	}

	public void clearTestResult() 
	{
		removeRuleComponentEvaluationStyle();	
		hideMappingResultComponent();
		setReadOnlyStyle(false);
	}
	
	private void setColorForInputComponents(String color)
	{
		condition.setStyleName(color);
		actions.setStyleName(color);
		setColorForParamList(color);
	}
	
	private void setColorForParamList(String color)
	{
		Iterator<Component> iter = paramsList.iterator();
		while (iter.hasNext())
		{
			Component c = iter.next();
			c.setStyleName(color);
		}		
	}
	
	private void removeRuleComponentEvaluationStyle()
	{
		condition.removeStyleName(Styles.trueConditionBackground.toString());
		condition.removeStyleName(Styles.errorBackground.toString());
		condition.removeStyleName(Styles.falseConditionBackground.toString());
		condition.setComponentError(null);
		condition.setValidationVisible(false);
		
		actions.removeStyleName(Styles.falseConditionBackground.toString());
		actions.removeStyleName(Styles.trueConditionBackground.toString());
		
		Iterator<Component> iter = paramsList.iterator();
		while (iter.hasNext())
		{
			Component c = iter.next();
			c.removeStyleName(Styles.falseConditionBackground.toString());
			c.removeStyleName(Styles.trueConditionBackground.toString());
			c.removeStyleName(Styles.errorBackground.toString());
			if (c instanceof ExtensionActionParameterComponent)
			{
				ExtensionActionParameterComponent extension = (ExtensionActionParameterComponent) c;
				extension.setComponentError(null);
				extension.setValidationVisible(false);
			}			
		}	
	}
	
	private void hideMappingResultComponent() 
	{
		mappingResultComponent.setVisible(false);
	}
	
	private void setReadOnlyStyle(boolean readOnly)
	{
		condition.setReadOnly(readOnly);
		actions.setReadOnly(readOnly);
		Iterator<Component> iter = paramsList.iterator();
		while (iter.hasNext())
		{
			Component c = iter.next();
			c.setReadOnly(readOnly);
		}			
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
