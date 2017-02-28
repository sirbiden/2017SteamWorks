package ControlSystem;

import java.util.ArrayList;
import java.util.List;

import Helpers.InterpolatingDouble;
import Helpers.InterpolatingTreeMap;
import Helpers.RigidTransform2d;
import Helpers.Rotation2d;
import Helpers.Translation2d;
import IO.Logger;
import SubSystems.DistanceController;
import SubSystems.TargetInfo;
import SubSystems.Turret;
import Utilities.Constants;
import Utilities.Util;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class FSM {

	public enum State{
    	DEFAULT, INIT,SHOOTER_STARTED,SHOOTER_WAITING,SHOOTER_READY,SHOOTER_SHOOTING, GEAR_GRAB, GEAR_SCORE, INTAKE_BALLS
    }
	private RoboSystem robot = RoboSystem.getInstance();
	private static FSM instance = null;
	public partsUpdate pu;
	private DistanceController dist;
	private State currentState = State.DEFAULT;
    protected InterpolatingTreeMap<InterpolatingDouble, RigidTransform2d> field_to_vehicle_;
    protected RigidTransform2d.Delta vehicle_velocity_;
    protected InterpolatingTreeMap<InterpolatingDouble, Rotation2d> turret_rotation_;
 //   protected GoalTracker goal_tracker_;
    protected Rotation2d camera_pitch_correction_;
    protected Rotation2d camera_yaw_correction_;
    protected double differential_height_;
    private Logger logger = Logger.getInstance();
    public static FSM getInstance()
    {
        if( instance == null )
            instance = new FSM();
        return instance;
    }
        
    public FSM() {
    	SmartDashboard.putString("FSM", "STARTED");
        pu = new partsUpdate();
    	pu.start();
    	dist = DistanceController.getInstance();
    	reset(0, new RigidTransform2d(), new Rotation2d());
    }
   
    public class partsUpdate extends Thread{
        private boolean keepRunning = true;
    	public void run(){
    		SmartDashboard.putString("FSM", "THREAD STARTED");
    		while(keepRunning){
				update();
				Timer.delay(0.01); //10ms Loop Rate
    		} 
        }
        public void kill(){
        	keepRunning = false;
        }
    }

    public void update(){ 
//    	robot.vision.update();
        robot.dt.update();
        robot.intake.update();
        robot.sweeper.SweeperDebug();
        robot.turret.update(robot.intake.getCurrentAngle());
        dist.update();
        robot.gearIntake.update();
        robot.shooter.update();
        
        switch(currentState){
        
        
        }
    }
    public synchronized void reset(double start_time, RigidTransform2d initial_field_to_vehicle,
            Rotation2d initial_turret_rotation) {
//        field_to_vehicle_ = new InterpolatingTreeMap<>(kObservationBufferSize);
//        field_to_vehicle_.put(new InterpolatingDouble(start_time), initial_field_to_vehicle);
//        vehicle_velocity_ = new RigidTransform2d.Delta(0, 0, 0);
//        turret_rotation_ = new InterpolatingTreeMap<>(kObservationBufferSize);
 //       turret_rotation_.put(new InterpolatingDouble(start_time), initial_turret_rotation);
//        goal_tracker_ = new GoalTracker();
        camera_pitch_correction_ = Rotation2d.fromDegrees(-Constants.kCameraPitchAngleDegrees);
        camera_yaw_correction_ = Rotation2d.fromDegrees(-Constants.kCameraYawAngleDegrees);
        differential_height_ = Constants.kCenterOfTargetHeight - Constants.kCameraZOffset;
    }
    public void addVisionUpdate(double timestamp, List<TargetInfo> vision_update) {
    	
        List<Translation2d> field_to_goals = new ArrayList<>();
//        RigidTransform2d field_to_camera = getFieldToCamera(timestamp);
        if (!(vision_update == null || vision_update.isEmpty())) {
        	SmartDashboard.putBoolean("VisionUpdate", true);
            for (TargetInfo target : vision_update) {
            	SmartDashboard.putNumber("TargetX", target.getX());
                SmartDashboard.putNumber("TargetY", target.getY());
                double ydeadband = (target.getY() > -Constants.kCameraDeadband
                        && target.getY() < Constants.kCameraDeadband) ? 0.0 : target.getY();

                // Compensate for camera yaw
                double xyaw = target.getX() * camera_yaw_correction_.cos() + ydeadband * camera_yaw_correction_.sin();
                double yyaw = ydeadband * camera_yaw_correction_.cos() - target.getX() * camera_yaw_correction_.sin();
                double zyaw = target.getZ();
                // Compensate for camera pitch
                double xr = zyaw * camera_pitch_correction_.sin() + xyaw * camera_pitch_correction_.cos();
                double yr = yyaw;
                double zr = zyaw * camera_pitch_correction_.cos() - xyaw * camera_pitch_correction_.sin();
                // find intersection with the goal
                if (zr > 0) {
                    double scaling = differential_height_ / zr;
                    double distance = Math.hypot(xr, yr) * scaling;
                    Rotation2d angle = new Rotation2d(xr, yr, true);
                    SmartDashboard.putNumber("Target_hypot", Math.hypot(xr, yr));
                    SmartDashboard.putNumber("Target_Angle", angle.getDegrees());
                    double targetDirection = robot.turret.getAngle()-angle.getDegrees();
                    SmartDashboard.putNumber("Angle to Target", targetDirection);
                    double sine = Math.sin(Math.toRadians(270-targetDirection));
                    SmartDashboard.putNumber("Target Sine", sine);
                    double cosine = Math.cos(Math.toRadians(270-targetDirection));
                    SmartDashboard.putNumber("Target Cosine", cosine);
                    double Dx = cosine * distance;
                    SmartDashboard.putNumber("Dx",Dx);
                    double Dy = sine * distance;
                    SmartDashboard.putNumber("Dy", Dy);
                    double x_offset = Constants.kBlueSideHopperX - (Dx);
                    double y_offset = Constants.kBlueSideHopperY - (Dy);
                    SmartDashboard.putNumber("Target_offset_x", x_offset);
                    SmartDashboard.putNumber("Target_offset_y", y_offset);
                    if(robot.turret.getCurrentState() == Turret.State.VisionTracking){
                    	robot.turret.moveDegrees(angle.getDegrees());
             //       	robot.dt.setOffsets(x_offset, y_offset);
                    }
                    else if(robot.turret.getCurrentState() == Turret.State.CalculatedTracking){
                    	double calculatedAngle = Math.atan((robot.dt.getRobotXInch()-Constants.kBlueSideHopperX)/(robot.dt.getRobotYInch()-Constants.kBlueSideHopperY));
                        SmartDashboard.putNumber("Est_Angle_Target", Math.toDegrees(calculatedAngle));
                        double robotAdjustedAngle = -Util.boundAngleNeg180to180Degrees(robot.intake.getCurrentAngle()-180 - Math.toDegrees(calculatedAngle));
                        SmartDashboard.putNumber("RobAdjustAngle", robotAdjustedAngle);
                        if(robot.turret.getCurrentState() == Turret.State.CalculatedTracking)
                        	robot.turret.setAngle(robotAdjustedAngle);
                    }
                    SmartDashboard.putNumber("Target_distance", distance);
                    
//                    field_to_goals.add(field_to_camera
 //                           .transformBy(RigidTransform2d
 //                                   .fromTranslation(new Translation2d(distance * angle.cos(), distance * angle.sin())))
 //                           .getTranslation());
                }
            }
        }else{
        	SmartDashboard.putNumber("TargetX", 0);
            SmartDashboard.putNumber("TargetY", 0);
            SmartDashboard.putBoolean("VisionUpdate", false);
            double calculatedAngle = Math.atan((robot.dt.getRobotXInch()-Constants.kBlueSideHopperX)/(robot.dt.getRobotYInch()-Constants.kBlueSideHopperY));
            SmartDashboard.putNumber("Est_Angle_Target", Math.toDegrees(calculatedAngle));
            double robotAdjustedAngle = -Util.boundAngleNeg180to180Degrees(robot.intake.getCurrentAngle()-180 - Math.toDegrees(calculatedAngle));
            SmartDashboard.putNumber("RobAdjustAngle", robotAdjustedAngle);
            if(robot.turret.getCurrentState() == Turret.State.CalculatedTracking)
            	robot.turret.setAngle(robotAdjustedAngle);
        }
        synchronized (this) {
//            goal_tracker_.update(timestamp, field_to_goals);
        }
    }

}