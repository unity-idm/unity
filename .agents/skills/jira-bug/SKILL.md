---
name: jira-bug
description: >-
  Report Unity bugs through Jira MCP using concise problem statements, reproduction steps, actual results,
  and expected behavior. Use when asked to file a bug or draft its ticket from an existing finding.
---

# Report a Jira bug

Invoke as `$jira-bug`. Follow the user's requested scope: drafting a ticket does not authorize creating it,
and permission to file one finding does not authorize filing the remaining findings.

## Defaults and inputs

Unless the user specifies otherwise, use:

- Jira site: `https://unity-idm.atlassian.net`.
- Project: `UY`.
- Issue type: `Bug`.
- Status: `Backlog`.

Reuse the conversation, designated findings, and existing QA evidence. Resolve any corrections to expected
behavior from the latest discussion before writing. Ask only if missing information prevents an accurate
report; do not require another approval when the user has already asked to create the ticket.

## Write the ticket

Make the ticket self-contained for someone who has not read the conversation.

- Title: name the affected feature or operation and the concrete failure, such as
  “UpMan REST project update clears form bindings and policy associations”. Do not use local finding IDs
  such as F1 as a substitute for describing the problem.
- Start with a short paragraph explaining what goes wrong and its practical consequence.
- Give numbered reproduction steps, including necessary setup, caller permissions or scope, the triggering
  action, and how to observe the failure. Use illustrative names; do not depend on temporary local fixtures.
- State actual behavior precisely: response status, relevant response fields, lost associations, or broken
  workflows. Distinguish deletion of an object from removal of its association where relevant.
- State expected behavior directly, including what must remain intact and any agreed fallback behavior.

Focus on the defect, reproduction, and expected outcome. Omit investigation history, source analysis,
commit archaeology, internal implementation suggestions, test-session chronology, and phrases such as
“confirmed by user”. Keep code documentation out; use a small request or response example only when it
makes reproduction clearer. Never include real credentials or tokens.

Use this description shape, adapting its length to the bug:

```markdown
<Short problem statement and impact.>

### Steps to reproduce

1. <Required setup.>
2. <Triggering action, with a minimal request example if useful.>
3. <Observe the result.>

### Actual behavior

<Concrete failure and relevant response/status.>

### Expected behavior

<Intended result and preservation requirements.>
```

Do not present desired behavior as something already verified. Keep observed results accurate even when
the requested fix changes existing semantics.

## Create and verify through MCP

Use Jira MCP for Jira operations. Use available primary tools directly; discover an operation only when its
callable interface is missing. Reuse the known site context. Prefer Markdown descriptions for this format.

For creation, set only the project, issue type, summary, and description. Do not add priority, assignee,
labels, components, versions, sprint, parent, links, attachments, or other fields unless the user asks.
Do not post comments or modify related tickets as part of filing a bug.

After creation, retain the returned issue key and read back its description and status. If it already has the
requested status, leave it there. Otherwise, list its available transitions, select the transition leading to
the requested status, and apply it without unrelated changes. Do not confuse a workflow status named
Backlog with a sprint/backlog placement operation.

If creation returns an issue key alongside a later error, continue with that issue instead of creating a
duplicate. For an ambiguous creation failure, check whether the issue was created before retrying. If a
required field or unavailable transition prevents the requested result, report that constraint without
inventing field values or choosing a different status.

Finish with a brief creation result and the clickable issue link at the end. For a draft-only request, return
the proposed title and description without writing to Jira.
