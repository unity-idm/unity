# UpMan HTTP contract coverage

The compatibility baseline is the deployed `RESTUpmanController` accessed through
`UpmanRESTTestBase` (authentication, database and shared exception mappings).
Fixtures under `src/main/resources/io/imunity/upman/rest/contract` are literal JSON,
independent of the DTO serializers and shared as generated documentation examples. New contract tests compare complete JSON trees
and check status and response media type separately.

Contract test names below omit the `Upman` prefix and `Test` suffix. `Workflow` means
`UpmanWorkflowIntegrationTest`; `Management` means `UpmanRestManagementIntegrationTest`. Service tests supplement
these HTTP tests but do not prove transport compatibility.

Run the HTTP fixtures from the repository root:

```sh
mvn -pl web-upman -am test '-Dtest=*HttpContractTest' \
  -Dsurefire.failIfNoSpecifiedTests=false -Dgpg.skip=true -Dhttp.proxyHost= -Dhttps.proxyHost=
```

The existing management/workflow integration tests and project, policy, form and
workflow service tests also remain migration gates. This command selects the new
HTTP contracts only; it does not replace the complete module or repository checks.

## Route inventory (51 operations)

| Method | Path | Controller operation | HTTP coverage |
| --- | --- | --- | --- |
| GET | `/v1/projects` | `getProjects` | ProjectHttpContract: complete trees, CRUD, media, failed updates; Management: CRUD |
| GET | `/v1/projects/{project-id}` | `getProject` | ProjectHttpContract: complete trees, CRUD, media, failed updates; Management: CRUD |
| POST | `/v1/projects` | `addProject` | ProjectHttpContract: complete trees, CRUD, media, failed updates; Management: CRUD |
| PUT | `/v1/projects/{project-id}` | `updateProject` | ProjectHttpContract: complete trees, CRUD, media, failed updates; Management: CRUD |
| DELETE | `/v1/projects/{project-id}` | `removeProject` | ProjectHttpContract: complete trees, CRUD, media, failed updates; Management: CRUD |
| GET | `/v1/projects/{project-id}/members` | `getProjectMembers` | MemberHttpContract: complete trees, CRUD and roles; Management: missing members |
| POST | `/v1/projects/{project-id}/invitations` | `sendInvitations` | InvitationHttpContract: populated trees, outcomes, resend/reinvite, delete; Workflow: missing/invalid resources |
| GET | `/v1/projects/{project-id}/invitations` | `getInvitations` | InvitationHttpContract: populated trees, outcomes, resend/reinvite, delete; Workflow: missing/invalid resources |
| GET | `/v1/projects/{project-id}/invitations/{code}` | `getInvitation` | InvitationHttpContract: populated trees, outcomes, resend/reinvite, delete; Workflow: missing/invalid resources |
| DELETE | `/v1/projects/{project-id}/invitations/{code}` | `removeInvitation` | InvitationHttpContract: populated trees, outcomes, resend/reinvite, delete; Workflow: missing/invalid resources |
| POST | `/v1/projects/{project-id}/invitations/{code}/resend` | `resendInvitation` | InvitationHttpContract: populated trees, outcomes, resend/reinvite, delete; Workflow: missing/invalid resources |
| POST | `/v1/projects/{project-id}/invitations/{code}/reinvite` | `reinvite` | InvitationHttpContract: populated trees, outcomes, resend/reinvite, delete; Workflow: missing/invalid resources |
| GET | `/v1/projects/{project-id}/groups` | `getGroups` | GroupHttpContract: complete trees, CRUD, delegation; Workflow: paths and validation |
| POST | `/v1/projects/{project-id}/groups` | `addGroup` | GroupHttpContract: complete trees, CRUD, delegation; Workflow: paths and validation |
| GET | `/v1/projects/{project-id}/groups/by-path` | `getGroup` | GroupHttpContract: complete trees, CRUD, delegation; Workflow: paths and validation |
| DELETE | `/v1/projects/{project-id}/groups/by-path` | `removeGroup` | GroupHttpContract: complete trees, CRUD, delegation; Workflow: paths and validation |
| PUT | `/v1/projects/{project-id}/groups` | `updateGroup` | GroupHttpContract: complete trees, CRUD, delegation; Workflow: paths and validation |
| PUT | `/v1/projects/{project-id}/groups/by-path/delegation` | `setGroupDelegation` | GroupHttpContract: complete trees, CRUD, delegation; Workflow: paths and validation |
| PUT | `/v1/projects/{project-id}/members/by-id/{entityId}` | `addGroupMember` | MemberHttpContract: complete trees, CRUD, roles, groupPath variants; Workflow: validation |
| GET | `/v1/projects/{project-id}/members/by-id/{entityId}` | `getGroupMember` | MemberHttpContract: complete trees, CRUD, roles, groupPath variants; Workflow: validation |
| DELETE | `/v1/projects/{project-id}/members/by-id/{entityId}` | `removeGroupMember` | MemberHttpContract: complete trees, CRUD, roles, groupPath variants; Workflow: validation |
| GET | `/v1/projects/{project-id}/members/by-id/{entityId}/role` | `getGroupMemberRole` | MemberHttpContract: complete trees, CRUD, roles, groupPath variants; Workflow: validation |
| PUT | `/v1/projects/{project-id}/members/by-id/{entityId}/role` | `setGroupMemberRole` | MemberHttpContract: complete trees, CRUD, roles, groupPath variants; Workflow: validation |
| GET | `/v1/projects/{project-id}/requests` | `getRequests` | RequestHttpContract and EnquiryHttpContract: populated trees, both decisions; Workflow: missing/invalid decisions |
| GET | `/v1/projects/{project-id}/requests/{requestId}` | `getRequest` | RequestHttpContract and EnquiryHttpContract: populated trees, both decisions; Workflow: missing/invalid decisions |
| POST | `/v1/projects/{project-id}/requests/{requestId}/state` | `decideRequest` | RequestHttpContract and EnquiryHttpContract: populated trees, both decisions; Workflow: missing/invalid decisions |
| GET | `/v1/projects/{project-id}/members/{userId}` | `getProjectMember` | MemberHttpContract: complete trees, CRUD and roles; Management: missing members |
| DELETE | `/v1/projects/{project-id}/members/{userId}` | `removeProjectMember` | MemberHttpContract: complete trees, CRUD and roles; Management: missing members |
| POST | `/v1/projects/{project-id}/members/{userId}` | `addProjectMember` | MemberHttpContract: complete trees, CRUD and roles; Management: missing members |
| GET | `/v1/projects/{project-id}/members/{userId}/role` | `getProjectMemberAuthorizationRole` | MemberHttpContract: complete trees, CRUD and roles; Management: missing members |
| PUT | `/v1/projects/{project-id}/members/{userId}/role` | `addProjectMemberAuthorizationRole` | MemberHttpContract: complete trees, CRUD and roles; Management: missing members |
| GET | `/v1/projects/{project-id}/policyDocuments` | `getPolicyDocuments` | PolicyHttpContract: complete trees, CRUD, revisions, foreign project denial |
| GET | `/v1/projects/{project-id}/policyDocuments/{policy-id}` | `getPolicyDocument` | PolicyHttpContract: complete trees, CRUD, revisions, foreign project denial |
| DELETE | `/v1/projects/{project-id}/policyDocuments/{policy-id}` | `removePolicyDocument` | PolicyHttpContract: complete trees, CRUD, revisions, foreign project denial |
| POST | `/v1/projects/{project-id}/policyDocuments` | `addPolicyDocument` | PolicyHttpContract: complete trees, CRUD, revisions, foreign project denial |
| PUT | `/v1/projects/{project-id}/policyDocuments` | `updatePolicyDocument` | PolicyHttpContract: complete trees, CRUD, revisions, foreign project denial |
| GET | `/v1/projects/{project-id}/registrationForm` | `getRegistrationForm` | FormHttpContract: explicit CRUD trees, layouts, links, generated/ignored bodies; EnquiryHttpContract/RequestHttpContract: pending flags |
| GET | `/v1/projects/{project-id}/registrationForm/link` | `getRegistrationFormLink` | FormHttpContract: explicit CRUD trees, layouts, links, generated/ignored bodies; EnquiryHttpContract/RequestHttpContract: pending flags |
| DELETE | `/v1/projects/{project-id}/registrationForm` | `removeRegistrationForm` | FormHttpContract: explicit CRUD trees, layouts, links, generated/ignored bodies; EnquiryHttpContract/RequestHttpContract: pending flags |
| POST | `/v1/projects/{project-id}/registrationForm` | `addRegistrationForm` | FormHttpContract: explicit CRUD trees, layouts, links, generated/ignored bodies; EnquiryHttpContract/RequestHttpContract: pending flags |
| PUT | `/v1/projects/{project-id}/registrationForm` | `updateRegistrationForm` | FormHttpContract: explicit CRUD trees, layouts, links, generated/ignored bodies; EnquiryHttpContract/RequestHttpContract: pending flags |
| GET | `/v1/projects/{project-id}/signUpEnquiry` | `getSignupEnquiryForm` | FormHttpContract: explicit CRUD trees, layouts, links, generated/ignored bodies; EnquiryHttpContract/RequestHttpContract: pending flags |
| GET | `/v1/projects/{project-id}/signUpEnquiry/link` | `getSignupEnquiryLink` | FormHttpContract: explicit CRUD trees, layouts, links, generated/ignored bodies; EnquiryHttpContract/RequestHttpContract: pending flags |
| DELETE | `/v1/projects/{project-id}/signUpEnquiry` | `removeSignupEnquiryForm` | FormHttpContract: explicit CRUD trees, layouts, links, generated/ignored bodies; EnquiryHttpContract/RequestHttpContract: pending flags |
| POST | `/v1/projects/{project-id}/signUpEnquiry` | `addSignupEnquiryForm` | FormHttpContract: explicit CRUD trees, layouts, links, generated/ignored bodies; EnquiryHttpContract/RequestHttpContract: pending flags |
| PUT | `/v1/projects/{project-id}/signUpEnquiry` | `updateSignupEnquiryForm` | FormHttpContract: explicit CRUD trees, layouts, links, generated/ignored bodies; EnquiryHttpContract/RequestHttpContract: pending flags |
| GET | `/v1/projects/{project-id}/membershipUpdateEnquiry` | `getMembershipUpdateEnquiryForm` | FormHttpContract: explicit CRUD trees, layouts, links, generated/ignored bodies; EnquiryHttpContract/RequestHttpContract: pending flags |
| GET | `/v1/projects/{project-id}/membershipUpdateEnquiry/link` | `getMembershipUpdateEnquiryLink` | FormHttpContract: explicit CRUD trees, layouts, links, generated/ignored bodies; EnquiryHttpContract/RequestHttpContract: pending flags |
| DELETE | `/v1/projects/{project-id}/membershipUpdateEnquiry` | `removeMembershipUpdateEnquiryForm` | FormHttpContract: explicit CRUD trees, layouts, links, generated/ignored bodies; EnquiryHttpContract/RequestHttpContract: pending flags |
| POST | `/v1/projects/{project-id}/membershipUpdateEnquiry` | `addMembershipUpdateEnquiryForm` | FormHttpContract: explicit CRUD trees, layouts, links, generated/ignored bodies; EnquiryHttpContract/RequestHttpContract: pending flags |
| PUT | `/v1/projects/{project-id}/membershipUpdateEnquiry` | `updateMembershipUpdateEnquiryForm` | FormHttpContract: explicit CRUD trees, layouts, links, generated/ignored bodies; EnquiryHttpContract/RequestHttpContract: pending flags |

