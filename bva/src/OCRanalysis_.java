import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import jdk.internal.util.xml.impl.Pair;

import java.util.*;


public class OCRanalysis_ implements PlugInFilter {


    public int setup(String arg, ImagePlus imp) {
        if (arg.equals("about")) {
            showAbout();
            return DONE;
        }


        return DOES_8G + DOES_STACKS + SUPPORTS_MASKING;
    } //setup

    //-------- the defined features ----------------
    public static int F_FGcount = 0;
    public static int F_MaxDistX = 1;
    public static int F_MaxDistY = 2;
    public static int F_AvgDistanceCentroide = 3;
    public static int F_MaxDistanceCentroide = 4;
    public static int F_MinDistanceCentroide = 5;
    public static int F_Circularity = 6;
    public static int F_CentroideRelPosX = 7;
    public static int F_CentroideRelPosY = 8;
    //----------------------------------------------


    public void run(ImageProcessor ip) {
        Vector<ImageFeatureBase> featureVect = new Vector<ImageFeatureBase>();
        featureVect.add(new ImageFeature_FGcount());
        featureVect.add(new ImageFeature_MaxDistX());
        featureVect.add(new ImageFeature_MaxDistY());
        featureVect.add(new ImageFeature_AvgDistanceCentroid());

        byte[] pixels = (byte[]) ip.getPixels();
        int width = ip.getWidth();
        int height = ip.getHeight();
        int[][] inDataArrInt = ImageJUtility.convertFrom1DByteArr(pixels, width, height);

        //(1) at first do some binarization
        int FG_VAL = 0; //black letters
        int BG_VAL = 255; //white background
        int MARKER_VAL = 127;

        int[][] binaryImgArr = new int[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (inDataArrInt[x][y] > MARKER_VAL)
                    binaryImgArr[x][y] = BG_VAL;
                else
                    binaryImgArr[x][y] = FG_VAL;
            }
        }

        ImageJUtility.showNewImage(binaryImgArr, width, height, "binary image at threh = " + MARKER_VAL);

        //(2) split the image according to fire-trough or multiple region growing
        Vector<Vector<SubImageRegion>> splittedCharacters = splitCharacters(binaryImgArr, width, height, BG_VAL, FG_VAL);
        System.out.println(splittedCharacters.size());

        // let the user specify the target character
        Map<Character, double[]> dictionary = new HashMap<>();

        dictionary.put('a', getDoubles(featureVect, FG_VAL, splittedCharacters, 0, 5));
        dictionary.put('b', getDoubles(featureVect, FG_VAL, splittedCharacters, 0, 39));
        dictionary.put('c', getDoubles(featureVect, FG_VAL, splittedCharacters, 0, 9));
        dictionary.put('d', getDoubles(featureVect, FG_VAL, splittedCharacters, 0, 26));
        dictionary.put('e', getDoubles(featureVect, FG_VAL, splittedCharacters, 0, 22));
        dictionary.put('f', getDoubles(featureVect, FG_VAL, splittedCharacters, 0, 4));
        dictionary.put('g', getDoubles(featureVect, FG_VAL, splittedCharacters, 0, 7));
        dictionary.put('h', getDoubles(featureVect, FG_VAL, splittedCharacters, 0, 10));
        dictionary.put('i', getDoubles(featureVect, FG_VAL, splittedCharacters, 0, 18));
        dictionary.put('j', getDoubles(featureVect, FG_VAL, splittedCharacters, 13, 0));
        dictionary.put('k', getDoubles(featureVect, FG_VAL, splittedCharacters, 11, 17));
        dictionary.put('l', getDoubles(featureVect, FG_VAL, splittedCharacters, 0, 22));
        dictionary.put('m', getDoubles(featureVect, FG_VAL, splittedCharacters, 0, 19));
        dictionary.put('n', getDoubles(featureVect, FG_VAL, splittedCharacters, 0, 3));
        dictionary.put('o', getDoubles(featureVect, FG_VAL, splittedCharacters, 2, 1));
        dictionary.put('p', getDoubles(featureVect, FG_VAL, splittedCharacters, 2, 5));
        dictionary.put('r', getDoubles(featureVect, FG_VAL, splittedCharacters, 1, 6));
        dictionary.put('s', getDoubles(featureVect, FG_VAL, splittedCharacters, 0, 8));
        dictionary.put('t', getDoubles(featureVect, FG_VAL, splittedCharacters, 2, 2));
        dictionary.put('u', getDoubles(featureVect, FG_VAL, splittedCharacters, 0, 11));

