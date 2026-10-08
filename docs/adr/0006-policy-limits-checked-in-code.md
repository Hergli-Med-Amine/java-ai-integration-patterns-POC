# 0006. Policy limits are checked in code, not by the model

Status: accepted

## Context
"Is my crypto exposure within policy?" needs a portfolio figure (from a tool) and a limit (from a retrieved document section). With both in the prompt, `qwen2.5:1.5b` still compared them wrongly in 10 of 10 replays, for example calling 12.70% "within the 5% limit". Comparing numbers is not something the model can be trusted with, as with the other calculations (see the system prompt).

The default `QuestionAnswerAdvisor` template also told the model to answer only from the retrieved excerpts. In replays of the same request, the model called a tool in 2 of 10 runs with that sentence and in 10 of 10 without it.

## Decision
A `policyCheck` tool compares the current client's portfolio with the investment policy limits that the portfolio data can answer: the asset-class ranges of each risk profile (section 2) and the crypto-asset rules (section 4). It returns, per limit, the current percent, the allowed range, a status and the deviation in percentage points and EUR. Limits it cannot check are listed in the result, so the model does not report the portfolio as fully compliant.

The limits are copied from `investment-policy.txt` into `PolicyCheck`. The advisor uses its own template, which sends portfolio figures to the tools and policy wording to the excerpts.

## Consequences
- The model reports a verdict computed in Java; the comparison is unit tested.
- The limits exist twice, in the document and in code, and must be changed together.
- Issuer, sector and region concentration limits (section 3) are not checked: the data has no bond ratings or home region, and the policy does not say how government bonds and cash count as sectors.
