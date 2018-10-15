package serial;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;
import processing.core.PApplet;

/*
 * Serial Reader 
 */
public class SerialReader implements DelimiterSerialCallBack{
	int baudrate = 9600; // does not matter, teensy uses USB protocol
	
	byte[] delimiter = { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff };

	SerialPort serialPort;
	DelimiterSerialReader reader;
	BufferCallBack callback;
	int fftSize;
	float[][] fftBuffer;
	int curBuffer = 0;

	public SerialReader(PApplet applet, BufferCallBack callback, int fftSize) {
		this.callback = callback;
		this.fftSize = fftSize;
		this.fftBuffer = new float[2][fftSize*2];

		for(String s : SerialPortList.getPortNames()) {
			if(s.contains("tty.usb")) {
				if(tryRepeatConnect(s))
					return;
			}
		}

		if(serialPort == null) {
			System.out.println("No usb serial port found!");
			System.exit(0); 
		}
	}
	
	public void writeToPort(byte send_buf[]){
		try {
			serialPort.writeBytes(send_buf);
		} catch (SerialPortException e) {
			e.printStackTrace();
		}
	}

	private boolean tryRepeatConnect(String portName) {
		System.out.println("Connecting to port " + portName + "...");
		for(int i = 0; i < 3; i++) {
			serialPort = tryConnect(portName, baudrate);
			if(serialPort == null)
				continue;

			try {
				reader = new DelimiterSerialReader(serialPort, this, delimiter);
				System.out.println("Connected!!");
				return true;
			} catch(SerialPortException e) {
				e.printStackTrace();
				continue;
			}
		}
		return false;
	}

	private static SerialPort tryConnect(String portName, int baudRate) {
		SerialPort port = null;
		try {
			port = new SerialPort(portName);
			port.openPort();
			port.setParams(baudRate, 8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			return port;
		} catch(SerialPortException e) {
			System.out.println("Failed; Retrying...");
			try {
				port.closePort();
			} catch(SerialPortException e2) {

			}
			return null;
		}
	}

	
	@Override
	public void onBuffer(byte[] buffer, int readlen) {
		if(readlen > 0 && convertBuffer(buffer, readlen, fftBuffer[curBuffer])) {
			
			// ADC stream
			if(readlen == fftBuffer[curBuffer].length *2){
				callback.onBuffer(fftBuffer[curBuffer]);
				curBuffer = 1 - curBuffer;
			}
			
			// Positional information
			if(readlen == 3){
				callback.onPositionalBuffer();
			}
		}
	}

	private static boolean convertBuffer(byte[] buffer, int readlen, float[] floatBuf) {
		if(readlen != floatBuf.length * 2 && readlen != 3) {
			System.out.println("Got bad buffer length: " + readlen);
			return false;
		}
		
		if(readlen == 3){
			return true;
		}

		for(int i = 0; i < floatBuf.length; i++) {
			int val = (buffer[2 * i] & 0xff) | ((buffer[2 * i + 1] & 0xff) << 8);
			floatBuf[i] = val;
		}
		return true;
	}

}
