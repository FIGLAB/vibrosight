package util;

/*
 *  Similar to Matlab Jet color map
 */
public class ColorMap{
    public int rgb[] = new int[3];
    public ColorMap() {}
    public int[] getColor(float x) {
      int[] rgb = new int[3];
      float a; // alpha
      if(x < 0.f) {
          rgb[0] = 0;
          rgb[1] = 0;
          rgb[2] = 0;
      }
      else if (x < 0.125f) {
          a = x/0.125f;
          rgb[0] = 0;
          rgb[1] = 0;
          rgb[2] = (int)(255*(0.5f+0.5f*a));
      }
      else if (x < 0.375f) {
          a = (x - 0.125f)/0.25f;
          rgb[0] = 0;
          rgb[1] = (int)(255*a);
          rgb[2] = 255;
      }
      else if (x < 0.625f) {
          a = (x - 0.375f)/0.25f;
          rgb[0] = (int)(255*a);
          rgb[1] = 255;
          rgb[2] = (int)(255*(1.f-a));
      }
      else if (x < 0.875f) {
          a = (x - 0.625f)/0.25f;
          rgb[0] = 255;
          rgb[1] = (int)(255*(1.f-a));
          rgb[2] = 0;
      }
      else if (x <= 1.0f) {
          a = (x - 0.875f)/0.125f;
          rgb[0] = (int)(255*(1.f-0.5f*a));
          rgb[1] = 0;
          rgb[2] = 0;
      }
      else {
          rgb[0] = 255;
          rgb[1] = 255;
          rgb[2] = 255;
      }
      
      return rgb;
    }
}