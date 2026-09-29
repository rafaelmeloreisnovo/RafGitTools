# Energy Cascade L0 V1

Freestanding fixed-point accounting kernel for energy-flow conservation.

It can:
- apply a Q16.16 conversion efficiency;
- calculate bounded round-trip efficiency;
- track external input, useful output, stored energy and losses;
- record recovered internal transfers without counting them as new external supply;
- reject ledgers whose accounted outputs exceed external input.

It cannot:
- control combustion, electrolysis, oxidizers, pressure vessels or ignition;
- size hardware;
- perform I/O/network/filesystem access.

Recovered energy is a transfer inside the original budget, not a second source.