## Observed compatibility details

- Project creation returns 200 with an `id` object. Project update and deletion return 204.
- Project responses include `projectId`, computed `urlId`, `public`, and null form/logo values.
  The historical `project-id` example does not match the actual response.
- Project writes accept JSON, text/plain and a body without Content-Type. The UTF-8
  fixture includes non-ASCII text. Unacceptable response Accept values return 406.
- Unknown project request properties are rejected. Malformed JSON, an empty body and
  wrong scalar types use the 400 JSON `{message,error}` envelope with message
  `Can't perform JSON deserialization` and error `BadRequestException`.
- An update without displayedName returns 400 with `IllegalArgumentException` and
  message `Displayed name have to be set`. Rejected updates leave the project unchanged.
- A literal JSON null project update returns 500 with an empty body. This surprising
  legacy behavior is characterized, not changed as part of the transport migration.
- Policy creation returns 200 with a numeric `id`. Revisions start at 1; updating
  with absent/false incrementRevision preserves the revision and true increments it.
- Missing policy inputs retain their existing rejection behavior: name returns 400;
  missing displayedName, mandatory, contentType or content returns empty 500; missing update
  id returns 403. Unknown content types return the 400 IllegalArgumentException envelope.
  Rejected requests leave existing documents unchanged. Schemas describe the required fields
  and EMBEDDED/LINK enum without changing these legacy validation statuses.
