<!--
Date changed: 20261111
Author: John Blue
Code generated with the help of Claude, an AI assistant made by Anthropic.
What changed: Added USB drive logging, brake mode in autonomous, the new stall counter,
  stored encoders, and the dated change history below. Added the original prompt
  that started this project (bottom of this file).
-->

# frc-test-bot-platform
code base for frc test bot

## NEO 550 Xbox Drive (roboRIO 1) - no vision

Left stick up/down = drive forward/backward (full stick = 25% output).
Right stick left/right = turn (full stick = 20%).
Both sticks centered = motors stop. Only REVLib is needed (no PhotonLib).

Gearing: VersaPlanetary **10:1** on all four wheels (`GEAR_RATIO = 10.0`).
Top speed at the 25% cap is about 4.8 ft/s. Total push at the 20 A limits is about 34 lbf.
All four gearboxes must stay the same ratio. Use white lithium grease on the gears.

### Change history

| Date | Version | What changed |
|---|---|---|
| 20261108 | v1 | First drive code: left stick forward/back, 25% cap, 20 A limits, coast, temp and stall shutdowns |
| 20261108 | v2 | Fixed forward/back direction (swapped left/right inversion); added turning |
| 20261108 | v3 | Turning moved to the right stick (split arcade) |
| 20261109 | v5-v6 | Open- and closed-loop ramps set to 0.25 s |
| 20261109 | v6 no-vision | Vision (AprilTag) code removed for the basic robot |
| 20261110 | v7 | 10:1 VersaPlanetary gearboxes installed (`GEAR_RATIO = 10.0`); wheel RPM on dashboard |
| 20261110 | v8 | Date, author, and Claude header added to every file |
| 20261111 | v9 | USB drive logging; brake in autonomous; stall counter fix; stored encoders |
| 20261111 | v9 | Original prompt added to this README |

### Setup (about 30 minutes)

1. Install **WPILib 2026** (not 2027; 2027 is for Systemcore only).
2. Image the roboRIO 1 with the 2026 image (FRC 2026 Game Tools).
3. Update all 4 SPARK MAX controllers to 2026 firmware with the REV Hardware Client.
   Confirm CAN IDs 5, 6, 7, 8 while you are there.
4. In WPILib VS Code: Create a new project -> Template -> Java -> **Timed Robot**.
   Enter team number 1018.
5. WPILib menu -> Manage Vendor Libraries -> Install new library (online):
   `https://software-metadata.revrobotics.com/REVLib-2026.json`
6. Replace `Robot.java` in `src/main/java/frc/robot/` with this one.
   Add `Constants.java` and `Drivetrain.java` next to it. Keep the template's `Main.java`.
7. Build, then deploy.

### First test (robot ON BLOCKS, wheels off the ground)

1. Enable Teleop. Push the stick forward a little. All 4 wheels should turn "forward."
2. If one side spins backward, flip `LEFT_INVERTED` or `RIGHT_INVERTED` in `Constants.java`.
3. Let go of the stick. Wheels coast to a stop.
4. Watch the dashboard (Elastic or Shuffleboard): temps, current, and lockout flags are under `Drive/`.

### Logging to a USB drive (added 20261111)

1. Format a USB flash drive as **FAT32** (32 GB or smaller). NTFS and exFAT will not work.
2. Plug it into a roboRIO USB port **before** turning on the robot (or restart robot code after).
3. Check the dashboard: `Log/USB drive found` and `Log/Logging on` should both be **true**.
4. Logs are saved in the `logs` folder on the drive:
   - `.wpilog` files from WPILib: everything on the dashboard, plus enable/disable and joysticks.
   - `.revlog` files from REV's Status Logger: SPARK MAX data (current, temperature, speed).
5. To view: take the drive out and open the files in **AdvantageScope** (installed with WPILib).
   Or in the Driver Station: gear icon -> Upload WPILogs.

**No USB drive?** Logging turns OFF (WPILib and REV), and the Driver Station shows a warning.
This protects the roboRIO 1's small internal flash from filling up and wearing out.
To log to the roboRIO anyway, set `ONLY_LOG_TO_USB = false` in `Constants.java` (not recommended).

### Brake and coast (added 20261111)

| Mode | Idle mode | Why |
|---|---|---|
| Autonomous | **Brake** | Stops at an exact spot instead of rolling |
| Teleop | Coast | Smooth for the driver, same as before |
| Disabled / Test | Coast | Easy to push the robot by hand |

