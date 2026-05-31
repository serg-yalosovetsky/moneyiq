Read the following project context files and confirm you've loaded them. After reading, give a one-sentence summary of: current DB version, what the user was last working on (based on recent ADRs), and which UI area the current conversation seems to be about.

Files to read in order:
1. `.context/SYSTEM_OVERVIEW.md`
2. `.context/DB_SCHEMA.md`
3. `.context/ADR_LOG.md` — focus on the last 5 ADRs
4. `.context/ui/UI_CONTRACTS.md` — index only (do not recurse into sub-files unless the user's task clearly targets a specific area)

If the user's task or $ARGUMENTS mentions a specific area, also read:
- accounts / adaptive tint / credit limit → `.context/ui/UI_ACCOUNTS.md`
- categories / chips / donut / icons → `.context/ui/UI_CATEGORIES.md` and `.context/ui/UI_CATEGORY_ICONS.md`
- calculator / keypad → `.context/ui/UI_CALCULATOR.md`
- budget / savings / income bar → `.context/ui/UI_BUDGET.md`
- transactions / overview → `.context/ui/UI_TRANSACTIONS.md`
- typography / sizing → `.context/ui/UI_TYPOGRAPHY.md`

End with: "Контекст завантажено. Готовий до роботи." and list which files were read.
