/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.tprofile;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.EffectAllowed;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import io.imunity.vaadin.elements.*;
import io.imunity.vaadin.endpoint.common.ExceptionMessageHumanizer;
import io.imunity.vaadin.endpoint.common.api.HtmlTooltipFactory;
import io.imunity.vaadin.endpoint.common.mvel.MVELExpressionField;
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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static io.imunity.vaadin.elements.CssClassNames.*;

public class RuleComponent extends VerticalLayout
{
	private final MessageSource msg;
	private final TypesRegistryBase<? extends TranslationActionFactory<?>> tc;
	private ActionEditor actionEditor;
	private MVELExpressionField condition;
	private MappingResultComponent mappingResultComponent;
	private final Callback callback;
	private MenuItem top;
	private MenuItem bottom;
	private final boolean editMode;
	private final ActionParameterComponentProvider actionComponentProvider;
	private FormLayout content;
	private Span info;
	private LinkButton showHide;
	private Binder<TranslationRule> binder;
	private LinkButton dragImg;
	private Component menuBar;
	private MenuItem embedProfileMenuItem;
	private final ProfileType profileType;
	private final NotificationPresenter notificationPresenter;
	private final HtmlTooltipFactory htmlTooltipFactory;

	public RuleComponent(MessageSource msg, TypesRegistryBase<? extends TranslationActionFactory<?>> tc,
						 TranslationRule toEdit, ActionParameterComponentProvider actionComponentProvider, ProfileType profileType,
						 Callback callback, NotificationPresenter notificationPresenter, HtmlTooltipFactory htmlTooltipFactory)
	{
		this.callback = callback;
		this.msg = msg;
		this.tc = tc;
		this.actionComponentProvider = actionComponentProvider;
		this.profileType = profileType;
		this.notificationPresenter = notificationPresenter;
		this.htmlTooltipFactory = htmlTooltipFactory;
		editMode = toEdit != null;
		initUI(toEdit);
	}

	private void initUI(TranslationRule toEdit)
	{
		VerticalLayout headerWrapper = new VerticalLayout();
		headerWrapper.setMargin(false);
		headerWrapper.setSpacing(false);
		headerWrapper.setPadding(false);
		
		HorizontalLayout header = new HorizontalLayout();
		header.setSizeFull();
		header.setMargin(false);
		header.setPadding(false);
	
		showHide = new LinkButton("", event -> showHideContent(!content.isVisible()));
		showHide.add(VaadinIcon.ANGLE_DOWN.create());
		header.add(showHide);
		header.setAlignItems(Alignment.CENTER);
		
		info = new Span("");
		info.setSizeFull();
		header.add(info);

		dragImg = new LinkButton("", e -> {});
		dragImg.add(VaadinIcon.RESIZE_H.create());
		dragImg.setSizeFull();
		dragImg.setWidth(1, Unit.EM);

		DragSource<LinkButton> dragSource =  DragSource.create(dragImg);
		dragSource.setEffectAllowed(EffectAllowed.MOVE);
		dragSource.setDragData(this);
		
		header.add(dragImg);

		ActionMenu actionMenu = new ActionMenu();
		actionMenu.addItem(new MenuButton(msg.getMessage("TranslationProfileEditor.remove"),
				VaadinIcon.TRASH), s -> callback.remove(RuleComponent.this));
		embedProfileMenuItem = actionMenu.addItem(new MenuButton(msg.getMessage("TranslationProfileEditor.embedProfile"),
				VaadinIcon.EXPAND_SQUARE), e -> onEmbedProfileAction(embedProfileMenuItem));
		embedProfileMenuItem.setVisible(false);
		top = actionMenu.addItem(new MenuButton(msg.getMessage("TranslationProfileEditor.moveTop"),
				VaadinIcon.ARROW_UP),
				s -> callback.moveTop(RuleComponent.this));	
		bottom = actionMenu.addItem(new MenuButton(msg.getMessage("TranslationProfileEditor.moveBottom"),
				VaadinIcon.ARROW_DOWN),
				s -> callback.moveBottom(RuleComponent.this));
		menuBar = actionMenu.getTarget();

		header.add(menuBar);
				
		header.addClickListener(event ->
		{
			if (event.getClickCount() != 2)
				return;
			showHideContent(!content.isVisible());			
		});
		
		headerWrapper.add(header);
		headerWrapper.add(new Hr());
			
		condition = new MVELExpressionField(msg,
				msg.getMessage("MVELExpressionField.conditionDesc"),
				MVELExpressionContext.builder().withTitleKey("TranslationProfileEditor.ruleConditionTitle")
						.withEvalToKey("MVELExpressionField.evalToBoolean").withVars(getConditionContextVars())
						.build(), htmlTooltipFactory);
		
		actionEditor = new ActionEditor(msg, tc, toEdit == null ? null : toEdit.getAction(),
				actionComponentProvider, this::onActionChanged, notificationPresenter);
		
		mappingResultComponent = new MappingResultComponent(msg);	
		
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		main.setPadding(false);
		content = new FormLayout();
		content.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		content.addFormItem(condition, msg.getMessage("TranslationProfileEditor.ruleCondition"));
		actionEditor.addToLayout(content);
		content.add(mappingResultComponent);
		showHideContent(false);
		
		main.add(headerWrapper);
		main.add(content);
	
		info.setText(actionEditor.getStringRepresentation());
		
		binder = new Binder<>(TranslationRule.class);
		condition.configureBinding(binder, "condition", true);		
		binder.setBean(new TranslationRule(editMode? Objects.requireNonNull(toEdit).getCondition() : "true", null));
		
		add(main);
		setMargin(false);
		setPadding(false);
	}
	
