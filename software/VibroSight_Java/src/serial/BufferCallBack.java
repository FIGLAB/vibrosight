package serial;

public interface BufferCallBack {
	void onBuffer(float[] buffer);
	void onPositionalBuffer();
}
