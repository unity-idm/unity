/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.types.registration.layout.BasicFormElement;
import pl.edu.icm.unity.types.registration.layout.FormCaptionElement;
import pl.edu.icm.unity.types.registration.layout.FormElement;
import pl.edu.icm.unity.types.registration.layout.FormLayout;
import pl.edu.icm.unity.types.registration.layout.FormLayoutElement;
import pl.edu.icm.unity.types.registration.layout.FormLocalSignupButtonElement;
import pl.edu.icm.unity.types.registration.layout.FormParameterElement;

/**
 * Utility class to deal with most common operations like form validation or
 * generating default layout.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public final class FormLayoutUtils
{
	public static boolean hasLocalSignupButton(FormLayout layout)
	{
		for (FormElement element : layout.getElements())
		{
			if (element.getType() == FormLayoutElement.LOCAL_SIGNUP)
				return true;
		}
		return false;
	}
	
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
		elements.addAll(getDefaultParametersLayout(FormLayoutElement.IDENTITY, form.getIdentityParams(), msg, 
				"RegistrationRequest.externalIdentities"));
		if (withCredentials)
		{
			elements.addAll(getDefaultBasicParamsLayout(FormLayoutElement.CREDENTIAL, form.getCredentialParams(), true));
		}
		elements.addAll(getDefaultParametersLayout(FormLayoutElement.ATTRIBUTE, form.getAttributeParams(), msg, 
				"RegistrationRequest.externalAttributes"));
		elements.addAll(getDefaultParametersLayout(FormLayoutElement.GROUP, form.getGroupParams(), msg, 
				"RegistrationRequest.externalGroups"));
		if (form.isCollectComments())
			elements.add(new BasicFormElement(FormLayoutElement.COMMENTS));
		elements.addAll(getDefaultBasicParamsLayout(FormLayoutElement.AGREEMENT, form.getAgreements(), true));
		return elements;
	}
	
	private static List<FormElement> getDefaultBasicParamsLayout(FormLayoutElement type, List<?> params, boolean addSeparator)
	{
		List<FormElement> ret = new ArrayList<>();
		for (int i=0; i<params.size(); i++)
		{
			ret.add(new FormParameterElement(type, i));
		}
		return ret;
	}
	
	private static List<FormElement> getDefaultParametersLayout(FormLayoutElement type, 
			List<? extends RegistrationParam> params, MessageSource msg, String readOnlyCaptionKey)
	{
		List<FormElement> ret = new ArrayList<>();
		
		for (int i=0; i<params.size(); i++)
		{
			RegistrationParam param = params.get(i);
			if (param.getRetrievalSettings().isInteractivelyEntered(false))
				ret.add(new FormParameterElement(type, i));
		}
		
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
	public static void updateRegistrationFormLayout(RegistrationFormLayouts layouts, RegistrationForm form)
	{
		updatePrimaryLayout(layouts, form);
		updateSecondaryLayout(layouts, form);
	}
	
	private static void updateSecondaryLayout(RegistrationFormLayouts layouts, RegistrationForm form)
	{
		if (layouts.getSecondaryLayout() == null)
			layouts.setSecondaryLayout(new FormLayout(new ArrayList<>()));
		
		FormLayout secondaryLayout = layouts.getSecondaryLayout();
		Set<String> definedElements = getDefinedElements(secondaryLayout);
		updateFormParametersInLayout(secondaryLayout, form, definedElements);
		updateOtherElementsInLayout(secondaryLayout, form, definedElements);
		if (!layouts.isLocalSignupEmbeddedAsButton())
			removeAllElements(secondaryLayout, FormLayoutElement.CREDENTIAL);
	}

	/**
	 * When local sign up, then only remote and local sign up elements along with captions.
	 */
	private static void updatePrimaryLayout(RegistrationFormLayouts layouts, RegistrationForm form)
	{
		FormLayout primaryLayout = layouts.getPrimaryLayout();
		if (primaryLayout == null)
			return;
		Set<String> definedElements = getDefinedElements(primaryLayout);

		if (form.isLocalSignupEnabled())
		{
			if (layouts.isLocalSignupEmbeddedAsButton())
			{
				addLocalSignupButtonElementIfMissing(primaryLayout, definedElements);
				for (FormLayoutElement type : FormLayoutElement.values())
				{
					if (type == FormLayoutElement.LOCAL_SIGNUP 
							|| type == FormLayoutElement.REMOTE_SIGNUP
							|| type == FormLayoutElement.REMOTE_SIGNUP_GRID
							|| type == FormLayoutElement.CAPTION)
						continue;
					removeAllElements(primaryLayout, type);
				}
			}
			else
			{
				removeBasicElementIfPresent(primaryLayout, FormLayoutElement.LOCAL_SIGNUP);
				updateFormParametersInLayout(primaryLayout, form, definedElements);
				updateOtherElementsInLayout(primaryLayout, form, definedElements);
			}
		} else
		{
			for (FormLayoutElement type : FormLayoutElement.values())
			{
				if (type == FormLayoutElement.REMOTE_SIGNUP
						|| type == FormLayoutElement.REMOTE_SIGNUP_GRID
						|| type == FormLayoutElement.CAPTION)
					continue;
				removeAllElements(primaryLayout, type);
			}
		}

		
		int externalSignUpSize = form.getExternalSignupSpec().getSpecs().size();
		List<AuthenticationOptionKey> gridSpecs = form.getExternalSignupGridSpec().getSpecs();

		for (int i = 0; i < externalSignUpSize; i++)
		{
			if (!gridSpecs.contains(form.getExternalSignupSpec().getSpecs().get(i)))
			{
				addParameterIfMissing(primaryLayout, FormLayoutElement.REMOTE_SIGNUP, i, definedElements);
			} else
			{
				removeParametersWithIndexIfPresent(primaryLayout, FormLayoutElement.REMOTE_SIGNUP, i);
			}
		}		
		removeParametersWithIndexLargerThen(primaryLayout, FormLayoutElement.REMOTE_SIGNUP, externalSignUpSize);


		if (form.getExternalSignupGridSpec().getSpecs().size() > 0)
		{
			addParameterIfMissing(primaryLayout, FormLayoutElement.REMOTE_SIGNUP_GRID, 0, definedElements);
		} else
		{
			removeParametersWithIndexIfPresent(primaryLayout, FormLayoutElement.REMOTE_SIGNUP_GRID, 0);
		}		
	}

	private static void removeAllElements(FormLayout layout, FormLayoutElement type)
	{
		Iterator<FormElement> iterator = layout.getElements().iterator();
		while(iterator.hasNext())
		{
			FormElement element = iterator.next();
			if (element.getType() == type)
				iterator.remove();
		}
	}

	/**
	 * removes all elements in layout that are not present in form and adds all form elements missing in layout
	 * at the end of it.
	 */
	public static void updateEnquiryLayout(FormLayout layout, EnquiryForm form)
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
			addParameterIfMissing(layout, FormLayoutElement.IDENTITY, i, definedElements);
		for (int i = 0; i < form.getAttributeParams().size(); i++)
			addParameterIfMissing(layout, FormLayoutElement.ATTRIBUTE, i, definedElements);
		for (int i = 0; i < form.getAgreements().size(); i++)
			addParameterIfMissing(layout, FormLayoutElement.AGREEMENT, i, definedElements);
		for (int i = 0; i < form.getGroupParams().size(); i++)
			addParameterIfMissing(layout, FormLayoutElement.GROUP, i, definedElements);
		for (int i = 0; i < form.getCredentialParams().size(); i++)
			addParameterIfMissing(layout, FormLayoutElement.CREDENTIAL, i, definedElements);
		
		removeParametersWithIndexLargerThen(layout, FormLayoutElement.IDENTITY, form.getIdentityParams().size());
		removeParametersWithIndexLargerThen(layout, FormLayoutElement.ATTRIBUTE, form.getAttributeParams().size());
		removeParametersWithIndexLargerThen(layout, FormLayoutElement.AGREEMENT, form.getAgreements().size());
		removeParametersWithIndexLargerThen(layout, FormLayoutElement.GROUP, form.getGroupParams().size());
		removeParametersWithIndexLargerThen(layout, FormLayoutElement.CREDENTIAL, form.getCredentialParams().size());
	}
	
	private static void updateOtherElementsInLayout(FormLayout layout, BaseForm form, Set<String> definedElements)
	{
		if (form.isCollectComments())
			addBasicElementIfMissing(layout, FormLayoutElement.COMMENTS, definedElements);
		else
			removeBasicElementIfPresent(layout, FormLayoutElement.COMMENTS);
		
		if (form instanceof RegistrationForm)
		{
			RegistrationForm registrationform = (RegistrationForm) form;
			if (registrationform.getCaptchaLength() > 0)
				addBasicElementIfMissing(layout, FormLayoutElement.CAPTCHA, definedElements);
			else
				removeBasicElementIfPresent(layout, FormLayoutElement.CAPTCHA);
			
			if (registrationform.getRegistrationCode() != null)
				addBasicElementIfMissing(layout, FormLayoutElement.REG_CODE, definedElements);
			else
				removeBasicElementIfPresent(layout, FormLayoutElement.REG_CODE);
		}
	}
	
	public static void validatePrimaryLayout(RegistrationForm form)
	{
		FormLayout layout = form.getFormLayouts().getPrimaryLayout();
		
		if (form.isLocalSignupEnabled())
		{
			if (form.getFormLayouts().isLocalSignupEmbeddedAsButton())
			{
				Set<String> definedElements = getDefinedElements(layout);
				checkLayoutElement(FormLayoutElement.LOCAL_SIGNUP.name(), definedElements);
				checkRemoteSignupElements(form, definedElements);

				if (!definedElements.isEmpty())
					throw new IllegalStateException("Form layout contains elements "
							+ "which are not defied in the form: " + definedElements);
			} else
			{
				validateLayout(layout, form, true, true);
			}
		} else
		{
			Set<String> definedElements = getDefinedElements(layout);
			checkRemoteSignupElements(form, definedElements);
			if (!definedElements.isEmpty())
				throw new IllegalStateException("Form layout contains elements "
						+ "which are not defied in the form: " + definedElements);
		}
	}
	
	public static void validateSecondaryLayout(RegistrationForm form)
	{
		FormLayout layout = form.getFormLayouts().getSecondaryLayout();
		if (RegistrationForm.isCredentialAvailableAtSecondaryFormLayout(form))
		{
			validateLayout(layout, form, true, false);
		} else
		{
			validateLayout(layout, form, false, false);
		}
	}
	
	public static void validateEnquiryLayout(EnquiryForm form)
	{
		FormLayout layout = form.getLayout();
		validateEnquiryElements(layout);
		validateLayout(layout, form, true, true);
	}
	
	private static void validateEnquiryElements(FormLayout layout)
	{
		Set<FormLayoutElement> enquiryElements = layout.getElements().stream()
				.map(FormElement::getType)
				.collect(Collectors.toSet());
		Set<FormLayoutElement> registrationOnly = Stream.of(FormLayoutElement.values())
				.filter(FormLayoutElement::isRegistrationOnly)
				.collect(Collectors.toSet());
		enquiryElements.retainAll(registrationOnly);
		if (!enquiryElements.isEmpty())
			throw new IllegalStateException("Enquiry layout consists of forbidded elements: " 
					+ enquiryElements.toString());
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
			checkLayoutElement(getIdOfElement(FormLayoutElement.IDENTITY, i), definedElements);
		for (int i = 0; i < form.getAttributeParams().size(); i++)
			checkLayoutElement(getIdOfElement(FormLayoutElement.ATTRIBUTE, i), definedElements);
		for (int i = 0; i < form.getAgreements().size(); i++)
			checkLayoutElement(getIdOfElement(FormLayoutElement.AGREEMENT, i), definedElements);
		for (int i = 0; i < form.getGroupParams().size(); i++)
			checkLayoutElement(getIdOfElement(FormLayoutElement.GROUP, i), definedElements);
		if (withCredentials)
		{
			for (int i = 0; i < form.getCredentialParams().size(); i++)
				checkLayoutElement(getIdOfElement(FormLayoutElement.CREDENTIAL, i), definedElements);
		}
	}
	
	private static void checkOtherElementsInLayout(BaseForm form, Set<String> definedElements, boolean withRemoteSignup)
	{
		if (form.isCollectComments())
			checkLayoutElement(FormLayoutElement.COMMENTS.name(), definedElements);
		
		if (form instanceof RegistrationForm)
		{
			RegistrationForm registrationform = (RegistrationForm) form;
			if (registrationform.getCaptchaLength() > 0)
				checkLayoutElement(FormLayoutElement.CAPTCHA.name(), definedElements);
			if (registrationform.getRegistrationCode() != null)
				checkLayoutElement(FormLayoutElement.REG_CODE.name(), definedElements);
			if (withRemoteSignup)
			{
				checkRemoteSignupElements(registrationform, definedElements);
			}
		}
	}
	
	private static void checkRemoteSignupElements(RegistrationForm registrationform, Set<String> definedElements)
	{
		
		List<AuthenticationOptionKey> gridSpecs = registrationform.getExternalSignupGridSpec().getSpecs();
		for (int i = 0; i < registrationform.getExternalSignupSpec().getSpecs().size(); i++)
		{
			if (!gridSpecs.contains(registrationform.getExternalSignupSpec().getSpecs().get(i)))
			{
				checkLayoutElement(getIdOfElement(FormLayoutElement.REMOTE_SIGNUP, i), definedElements);
			}
		}
		if (gridSpecs.size() > 0)
		{
			checkLayoutElement(getIdOfElement(FormLayoutElement.REMOTE_SIGNUP_GRID, 0), definedElements);		
		}
	}
	
	private static void removeParametersWithIndexLargerThen(FormLayout layout, FormLayoutElement type, int size)
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
	
	private static void removeParametersWithIndexIfPresent(FormLayout layout, FormLayoutElement type, int index)
	{
		Iterator<FormElement> iterator = layout.getElements().iterator();
		while (iterator.hasNext())
		{
			FormElement formElement = iterator.next();
			if (formElement.getType().equals(type) && 
					((FormParameterElement)formElement).getIndex() == index)
				iterator.remove();
		}
	}

	private static void removeBasicElementIfPresent(FormLayout layout, FormLayoutElement type)
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

	private static void addParameterIfMissing(FormLayout layout, FormLayoutElement type, int index, Set<String> definedElements)
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
		return element.getType().name();
	}
	
	private static String getIdOfElement(FormLayoutElement type, int idx)
	{
		return String.format("%s_%d", type.name(), idx);
	}
	
	private static void addBasicElementIfMissing(FormLayout layout, FormLayoutElement type, Set<String> definedElements)
	{
		if (!definedElements.contains(type.name()))
			layout.getElements().add(new BasicFormElement(type));
	}
	
	private static void addLocalSignupButtonElementIfMissing(FormLayout layout, Set<String> definedElements)
	{
		if (!definedElements.contains(FormLayoutElement.LOCAL_SIGNUP.name()))
			layout.getElements().add(new FormLocalSignupButtonElement());
	}
	
	private FormLayoutUtils()
	{
	}
}
