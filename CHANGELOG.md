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

# 2.3.1

- fix possible CME in conflict fixing
- increase unit testing for conflict fixing

# 2.4.0

- implement mojang mapping proposal ([#16](https://github.com/QuiltMC/quilt-enigma-plugin/pull/16))
  - consists of 2 components
  - mojmap package proposer
    - proposes packages based on the package structure of mojmap
    - allows editing of names based on a JSON file, but structure is immutable and controlled by mojmap
  - mojmap name proposer
    - proposes fallback names based on mojmap for methods, fields and classes
    - does not provide parameters as mojmap does not include them
  - fully documented in javadoc and unit tested
- add `diffQMap` task ([#23](https://github.com/QuiltMC/quilt-enigma-plugin/pull/23))
  - allows testing of plugin updates by comparing against current QM
- fix and improve proposal for non-hashed names ([#22](https://github.com/QuiltMC/quilt-enigma-plugin/pull/22))
  - solves weird diffs when updating plugin in QM
