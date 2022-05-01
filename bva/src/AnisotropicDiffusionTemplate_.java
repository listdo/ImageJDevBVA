import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;

import static java.lang.Math.exp;
import static java.lang.Math.pow;

public class AnisotropicDiffusionTemplate_ implements PlugInFilter {


    public int setup(String arg, ImagePlus imp) {
        if (arg.equals("about")) {
            showAbout();
            return DONE;
        }
        return DOES_8G + DOES_STACKS + SUPPORTS_MASKING;
    } //setup


    public void run(ImageProcessor ip) {
        byte[] pixels = (byte[]) ip.getPixels();
        int width = ip.getWidth();
        int height = ip.getHeight();

        int numOfIterations = 15;
        int convMaskRadius = 1;
        double kappa = 30.0;
        int diffFunctOption = 1; //mode 1 or mode 2

        //Center pixel distances ==> to ensure lower weight for diagonal neighbours
        double dx = 1.0;
        double dy = 1.0;
        double dd = Math.sqrt(2.0);

        double[][] hN = new double[][]{
                {0.0, 1.0, 0.0},
                {0.0, -1.0, 0.0},
                {0.0, 0.0, 0.0}};

        double[][] hNE = new double[][]{
                {0.0, 0.0, 1.0},
                {0.0, -1.0, 0.0},
                {0.0, 0.0, 0.0}};

        double[][] hE = new double[][]{
                {0.0, 0.0, 0.0},
                {0.0, -1.0, 1.0},
                {0.0, 0.0, 0.0}};

        double[][] hSE = new double[][]{
                {0.0, 0.0, 0.0},
                {0.0, -1.0, 0.0},
                {0.0, 0.0, 1.0}};

        double[][] hS = new double[][]{
                {0.0, 0.0, 0.0},
                {0.0, -1.0, 0.0},
                {0.0, 1.0, 0.0}};

        double[][] hSW = new double[][]{
                {0.0, 0.0, 0.0},
                {0.0, -1.0, 0.0},
                {1.0, 0.0, 0.0}};

        double[][] hW = new double[][]{
                {0.0, 0.0, 0.0},
                {1.0, -1.0, 0.0},
                {0.0, 0.0, 0.0}};

        double[][] hNW = new double[][]{
                {1.0, 0.0, 0.0},
                {0.0, -1.0, 0.0},
                {0.0, 0.0, 0.0}};

        int[][] inDataArrInt = ImageJUtility.convertFrom1DByteArr(pixels, width, height);
        double[][] outImg = ImageJUtility.convertToDoubleArr2D(inDataArrInt, width, height);

        //now do some iterations
        for (int iter = 0; iter < numOfIterations; iter++) {
            //do convolutions in all directions; nabla == gradient symbol (triangle with top-down);
            double[][] nablaN = ConvolutionFilter.convolveDouble(outImg, width, height, hN, convMaskRadius);
            double[][] nablaNE = ConvolutionFilter.convolveDouble(outImg, width, height, hNE, convMaskRadius);
            double[][] nablaSE = ConvolutionFilter.convolveDouble(outImg, width, height, hSE, convMaskRadius);
            double[][] nablaE = ConvolutionFilter.convolveDouble(outImg, width, height, hE, convMaskRadius);
            double[][] nablaS = ConvolutionFilter.convolveDouble(outImg, width, height, hS, convMaskRadius);
            double[][] nablaSW = ConvolutionFilter.convolveDouble(outImg, width, height, hSW, convMaskRadius);
            double[][] nablaW = ConvolutionFilter.convolveDouble(outImg, width, height, hW, convMaskRadius);
            double[][] nablaNW = ConvolutionFilter.convolveDouble(outImg, width, height, hNW, convMaskRadius);

            double[][] cN = new double[width][height];
            double[][] cNE = new double[width][height];
            double[][] cE = new double[width][height];
            double[][] cSE = new double[width][height];
            double[][] cS = new double[width][height];
            double[][] cSW = new double[width][height];
            double[][] cW = new double[width][height];
            double[][] cNW = new double[width][height];

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    for (int x_i = 0; x_i < width; x_i++) {
                        for (int y_i = 0; y_i < width; y_i++) {
                            //diffuse
                            if (diffFunctOption == 1) { //MODE 1
                                cN[x][y] = exp(-pow((-nablaN[x_i][y_i] / kappa), 2));
                                cNE[x][y] = exp(-pow((-nablaNE[x_i][y_i] / kappa), 2));
                                cE[x][y] = exp(-pow((-nablaE[x_i][y_i] / kappa), 2));
                                cSE[x][y] = exp(-pow((-nablaSE[x_i][y_i] / kappa), 2));
                                cS[x][y] = exp(-pow((-nablaS[x_i][y_i] / kappa), 2));
                                cSW[x][y] = exp(-pow((-nablaSW[x_i][y_i] / kappa), 2));
                                cNW[x][y] = exp(-pow((-nablaNW[x_i][y_i] / kappa), 2));
                            } else if (diffFunctOption == 2) { //MODE 2
                                cN[x][y] = 1.0 / (1.0 + Math.pow(nablaN[x_i][y_i] / kappa, 2));
                                cNE[x][y] = 1.0 / (1.0 + Math.pow(nablaNE[x_i][y_i] / kappa, 2));
                                cE[x][y] = 1.0 / (1.0 + Math.pow(nablaE[x_i][y_i] / kappa, 2));
                                cSE[x][y] = 1.0 / (1.0 + Math.pow(nablaSE[x_i][y_i] / kappa, 2));
                                cS[x][y] = 1.0 / (1.0 + Math.pow(nablaS[x_i][y_i] / kappa, 2));
                                cSW[x][y] = 1.0 / (1.0 + Math.pow(nablaSW[x_i][y_i] / kappa, 2));
                                cNW[x][y] = 1.0 / (1.0 + Math.pow(nablaNW[x_i][y_i] / kappa, 2));
                            }
                        }
                    }
                }
            }

            //now finally diffuse all pixels: aggregate weighted Cn * nablaN, aso.
            //outImg[x_i][y_i] = outImg[x_i][y_i] + normFactor * (weightN * cN[x_i][y_i] * nablaN[x_i][y_i] + weightS * ....)
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    outImg[x][y] = outImg[x][y] + dx *
                            (
                                    cN[x][y] * nablaN[x][y] +
                                            cS[x][y] * nablaS[x][y] +
                                            cE[x][y] * nablaE[x][y] +
                                            cW[x][y] * nablaW[x][y] +
                                            dd * (cNE[x][y] * nablaNE[x][y] + cSW[x][y] * nablaSW[x][y]) +
                                            dd * (cNW[x][y] * nablaNW[x][y] + cSE[x][y] * nablaSE[x][y])
                            );
                }
            }
            ImageJUtility.showNewImage(outImg, width, height, "anisotropic diffusion");
        }


        ImageJUtility.showNewImage(outImg, width, height, "anisotropic diffusion");
    } //run

    void showAbout() {
        IJ.showMessage("About Template_...",
                "this is a PluginFilter template\n");
    } //showAbout

} //class AnisotropicDiffusionTemplate_