        dictionary.put('A', getDoubles(featureVect, FG_VAL, splittedCharacters, 0, 2));
        dictionary.put('B', getDoubles(featureVect, FG_VAL, splittedCharacters, 16, 00));
        dictionary.put('I', getDoubles(featureVect, FG_VAL, splittedCharacters, 0, 0));
        dictionary.put('G', getDoubles(featureVect, FG_VAL, splittedCharacters, 2, 0));
        dictionary.put('H', getDoubles(featureVect, FG_VAL, splittedCharacters, 10, 0));
        dictionary.put('W', getDoubles(featureVect, FG_VAL, splittedCharacters, 7, 0));

        dictionary.put('U', getDoubles(featureVect, FG_VAL, splittedCharacters, 1, 20));
        dictionary.put('N', getDoubles(featureVect, FG_VAL, splittedCharacters, 4, 24));
        dictionary.put('Z', getDoubles(featureVect, FG_VAL, splittedCharacters, 19, 20));

        dictionary.put('J', getDoubles(featureVect, FG_VAL, splittedCharacters, 20, 7));
        dictionary.put('T', getDoubles(featureVect, FG_VAL, splittedCharacters, 20, 0));


        for (Map.Entry<Character, double[]> featureResArr : dictionary.entrySet()) {
            //get min/max for each feature and all possible letters ==> use for normalization of vector array
            //==> [0;1]
            //==> required for normalization
            int index = 0;
            Vector<double[]> normArr = calculateNormArr(splittedCharacters, FG_VAL, featureVect);
            for (double[] minMaxArr : normArr) {
                System.out.println("FEATURE IDX\t" + featureVect.get(index).description + "\tmin\t" + minMaxArr[0] + "\tmax\t" + minMaxArr[1]);
                index++;
            }

            int hitCount = 0; //count the number of detected characters

            for (Vector<SubImageRegion> row : splittedCharacters) {
                for (SubImageRegion letter : row) {
                    System.out.println(letter);

                    double[] test = calcFeatureArr(letter, FG_VAL, featureVect);

                    if (isMatchingChar(test, featureResArr.getValue(), normArr)) {
                        hitCount++;

                        binaryImgArr = markRegionInImage(binaryImgArr, letter, FG_VAL, MARKER_VAL);
                    }
                }
            }

            IJ.log("# of letters detected = " + hitCount);
        }


