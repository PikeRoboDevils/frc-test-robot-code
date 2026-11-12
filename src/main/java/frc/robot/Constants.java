// Date changed: 20261111
// Author: John Blue
// Code generated with the help of Claude, an AI assistant made by Anthropic.
// What changed: Added BRAKE_IN_AUTONOMOUS (brake in autonomous, coast otherwise),
//   STALL_DRAIN_RATE for the new stall counter, and a Logging section for the USB drive.
//   All CAN IDs, current limits, ramps, caps, and temperatures unchanged.

package frc.robot;

/**
 * All the numbers for the robot live here, so students can change them in one place.
 *
 * Units: meters, seconds, amps, volts, degrees Celsius, RPM.
 */
public final class Constants {
  private Constants() {}

  /** CAN IDs. The roboRIO is the CAN host (0). No two devices may share an ID. */
  public static final class CanIds {
    public static final int PDH = 1; // REV Power Distribution Hub
    public static final int FRONT_LEFT = 5;
    public static final int BACK_LEFT = 6;
    public static final int BACK_RIGHT = 7;
    public static final int FRONT_RIGHT = 8;
  }

  /** Driver Station USB port for the Xbox controller. */
  public static final class OperatorConstants {
    public static final int DRIVER_PORT = 0;
  }

  /** Motor controller settings. */
  public static final class Drive {
    // REV locked-rotor test: a NEO 550 at a 20 A limit survived the full 220 s test.
    // At 40 A it failed in about 27 s. Kept at 20 A even with the 10:1 gearboxes.
    public static final int SMART_CURRENT_LIMIT_AMPS = 20;

    // Hard backup limit. The SPARK MAX briefly cuts output if current goes above this.
    public static final double SECONDARY_CURRENT_LIMIT_AMPS = 30.0;

    // Seconds to go from 0 to 100% output. 0.25 output takes 0.0625 s to reach.
    // Softens current spikes so the battery voltage does not sag.
    // Open loop = plain "set this % output" control (what the drive uses now).
    public static final double OPEN_LOOP_RAMP_SECONDS = 0.25;
    // Closed loop = PID control on the SPARK MAX (velocity/position). Not used yet,
    // but set now so it is already safe if PID control is added later.
    public static final double CLOSED_LOOP_RAMP_SECONDS = 0.25;

    // Makes "25%" mean 3 volts no matter how charged the battery is.
    public static final double NOMINAL_VOLTAGE = 12.0;

    // Top speed with the stick pushed all the way: 25% of full output.
    // No wheel on either side is ever sent more than this, even while turning.
    public static final double MAX_OUTPUT = 0.25;

    // Turn strength with the stick pushed all the way left or right.
    // Turning in place drags the wheels sideways, which pulls extra current,
    // so this starts lower than MAX_OUTPUT.
    public static final double MAX_TURN_OUTPUT = 0.20;

    // Stick values smaller than this count as "neutral" (motors stop).
    public static final double STICK_DEADBAND = 0.10;

    // Set to true for any side that drives backward when you push forward.
    // Swapped after the first drive test (robot drove backward on a forward stick).
    public static final boolean LEFT_INVERTED = true;
    public static final boolean RIGHT_INVERTED = false;

    // Idle mode = what the motors do when told "0":
    //   Coast: wheels roll freely to a stop (gentle, easy to push the robot by hand).
    //   Brake: motors resist turning, so the robot stops quickly in one spot.
    // true = BRAKE during autonomous (stops at an exact spot), COAST in teleop and disabled.
    // false = COAST all the time (same as before 20261111).
    public static final boolean BRAKE_IN_AUTONOMOUS = true;
  }

  /** Robot measurements (for later use: odometry, autonomous). */
  public static final class Geometry {
    public static final double WHEEL_DIAMETER_M = 0.1016; // 4 in
    public static final double WHEEL_WIDTH_M = 0.0254; // 1 in
    public static final double OUTSIDE_WIDTH_M = 0.218; // outside edge to outside edge
    // Track width is wheel center to wheel center: 0.218 - 0.0254 = 0.1926 m
    public static final double TRACK_WIDTH_M = OUTSIDE_WIDTH_M - WHEEL_WIDTH_M;
    public static final double WHEELBASE_M = 0.229; // front axle to back axle
    // VersaPlanetary 10:1 on every wheel: the motor turns 10 times for each wheel turn.
    // Top wheel speed: 11,000 RPM / 10 = 1,100 wheel RPM = about 19 ft/s at 100% output,
    // about 4.8 ft/s at the 25% MAX_OUTPUT cap.
    public static final double GEAR_RATIO = 10.0;
    public static final double WHEEL_CIRCUMFERENCE_M = Math.PI * WHEEL_DIAMETER_M; // 0.319 m
  }

  /** Protection settings. */
  public static final class Safety {
    // The motor's sensor reads lower than the real coil temperature (REV says it lags),
    // so these are set low on purpose.
    public static final double TEMP_WARN_C = 55.0; // show a warning on the dashboard
    public static final double TEMP_SHUTDOWN_C = 70.0; // stop all drive motors
    public static final double TEMP_RESUME_C = 50.0; // allow driving again once cooled

    // Stall = motor is being told to move, is pulling a lot of current, but is not turning.
    public static final double STALL_MIN_OUTPUT = 0.05; // only check when output is above 5%
    public static final double STALL_CURRENT_AMPS = 15.0;
    // MOTOR RPM (before the gearbox). With 10:1, 60 motor RPM = 6 wheel RPM, about 0.03 m/s.
    public static final double STALL_MAX_RPM = 60.0;
    // Stall counter: adds 1 second per second while stalled. Drive turns OFF at this value.
    public static final double STALL_TIME_S = 2.0;
    // While NOT stalled, the counter drains at this fraction of real time (0.5 = half speed).
    // A one-loop flicker (20 ms) only removes 0.01 s, so it can't hide a real stall.
    public static final double STALL_DRAIN_RATE = 0.5;
    public static final double STALL_COOLDOWN_S = 3.0; // minimum rest before driving again
  }

  /** Data logging to a USB flash drive plugged into the roboRIO. */
  public static final class Logging {
    // Where the roboRIO mounts a USB flash drive. Logs go in a "logs" folder on the drive.
    public static final String USB_PATH = "/u";

    // The roboRIO 1 has very little internal flash, and flash wears out with heavy writing.
    // true = if no USB drive is found at startup, turn OFF WPILib logging AND REV's
    //        Status Logger, so nothing writes to the roboRIO's internal flash.
    public static final boolean ONLY_LOG_TO_USB = true;
  }
}
