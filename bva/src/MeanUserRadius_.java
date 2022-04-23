import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

/**
 * Applies a simple mean low-pass filter on a given image. (Lab 6)
 */
public class MeanUserRadius_ implements PlugInFilter {

	public int setup(String arg, ImagePlus imp) {
		if (arg.equals("about"))
			{showAbout(); return DONE;}
		return DOES_8G+DOES_STACKS+SUPPORTS_MASKING;
	} //setup


	public void run(ImageProcessor ip) {
		byte[] pixels = (byte[])ip.getPixels();
		int width = ip.getWidth();
		int height = ip.getHeight();
		int[][] inDataArrInt = ImageJUtility.convertFrom1DByteArr(pixels, width, height);
        double[][] inDataArrDbl = ImageJUtility.convertToDoubleArr2D(inDataArrInt, width, height);

		int tgtRadius = 4; //size of mask = 2 * radius + 1

		double[][] meanKernel = ConvolutionFilter.getMeanMask(tgtRadius);
		double[][] resultImg = ConvolutionFilter.convolveDouble(inDataArrDbl, width, height, meanKernel, tgtRadius);

		//finally show the image
		ImageJUtility.showNewImage(resultImg, width, height, "mean filtered r=" + tgtRadius);

	} //run

	void showAbout() {
		IJ.showMessage("About Template_...",
			"this is a PluginFilter template\n");
	} //showAbout
} //class Template_

