// Date changed: 20261111
// Author: John Blue
// Code generated with the help of Claude, an AI assistant made by Anthropic.
// What changed: Stall check now uses a counter that fills while stalled and drains slowly,
//   so a one-loop flicker can't reset it. Added setBrakeMode() for autonomous.
//   Encoders are stored in variables. Current limits and other safety logic unchanged.

package frc.robot;

import com.revrobotics.PersistMode;
import com.revrobotics.REVLibError;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.ResetMode;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;
import com.revrobotics.spark.config.SparkMaxConfig;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.Constants.CanIds;
import frc.robot.Constants.Drive;
import frc.robot.Constants.Geometry;
import frc.robot.Constants.Safety;

/**
 * Four NEO 550 motors, one SPARK MAX each, driving like a tank (left side and right side).
 *
 * <p>The front motor on each side is the "leader." The back motor on that side "follows" it, so
 * both wheels on a side always get the same output.
 */
public class Drivetrain {
  // NEO 550 is brushless. Never change this to kBrushed.
  private final SparkMax frontLeft = new SparkMax(CanIds.FRONT_LEFT, MotorType.kBrushless);
  private final SparkMax backLeft = new SparkMax(CanIds.BACK_LEFT, MotorType.kBrushless);
  private final SparkMax frontRight = new SparkMax(CanIds.FRONT_RIGHT, MotorType.kBrushless);
  private final SparkMax backRight = new SparkMax(CanIds.BACK_RIGHT, MotorType.kBrushless);

  // Each motor's built-in encoder, stored once so we don't look it up every loop.
  // Readings are MOTOR RPM (before the 10:1 gearbox).
  private final RelativeEncoder frontLeftEncoder = frontLeft.getEncoder();
  private final RelativeEncoder backLeftEncoder = backLeft.getEncoder();
  private final RelativeEncoder frontRightEncoder = frontRight.getEncoder();
  private final RelativeEncoder backRightEncoder = backRight.getEncoder();

  // The same order is used in all three lists below.
  private final SparkMax[] allMotors = {frontLeft, backLeft, frontRight, backRight};
  private final RelativeEncoder[] allEncoders = {
    frontLeftEncoder, backLeftEncoder, frontRightEncoder, backRightEncoder
  };
  private final String[] names = {"FL(5)", "BL(6)", "FR(8)", "BR(7)"};

  // DifferentialDrive includes a safety watchdog: if robot code stops sending
  // commands for 0.1 s, it stops the motors.
  private final DifferentialDrive drive = new DifferentialDrive(frontLeft::set, frontRight::set);

  // Safety state
  private boolean tempLockout = false;
  private boolean stallLockout = false;
  private double stallSeconds = 0.0; // stall counter (see checkStall)
  private double lastStallCheckTime = -1.0; // -1 = not checked yet
  private final Timer cooldownTimer = new Timer(); // how long we have rested after a stall
  private double lastRequestedOutput = 0.0;
  private boolean brakeOn = false; // baseConfig() starts in coast

  public Drivetrain() {
    configure(frontLeft, leaderConfig(Drive.LEFT_INVERTED), "FL(5)");
    configure(frontRight, leaderConfig(Drive.RIGHT_INVERTED), "FR(8)");
    // A follower copies its leader, including the leader's direction.
    configure(backLeft, followerConfig(CanIds.FRONT_LEFT), "BL(6)");
    configure(backRight, followerConfig(CanIds.FRONT_RIGHT), "BR(7)");

    drive.setDeadband(0.0); // Robot.java already applies the stick deadband
    drive.setSafetyEnabled(true);
    SmartDashboard.putBoolean("Drive/Brake mode", brakeOn);
  }

  /** Settings every SPARK MAX on this robot gets. */
  private static SparkMaxConfig baseConfig() {
    SparkMaxConfig config = new SparkMaxConfig();
    config.idleMode(IdleMode.kCoast); // saved default; autonomous switches to brake
    config.smartCurrentLimit(Drive.SMART_CURRENT_LIMIT_AMPS);
    config.secondaryCurrentLimit(Drive.SECONDARY_CURRENT_LIMIT_AMPS);
    config.openLoopRampRate(Drive.OPEN_LOOP_RAMP_SECONDS);
    config.closedLoopRampRate(Drive.CLOSED_LOOP_RAMP_SECONDS);
    config.voltageCompensation(Drive.NOMINAL_VOLTAGE);
    return config;
  }

