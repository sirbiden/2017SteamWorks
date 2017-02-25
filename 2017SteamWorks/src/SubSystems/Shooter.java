package SubSystems;        

import com.ctre.CANTalon;
import com.ctre.CANTalon.FeedbackDevice;
import com.ctre.CANTalon.TalonControlMode;
import com.ctre.CANTalon.VelocityMeasurementPeriod;

import Utilities.Constants;
import Utilities.Ports;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Shooter {
    private static Shooter instance = null;
    public static enum Status{
		OFF, STARTED, WAITING, READY
	}
    private Status status = Status.OFF;
    private CANTalon motor1,motor2;
	public Shooter(){
		motor1 = new CANTalon(Ports.SHOOTER_MOTOR_MASTER);
    	motor1.setEncPosition(0);
    	motor1.setFeedbackDevice(FeedbackDevice.QuadEncoder);
    	motor1.reverseSensor(true);
    	motor1.reverseOutput(false);
    	motor1.configEncoderCodesPerRev(4096/4);
    	motor1.configNominalOutputVoltage(+0f, -0f);
    	motor1.configPeakOutputVoltage(12f, -0f);
    	motor1.setAllowableClosedLoopErr(0); 
    	motor1.changeControlMode(TalonControlMode.Speed);
    	motor1.set(0);        
    	motor1.setPID(2, 0.00, 30, 0.023, 0, 0.0, 0); //p at 0.25 originally, 2p and 30d works well
    	motor1.SetVelocityMeasurementPeriod(VelocityMeasurementPeriod.Period_10Ms);
    	motor1.SetVelocityMeasurementWindow(32);
    	
		motor2 = new CANTalon(Ports.SHOOTER_MOTOR_SLAVE);
        motor2.changeControlMode(TalonControlMode.Follower);
        motor2.set(Ports.SHOOTER_MOTOR_MASTER);
        motor2.reverseOutput(true);
	}
	public static Shooter getInstance()
    {
        if( instance == null )
            instance = new Shooter();
        return instance;
    }
    
    public void update(){
    	switch(status){
		    case STARTED:
		    	setSpeed(Constants.SHOOTING_SPEED);
//		    	setSpeed(.90);
		    	status = Status.WAITING;
		    	break;
		    case WAITING:
		    	if(onTarget()){
		    		status = Status.READY;
		    	}
		    	break;
		    case READY:
		    	
		    	break;
		    case OFF:
		    	setSpeed(0);
		    	break;
		    default:
    	}
    	
    	SmartDashboard.putNumber("SHOOTER_SPEED", getSpeed());
		SmartDashboard.putNumber("SHOOTER_TARGET", motor1.getSetpoint());
    }
    public void setSpeed(double speed){
    	motor1.set(speed);
//    	motor2.set(-speed);
    }
    public void setState(Status newState){
    	status = Status.STARTED;
    }
    public void stop(){
    	status = Status.OFF;
    }
    public double getSpeed(){
    	return motor1.getSpeed();
    }
    /*
    public void bumpUp(double increase){
    	leftShooter.setSpeed(leftShooter.motor.get() + increase);
    	rightShooter.setSpeed(leftShooter.motor.get() + increase);
    }
    public void bumpDown(double decrease){
    	leftShooter.setSpeed(leftShooter.motor.get() - decrease);
    	rightShooter.setSpeed(leftShooter.motor.get() - decrease);
    }*/
    public boolean onTarget(){
    	if(Math.abs(motor1.getSetpoint() - motor1.getSpeed()) < Constants.SHOOTER_ERROR){
    		return true;
    	}
    	return false;
    }
    
}
