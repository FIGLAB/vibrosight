/* Future Interfacs Group
 * Carnegie Mellon University
 * www.figlab.com
 * 
 * */

package util;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PFont;
import processing.core.PGraphics;

public class ScrollingSpectrogramLayer {
	
	////////////////////////////////////////
	//   ScrollingSpectrogramLayer:
	//   A wrapper for creating spectrograms that auto-scroll
	//   when data on screen reaches maximum width.
	////////////////////////////////////////
	PApplet delegate;	// The PApplet holding the main UI thread
	
	// Off-screen buffers
	private PGraphics mainCanvas;
	public PGraphics canvas;
	private PGraphics prevCanvas;
	private PGraphics labelCanvas;
	public int width;
	public int height;
	
	// Spectrogram Dimensions
	public int SPEC_SIZE = 64;
	public float[] spectrogram = new float[SPEC_SIZE];
	int specPointer = -1;
	
	// UI-related variables
	boolean invalidateCanvas = false;
	boolean connected = false;
	public boolean preventBuffering = true;
	public float rotation = 0;
	public float ScaleY = 1.0f;
	public float ScaleX = 1.0f;
	public boolean canvasSwitched = false;
	public boolean onFinalFrame = false;
	
	// Spectrogram Colors
	ColorMap cm = new ColorMap();
	PFont sensorFont;
	PFont labelFont;
	public double scaler = (255.0/2)/Math.log10(2048);
	
	// Constructor:
	// applet = main PApplet holding the UI thread
	// graphicsLayer = graphics context used by the PApplet
	public ScrollingSpectrogramLayer(PApplet applet, PGraphics graphicsLayer)
	{
		mainCanvas = graphicsLayer;
		canvas = applet.createGraphics(mainCanvas.width, mainCanvas.height);
		prevCanvas = applet.createGraphics(mainCanvas.width, mainCanvas.height);
		labelCanvas = applet.createGraphics(mainCanvas.width, mainCanvas.height);
		delegate = applet;
		width = graphicsLayer.width;
		height = graphicsLayer.height;
		setup();
	}
	
	public void setup()
	{
		prevCanvas.beginDraw();
		prevCanvas.background(0);
		prevCanvas.endDraw();
	}
	
	//  This function will be called by the delegate to render elements into
	//  the graphics buffer. Ideally, it is called in the draw loop.
	public void draw()
	{
		if (invalidateCanvas || preventBuffering) {
			canvas.beginDraw();
				SpectrogramView();
				invalidateCanvas = false;
			canvas.endDraw();
			
		}
		moveSpectrogram();
	}
	
	public PGraphics canvas()
	{
		if (!canvasSwitched) {
			mainCanvas.beginDraw();
				mainCanvas.background(0);
				mainCanvas.image(canvas,0,0);
				mainCanvas.image(labelCanvas,10,0);
			mainCanvas.endDraw();
		} else {
			mainCanvas.beginDraw();
				mainCanvas.background(0);
				mainCanvas.pushMatrix();
					mainCanvas.translate(-specPointer, 0);
					mainCanvas.image(prevCanvas,0,0);
					mainCanvas.image(canvas,prevCanvas.width,0);
				mainCanvas.popMatrix();
				mainCanvas.image(labelCanvas,10,0);
			mainCanvas.endDraw();
		}
		return mainCanvas;
	}
	
	///////////////////////////////
	// To render the spectrogram, loop through
	// each bin of the FFT signal, and map a color
	// value based on a color scheme (see ColorMapper.getColor())
	///////////////////////////////
	void renderSpectrogram(float[] signal)
	{
		float y = 0;
		canvas.fill(0);
		int specSize = signal.length;
		for (int i=0; i<signal.length; i++)
		{
			y = specSize - i - 1;
			
			double v = Math.max(0,scaler*Math.log10(signal[i]));
			int[] rgb = cm.getColor((float) (v/255.0));
			canvas.strokeWeight(1);
			canvas.stroke(rgb[0], rgb[1], rgb[2]);
			canvas.point(0,y);
		}
	}
	
	///////////////////////////////
	// Wrapper function called by draw()
	///////////////////////////////
	void SpectrogramView()
	{
		if (connected) {
			float k=0.0f;
			float verticalPadding = 10;
			canvas.pushMatrix();
			canvas.translate(0,(k*(verticalPadding+SPEC_SIZE*0.75f)));
			canvas.translate(specPointer,verticalPadding);
				canvas.pushMatrix();
					canvas.scale(ScaleX,ScaleY);
					try {
						renderSpectrogram(spectrogram);
					} catch (Exception e) {
						renderSpectrogram(spectrogram);
					}
				canvas.popMatrix();
			canvas.popMatrix();
		}
	}
	
	///////////////////////////////
	// Flag to determine whether a device / stream is active
	///////////////////////////////
	public void setSensorConnected(boolean v)
	{
		connected = v;
	}
	
	///////////////////////////////
	// This function automatically advances the spectrogram,
	// and updates the off-screen buffers.
	///////////////////////////////
	public void moveSpectrogram()
	{
		// Increment spectrogram position
		specPointer = specPointer + 1;
		
		if (specPointer>canvas.width) {
			
			// 1. Copy last half of canvas to the first half
			prevCanvas.beginDraw();
				prevCanvas.image(canvas,0,0);
			prevCanvas.endDraw();
			
			// 2. Draw current canvas
			canvas.beginDraw();
				canvas.background(0);
			canvas.endDraw();
			
			// 3. Set the pointer back to the halfway point
			specPointer = 0;
			if (!canvasSwitched) {
				canvasSwitched = true;
			}
		}
	}
	
	// Resets the spectrogram back to its original state
	public void resetSpectrogram()
	{
		specPointer = 0;
		delegate.textFont(sensorFont);
		canvas.textAlign(PConstants.RIGHT,PConstants.CENTER);
		onFinalFrame = false;
		canvas.beginDraw();
		canvas.background(0);
		canvas.endDraw();
	}
	
	// This function forces the spectrogram to be rendered,
	// regardless if new data is available. Function is remotely called by delegate
	public void invalidateCanvas()
	{
		invalidateCanvas = true;
	}
	
}
