package PIDController;

import android.R.bool;
import android.R.integer;
import android.R.layout;
import android.util.Log;

public class PIDController {
	
	private double _minOutput;
	private double _maxOutput;
	private long _lastTime;
	private double _lastInput;
	public long _sampleTime;
	private Direction _controllerDirection;
	private double _kp;
	private double _ki;
	private double _kd;
	private boolean _inAuto = false;
	
	public double Output;
	public double Input;
	public double SetPoint;
	public double ITerm;
	
	private double _dispKp;
	private double _dispKi;
	private double _dispKd;
	
	public double GetKp()
	{
		return _dispKp;
	}
	
	public double GetKi()
	{
		return _dispKi;
	}
	
	public double GetKd()
	{
		return _dispKd;
	}
	
	public enum Direction
	{
		normal,
		reverse
	}
	
	public PIDController(double setpoint, double kp,double ki,double kd, Direction ControllerDirection)
	{
		SetPoint = setpoint;
		_inAuto = false;
		
		SetOutputLimits(-500,500);
		_sampleTime = 1000/50;
		
		SetControllerDirection(ControllerDirection);
		SetTunings(kp, ki, kd);
		
		_lastTime = System.currentTimeMillis() - _sampleTime;
	}
	
	
	public void SetTunings(double Kp,double Ki,double Kd)
	{
		if(Kp<0 || Ki<0|| Kd<0)return;
		
		_dispKp = Kp;
		_dispKi = Ki;
		_dispKd = Kd;
		
		double sampleTimeInSec = (((double)_sampleTime/1000.0f));
		_kp = Kp;
		_ki = Ki * sampleTimeInSec;
		_kd = Kd / sampleTimeInSec;
		
		if(_controllerDirection == Direction.reverse)
		{
			_kp = (0-_kp);
			_ki = (0-_ki);
			_kd = (0-_kd);
		}
	}
	
	public void SetSampleTime(long NewSampleTime)
	{
		if(NewSampleTime > 0)
		{
			double ratio = (double)NewSampleTime / (double)_sampleTime;
			
			_ki *= ratio;
			_kd /= ratio;
			_sampleTime = NewSampleTime;
		}
	}
	
	
	public void SetOutputLimits(double min,double max)
	{
		if(min>=max)return;
		
		_minOutput = min;
		_maxOutput = max;
		
		if(_inAuto)
		{
			if(Output>_maxOutput)Output = _maxOutput;
			else if(Output < _minOutput) Output = _minOutput;
			
			if(ITerm>_maxOutput)ITerm = _maxOutput;
			else if(ITerm < _minOutput) ITerm = _minOutput;
		}
	}
	
	public void SetMode(boolean auto)
	{
		if(auto  && !_inAuto)
		{
			Initialize();
		}
		
		_inAuto=auto;
	}
	
	public void Initialize()
	{
		ITerm = Output;
		_lastInput = Input;
		
		if(ITerm>_maxOutput)ITerm = _maxOutput;
		else if(ITerm < _minOutput) ITerm = _minOutput;
	}
	
	public void SetControllerDirection(Direction direction)
	{
		if(_inAuto && direction != _controllerDirection)
		{
			_kp = (-_kp);
			_ki = (-_ki);
			_kd = (-_kd);
		}
		
		_controllerDirection = direction;
	}
	
	public boolean Compute()
	{
		if(!_inAuto)return false;
		
		long now = System.currentTimeMillis();
		long timeChange = now - _lastTime;
		
		if(timeChange >= _sampleTime)
		{
			double error = SetPoint - Input;
			
			
			
			ITerm += _ki*error;
			
			if(ITerm > _maxOutput) ITerm = _maxOutput;
			else if(ITerm < _minOutput) ITerm = _minOutput;
			
			double dInput = Input - _lastInput;
			double output = _kp * error + ITerm - _kd * dInput;
			
			//Log.d("pid", "output: P="+_kp * error+" I="+ITerm+" D="+- _kd * dInput);
			//Log.d("pid", "kd:"+_kd);
			
			if(output > _maxOutput) output = _maxOutput;
			else if(output < _minOutput) output = _minOutput;
			
			Output = output;
			
			_lastInput = Input;
			_lastTime = now;
			
			return true;
		}
		
		return false;
	}

}