  private static SparkMaxConfig leaderConfig(boolean inverted) {
    SparkMaxConfig config = baseConfig();
    config.inverted(inverted);
    return config;
  }

  private static SparkMaxConfig followerConfig(int leaderCanId) {
    SparkMaxConfig config = baseConfig();
    config.follow(leaderCanId); // same direction as the leader
    return config;
  }

  /**
   * Sends settings to a SPARK MAX. kResetSafeParameters clears old settings first.
   * kPersistParameters saves them so they survive a power cycle.
   * This writes to the SPARK MAX's flash memory, so it runs ONLY ONCE, at startup.
   */
  private static void configure(SparkMax motor, SparkMaxConfig config, String name) {
    REVLibError result =
        motor.configure(config, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
    if (result != REVLibError.kOk) {
      DriverStation.reportError(
          "SPARK MAX " + name + " did not accept its settings: " + result
              + ". Check CAN wiring, CAN ID, and firmware.",
          false);
    }
  }

  /**
   * Switch all four motors between brake (true) and coast (false).
   *
   * <p>Only the idle mode changes. kNoResetSafeParameters keeps every other setting
   * (current limits, ramps, inversion, following). kNoPersistParameters means this is NOT
   * saved to flash, so it is safe to call at every mode change, and the saved default
   * (coast) comes back after a power cycle.
   */
  public void setBrakeMode(boolean brake) {
    if (brake == brakeOn) {
      return; // already in that mode, nothing to send
    }
    SparkMaxConfig config = new SparkMaxConfig();
    config.idleMode(brake ? IdleMode.kBrake : IdleMode.kCoast);
    for (int i = 0; i < allMotors.length; i++) {
      REVLibError result =
          allMotors[i].configure(
              config, ResetMode.kNoResetSafeParameters, PersistMode.kNoPersistParameters);
      if (result != REVLibError.kOk) {
        DriverStation.reportError(
            "SPARK MAX " + names[i] + " did not switch idle mode: " + result, false);
      }
    }
    brakeOn = brake;
    SmartDashboard.putBoolean("Drive/Brake mode", brakeOn);
  }

  /**
   * Drive forward/backward and turn at the same time ("arcade drive").
   *
   * @param forward positive = forward, negative = backward
   * @param turn positive = turn left (counterclockwise), negative = turn right
   */
  public void arcadeDrive(double forward, double turn) {
    // Remember whether the driver is touching the stick at all (used by the stall reset).
    lastRequestedOutput = Math.max(Math.abs(forward), Math.abs(turn));
    if (tempLockout || stallLockout) {
      forward = 0.0;
      turn = 0.0;
    }

    // Mix: to turn left, the left side slows down and the right side speeds up.
    double left = forward - turn;
    double right = forward + turn;

    // If either side is above the cap, shrink BOTH sides by the same amount.
    // This keeps the turn shape the same while never going over MAX_OUTPUT.
    double biggest = Math.max(Math.abs(left), Math.abs(right));
    if (biggest > Drive.MAX_OUTPUT) {
      double scale = Drive.MAX_OUTPUT / biggest;
      left *= scale;
      right *= scale;
    }

    SmartDashboard.putNumber("Drive/Left output", left);
    SmartDashboard.putNumber("Drive/Right output", right);
    drive.tankDrive(left, right, false);
  }

  public void stop() {
    lastRequestedOutput = 0.0;
    drive.stopMotor();
  }

  /** Call this every robot loop (every 20 ms), enabled or disabled. */
  public void periodic() {
    checkTemperature();
    checkStall();
    publishDashboard();
  }

  private void checkTemperature() {
    double hottest = 0.0;
    for (SparkMax motor : allMotors) {
      hottest = Math.max(hottest, motor.getMotorTemperature());
    }

    if (!tempLockout && hottest >= Safety.TEMP_SHUTDOWN_C) {
      tempLockout = true;
      DriverStation.reportWarning(
          String.format(
              "Drive motor at %.0f C. Drive is OFF until all motors cool to %.0f C.",
              hottest, Safety.TEMP_RESUME_C),
          false);
    } else if (tempLockout && hottest <= Safety.TEMP_RESUME_C) {
      tempLockout = false;
      DriverStation.reportWarning("Drive motors cooled down. Drive is ON again.", false);
    }

    SmartDashboard.putNumber("Drive/Hottest motor C", hottest);
    SmartDashboard.putBoolean("Drive/Temp warning", hottest >= Safety.TEMP_WARN_C);
    SmartDashboard.putBoolean("Drive/Temp lockout", tempLockout);
  }

  /**
   * Stall check with a "leaky bucket" counter.
   *
   * <p>While any motor is stalled, the counter fills at real time (1 s per s). While none is
   * stalled, it drains at STALL_DRAIN_RATE (half speed). Drive turns OFF when the counter
   * reaches STALL_TIME_S. A brief flicker out of the stall only drains a tiny amount, so a real
   * stall still trips. Example: stalled 80% of the time -> fills 0.8 - 0.1 = 0.7 s per s -> trips
   * in about 2.9 s.
   */
  private void checkStall() {
    boolean anyStalled = false;
    for (int i = 0; i < allMotors.length; i++) {
      boolean pushing = Math.abs(allMotors[i].getAppliedOutput()) >= Safety.STALL_MIN_OUTPUT;
      boolean highCurrent = allMotors[i].getOutputCurrent() >= Safety.STALL_CURRENT_AMPS;
      boolean notTurning = Math.abs(allEncoders[i].getVelocity()) < Safety.STALL_MAX_RPM;
      if (pushing && highCurrent && notTurning) {
        anyStalled = true;
      }
    }

    // Time since the last check (normally 0.02 s). Capped so a long pause can't jump the count.
    double now = Timer.getFPGATimestamp();
    double dt = (lastStallCheckTime < 0.0) ? 0.02 : Math.min(now - lastStallCheckTime, 0.1);
    lastStallCheckTime = now;

    if (anyStalled) {
      stallSeconds += dt;
    } else {
      stallSeconds = Math.max(0.0, stallSeconds - dt * Safety.STALL_DRAIN_RATE);
    }

    if (!stallLockout && stallSeconds >= Safety.STALL_TIME_S) {
      stallLockout = true;
      cooldownTimer.reset();
      cooldownTimer.start();
      DriverStation.reportWarning(
          "Drive motor stalled (pushing but not turning). Drive is OFF. "
              + "Let go of the stick to reset.",
          false);
    }

    // Unlock only after resting AND the driver has let go of the stick,
    // so the robot never lurches forward by surprise.
    if (stallLockout
        && cooldownTimer.get() >= Safety.STALL_COOLDOWN_S
        && lastRequestedOutput == 0.0) {
      stallLockout = false;
      stallSeconds = 0.0;
      cooldownTimer.stop();
      DriverStation.reportWarning("Stall reset. Drive is ON again.", false);
    }

    SmartDashboard.putNumber("Drive/Stall counter s", stallSeconds);
    SmartDashboard.putBoolean("Drive/Stall lockout", stallLockout);
  }

  private void publishDashboard() {
    for (int i = 0; i < allMotors.length; i++) {
      SparkMax motor = allMotors[i];
      double motorRpm = allEncoders[i].getVelocity();
      String key = "Drive/" + names[i] + "/";
      SmartDashboard.putNumber(key + "Temp C", motor.getMotorTemperature());
      SmartDashboard.putNumber(key + "Current A", motor.getOutputCurrent());
      SmartDashboard.putNumber(key + "Output", motor.getAppliedOutput());
      SmartDashboard.putNumber(key + "Motor RPM", motorRpm);
      SmartDashboard.putNumber(key + "Wheel RPM", motorRpm / Geometry.GEAR_RATIO);
    }
    // Robot speed in meters per second, from the front-left encoder.
    // The encoder counts MOTOR turns, so divide by the gear ratio to get wheel turns.
    double wheelRps = frontLeftEncoder.getVelocity() / 60.0 / Geometry.GEAR_RATIO;
    SmartDashboard.putNumber("Drive/Speed m per s", wheelRps * Geometry.WHEEL_CIRCUMFERENCE_M);
  }
}
