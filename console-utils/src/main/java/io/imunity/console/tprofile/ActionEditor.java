/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.tprofile;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static io.imunity.vaadin.elements.CssClassNames.ERROR_BACKGROUND;
import static io.imunity.vaadin.elements.CssClassNames.FALSE_CONDITION_BACKGROUND;
import static io.imunity.vaadin.elements.CssClassNames.TRUE_CONDITION_BACKGROUND;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.Logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.html.Span;

import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.ExceptionMessageHumanizer;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;
import pl.edu.icm.unity.base.translation.TranslationAction;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * Responsible for editing of a single {@link TranslationAction}
 */
public class ActionEditor extends FormLayoutEmbeddable
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, ActionEditor.class);
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	private final TypesRegistryBase<? extends TranslationActionFactory<?>> tc;

	private SelectWithDynamicTooltip<String> actions;
	private Span actionParams;
	private final ActionParameterComponentProvider actionComponentProvider;
	private final List<ActionParameterComponent> paramComponents = new ArrayList<>();
	private final BiConsumer<String, Optional<TranslationAction>> callback;
	private EditorContext editorContext = EditorContext.EDITOR;

	
	public ActionEditor(MessageSource msg, TypesRegistryBase<? extends TranslationActionFactory<?>> tc,
			TranslationAction toEdit, ActionParameterComponentProvider actionComponentProvider,
			BiConsumer<String, Optional<TranslationAction>> callback, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.tc = tc;
		this.actionComponentProvider = actionComponentProvider;
		this.callback = callback;
		this.notificationPresenter = notificationPresenter;
		initUI(toEdit);
	}
	
	
	public ActionEditor(MessageSource msg, TypesRegistryBase<? extends TranslationActionFactory<?>> tc,
						TranslationAction toEdit, ActionParameterComponentProvider actionComponentProvider,
						NotificationPresenter notificationPresenter)
	{
		this(msg, tc, toEdit, actionComponentProvider, null, notificationPresenter);
	}

	private void initUI(TranslationAction toEdit)
	{
		actions = new SelectWithDynamicTooltip<>();
		actions.setLabel(msg.getMessage("ActionEditor.ruleAction"));
		ArrayList<String> items = new ArrayList<>();
		tc.getAll().stream()
				.map(af -> af.getActionType().getName())
				.sorted()
				.forEach(items::add);
	
		actions.setItems(items);
		actions.setRequiredIndicatorVisible(true);
		actions.setWidth(TEXT_FIELD_MEDIUM.value());

		actionParams = new Span();
		actionParams.setText(msg.getMessage("ActionEditor.actionParameters"));

		addComponents(actions, actionParams);
		
		if (toEdit != null)
		{
			setInput(toEdit);
			callback.accept(getStringRepresentation(), getActionIfValid());

		} else
		{
			if (!items.isEmpty())
			{
				actions.setValue(items.iterator().next());
				setParams(actions.getValue(), null);
			}
		}
		actions.addValueChangeListener(e ->
		{
			setParams(actions.getValue(), null);
			if (callback != null)
				callback.accept(getStringRepresentation(), getActionIfValid());
		});
		
	
		
	}

	public void setInput(TranslationAction toEdit)
	{
		actions.setValue(toEdit.getName());
		setParams(actions.getValue(), toEdit.getParameters());
	}
	
	public Optional<TranslationAction> getActionIfValid()
	{
		try
		{
			return Optional.of(getAction());
		} catch (FormValidationException e)
		{
			return Optional.empty();
		}
	}
	
	private void setParams(String action, String[] values)
	{
		Runnable paramCallback = () -> {
			if (callback != null)
				callback.accept(getStringRepresentation(), getActionIfValid()); 
		};
		removeComponents(paramComponents.stream()
				.map(component -> (Component)component)
				.collect(toList()));
		paramComponents.clear();
		
		TranslationActionFactory<?> factory = getActionFactory(action);
		if (factory == null)
			return;
		
		actions.setTooltipText(msg.getMessage(factory.getActionType().getDescriptionKey()));
		ActionParameterDefinition[] params = factory.getActionType().getParameters();	
		for (int i = 0; i < params.length; i++)
		{
			ActionParameterComponent p = actionComponentProvider.getParameterComponent(params[i], editorContext);
			p.addValueChangeCallback(paramCallback);
			if (values != null && values.length > i)
			{
				p.setActionValue(values[i]);
			}		
			paramComponents.add(p);
			addComponent((Component) p);
		}
		actionParams.setVisible(!paramComponents.isEmpty());
	}

	private String[] getActionParams() throws FormValidationException
	{
		List<String> params = new ArrayList<>();
		boolean errors = false;
		for (ActionParameterComponent tc : paramComponents)
		{
			if (!tc.isValid())
			{
				errors = true;
			} else
			{
				params.add(tc.getActionValue());
			}

		}

		if (errors)
			throw new FormValidationException();
		String[] wrapper = new String[params.size()];
		return params.toArray(wrapper);
	}
	
	private TranslationActionFactory<?> getActionFactory(String action)
	{
		TranslationActionFactory<?> factory = null;
		try
		{
			factory = tc.getByName(action);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("ActionEditor.errorGetActions"), e.getMessage());
		}
		return factory;
	}
	

	public TranslationAction getAction() throws FormValidationException
	{
		TranslationActionFactory<?> factory = getActionFactory(actions.getValue());
		try
		{
			return factory.getInstance(getActionParams());
		} catch (FormValidationException e)
		{
			throw e;
		} catch (Exception e)
		{
			log.debug("Got profile's action validation exception", e);
			String error = msg.getMessage("ActionEditor.parametersError", e.getMessage());
			for (ActionParameterComponent tc: paramComponents)
				((Component)tc).getElement().setProperty("errorMessage", error);
			throw new FormValidationException(error);
		}
	}
	
	public void setReadOnlyStyle(boolean readOnly)
	{
		actions.setReadOnly(readOnly);
		for (ActionParameterComponent param: paramComponents)
			param.setReadOnly(readOnly);
	}
	
	
	public void indicateExpressionError(Exception e) 
	{
		for (ActionParameterComponent c: paramComponents)
		{
			if (c instanceof ExpressionActionParameterComponent extension)
			{
				extension.setErrorMessage(ExceptionMessageHumanizer.getHumanReadableMessage(e));
				extension.addClassNameToField(ERROR_BACKGROUND.getName());
			}
		}	
	}
	
	public void setEvaluationStyle(String style)
	{
		for (ActionParameterComponent c : paramComponents)
		{
			if (c instanceof ExpressionActionParameterComponent extension)
			{
				extension.addClassNameToField(style);
			}
		}
	}
	
	public void removeComponentEvaluationStyle()
	{
		actions.removeClassName(FALSE_CONDITION_BACKGROUND.getName());
		actions.removeClassName(TRUE_CONDITION_BACKGROUND.getName());
		
		for (ActionParameterComponent c: paramComponents)
		{
			((HasStyle)c).removeClassName(FALSE_CONDITION_BACKGROUND.getName());
			((HasStyle)c).removeClassName(TRUE_CONDITION_BACKGROUND.getName());
			((HasStyle)c).removeClassName(ERROR_BACKGROUND.getName());
			if (c instanceof ExpressionActionParameterComponent extension)
			{
				extension.setErrorMessage(null);	
				extension.removeClassNameFromField(FALSE_CONDITION_BACKGROUND.getName());
				extension.removeClassNameFromField(TRUE_CONDITION_BACKGROUND.getName());
				extension.removeClassNameFromField(ERROR_BACKGROUND.getName());
			}			
		}	
	}
	
	public String getStringRepresentation()
	{
		StringBuilder rep = new StringBuilder();
		rep.append(actions.getValue());
		rep.append("|");
		for (ActionParameterComponent tc: paramComponents)
		{
			String caption = tc.getLabel();
			if (caption != null && !caption.endsWith(":"))
				caption = caption + ":";
			rep.append(caption).append(" ").append(tc.getActionValueRepresentation(msg) != null
					&& !tc.getActionValue().equals("null")
					? tc.getActionValueRepresentation(msg)
					: " ");
			rep.append("|");
		}
		
		return rep.substring(0, rep.length() - 1);
	}


	public void refresh()
	{
		setParams(actions.getValue(), paramComponents.stream().map(ActionParameterComponent::getActionValue)
				.toArray(String[]::new));
		if (callback != null)
			callback.accept(getStringRepresentation(), getActionIfValid());
	}


	public void setContext(EditorContext context)
	{
		editorContext = context;
	}
}