        ImageJUtility.showNewImage(binaryImgArr, width, height, "result image with marked letters");

    } //run

    private double[] getDoubles(Vector<ImageFeatureBase> featureVect, int FG_VAL, Vector<Vector<SubImageRegion>> splittedCharacters, int tgtCharRow, int tgtCharCol) {
        SubImageRegion charROI = splittedCharacters.get(tgtCharRow).get(tgtCharCol);

        ImageJUtility.showNewImage(charROI.subImgArr, charROI.width, charROI.height, "char at pos " + tgtCharRow + " / " + tgtCharCol);

        //calculate features of reference character
        double[] featureResArr = calcFeatureArr(charROI, FG_VAL, featureVect);
        printoutFeatureRes(featureResArr, featureVect);
        return featureResArr;
    }

    public int[][] markRegionInImage(int[][] inImgArr, SubImageRegion imgRegion, int colorToReplace, int tgtColor) {

        for (int x = imgRegion.startX; x < imgRegion.startX + imgRegion.width; x++) {
            for (int y = imgRegion.startY; y < imgRegion.startY + imgRegion.height; y++) {
                if (inImgArr[x][y] == colorToReplace) {
                    inImgArr[x][y] = tgtColor;
                }
            }
        }

        return inImgArr;
    }

    boolean isMatchingChar(double[] currFeatureArr, double[] refFeatureArr, Vector<double[]> normFeatureArr) {

        double matchValue = 0.0F;

        // did not use correlation coefficient this was more logic for me
        // assumption at this is that all features are made equal
        for (int i = 0; i < currFeatureArr.length; i++) {
            double normalizedF = (currFeatureArr[i] - normFeatureArr.get(i)[0]) / (normFeatureArr.get(0)[1] - normFeatureArr.get(i)[0]);
            double refF = (refFeatureArr[i] - normFeatureArr.get(i)[0]) / (normFeatureArr.get(0)[1] - normFeatureArr.get(i)[0]);

            double matchDifference = Math.abs(normalizedF - refF);

            matchValue += matchDifference;
        }

        matchValue /= currFeatureArr.length;

        // Old version, Holzhammer version funktionier bei Bildfehler nichtmehr
        // for (int i = 0; i < currFeatureArr.length; i++) {
        //     if (currFeatureArr[i] != refFeatureArr[i])
        //         return false;
        // }

        // the value 0.0175 was manually tested
        return !(matchValue > 0.0175);
    }


    void printoutFeatureRes(double[] featureResArr, Vector<ImageFeatureBase> featuresToUse) {
        IJ.log("========== features =========");
        for (int i = 0; i < featuresToUse.size(); i++) {
            IJ.log("res of F " + i + ", " + featuresToUse.get(i).description + " is " + featureResArr[i]);
        }
    }


    double[] calcFeatureArr(SubImageRegion region, int FGval, Vector<ImageFeatureBase> featuresToUse) {
        //TODO implementation required
        double[] featureResArr = new double[featuresToUse.size()];
        int idx = 0;
        for (ImageFeatureBase ifb : featuresToUse) {
            double resVal = ifb.CalcFeatureVal(region, FGval);
            featureResArr[idx] = resVal;
            idx++;
        }

        return featureResArr;
    }

    Vector<double[]> calculateNormArr(Vector<Vector<SubImageRegion>> inputRegions, int FGval, Vector<ImageFeatureBase> featuresToUse) {
        Vector<double[]> returnVec = new Vector<>();

        for (ImageFeatureBase ifb : featuresToUse) {
            double[] resArrMinMax = new double[3];

            double minValue = Double.MAX_VALUE;
            double maxValue = Double.MIN_VALUE;
            double meanValue = 0;
            int count = 0;

            // get all extracted chars for min/max search
            for (Vector<SubImageRegion> rowVector : inputRegions) {
                for (SubImageRegion charReg : rowVector) {
                    double featureVal = ifb.CalcFeatureVal(charReg, FGval);

                    if (featureVal < minValue)
                        minValue = featureVal;

                    if (featureVal > maxValue)
                        maxValue = featureVal;

                    meanValue += featureVal;
                    count++;
                }
            }

            resArrMinMax[0] = minValue;
            resArrMinMax[1] = maxValue;
            resArrMinMax[2] = meanValue / count;

            returnVec.add(resArrMinMax);
        }

        return returnVec;
    }

    //outer Vector ==> lines, inner vector characters per line, i.e. columns
    public Vector<Vector<SubImageRegion>> splitCharacters(int[][] inImg, int width, int height, int BG_val, int FG_val) {
        Vector<Vector<SubImageRegion>> returnCharMatrix = new Vector<Vector<SubImageRegion>>();

        // iterate all lines
        for (int y = 0; y < height; ) {
            if (!isEmptyRow(inImg, width, y, BG_val)) {
                int startX = 0; // line for subimage
                int startY = y;

                int columnCount = 1;

                while (((y + columnCount) < height) && (!isEmptyRow(inImg, width, y + columnCount, BG_val))) { // while lines and not empty
                    columnCount++;
                }

                SubImageRegion row = new SubImageRegion(startX, startY, width, columnCount, inImg);
                Vector<SubImageRegion> charsInRow = splitCharactersVertically(row, BG_val, FG_val, inImg);

                returnCharMatrix.add(charsInRow);

                y += (columnCount + 1);
            } else { // if new line
                y++;
            } // else - empty row to be skipped
        }

        return returnCharMatrix;
    }

    public Vector<SubImageRegion> splitLine(int[][] inImg, int width, int height, int BG_val) {


        return null;
    }

    public Vector<SubImageRegion> splitCharactersVertically(SubImageRegion rowImage, int BG_val, int FG_val, int[][] origImg) {
        Vector<SubImageRegion> returnCharArr = new Vector<SubImageRegion>();

        for (int x = 0; x < rowImage.width; ) {
            if (!isEmptyColumn(rowImage.subImgArr, rowImage.height, x, BG_val)) {
                int startX = x;
                int startY = 0;
                int rowCount = 1;

                while (((x + rowCount) < rowImage.width) &&
                        (!isEmptyColumn(rowImage.subImgArr, rowImage.height, x + rowCount, BG_val))) {
                    rowCount++;
                }

                SubImageRegion charReg = new SubImageRegion(startX + rowImage.startX, startY + rowImage.startY, rowCount, rowImage.height, origImg);

                returnCharArr.add(charReg);
                x += (rowCount + 1);
            } // new char found
            else {
                x++;
            }
        }

        return returnCharArr;
    }

    //probably useful helper method
    public boolean isEmptyRow(int[][] inImg, int width, int rowIdx, int BG_val) {
        for (int x = 0; x < width; x++) {
            if (inImg[x][rowIdx] != BG_val) {
                return false;
            }
        }

        return true;
    }

    //probably useful helper method
    public boolean isEmptyColumn(int[][] inImg, int height, int colIdx, int BG_val) {
        for (int y = 0; y < height; y++) {
            if (inImg[colIdx][y] != BG_val) {
                return false;
            }
        }

        return true;
    }


    void showAbout() {
        IJ.showMessage("About Template_...",
                "this is a RegionGrowing_ template\n");
    } //showAbout


    //the features to implement


    /*
    class ImageFeatureF_FGcount extends ImageFeatureBase {

        public ImageFeatureF_FGcount() {
            this.description = "Pixelanzahl";
        }

        public double CalcFeatureVal(SubImageRegion imgRegion, int FG_val) {
            return -1; //TODO implementation required
        }
    }

    class ImageFeatureF_MaxDistX extends ImageFeatureBase {

        public ImageFeatureF_MaxDistX() {
            this.description = "maximale Ausdehnung in X-Richtung";
        }

        public double CalcFeatureVal(SubImageRegion imgRegion, int FG_val) {
            return -1; //TODO implementation required
        }

    }

    class ImageFeatureF_MaxDistY extends ImageFeatureBase {

        public ImageFeatureF_MaxDistY() {
            this.description = "maximale Ausdehnung in Y-Richtung";
        }

        public double CalcFeatureVal(SubImageRegion imgRegion, int FG_val) {
            return -1; //TODO implementation required
        }

    }

    class ImageFeatureF_AvgDistanceCentroide extends ImageFeatureBase {

        public ImageFeatureF_AvgDistanceCentroide() {
            this.description = "mittlere Distanz zum Centroide";
        }

        public double CalcFeatureVal(SubImageRegion imgRegion, int FG_val) {
            return -1; //TODO implementation required
        }
    }

    class ImageFeatureF_MaxDistanceCentroide extends ImageFeatureBase {

        public ImageFeatureF_MaxDistanceCentroide() {
            this.description = "maximale Distanz zum Centroide";
        }

        public double CalcFeatureVal(SubImageRegion imgRegion, int FG_val) {
            return -1; //TODO implementation required
        }
    }

    class ImageFeatureF_MinDistanceCentroide extends ImageFeatureBase {

        public ImageFeatureF_MinDistanceCentroide() {
            this.description = "minimale Distanz zum Centroide";
        }

        public double CalcFeatureVal(SubImageRegion imgRegion, int FG_val) {
            return -1; //TODO implementation required
        }

    }

    class ImageFeatureF_Circularity extends ImageFeatureBase {

        public ImageFeatureF_Circularity() {
            this.description = "Circularit??t";
        }

        public double CalcFeatureVal(SubImageRegion imgRegion, int FG_val) {
            return -1; //TODO implementation required
        }

    }

    class ImageFeatureF_CentroideRelPosX extends ImageFeatureBase {

        public ImageFeatureF_CentroideRelPosX() {
            this.description = "relative x-Position des Centroide";
        }

        public double CalcFeatureVal(SubImageRegion imgRegion, int FG_val) {
            return -1; //TODO implementation required
        }

    }

    class ImageFeatureF_CentroideRelPosY extends ImageFeatureBase {

        public ImageFeatureF_CentroideRelPosY() {
            this.description = "relative y-Position des Centroide";
        }

        public double CalcFeatureVal(SubImageRegion imgRegion, int FG_val) {
            return -1; //TODO implementation required
        }

    }
    */

    public abstract class ImageFeatureBase {

        String description;

        public abstract double CalcFeatureVal(SubImageRegion imgRegion, int FG_val);

    }

    public class ImageFeature_FGcount extends ImageFeatureBase {

        public ImageFeature_FGcount() {
            this.description = "FG Count";
        }

        @Override
        public double CalcFeatureVal(SubImageRegion imgRegion, int FG_val) {
            int sum = 0;
            for (int x = 0; x < imgRegion.width; x++) {
                for (int y = 0; y < imgRegion.height; y++) {
                    if (imgRegion.subImgArr[x][y] == FG_val)
                        sum++;
                } //for y
            } //for x

            return sum;
        }
    }

    public class ImageFeature_MaxDistX extends ImageFeatureBase {

        public ImageFeature_MaxDistX() {
            this.description = "ImageFeature_MaxDistX";
        }

        @Override
        public double CalcFeatureVal(SubImageRegion imgRegion, int FG_val) {

            return imgRegion.width;
        }
    }

    public class ImageFeature_MaxDistY extends ImageFeatureBase {

        public ImageFeature_MaxDistY() {
            this.description = "ImageFeature_MaxDistY";
        }

        @Override
        public double CalcFeatureVal(SubImageRegion imgRegion, int FG_val) {

            return imgRegion.height;
        }
    }

    public class ImageFeature_AvgDistanceCentroid extends ImageFeatureBase {

        public ImageFeature_AvgDistanceCentroid() {
            this.description = "ImageFeature_AvgDistanceCentroid";
        }

        @Override
        public double CalcFeatureVal(SubImageRegion imgRegion, int FG_val) {
            double[] centroidArr = GetCentroidFromSubImageRegion(imgRegion, FG_val);
            double avgDist = 0.0;
            int fgCount = 0;

            for (int x = 0; x < imgRegion.width; x++) {
                for (int y = 0; y < imgRegion.height; y++) {
                    if (imgRegion.subImgArr[x][y] == FG_val)
                        fgCount++;
                    double diffX = centroidArr[0] - x;
                    double diffY = centroidArr[1] - y;
                    double dist = Math.sqrt(diffX * diffX + diffY * diffY);
                    avgDist += dist;
                } //for y
            } //for x

            return avgDist /= fgCount;
        }
    }

    /*
    pos o, pos 1: centroid x, y
     */
    public static double[] GetCentroidFromSubImageRegion(SubImageRegion imgRegion, int FG_val) {
        int fgCount = 0;
        double xSum = 0.0;
        double ySum = 0.0;

        //add all FG pixels
        for (int x = 0; x < imgRegion.width; x++) {
            for (int y = 0; y < imgRegion.height; y++) {
                if (imgRegion.subImgArr[x][y] == FG_val)
                    fgCount++;
                xSum += x;
                ySum += y;
            } //for y
        } //for x

        //calc the averag
        xSum /= fgCount;
        ySum /= fgCount;

        return new double[]{xSum, ySum};
    } //GetCentroidFromSubImageRegion

    public class SubImageRegion {

        public int startX; //relative to the original image ==> pos of top left pixel
        public int startY;
        public int width;
        public int height;

        public int[][] subImgArr;

        public SubImageRegion(int startX, int startY, int width, int height, int[][] origImgArr) {
            this.startX = startX;
            this.startY = startY;
            this.height = height;
            this.width = width;
            this.subImgArr = new int[width][height];
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    this.subImgArr[x][y] = origImgArr[x + startX][y + startY];
                } //for y
            } //for x
        } //SubImageRegion

    }

} //class OCRanalysis_



