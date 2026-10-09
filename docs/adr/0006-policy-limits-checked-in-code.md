# 0006. Policy limits are checked in code, not by the model

Status: accepted

## Context
Replays on my machine against `qwen2.5:1.5b` showed two faults. With Spring AI's default retrieval template ("if the answer is not in the context, say you can't answer"), the model called a tool in 2 of 10 runs; without it, in 10 of 10. And with the correct figures in the prompt, it still compared them with the limits wrongly in 10 of 10 runs (e.g. "12.7% is within the 5% limit").

## Decision
The model picks tools and writes the answer; Java does every lookup, calculation and comparison. A `policyCheck` tool compares the portfolio with the limits the data can answer: the asset-class ranges per risk profile (policy section 2) and the crypto rules (section 4). The limits are copied into `PolicyCheck` from `investment-policy.txt`. The retrieval advisor uses our own template, which tells the model to use tools for portfolio figures.

## Consequences
- The comparison is unit-tested and no longer depends on the model's arithmetic.
- The limits exist twice, in the policy text and in code, and must change together.
- Issuer, sector and region limits (section 3) are not checked: the data has no bond ratings or home region, and the sector rule is ambiguous for government bonds and cash. The tool lists them as not checked, so the model cannot claim full compliance.
