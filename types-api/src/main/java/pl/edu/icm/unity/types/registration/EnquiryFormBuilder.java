/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.layout.FormLayout;


/**
 * Builder of {@link EnquiryForm}.
 */
public class EnquiryFormBuilder extends BaseFormBuilder<EnquiryFormBuilder>
{
	private EnquiryForm instance;
	
	public EnquiryFormBuilder()
	{
		super(new EnquiryForm());
		instance = (EnquiryForm) super.getInstance();
	}
	
	public EnquiryForm build()
	{
		instance.validateEnquiry();
		return instance;
	}
	
	public EnquiryFormBuilder withTargetGroups(String[] aValue)
	{
		instance.setTargetGroups(aValue);

		return this;
	}
	
	public EnquiryFormBuilder withTargetCondition(String aValue)
	{
		instance.setTargetCondition(aValue);

		return this;
	}

	public EnquiryFormBuilder withType(EnquiryType aValue)
	{
		instance.setType(aValue);

		return this;
	}

	public EnquiryFormBuilder withNotificationsConfiguration(EnquiryFormNotifications aValue)
	{
		instance.setNotificationsConfiguration(aValue);

		return this;
	}
	
	public EnquiryFormBuilder withLayout(FormLayout layout)
	{
		instance.setLayout(layout);
		
		return this;
	}

	public EnquiryFormNotificationsBuilder withNotificationsConfiguration()
	{
		EnquiryFormNotifications obj = new EnquiryFormNotifications();

		withNotificationsConfiguration(obj);

		return new EnquiryFormNotificationsBuilder(obj, this);
	}

	public static class EnquiryFormNotificationsBuilder
	{
		private EnquiryFormNotifications instance;
		private EnquiryFormBuilder parent;
		
		protected EnquiryFormNotificationsBuilder(EnquiryFormNotifications aInstance,
				EnquiryFormBuilder parent)
		{
			instance = aInstance;
			this.parent = parent;
		}

		public EnquiryFormNotifications build()
		{
			return instance;
		}

		public EnquiryFormNotificationsBuilder withSubmittedTemplate(String aValue)
		{
			instance.setSubmittedTemplate(aValue);
			return this;
		}
		
		public EnquiryFormNotificationsBuilder withAcceptedTemplate(String aValue)
		{
			instance.setAcceptedTemplate(aValue);
			return this;
		}
		
		public EnquiryFormNotificationsBuilder withRejectedTemplate(String aValue)
		{
			instance.setRejectedTemplate(aValue);
			return this;
		}
		
		public EnquiryFormNotificationsBuilder withUpdatedTemplate(String aValue)
		{
			instance.setUpdatedTemplate(aValue);
			return this;
		}
		
		public EnquiryFormNotificationsBuilder withEnquiryToFillTemplate(String aValue)
		{
			instance.setEnquiryToFillTemplate(aValue);
			return this;
		}

		public EnquiryFormNotificationsBuilder withInvitationTemplate(String aValue)
		{
			instance.setInvitationTemplate(aValue);
			return this;
		}
		
		public EnquiryFormNotificationsBuilder withAdminsNotificationGroup(String aValue)
		{
			instance.setAdminsNotificationGroup(aValue);
			return this;
		}
		
		public EnquiryFormBuilder endNotificationsConfiguration()
		{
			return parent;
		}
	}
}
