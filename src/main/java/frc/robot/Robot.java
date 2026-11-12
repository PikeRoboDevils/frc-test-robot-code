// Date changed: 20261111
// Author: John Blue
// Code generated with the help of Claude, an AI assistant made by Anthropic.
// What changed: Added data logging to a USB flash drive (WPILib log + Driver Station and
//   joystick data). With no USB drive, logging is turned off to protect the roboRIO 1's
//   flash. Brake mode during autonomous, coast in teleop/disabled/test. Driving unchanged.

package frc.robot;

import com.revrobotics.util.StatusLogger;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.wpilibj.DataLogManager;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.PowerDistribution;
import edu.wpi.first.wpilibj.PowerDistribution.ModuleType;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.Constants.CanIds;
import frc.robot.Constants.Drive;
import frc.robot.Constants.Logging;
import frc.robot.Constants.OperatorConstants;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Drive with the Xbox controller ("split arcade").
 *
 * <ul>
 *   <li>LEFT stick up/down: forward/backward. Full stick = 25% of full motor output.
 *   <li>RIGHT stick left/right: turn left/right. Full stick = 20% turn strength.
 *   <li>Both sticks in the middle: motors stop.
 *   <li>Stick pushed a little: very slow.
 *   <li>No wheel is ever sent more than 25%, even when driving and turning together.
 * </ul>
 */
public class Robot extends TimedRobot {
  private final XboxController driver = new XboxController(OperatorConstants.DRIVER_PORT);
  private final Drivetrain drivetrain = new Drivetrain();
  private final PowerDistribution pdh = new PowerDistribution(CanIds.PDH, ModuleType.kRev);

  public Robot() {
    startLogging();
  }

  /**
   * Turns on data logging if a USB flash drive is plugged into the roboRIO.
   *
   * <p>WPILib's DataLogManager saves to a "logs" folder on the USB drive. It records everything
   * sent to the dashboard (temps, current, lockouts, speed). DriverStation.startDataLog adds
   * enable/disable and joystick data. REV's Status Logger (built into REVLib 2026, on by
   * default) also saves SPARK MAX data to the same USB drive. Open both kinds of files in
   * AdvantageScope.
   */
  private void startLogging() {
    boolean usb = usbDriveReady();
    SmartDashboard.putBoolean("Log/USB drive found", usb);

    if (usb || !Logging.ONLY_LOG_TO_USB) {
      DataLogManager.start(); // USB drive if found, otherwise roboRIO internal flash
      DriverStation.startDataLog(DataLogManager.getLog()); // DS control + joystick data
      SmartDashboard.putBoolean("Log/Logging on", true);
    } else {
      // Protect the roboRIO 1's small internal flash: no logging without a USB drive.
      StatusLogger.disableAutoLogging(); // stop REV's logger too
      SmartDashboard.putBoolean("Log/Logging on", false);
      DriverStation.reportWarning(
          "No USB drive found. Logging is OFF to protect the roboRIO's flash. "
              + "Plug in a FAT32 USB drive and restart robot code to log.",
          false);
    }
  }

  /**
   * True if a USB drive is mounted and writable. This is the same check WPILib's DataLogManager
   * uses to decide whether to log to the USB drive.
   */
  static boolean usbDriveReady() {
    try {
      Path usb = Paths.get(Logging.USB_PATH).toRealPath();
      return Files.isWritable(usb);
    } catch (IOException | SecurityException e) {
      return false; // no drive mounted
    }
  }

  /**
   * Turns the raw stick value into a motor output.
   *
   * <p>1. Deadband: anything under 10% is zero. 2. Square it (keeps the sign) so small pushes
   * are very gentle. 3. Scale so full stick = 25%.
   *
   * <p>Examples: stick 0.05 -> 0.000, stick 0.50 -> 0.049, stick 0.75 -> 0.130, stick 1.00 ->
   * 0.250
   */
  static double shapeStick(double raw) {
    return shapeStick(raw, Drive.MAX_OUTPUT);
  }

  /** Same deadband and squaring, with a chosen top value (used for turning). */
  static double shapeStick(double raw, double max) {
    double x = MathUtil.applyDeadband(raw, Drive.STICK_DEADBAND); // 0 to 1 after the deadband
    return Math.copySign(x * x, x) * max;
  }

  @Override
  public void robotPeriodic() {
    drivetrain.periodic();
    SmartDashboard.putNumber("Power/Battery V", RobotController.getBatteryVoltage());
    SmartDashboard.putNumber("Power/Total A", pdh.getTotalCurrent());
  }

  @Override
  public void autonomousInit() {
    // Brake so autonomous stops at an exact spot instead of rolling.
    drivetrain.setBrakeMode(Drive.BRAKE_IN_AUTONOMOUS);
  }

  @Override
  public void autonomousPeriodic() {
    drivetrain.stop(); // no autonomous routine yet
  }

  @Override
  public void teleopInit() {
    drivetrain.setBrakeMode(false); // coast for driver control
  }

  @Override
  public void teleopPeriodic() {
    // On an Xbox stick, pushing forward gives a NEGATIVE number, so flip it.
    double stickForward = -driver.getLeftY();
    // RIGHT stick turns. Pushing it LEFT gives a NEGATIVE number.
    // A left turn is positive, so flip it.
    double stickTurn = -driver.getRightX();

    double forward = shapeStick(stickForward, Drive.MAX_OUTPUT);
    double turn = shapeStick(stickTurn, Drive.MAX_TURN_OUTPUT);

    SmartDashboard.putNumber("Drive/Requested forward", forward);
    SmartDashboard.putNumber("Drive/Requested turn", turn);
    drivetrain.arcadeDrive(forward, turn);
  }

  @Override
  public void disabledInit() {
    drivetrain.stop();
    drivetrain.setBrakeMode(false); // coast, so the robot is easy to push by hand
  }

  @Override
  public void testInit() {
    drivetrain.setBrakeMode(false);
  }

  @Override
  public void testPeriodic() {
    drivetrain.stop();
  }
}
