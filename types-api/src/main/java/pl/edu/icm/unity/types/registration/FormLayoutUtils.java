/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.registration.layout.BasicFormElement;
import pl.edu.icm.unity.types.registration.layout.FormCaptionElement;
import pl.edu.icm.unity.types.registration.layout.FormElement;
import pl.edu.icm.unity.types.registration.layout.FormLayout;
import pl.edu.icm.unity.types.registration.layout.FormLayoutType;
import pl.edu.icm.unity.types.registration.layout.FormParameterElement;
import pl.edu.icm.unity.types.registration.layout.FormRemoteSignupElement;
import pl.edu.icm.unity.types.registration.layout.FormSeparatorElement;

/**
 * Utility class to deal with most common operations like form validation or
 * generating default layout.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public final class FormLayoutUtils
{
	public static List<FormElement> getDefaultFormLayoutElementsWithoutCredentials(BaseForm form, MessageSource msg)
	{
		return getDefaultFormLayoutElements(form, msg, false);
	}
	
	public static List<FormElement> getDefaultFormLayoutElements(BaseForm form, MessageSource msg)
	{
		return getDefaultFormLayoutElements(form, msg, true);
	}
	
	private static List<FormElement> getDefaultFormLayoutElements(BaseForm form, MessageSource msg, boolean withCredentials)
	{
		List<FormElement> elements = new ArrayList<>();
		elements.addAll(getDefaultParametersLayout(FormLayoutType.IDENTITY, form.getIdentityParams(), msg, 
				"RegistrationRequest.identities", "RegistrationRequest.externalIdentities"));
		if (withCredentials)
		{
			elements.addAll(getDefaultBasicParamsLayout(FormLayoutType.CREDENTIAL, form.getCredentialParams(), msg, 
					"RegistrationRequest.credentials", true));
		}
		elements.addAll(getDefaultParametersLayout(FormLayoutType.ATTRIBUTE, form.getAttributeParams(), msg, 
				"RegistrationRequest.attributes", "RegistrationRequest.externalAttributes"));
		elements.addAll(getDefaultParametersLayout(FormLayoutType.GROUP, form.getGroupParams(), msg, 
				"RegistrationRequest.groups", "RegistrationRequest.externalGroups"));
		if (form.isCollectComments())
			elements.add(new BasicFormElement(FormLayoutType.COMMENTS));
		elements.addAll(getDefaultBasicParamsLayout(FormLayoutType.AGREEMENT, form.getAgreements(), msg, 
				"RegistrationRequest.agreements", true));
		return elements;
	}
	
	private static List<FormElement> getDefaultBasicParamsLayout(FormLayoutType type, List<?> params, 
			MessageSource msg, String captionKey, boolean addSeparator)
	{
		List<FormElement> ret = new ArrayList<>();
		if (!params.isEmpty())
			ret.add(new FormCaptionElement(new I18nString(captionKey, msg)));
		for (int i=0; i<params.size(); i++)
		{
			if (addSeparator && i > 0)
				ret.add(new FormSeparatorElement());
			ret.add(new FormParameterElement(type, i));
		}
		return ret;
	}
	
	private static List<FormElement> getDefaultParametersLayout(FormLayoutType type, List<? extends RegistrationParam> params, 
			MessageSource msg, String captionKey, String readOnlyCaptionKey)
	{
		List<FormElement> ret = new ArrayList<>();
		
		for (int i=0; i<params.size(); i++)
		{
			RegistrationParam param = params.get(i);
			if (param.getRetrievalSettings().isInteractivelyEntered(false))
				ret.add(new FormParameterElement(type, i));
		}
		
		if (!ret.isEmpty())
			ret.add(0, new FormCaptionElement(new I18nString(captionKey, msg)));

		int interactiveSize = ret.size();
		for (int i=0; i<params.size(); i++)
		{
			RegistrationParam param = params.get(i);
			if (param.getRetrievalSettings().isPotentiallyAutomaticAndVisible())
				ret.add(new FormParameterElement(type, i));
		}
		
		if (interactiveSize < ret.size())
			ret.add(interactiveSize, new FormCaptionElement(new I18nString(readOnlyCaptionKey, msg)));
		
		return ret;
	}
	
	/**
	 * removes all elements in layout that are not present in form and adds all form elements missing in layout
	 * at the end of it.
	 */
	public static void updateLayout(FormLayout layout, BaseForm form)
	{
		if (layout == null)
			return;
		
		Set<String> definedElements = getDefinedElements(layout);
		updateFormParametersInLayout(layout, form, definedElements);
		updateOtherElementsInLayout(layout, form, definedElements);
	}
	
	private static void updateFormParametersInLayout(FormLayout layout, BaseForm form, Set<String> definedElements)
	{
		for (int i = 0; i < form.getIdentityParams().size(); i++)
			addParameterIfMissing(layout, FormLayoutType.IDENTITY, i, definedElements);
		for (int i = 0; i < form.getAttributeParams().size(); i++)
			addParameterIfMissing(layout, FormLayoutType.ATTRIBUTE, i, definedElements);
		for (int i = 0; i < form.getAgreements().size(); i++)
			addParameterIfMissing(layout, FormLayoutType.AGREEMENT, i, definedElements);
		for (int i = 0; i < form.getGroupParams().size(); i++)
			addParameterIfMissing(layout, FormLayoutType.GROUP, i, definedElements);
		for (int i = 0; i < form.getCredentialParams().size(); i++)
			addParameterIfMissing(layout, FormLayoutType.CREDENTIAL, i, definedElements);
		
		removeParametersWithIndexLargerThen(layout, FormLayoutType.IDENTITY, form.getIdentityParams().size());
		removeParametersWithIndexLargerThen(layout, FormLayoutType.ATTRIBUTE, form.getAttributeParams().size());
		removeParametersWithIndexLargerThen(layout, FormLayoutType.AGREEMENT, form.getAgreements().size());
		removeParametersWithIndexLargerThen(layout, FormLayoutType.GROUP, form.getGroupParams().size());
		removeParametersWithIndexLargerThen(layout, FormLayoutType.CREDENTIAL, form.getCredentialParams().size());
	}
	
	private static void updateOtherElementsInLayout(FormLayout layout, BaseForm form, Set<String> definedElements)
	{
		if (form.isCollectComments())
			addBasicElementIfMissing(layout, FormLayoutType.COMMENTS, definedElements);
		else
			removeBasicElementIfPresent(layout, FormLayoutType.COMMENTS);
		
		if (form instanceof RegistrationForm)
		{
			RegistrationForm registrationform = (RegistrationForm) form;
			if (registrationform.getCaptchaLength() > 0)
				addBasicElementIfMissing(layout, FormLayoutType.CAPTCHA, definedElements);
			else
				removeBasicElementIfPresent(layout, FormLayoutType.CAPTCHA);
			
			if (registrationform.getRegistrationCode() != null)
				addBasicElementIfMissing(layout, FormLayoutType.REG_CODE, definedElements);
			else
				removeBasicElementIfPresent(layout, FormLayoutType.REG_CODE);
		}
	}
	
	public static void validatePrimaryLayout(RegistrationForm form)
	{
		FormLayout layout = form.getFormLayouts().getPrimaryLayout();
		if (form.getFormLayouts().isLocalSignupEmbeddedAsButton())
		{
			Set<String> definedElements = getDefinedElements(layout);
			checkLayoutElement(FormLayoutType.LOCAL_SIGNUP.name(), definedElements);
			checkRemoteSignupElements(form, definedElements);
			if (!definedElements.isEmpty())
				throw new IllegalStateException("Form layout contains elements "
						+ "which are not defied in the form: " + definedElements);
		} else
		{
			validateLayout(layout, form);
		}
	}
	
	public static void validateSecondaryLayout(RegistrationForm form)
	{
		FormLayout layout = form.getFormLayouts().getSecondaryLayout();
		if (form.getFormLayouts().isLocalSignupEmbeddedAsButton())
		{
			validateLayout(layout, form, true, false);
		} else
		{
			validateLayout(layout, form, false, false);
		}
	}
	
	public static void validateLayout(FormLayout layout, BaseForm form)
	{
		validateLayout(layout, form, true, true);
	}
	
	private static void validateLayout(FormLayout layout, BaseForm form, boolean withCredentials, boolean withRemoteSignup)
	{
		Set<String> definedElements = getDefinedElements(layout);
		
		checkFormParametersInLayout(layout, form, definedElements, withCredentials);
		checkOtherElementsInLayout(form, definedElements, withRemoteSignup);
		if (!definedElements.isEmpty())
			throw new IllegalStateException("Form layout contains elements "
					+ "which are not defied in the form: " + definedElements);
	}
	
	private static void checkFormParametersInLayout(FormLayout layout, BaseForm form, Set<String> definedElements, 
			boolean withCredentials)
	{
		for (int i = 0; i < form.getIdentityParams().size(); i++)
			checkLayoutElement(getIdOfElement(FormLayoutType.IDENTITY, i), definedElements);
		for (int i = 0; i < form.getAttributeParams().size(); i++)
			checkLayoutElement(getIdOfElement(FormLayoutType.ATTRIBUTE, i), definedElements);
		for (int i = 0; i < form.getAgreements().size(); i++)
			checkLayoutElement(getIdOfElement(FormLayoutType.AGREEMENT, i), definedElements);
		for (int i = 0; i < form.getGroupParams().size(); i++)
			checkLayoutElement(getIdOfElement(FormLayoutType.GROUP, i), definedElements);
		if (withCredentials)
		{
			for (int i = 0; i < form.getCredentialParams().size(); i++)
				checkLayoutElement(getIdOfElement(FormLayoutType.CREDENTIAL, i), definedElements);
		}
	}
	
	private static void checkOtherElementsInLayout(BaseForm form, Set<String> definedElements, boolean withRemoteSignup)
	{
		if (form.isCollectComments())
			checkLayoutElement(FormLayoutType.COMMENTS.name(), definedElements);
		
		if (form instanceof RegistrationForm)
		{
			RegistrationForm registrationform = (RegistrationForm) form;
			if (registrationform.getCaptchaLength() > 0)
				checkLayoutElement(FormLayoutType.CAPTCHA.name(), definedElements);
			if (registrationform.getRegistrationCode() != null)
				checkLayoutElement(FormLayoutType.REG_CODE.name(), definedElements);
			if (withRemoteSignup)
			{
				checkRemoteSignupElements(registrationform, definedElements);
			}
		}
	}
	
	private static void checkRemoteSignupElements(RegistrationForm registrationform, Set<String> definedElements)
	{
		for (int i = 0; i < registrationform.getExternalSignupSpec().getSpecs().size(); i++)
			checkLayoutElement(getIdOfElement(FormLayoutType.REMOTE_SIGNUP, i), definedElements);
	}
	
	private static void removeParametersWithIndexLargerThen(FormLayout layout, FormLayoutType type, int size)
	{
		Iterator<FormElement> iterator = layout.getElements().iterator();
		while (iterator.hasNext())
		{
			FormElement formElement = iterator.next();
			if (formElement.getType().equals(type) && 
					((FormParameterElement)formElement).getIndex() >= size)
				iterator.remove();
		}
	}

	private static void removeBasicElementIfPresent(FormLayout layout, FormLayoutType type)
	{
		for (int i = 0; i < layout.getElements().size(); i++)
		{
			FormElement formElement = layout.getElements().get(i);
			if (formElement.getType().equals(type))
			{
				layout.getElements().remove(i);
				return;
			}
		}
	}

	private static Set<String> getDefinedElements(FormLayout layout)
	{
		Set<String> definedElements = new HashSet<>();
		for (FormElement element: layout.getElements())
		{
			String id = getIdOfElement(element);
			if (id != null)
				definedElements.add(id);
		}
		return definedElements;
	}
	
	private static void checkLayoutElement(String key, Set<String> definedElements)
	{
		if (!definedElements.remove(key))
			throw new IllegalStateException("Form layout does not define position of " + key);
	}

	private static void addParameterIfMissing(FormLayout layout, FormLayoutType type, int index, Set<String> definedElements)
	{
		if (!definedElements.contains(getIdOfElement(type, index)))
			layout.getElements().add(new FormParameterElement(type, index));
	}
	
	private static String getIdOfElement(FormElement element)
	{
		if (!element.isFormContentsRelated())
			return null;
		if (element instanceof FormParameterElement)
			return getIdOfElement(element.getType(), ((FormParameterElement)element).getIndex());
		if (element instanceof FormRemoteSignupElement)
			return getIdOfElement(element.getType(), ((FormRemoteSignupElement)element).getIndex());
		return element.getType().name();
	}
	
	private static String getIdOfElement(FormLayoutType type, int idx)
	{
		return String.format("%s_%d", type.name(), idx);
	}
	
	private static void addBasicElementIfMissing(FormLayout layout, FormLayoutType type, Set<String> definedElements)
	{
		if (!definedElements.contains(type.name()))
			layout.getElements().add(new BasicFormElement(type));
	}
	
	private FormLayoutUtils()
	{
	}
}
