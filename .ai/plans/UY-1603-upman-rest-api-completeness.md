# UY-1603: Complete the UpMan REST API

## Goal

Extend UpMan REST API v1 so a REST manager can send project invitations, manage subgroups and their members, and review user requests. Cover the other capabilities available in the UpMan UI but absent from REST while keeping existing project, form, policy document, and project member routes compatible.

## Current state and audit

`web-upman/src/main/java/io/imunity/upman/rest/RESTUpmanController.java` exposes projects, forms, policy documents, and project level membership. The UpMan UI already uses these engine APIs for the missing workflows:

| UI capability | Engine API | REST gap |
| --- | --- | --- |
| Create and send invitations | `ProjectInvitationsManagement` | Send invitations |
| List, remove, resend, and reinvite invitations | `ProjectInvitationsManagement` | Entire invitation lifecycle |
| View, create, rename, and delete subgroups | `DelegatedGroupManagement` | Subgroup discovery and management |
| Change subgroup visibility and delegation | `DelegatedGroupManagement` | Public/private mode and subproject configuration |
| View, add, and remove subgroup members; set their roles | `DelegatedGroupManagement` | Subgroup membership and roles |
| List, accept, and decline signup and membership update requests | `ProjectRequestManagement` | Request review |
| Get public registration, signup, and membership update form links | `ProjectRequestManagement` | Form links |

Two existing behaviors need attention as part of the enhancement:

1. `ProjectRequestManagementImpl` authorizes the supplied project, then processes a registration or enquiry by request ID without checking that its form belongs to that project. Add the ownership check in the engine before exposing REST accept/decline actions.
2. `RestProjectService.getProjects()` can return nested delegated projects with slash-containing IDs, while `ProjectPathProvider.getProjectPath()` rejects slashes. Give nested projects a URL-safe way to be addressed without breaking current direct-project routes.

## Proposed API contract

Keep all additions under `/v1/projects/{project-id}`. Finalize names and JSON examples in the API reference before coding. Use project-relative group paths in payloads and query parameters; validate and resolve them under the selected project. This avoids relying on URL-encoded slashes for nested groups.

| Resource | Proposed operations | Key contract points |
| --- | --- | --- |
| `/invitations` | `POST`, `GET` | Send one or more invitations; accept email, selected subgroup paths, whether recipients may change groups, and expiration. Return outcomes per email, including already-member addresses and any partial send failures. |
| `/invitations/{code}` | `GET`, `DELETE` | Only reveal or remove invitations belonging to the selected project. |
| `/invitations/{code}/resend`, `/reinvite` | `POST` | Preserve engine rules for resend validity and reinvitation. Document that reinvitation replaces the code. |
| `/groups` | `GET`, `POST`, `PUT` | Return the hierarchy with paths and metadata; create under a specified parent and return the generated path; update a subgroup's localized display name and public/private mode using that path in the request body. |
| `/groups/by-path?path=...` | `GET`, `DELETE` | Address a nested subgroup by project-relative path. Use the engine's distinct deletion rules for regular groups and delegated subprojects, including form cleanup. |
| `/groups/by-path/delegation?path=...` | `PUT` | Change subgroup delegation settings independently. |
| `/members?groupPath=...` | `GET` | Extend listing to a subgroup; an absent `groupPath` retains current project-root behavior. Include stable entity IDs in new responses. |
| `/members/by-id/{entityId}?groupPath=...` | `PUT`, `DELETE` | Add or remove a user from a subgroup, including a user without a usable email. Preserve existing email-based project member routes. |
| `/members/by-id/{entityId}/role?groupPath=...` | `GET`, `PUT` | Read and set subgroup roles where delegation permits them. |
| `/requests`, `/requests/{requestId}` | `GET` | List pending signup and membership update requests; return submitted details needed for a decision. Include operation, type, time, email verification state, and selected groups. |
| `/requests/{requestId}/state?decision=...` | `POST` | Accept or decline a pending request using the query parameter. Resolve operation and type from the stored request; never trust caller-supplied values. Reject a missing, completed, or foreign-project request. |
| `/registrationForm/link`, `/signUpEnquiry/link`, `/membershipUpdateEnquiry/link` | `GET` | Return each public form link as a separate resource. Report a missing, invitation-only, or unavailable public link as `404`. |

Use the established JSON and HTTP conventions: `201` for created resources, `204` for completed changes, `400` for invalid input, `403` for denied access, `404` for out-of-scope or missing resources, and `409` for state conflicts. State invitation form prerequisites and recursive subgroup deletion clearly in the documentation.

## Implementation sequence

1. Add immutable REST DTOs and mapping code in `web-upman`, following the repository's record and package-by-feature guidance. Add a shared project/group path resolver that rejects traversal and paths outside the configured root.
2. Add project-scoped REST services and controller routes for invitations, subgroups, subgroup members, requests, and form links. Check the configured REST management role on every operation and enforce project ownership before invoking engine operations. Where engine APIs require an UpMan UI role that a REST-only manager lacks, introduce a deliberate authorized adapter while retaining engine domain invariants.
3. Fix request ownership in `engine/src/main/java/pl/edu/icm/unity/engine/project/ProjectRequestManagementImpl.java`. Match the request ID, type, and form against the selected project's configured registration or enquiry form within the decision transaction. Use the same scoped lookup for request detail and decisions.
4. Resolve nested project addressing without changing existing direct-project URLs. Add a URL-safe lookup mechanism and test both direct and nested projects returned by the project listing.
5. Update `documentation/src/main/rest-api/upman-rest-api-v1.txt` with all routes, payloads, response codes, authorization, and side effects.

## Verification and completion criteria

- Add JUnit 5 service tests for path resolution, DTO mapping, invitation outcomes, subgroup rules, and request ownership. Name new tests and methods according to `AGENTS.md`.
- Extend `web-upman` HTTP integration tests to cover every new workflow, nested subgroup paths, REST-only managers, users without email, invalid input, duplicate actions, and access across projects.
- Add an engine regression test proving that a manager for one project cannot accept or decline another project's request by ID.
- Run affected module tests, then the repository's required Maven verification build. Update the plan if implementation exposes a persisted schema change; none is expected from the API and service changes described here.