The switch is not saved to the SPARK MAX's flash, so it is safe at every mode change.
Coast is still the saved setting after a power cycle. To stay in coast all the time,
set `BRAKE_IN_AUTONOMOUS = false`. Dashboard shows `Drive/Brake mode`.

### Stall counter (changed 20261111)

The old check restarted its 2-second timer if a stall reading flickered for even one loop.
The new check is a counter: it fills 1 second per second while any motor is stalled and drains
at half speed when none is. Drive turns OFF when the counter reaches 2 seconds.
A one-loop flicker only drains 0.01 s, so a real stall still trips.
Watch it on the dashboard: `Drive/Stall counter s`.

### Safety features

| Feature | Setting | Where |
|---|---|---|
| Current limit per motor | 20 A | `SMART_CURRENT_LIMIT_AMPS` |
| Backup hard limit | 30 A | `SECONDARY_CURRENT_LIMIT_AMPS` |
| Ramp, open loop (soft start) | 0.25 s to full | `OPEN_LOOP_RAMP_SECONDS` |
| Ramp, closed loop (PID, future use) | 0.25 s to full | `CLOSED_LOOP_RAMP_SECONDS` |
| Max output | 25% | `MAX_OUTPUT` |
| Max turn | 20% | `MAX_TURN_OUTPUT` |
| Temp warning | 55 C | `TEMP_WARN_C` |
| Temp shutdown | 70 C, resumes at 50 C | `TEMP_SHUTDOWN_C`, `TEMP_RESUME_C` |
| Stall shutdown | >15 A, <60 motor RPM; counter reaches 2 s | `Safety` class |
| Lost-signal stop | 0.1 s watchdog | built into DifferentialDrive |
| Idle mode | Coast (brake in autonomous) | `BRAKE_IN_AUTONOMOUS` |
| Logging | USB drive only | `ONLY_LOG_TO_USB` |

### Original prompt (20261108)

This is the request, written by John Blue, that started this project. It is copied
exactly as typed, including typos. The temperature line ends mid-sentence; the
thresholds in `Constants.java` (55 C warning, 70 C shutdown) were chosen afterward.
Later changes (10:1 gearing, turning on the right stick, 0.25 s ramps, brake in
autonomous, USB logging) came from follow-up requests and are listed in the
change history above.

```text
i have a FIRST Roboticw competition robot to write code for

The robot has a roborio 1 (not a roborio 2).

There are 4 motors, each a neo 550 (see https://www.revrobotics.com/rev-21-1651/ and https://www.revrobotics.com/content/docs/REV-21-1651-DS.pdf )

There are 4 sparkmax motor controllers (see https://www.revrobotics.com/rev-11-2158/ )

Each motor has a wheel that is 4 inches in diameter and 1 inch wide. The wheels have a 1/2 inch hex shaft. "The inside black portion of these wheels are made from molded polycarbonate plastic. The outer tread is TPU (Thermoplastic PolyUrethane). This robust but lightweight wheel provides good traction on carpet and soft tiles." via andymark https://andymark.com/collections/compliant-intake/products/stealth-and-sushi-wheels

The each neo 550 motor has a versaplanetary gear box but the motor drives the 1/2 inch hex shaft directly (no gears inside the versaplanetary).

The wheels and neos and sparkmaxe motor are organized as follows 

the wheel base is rectangular 

the wheel base width (outside edge of left side wheels to outside edge of right side wheel) is 218 millimeters wide

The wheel bas length (center of axel front to center of axel back) is 229 millimeters

The front left wheel is can id 5
The front right wheel is can id 8
The back left wheel is can id 6
The back right wheel is can id 7

The roborio can id is 0

Rev power distribution hub can id is 1

The robot is using older wifi hotspot radio using an OM5P.

The OM5P is powered from the Rev power distribution hub with a rev radio power module.


When the joystick is in neutral the motors are stopped.

Setup the neo so the motor temperture is monitored and if it goes above 

All sparkmaxes should have idle mode set to coast

set all sparkaxes Smart Current Limit to 20A see https://www.revrobotics.com/neo-550-brushless-motor-locked-rotor-testing/

Generate code to allow the robot to be driven with an Xbox controller
=> with the left side joystick controlling going forward = robot going forward.
=> with the left side joystick controlling going back = robot going backward.

The speed of the motors going forward or backward should be defined by the joystick 
=> pushing forward a bit should be really slow. 
=> pushing forward a all the way should be really 25% of the max
=> we are working to see the robot move, not go all out fast.

Keep the robot safe => avoid motor burn out, avoid sparkmeax burnout, avoid circuit breakers being blown.
```
