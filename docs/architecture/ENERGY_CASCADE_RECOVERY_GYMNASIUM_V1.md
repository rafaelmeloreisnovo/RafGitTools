# Energy Cascade Recovery Gymnasium V1

State: IMPLEMENTED_UNTESTED
claim_allowed: false

## Core idea

Treat the propulsion system as an energy-flow graph rather than a single engine.

Chemical energy -> combustion -> shaft work + exhaust + coolant/oil heat + friction/radiation.
Each non-useful stream becomes a candidate input for a later converter, but every conversion is entered into the loss ledger.

## Why the user's 15% number is not a universal engine-efficiency number

DOE's 2024 vehicle-level comparison reports about 30% tank-to-wheels for a typical conventional gasoline vehicle depending on drive cycle. Older DOE/FuelEconomy data report roughly 12-30% of fuel energy moving a conventional car down the road. Separately, DOE sources cite roughly 30% of fuel chemical energy leaving a typical ICE in hot exhaust.

These are different boundaries. This gymnasium therefore stores an energy map instead of one fixed efficiency constant.

## Cascade

1. Primary combustion engine produces shaft power.
2. Turbocharger reuses exhaust pressure/enthalpy for intake compression.
3. Turbocompound/electric turbo-generator can recover additional exhaust power.
4. Motor-generator can harvest regenerative events and provide torque fill.
5. DC bus dispatches power to motor, compressor, auxiliaries and storage.
6. Supercapacitor bank handles high-power short-duration transients.
7. Battery/other storage handles longer-duration energy.
8. TEG/Rankine/high-temperature electrolysis heat-assist remain waste-heat recovery candidates.
9. Water from a hydrogen conversion path may be condensed and reused as electrolyzer feed.
10. Electrolysis consumes electricity/heat; it does not regenerate hydrogen for free.

## Hydrogen/water loop

H2 -> useful conversion -> H2O can be a material loop.
H2O -> electrolysis -> H2 requires input energy.

Therefore:
MATERIAL_LOOP_CLOSED does not imply ENERGY_LOOP_CLOSED.

A reversible hydrogen/fuel-cell system can be useful for storage, but measured/target round-trip efficiencies remain below 100%.

## Supercapacitors

Supercapacitors are modeled as a power buffer. Carbon nanotube/graphene electrodes are a material-development candidate because of conductivity and surface area, but present reviews still identify practical energy-density, stability and manufacturing constraints.

## Safety boundary

No reactive-gas ratios, N2O/O2 enrichment fractions, hydrogen storage pressures, ignition timing, boost targets, accumulator pressures or electrolysis construction recipes are encoded.

## R3

F_ok = energy-flow ontology + loss ledger + exhaust recovery + DC bus + supercap + water/H2 material loop.
F_gap = exact energy maps, converter efficiencies, device sizing, materials, thermal limits and mission profile.
F_next = build a synthetic fixed-point energy-ledger fixture and prove no double counting or >100% round-trip promotion.
