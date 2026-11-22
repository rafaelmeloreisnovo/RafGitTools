# Authorial source archive

This directory contains **copied source snapshots with explicit provenance**, not canonical RafGitTools implementation.

For every imported item:

`source repo/ref/path/blob -> authorship evidence -> governing license -> target path -> integration state`

Default integration state: `NOT_IN_APP_BUILD`.

If a future change wants to use an imported module, create a successor adapter in a normal RafGitTools implementation path and prove:

`rights -> dependency boundary -> tests -> build -> runtime/evidence -> claim`.

Never edit a copied source snapshot to make it look native; preserve it as evidence and derive a separately documented successor.
