package pl.edu.icm.unity.engine.api.project;

public interface ProjectManagementFactory
{
	/** Creates independent project services with the supplied policy and normal transaction handling. */
	ProjectManagement create(ProjectAuthorization authorization);

	record ProjectManagement(DelegatedGroupManagement groups, ProjectInvitationsManagement invitations,
			ProjectRequestManagement requests)
	{
	}
}
