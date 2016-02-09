/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.tprofile;

import java.util.Map;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.registries.TypesRegistryBase;
import pl.edu.icm.unity.server.translation.TranslationActionFactory;
import pl.edu.icm.unity.server.translation.TranslationActionInstance;
import pl.edu.icm.unity.server.translation.TranslationCondition;
import pl.edu.icm.unity.server.translation.TranslationRuleInstance;
import pl.edu.icm.unity.server.translation.in.InputTranslationAction;
import pl.edu.icm.unity.server.translation.in.InputTranslationProfile;
import pl.edu.icm.unity.server.translation.in.MappingResult;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.translation.TranslationRule;
import pl.edu.icm.unity.webadmin.tprofile.ActionParameterComponentFactory.Provider;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;

import com.vaadin.server.UserError;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Responsible for editing of a single TranslationRule
 * 
 * @author P. Piernik
 * @contributor Roman Krysinski
 * 
 */
public class RuleComponent extends CustomComponent
{
	private UnityMessageSource msg;
	private TypesRegistryBase<? extends TranslationActionFactory> tc;
	private ActionEditor actionEditor;
	private AbstractTextField condition;
	private MappingResultComponent mappingResultComponent;
	private Callback callback;
	private Button up;
	private Button top;
	private Button down;
	private Button bottom;
	private boolean editMode;
	private Provider actionComponentProvider;

	public RuleComponent(UnityMessageSource msg, TypesRegistryBase<? extends TranslationActionFactory> tc,
			TranslationRuleInstance<?> toEdit, Provider actionComponentProvider, Callback callback)
	{
		this.callback = callback;
		this.msg = msg;
		this.tc = tc;
		this.actionComponentProvider = actionComponentProvider;
		editMode = toEdit != null;
		initUI(toEdit);
	}

	private void initUI(TranslationRuleInstance<?> toEdit)
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

		condition = new MVELExpressionField(msg, msg.getMessage("TranslationProfileEditor.ruleCondition"), 
				msg.getMessage("MVELExpressionField.conditionDesc"));

		actionEditor = new ActionEditor(msg, tc, toEdit == null ? null : toEdit.getAction(), 
				actionComponentProvider);

		Label separator = new Label();
		separator.addStyleName(Styles.horizontalLine.toString());

		mappingResultComponent = new MappingResultComponent(msg);

		VerticalLayout main = new VerticalLayout();
		FormLayout contents = new FormLayout();
		
		main.addComponents(separator, toolbar);
		
		contents.addComponent(condition);
		actionEditor.addToLayout(contents);
		contents.addComponents(mappingResultComponent);
		
		main.addComponent(contents);
		
		main.setSpacing(false);
		main.setMargin(false);
		setCompositionRoot(main);
		
		if (editMode)
		{
			condition.setValue(toEdit.getCondition());
		} else
		{
			condition.setValue("true");
		}
	}

	public TranslationRule getRule() throws FormValidationException
	{
		TranslationActionInstance action = actionEditor.getAction();
		TranslationCondition cnd = new TranslationCondition();
		cnd.setCondition(condition.getValue());			
		
		return new TranslationRule(condition.getValue(), action);
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
		boolean ok = true;
		try
		{
			actionEditor.getAction();
		} catch (Exception e)
		{
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
		TranslationRule rule = null;
		try 
		{
			rule = getRule();
		} catch (Exception e)
		{
			actionEditor.indicateExtensionError(e);
			return;
		}
		
		Map<String, Object> mvelCtx = InputTranslationProfile.createMvelContext(remoteAuthnInput);
		TranslationCondition conditionRule = new TranslationCondition(rule.getCondition());
		try 
		{
			boolean result = conditionRule.evaluate(mvelCtx);
			setLayoutForEvaludatedCondition(result);
		} catch (EngineException e) 
		{
			indicateConditionError(e);
		}
		
		InputTranslationAction action = (InputTranslationAction) rule.getAction();
		try 
		{
			MappingResult mappingResult = action.invoke(remoteAuthnInput, mvelCtx, null);
			displayMappingResult(mappingResult);
		} catch (EngineException e) 
		{
			actionEditor.indicateExtensionError(e);
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


	public void clearTestResult() 
	{
		removeRuleComponentEvaluationStyle();	
		hideMappingResultComponent();
		setReadOnlyStyle(false);
	}
	
	private void setColorForInputComponents(String style)
	{
		condition.setStyleName(style);
		actionEditor.setStyle(style);
	}
	
	private void removeRuleComponentEvaluationStyle()
	{
		condition.removeStyleName(Styles.trueConditionBackground.toString());
		condition.removeStyleName(Styles.errorBackground.toString());
		condition.removeStyleName(Styles.falseConditionBackground.toString());
		condition.setComponentError(null);
		condition.setValidationVisible(false);
		
		actionEditor.removeComponentEvaluationStyle();
	}
	
	private void hideMappingResultComponent() 
	{
		mappingResultComponent.setVisible(false);
	}
	
	private void setReadOnlyStyle(boolean readOnly)
	{
		condition.setReadOnly(readOnly);
		actionEditor.setReadOnlyStyle(readOnly);
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
