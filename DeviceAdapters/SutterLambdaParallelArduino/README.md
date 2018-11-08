Sutter Lambda control using parallel port via Arduino
=====================================================

Serial connection (from computer to Arduino) is 9600 8N1.

This controlls the Wheel A of a Sutter Lambda 10-3 (may also work with other
models) using its parallel port, serving as a TTL-controllable adapter.
Sequencing is supported.

Here are the TTL connections:

| Arduino Pin | AVR Pin | Parallel DB-25 Pin | Direction | Description |
|-------------|---------|--------------------|-----------|-------------|
|           2 |     PD2 |                  - |   Input   | TRIGGER input (starts wheel movement) |
|           3 |     PD3 |                 11 |   Input   | Busy line from Lambda |
|           4 |     PD4 |                 12 |   Input   | Error line from Lambda |
|           5 |     PD5 |                  - |  Output   | BUSY signal (indicates wheel moving) |
|           6 |     PD6 |                  2 |  Output   | Parallel data bit 0 to Lambda |
|           7 |     PD7 |                  3 |  Output   | Parallel data bit 1 to Lambda |
|           8 |     PB0 |                  4 |  Output   | Parallel data bit 2 to Lambda |
|           9 |     PB1 |                  5 |  Output   | Parallel data bit 3 to Lambda |
|          10 |     PB2 |                  6 |  Output   | Parallel data bit 4 to Lambda |
|          11 |     PB3 |                  7 |  Output   | Parallel data bit 5 to Lambda |
|          12 |     PB4 |                  8 |  Output   | Parallel data bit 6 to Lambda |
|          13 |     PB5 |                  9 |  Output   | Parallel data bit 7 to Lambda |

Ground connections to the Lambda and master controller are also required and
not shown in the above table.

TRIGGER and BUSY operation
--------------------------

1. This controller expects some other controller to coordinate the busy
   state of the wheel with the acquisition. This other controller (the
   "master controller") should observe the BUSY output (Arduino pin 5) and
   ensure that image acquisition does not happen before the wheel finishes
   moving.

1. Every effort is made to avoid dropping incoming TRIGGERs, even if they are
   received while still BUSY.

1. This controller always sets BUSY high as soon as it receives a TRIGGER (or
   is commanded via serial command to move), where "as soon as" means within
   microseconds in the absence of interfering serial communication. BUSY then
   remains high until the wheel finishes moving.

1. After commanding the Lambda to move the wheel, this controller allows
   60 µs for the Lambda to indicate that it is busy (or has an error). It
   also keeps the BUSY output high until it is reasonably sure that the
   Lambda is quiescent, by ignoring short (less than 60 µs) pulses of
   non-busy and non-error state of the Lambda. In this way, this controller
   produces as clean a BUSY signal as possible for consumption by the master
   controller. It is hoped that this covers the case where the move fails and
   the Lambda re-homes the wheel — the BUSY line should remain high the
   entire time, so that the overall acquisition will pause but not fail.

µManager Device Properties
--------------------------

- `State` and `Label` — The filter wheel position.

- `Speed` — Filter wheel speed (see Sutter manual).

- `UseSequencing` — If Yes, MDA may use hardware sequencing for this device
  (no effect on device behavior).

- `ClosedPosition` — The filter wheel position to use for the "closed" state,
  when using with State Device Shutter.