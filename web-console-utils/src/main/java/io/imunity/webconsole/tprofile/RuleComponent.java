/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.tprofile;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.vaadin.data.Binder;
import com.vaadin.server.UserError;
import com.vaadin.shared.ui.dnd.EffectAllowed;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.dnd.DragSourceExtension;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationAction;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.bulkops.EntityMVELContextKey;
import pl.edu.icm.unity.engine.api.mvel.MVELExpressionContext;
import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;
import pl.edu.icm.unity.engine.api.translation.TranslationCondition;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationMVELContextKey;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationContextFactory;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationMVELContextKey;
import pl.edu.icm.unity.engine.api.translation.in.MappingResult;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationMVELContextKey;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import pl.edu.icm.unity.engine.translation.in.action.IncludeInputProfileActionFactory;
import pl.edu.icm.unity.engine.translation.out.action.IncludeOutputProfileActionFactory;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.mvel.MVELExpressionField;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

/**
 * Responsible for editing of a single TranslationRule
 * 
 * @author P. Piernik
 * @contributor Roman Krysinski
 * 
 */
public class RuleComponent extends CustomComponent
{
	private MessageSource msg;
	private TypesRegistryBase<? extends TranslationActionFactory<?>> tc;
	private ActionEditor actionEditor;
	private MVELExpressionField condition;
	private MappingResultComponent mappingResultComponent;
	private Callback callback;
	private MenuItem top;
	private MenuItem bottom;
	private boolean editMode;
	private ActionParameterComponentProvider actionComponentProvider;
	private FormLayout content;
	private Label info;
	private Button showHide;
	private Binder<TranslationRule> binder;
	private Button dragImg;
	private HamburgerMenu<String> menuBar;
	private MenuItem embedProfileMenuItem;
	private final ProfileType profileType;
	
	public RuleComponent(MessageSource msg, TypesRegistryBase<? extends TranslationActionFactory<?>> tc,
			TranslationRule toEdit, ActionParameterComponentProvider actionComponentProvider, ProfileType profileType,
			Callback callback)
	{
		this.callback = callback;
		this.msg = msg;
		this.tc = tc;
		this.actionComponentProvider = actionComponentProvider;
		this.profileType = profileType;
		editMode = toEdit != null;
		initUI(toEdit);
	}

	private void initUI(TranslationRule toEdit)
	{
		VerticalLayout headerWrapper = new VerticalLayout();
		headerWrapper.setMargin(false);
		headerWrapper.setSpacing(false);
		
		HorizontalLayout header = new HorizontalLayout();
		header.setSizeFull();
		header.setMargin(false);
	
		showHide = new Button(Images.downArrow.getResource());
		showHide.addStyleName(Styles.vButtonLink.toString());
		showHide.addStyleName(Styles.toolbarButton.toString());
		showHide.addStyleName(Styles.vButtonBorderless.toString());
		showHide.addClickListener(event -> showHideContent(!content.isVisible()));
		header.addComponent(showHide);
		header.setComponentAlignment(showHide, Alignment.MIDDLE_LEFT);
		
		info = new Label("");
		info.setSizeFull();
		header.addComponent(info);	
		header.setComponentAlignment(info, Alignment.MIDDLE_LEFT);
		header.setExpandRatio(info, 1);

		dragImg = new Button(Images.resize.getResource());
		dragImg.setSizeFull();
		dragImg.setWidth(1, Unit.EM);
		dragImg.setStyleName(Styles.vButtonLink.toString());
		dragImg.addStyleName(Styles.vButtonBorderless.toString());
		dragImg.addStyleName(Styles.link.toString());
		dragImg.addStyleName(Styles.dragButton.toString());
		
		DragSourceExtension<Button> dragSource = new DragSourceExtension<>(dragImg);
		dragSource.setEffectAllowed(EffectAllowed.MOVE);
		dragSource.setDragData(this);
		
		header.addComponent(dragImg);	
		header.setComponentAlignment(dragImg, Alignment.MIDDLE_RIGHT);
		
		menuBar = new HamburgerMenu<String>();			
		menuBar.addItem(msg.getMessage("TranslationProfileEditor.remove"), 
				Images.remove.getResource(), s -> callback.remove(RuleComponent.this));
		embedProfileMenuItem = menuBar.addItem(msg.getMessage("TranslationProfileEditor.embedProfile"), 
				Images.embed.getResource(), this::onEmbedProfileAction);
		top = menuBar.addItem(msg.getMessage("TranslationProfileEditor.moveTop"), 
				Images.topArrow.getResource(), 
				s -> callback.moveTop(RuleComponent.this));	
		bottom = menuBar.addItem(msg.getMessage("TranslationProfileEditor.moveBottom"), 
				Images.bottomArrow.getResource(), 
				s -> callback.moveBottom(RuleComponent.this));
		

		header.addComponent(menuBar);
		header.setComponentAlignment(menuBar, Alignment.MIDDLE_RIGHT);
		header.setExpandRatio(menuBar, 0);
				
		header.addLayoutClickListener(event ->
		{
			if (!event.isDoubleClick())
				return;
			showHideContent(!content.isVisible());			
		});
		
		headerWrapper.addComponent(header);
		headerWrapper.addComponent(HtmlTag.horizontalLine());
			
		condition = new MVELExpressionField(msg, msg.getMessage("TranslationProfileEditor.ruleCondition"),
				msg.getMessage("MVELExpressionField.conditionDesc"),
				MVELExpressionContext.builder().withTitleKey("TranslationProfileEditor.ruleConditionTitle")
						.withEvalToKey("MVELExpressionField.evalToBoolean").withVars(getConditionContextVars())
						.build());
		condition.setStyleName(Styles.vTiny.toString());
		condition.setWidth(100, Unit.PERCENTAGE);
		actionEditor = new ActionEditor(msg, tc, toEdit == null ? null : toEdit.getAction(),
				actionComponentProvider, this::onActionChanged);
		
		mappingResultComponent = new MappingResultComponent(msg);	
		
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		content = new FormLayout();	
		content.addComponent(condition);
		actionEditor.addToLayout(content);
		content.addComponents(mappingResultComponent);
		content.setMargin(false);
		content.setSpacing(true);
		showHideContent(false);
		
		main.addComponent(headerWrapper);
		main.addComponent(content);
	
		setCompositionRoot(main);
		info.setValue(actionEditor.getStringRepresentation());
		
		binder = new Binder<>(TranslationRule.class);
		condition.configureBinding(binder, "condition", true);		
		binder.setBean(new TranslationRule(editMode? toEdit.getCondition() : "true", null));
	}
	
