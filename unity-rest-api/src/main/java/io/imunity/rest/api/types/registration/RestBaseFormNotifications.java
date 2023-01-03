/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import java.util.Objects;

public class RestBaseFormNotifications
{
	public final String rejectedTemplate;
	public final String acceptedTemplate;
	public final String updatedTemplate;
	public final String invitationTemplate;
	public final String invitationProcessedTemplate;
	public final String adminsNotificationGroup;
	public final boolean sendUserNotificationCopyToAdmin;

	protected RestBaseFormNotifications(RestBaseFormNotificationsBuilder<?> builder)
	{
		this.rejectedTemplate = builder.rejectedTemplate;
		this.acceptedTemplate = builder.acceptedTemplate;
		this.updatedTemplate = builder.updatedTemplate;
		this.invitationTemplate = builder.invitationTemplate;
		this.invitationProcessedTemplate = builder.invitationProcessedTemplate;
		this.adminsNotificationGroup = builder.adminsNotificationGroup;
		this.sendUserNotificationCopyToAdmin = builder.sendUserNotificationCopyToAdmin;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(acceptedTemplate, adminsNotificationGroup, invitationProcessedTemplate, invitationTemplate,
				rejectedTemplate, sendUserNotificationCopyToAdmin, updatedTemplate);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RestBaseFormNotifications other = (RestBaseFormNotifications) obj;
		return Objects.equals(acceptedTemplate, other.acceptedTemplate)
				&& Objects.equals(adminsNotificationGroup, other.adminsNotificationGroup)
				&& Objects.equals(invitationProcessedTemplate, other.invitationProcessedTemplate)
				&& Objects.equals(invitationTemplate, other.invitationTemplate)
				&& Objects.equals(rejectedTemplate, other.rejectedTemplate)
				&& sendUserNotificationCopyToAdmin == other.sendUserNotificationCopyToAdmin
				&& Objects.equals(updatedTemplate, other.updatedTemplate);
	}

	public static class RestBaseFormNotificationsBuilder<T extends RestBaseFormNotificationsBuilder<?>>
	{
		private String rejectedTemplate;
		private String acceptedTemplate;
		private String updatedTemplate;
		private String invitationTemplate;
		private String invitationProcessedTemplate;
		private String adminsNotificationGroup;
		private boolean sendUserNotificationCopyToAdmin;

		protected RestBaseFormNotificationsBuilder()
		{
		}

		@SuppressWarnings("unchecked")
		public T withRejectedTemplate(String rejectedTemplate)
		{
			this.rejectedTemplate = rejectedTemplate;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withAcceptedTemplate(String acceptedTemplate)
		{
			this.acceptedTemplate = acceptedTemplate;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withUpdatedTemplate(String updatedTemplate)
		{
			this.updatedTemplate = updatedTemplate;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withInvitationTemplate(String invitationTemplate)
		{
			this.invitationTemplate = invitationTemplate;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withInvitationProcessedTemplate(String invitationProcessedTemplate)
		{
			this.invitationProcessedTemplate = invitationProcessedTemplate;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withAdminsNotificationGroup(String adminsNotificationGroup)
		{
			this.adminsNotificationGroup = adminsNotificationGroup;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withSendUserNotificationCopyToAdmin(boolean sendUserNotificationCopyToAdmin)
		{
			this.sendUserNotificationCopyToAdmin = sendUserNotificationCopyToAdmin;
			return (T) this;
		}

		public RestBaseFormNotifications build()
		{
			return new RestBaseFormNotifications(this);
		}
	}

}
