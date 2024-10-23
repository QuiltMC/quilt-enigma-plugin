# 2.2.1

- skip simple-type field names in delegate parameter name proposer
  - this fixes a crash on name proposal in QM

# 2.2.2

- update dependencies to fix missing ASM snapshot

# 2.2.3

- allow simple type names to be inherited to subclasses
- move to `quilt-parsers` instead of the deprecated `quilt-json5`

# 2.3.0

- automatically fix conflicts with proposed parameters
- fix constant field name finding being broken by an ASM update
- update ASM to `9.7.1`