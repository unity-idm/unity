# Create Development Plan

## Input - Feature description
---------> PASTE FEATURE DESCRIPTION HERE <-----------

## Goal
Generate a complete Development Plan for general feature implementation with thorough research. Ensure context is
passed to the AI agent to enable self-validation and iterative refinement. Learn the feature file first to
understand what needs to be created, how the examples provided help, and any other considerations.

The AI agent only gets the context you are appending to the Development Plan and training data. Assume the AI agent 
has access to the codebase and the same knowledge cutoff as you, so its important that your research findings are included or referenced in the Development Plan. The Agent has Websearch capabilities, so pass urls to documentation and examples.

## Output
Record the development plan in `.agents/plans` directory in markdown format. Feature name shall be used as the file name.

## Research Process

1. **Codebase Analysis**
    - Search for similar features/patterns in the codebase
    - Identify files to reference in Development Plan
    - Note existing conventions to follow
    - Check test patterns for validation approach

2. **External Research**
    - Search for similar features/patterns online
    - Library documentation (include specific URLs)
    - Implementation examples (GitHub/StackOverflow/blogs)
    - Best practices and common pitfalls

3. **User Clarification** (if needed)
    - Specific patterns to mirror and where to find them?
    - Integration requirements and where to find them?

## Development Plan Generation

Using `.agents/templates/plan-template.md` as template:

### Critical Context to Include and pass to the AI agent as part of the Development Plan
- **Documentation**: URLs with specific sections
- **Code Examples**: Real snippets from codebase
- **Gotchas**: Library quirks, version issues
- **Patterns**: Existing approaches to follow
- **Testing Patterns**: Where relevant, identify and reflect common testing approaches already used (e.g. JUnit, Mockito, integration test strategies)

### Implementation Blueprint
- Start with code showing approach
- Reference real files for patterns
- Include error handling strategy
- list tasks to be completed to fullfill the Development Plan in the order they should be completed
- for key features and logic, specify **unit and/or integration tests** to be written:
    - Explicitly state which functionalities require tests
    - If applicable, reuse patterns from existing test classes
    - Where feasible, **recommend test names and structure** (e.g. `shouldReturnXWhenY`)
    - Highlight edge cases to cover and interactions that must be verified

### Additional Guidelines
- *** CRITICAL AFTER YOU ARE DONE RESEARCHING AND EXPLORING THE CODEBASE BEFORE YOU START WRITING THE DEVELOPMENT PLAN ***
- *** ULTRATHINK ABOUT THE DEVELOPMENT PLAN AND PLAN YOUR APPROACH THEN START WRITING THE DEVELOPMENT PLAN ***
- Before returning the plan, ensure all Quality Checklist items are checked. If any are unchecked (including the “Plan Score” final section), revise before submission.

## Quality Checklist
- [ ] All necessary context included
- [ ] References existing patterns
- [ ] Clear implementation path
- [ ] Error handling documented
- [ ] Testing approach and test coverage recommendations provided
- [ ] Plan Score (Score + Justification) included as the final section

### Final Section: Plan Score (Mandatory)

At the very end of the Development Plan, include a section titled exactly: “Plan Score”.

Format:
- Score: X/10
- Justification:
    - Completeness of context: …
    - Alignment with existing patterns: …
    - Clarity and precision of instructions: …
    - Depth of testing integration: …

Completion gate:
- Do not submit the plan if the “Plan Score” section is missing.
- Do not submit the plan if the Score or any of the four Justification bullets are missing.

**Reminder:** The goal is **one-pass implementation success** via exhaustive context and crystal-clear planning.