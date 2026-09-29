# Aviation Radial / Rotary / Hybrid Propulsion Gymnasium V1

State: IMPLEMENTED_UNTESTED
claim_allowed: false

This gymnasium separates established mechanisms from candidate synthesis.

## Established reference layer

- Conventional radial engine: cylinders arranged around a crankcase with a rotating crankshaft.
- Historical rotary aircraft engine: crankshaft attached to airframe while the engine/cylinder mass rotated with the propeller.
- Radial-engine hydraulic/liquid lock: liquid can accumulate in lower cylinders and damage connecting rods if rotation is forced.
- Dual ignition: two independent magnetos and two spark plugs per cylinder are common reliability features in certificated piston aircraft.
- Sodium-filled valve stems: documented aircraft-engine technique for carrying heat from the valve head toward the stem/guide/head path.
- Electrified propulsion and driven turbo/CVT/electric-assist architectures exist as real engineering families.

## Candidate synthesis layer

The user's requested synthesis is represented as candidate nodes, not as proven improvements:

- rotating cylinder-liner concept intended to test wear distribution in X/Y;
- precision rolling-element bearing substitution at selected angular-shaft interfaces;
- combustion + electric motor/generator torque summing;
- CVT or traction-drive controlled boost;
- hydraulic/pneumatic/flywheel transient energy buffers;
- star/delta, brushed/brushless and other motor topology alternatives;
- staged torque-curve control;
- fuel thermal preconditioning.

Every candidate must traverse:
CANDIDATE -> LOAD_CASE -> MATERIAL -> THERMAL -> LUBRICATION -> DYNAMICS -> FAILURE_MODES -> TEST -> EVIDENCE.

No candidate is promoted merely because an analogy is mechanically plausible.

## Safety boundary

Fuel/oxidizer/ignition are represented as abstract combustion inputs. The model does not contain reactive-gas proportions, oxidizer enrichment recipes, injection pressures, ignition timing recipes, accumulator pressure recipes, boost targets or fuel-temperature setpoints.

## R3

F_ok = documented aviation reference mechanisms + typed candidate architecture graph.
F_gap = quantitative load maps, materials, tribology, thermal limits, rotor/propeller maps, exact mission profile and validation.
F_next = synthetic system-model fixture that computes relations/state only, without physical tuning setpoints.