	private Map<String, String> getConditionContextVars()
	{
		switch(profileType) {
		case BULK_ENTITY_OPS:
			return EntityMVELContextKey.toMap();
		case INPUT:
			return InputTranslationMVELContextKey.toMap();
		case OUTPUT:
			return OutputTranslationMVELContextKey.toMap();
		case REGISTRATION:
			return RegistrationMVELContextKey.toMap();
		default:
			return new HashMap<>();		
		}
	}

	private void onEmbedProfileAction(MenuItem item)
	{
		TranslationAction action;
		try
		{
			action = actionEditor.getAction();
		} catch (FormValidationException e)
		{
			NotificationPopup.showFormError(msg);
			return;
		}
		String profile = action.getParameters()[0];
		ProfileType profileType = IncludeInputProfileActionFactory.NAME.equals(action.getName()) ? 
				ProfileType.INPUT : ProfileType.OUTPUT;
		callback.embedProfile(RuleComponent.this, profile, profileType);
	}
	
	private void onActionChanged(String actionStr, Optional<TranslationAction> action)
	{
		info.setValue(actionStr);
		if (action.isPresent())
		{
			String actionName = action.get().getName();
			boolean enableEmbed = IncludeInputProfileActionFactory.NAME.equals(actionName)
					|| IncludeOutputProfileActionFactory.NAME.equals(actionName);
			embedProfileMenuItem.setVisible(enableEmbed);
		} else
		{
			embedProfileMenuItem.setVisible(false);
		}
	}
	
	public void setReadOnlyMode(boolean readOnly)
	{
		condition.setReadOnly(readOnly);
		dragImg.setVisible(!readOnly);
		actionEditor.setReadOnlyStyle(readOnly);
		menuBar.setVisible(!readOnly);
	}

	public TranslationRule getRule() throws FormValidationException
	{	
		if (!binder.isValid())
		{
			binder.validate();
			throw new FormValidationException();
		}
		TranslationRule rule = binder.getBean();
		rule.setTranslationAction(actionEditor.getAction());
		return rule;
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
		showHideContent(true);
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
		
		if (!binder.isValid())
		{
			binder.validate();
			ok = false;
		}
		
		if (!ok)
			showHideContent(true);
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
			actionEditor.indicateExpressionError(e);
			return;
		}
		
		Map<String, Object> mvelCtx = InputTranslationContextFactory.createMvelContext(remoteAuthnInput);
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
			actionEditor.indicateExpressionError(e);
		}
	}
	
	private void setLayoutForEvaludatedCondition(boolean conditionResult) 
	{
		removeRuleComponentEvaluationStyle();
		setColorForInputComponents((conditionResult ? 
				Styles.trueConditionBackground : Styles.falseConditionBackground).toString());
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
	
	private void showHideContent(boolean show)
	{
		showHide.setIcon(show ? Images.upArrow.getResource()
				: Images.downArrow.getResource());
		content.setVisible(show);
	}
	
	public interface Callback
	{
		boolean remove(RuleComponent rule);
		boolean moveTop(RuleComponent rule);
		boolean moveBottom(RuleComponent rule);
		boolean embedProfile(RuleComponent rule, String profile, ProfileType profileType);
	}

	public void refresh()
	{
		actionEditor.refresh();
	}
}