	private Map<String, String> getConditionContextVars()
	{
		return switch (profileType)
		{
			case BULK_ENTITY_OPS -> EntityMVELContextKey.toMap();
			case INPUT -> InputTranslationMVELContextKey.toMap();
			case OUTPUT -> OutputTranslationMVELContextKey.toMap();
			case REGISTRATION -> RegistrationMVELContextKey.toMap();
		};
	}

	private void onEmbedProfileAction(MenuItem item)
	{
		TranslationAction action;
		try
		{
			action = actionEditor.getAction();
		} catch (FormValidationException e)
		{
			notificationPresenter.showError(msg.getMessage("Generic.formError"), msg.getMessage("Generic.formErrorHint"));
			return;
		}
		String profile = action.getParameters()[0];
		ProfileType profileType = IncludeInputProfileActionFactory.NAME.equals(action.getName()) ? 
				ProfileType.INPUT : ProfileType.OUTPUT;
		callback.embedProfile(RuleComponent.this, profile, profileType);
	}
	
	private void onActionChanged(String actionStr, Optional<TranslationAction> action)
	{
		info.setText(actionStr);
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
			setLayoutForEvaluatedCondition(result);
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
	
	private void setLayoutForEvaluatedCondition(boolean conditionResult)
	{
		removeRuleComponentEvaluationStyle();
		setColorForInputComponents((conditionResult ?
				TRUE_CONDITION_BACKGROUND.getName() : FALSE_CONDITION_BACKGROUND.getName()));
	}
	
	private void displayMappingResult(MappingResult mappingResult) 
	{
		mappingResultComponent.displayMappingResult(mappingResult);
		mappingResultComponent.setVisible(true);
	}
	
	private void indicateConditionError(Exception e) 
	{
		condition.addClassNameToField(ERROR_BACKGROUND.getName());
		condition.setErrorMessage(ExceptionMessageHumanizer.getHumanReadableMessage(e));
	}


	public void clearTestResult() 
	{
		removeRuleComponentEvaluationStyle();	
		hideMappingResultComponent();
		setReadOnlyStyle(false);
	}
	
	private void setColorForInputComponents(String style)
	{
		condition.addClassNameToField(style);
		actionEditor.setEvaluationStyle(style);
	}
	
	private void removeRuleComponentEvaluationStyle()
	{
		condition.removeClassNameFromField(TRUE_CONDITION_BACKGROUND.getName());
		condition.removeClassNameFromField(ERROR_BACKGROUND.getName());
		condition.removeClassNameFromField(FALSE_CONDITION_BACKGROUND.getName());
		condition.setErrorMessage(null);
		
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
		showHide.removeAll();
		showHide.add(show ? VaadinIcon.ANGLE_DOWN.create() : VaadinIcon.ANGLE_UP.create());
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

	public void setEditorContext(EditorContext editorContext)
	{
		if (editorContext.equals(EditorContext.WIZARD))
		{	condition.setWidthFull();
			condition.addClassNameToField(CssClassNames.WIDTH_FULL.getName());
		}
		actionEditor.setContext(editorContext);
		
	}
}