- A policy outside a project's scope, including a policy removed from that project,
  returns 403 with `AuthorizationException`, rather than 404. Failed foreign-project
  writes preserve the owning project's policy.
- All three form POSTs ignore absent, empty, malformed and JSON null bodies with
  autogenerate=true when Content-Type is application/json. A bodyless request without
  Content-Type returns 415. Form mutations return 204.
- Explicit form fixtures preserve inherited mixed-case fields and the five `clazz`
  layout discriminators. Registration reads expand null FormInformation2ndStage
  into an i18n object with null DefaultValue and empty Map.
- Membership-update forms use STICKY. Updating a membership form of another type
  clears its project attachment in the existing storage dependency handler.
- Absent groupPath selects email/role/attributes member objects. Empty or nonempty
  groupPath selects entityId/name/email/role/attributes member objects. Adding a
  subgroup member also adds the project membership; removing it preserves that parent
  membership. Demoting the last project manager returns 409 with the OneManagerRemainsException JSON envelope.

- Group listing includes the project root at path "", followed by descendants sorted
  by relative path. Subgroup creation returns 201 with a path object.
- Invitation POST returns 201 with one outcome per distinct email, retaining email
  order. Existing members have status alreadyMember. Invitation expiration and
  lastSentTime are numeric epoch seconds, including fractions. Resend increments
  numberOfSends; unsent invitations preserve lastSentTime:null and numberOfSends:0.
  Reinvite replaces the code and removes the original invitation.
