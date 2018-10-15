package serial;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

/*
 * Serial Reader primitives 
 */
public class DelimiterSerialReader implements SerialPortEventListener {
	SerialPort port;
	DelimiterSerialCallBack callback;
	ByteArraySearcher patsearcher;
	byte[] pattern;
	byte[] buffer;
	int bufpos;

	public DelimiterSerialReader(SerialPort port, DelimiterSerialCallBack callback, byte[] pattern) throws SerialPortException {
		this.buffer = new byte[256];
		this.bufpos = 0;
		this.port = port;
		this.callback = callback;
		this.patsearcher = new ByteArraySearcher(pattern);
		this.pattern = pattern;
		port.addEventListener(this, SerialPort.MASK_RXCHAR);
	}

	private void handleReadBuffer(byte[] read) {
		if(buffer.length < read.length + bufpos) {
			int newsize = Math.max(buffer.length << 1, read.length + bufpos);
			byte[] newBuffer = new byte[newsize];
			System.arraycopy(buffer, 0, newBuffer, 0, bufpos);
			buffer = newBuffer;
		}
		System.arraycopy(read, 0, buffer, bufpos, read.length);

		// Search for the delimiter
		int searchstart = Math.max(bufpos - pattern.length + 1, 0);
		bufpos += read.length;
		while(true) {
			int index = patsearcher.searchHayStack(buffer, searchstart, bufpos - searchstart);

			if(index != -1) {
				callback.onBuffer(buffer, index);
				index += pattern.length; 
				searchstart = 0;
				bufpos -= index;
				System.arraycopy(buffer, index, buffer, 0, bufpos);
			} else {
				break;
			}
		}
	}

	@Override
	public void serialEvent(SerialPortEvent event) {
		if (event.getEventType() != SerialPortEvent.RXCHAR) {
			return;
		}
		try {
			while(port.getInputBufferBytesCount() > 0) {
				handleReadBuffer(port.readBytes());
			}
		} catch (SerialPortException e) {
	        throw new RuntimeException("Error from serial port " + e.getPortName() + ": " + e.getExceptionType());
	    }
	}
}