- Registration request reads include numeric requestedTime, nullable name, email,
  emailVerified, and relative group paths. Both accept and decline return 204 and
  remove the request from the pending listing.
- Registration form ignoreRequests=false rejects an update with pending requests
  using SchemaConsistencyException (400), without changing the pending request.
  ignoreRequests=true permits the update and preserves it. dropRequests=true
  removes stored requests.
- **Existing persistence bug:** registration form deletion with dropRequests=false
  returns SchemaConsistencyException (400) but detaches the form from the project.
  The form and request remain stored. The test records that partial mutation and
  reattaches the form through the engine API before testing successful deletion.
- Project scalar coercion accepts string booleans, integer booleans, and numeric
  localized names. Omitted readOnlyAttributes remains null. Form builder failures
  use the legacy BadRequestException envelope. An invalid Boolean query value acts
  as false; a nonnumeric entity ID in the path returns an empty 404.
- Direct authentication without the UpMan manager attribute returns a JSON 403
  AuthorizationException for reads and mutations; the project remains unchanged.
- Form/invitation URL assertions use the actual URL support service independently
  of DTO serialization. The inherited engine test configuration mocks the shared
  UI endpoint, so these tests cover link transport rather than a working UI URL.
- Dynamic IDs and timestamps in the representative invitation/request fixtures are
  replaced using stored domain data. JSON numbers are parsed as BigDecimal to
  compare timestamp fractions without floating-point rounding.

- Both enquiry kinds preserve complete pending request trees and accept/decline decisions.
  Their ignoreRequests/dropRequests flags match registration behavior, including form
  detachment after a rejected deletion with pending responses.
- Member fixtures include Unicode names, multivalued project attributes and null email
  on both member representations. The email-shaped member list returns email:null when
  a member has no email; numeric member reads/lists also include entityId and name.
- Invitation input accepts integer and fractional numeric epoch seconds as well as
  ISO timestamps. Fractions beyond milliseconds are truncated by persistence. Selected
  subgroup paths and allowModifyGroups survive the round trip. Reading or deleting a
  code through a sibling project returns empty 404 and preserves the owning invitation.

- A misconfigured project produces a failed outcome for each affected invitation email,
  preserves batch order and continues after a failure. Already-member outcomes are still
  returned in the same batch, and no invitation is stored for failed creation.
- Reading or deciding a registration request through a sibling project returns an empty
  404 and leaves the owning request pending and unchanged.
- Runtime documentation is public when explicitly enabled. API requests without credentials still
  return the existing 400 Authentication failed envelope with the direct password authenticator.

## Verification gates

The independent route inventory in `contract/routes.txt` is checked against the generated
schema. `UpmanOpenApiHttpContractTest` compares live and offline documents. The complete
module suite retains nested/percent-encoded ID and sibling isolation checks. Export
validation checks example schemas, numeric timestamps, both member list representations
and every form layout discriminator. The repository headless full build remains the final
regression gate.
